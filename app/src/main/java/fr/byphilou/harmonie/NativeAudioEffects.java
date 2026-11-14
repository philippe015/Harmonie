package fr.byphilou.harmonie;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.audiofx.AudioEffect;
import android.media.audiofx.DynamicsProcessing;
import android.media.audiofx.Equalizer;
import android.os.Build;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Egaliseur natif attaché à la session audio ExoPlayer, et NON à la WebView.
 * Doit être invoqué uniquement depuis le Looper principal du PlaybackService.
 * API 28+: huit bandes DynamicsProcessing. API 26/27 ou appareil non compatible:
 * Equalizer du système avec interpolation sur les fréquences de bandes disponibles.
 * L'effet est désactivé en cas d'erreur : la lecture radio reste prioritaire.
 */
final class NativeAudioEffects {
    static final String PREFS = "harmonie_native_fx_v3512";
    static final String ACTION_UPDATE = "fr.byphilou.harmonie.UPDATE_AUDIO_EFFECTS";
    static final float[] CUT_OFF_HZ = {60f, 150f, 400f, 1000f, 2400f, 6000f, 10000f, 20000f};
    private static final String TAG="HarmonieNativeFX";

    private final Context context;
    private DynamicsProcessing dynamic;
    private Equalizer fallback;
    private int sessionId=0;
    private boolean radio=false;

    NativeAudioEffects(Context context){ this.context=context.getApplicationContext(); }

    private SharedPreferences prefs(){ return context.getSharedPreferences(PREFS,Context.MODE_PRIVATE); }
    private void status(String message){
        prefs().edit().putString("status",message).putInt("audioSessionId",sessionId).apply();
        Log.i(TAG,message);
    }
    private void closeEffect(){
        if(dynamic!=null){try{dynamic.setEnabled(false);dynamic.release();}catch(Exception ignored){}dynamic=null;}
        if(fallback!=null){try{fallback.setEnabled(false);fallback.release();}catch(Exception ignored){}fallback=null;}
    }
    void release(){closeEffect();sessionId=0;}

    void onSessionChanged(int id){
        if(id==sessionId && (dynamic!=null || fallback!=null))return;
        closeEffect();
        sessionId=id;
        if(id<=0 || id==0){status("Aucune session audio Android active");return;}
        try{
            if(Build.VERSION.SDK_INT>=28){
                DynamicsProcessing.Config cfg=new DynamicsProcessing.Config.Builder(
                        DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                        2,true,8,false,0,false,0,false).build();
                dynamic=new DynamicsProcessing(0,id,cfg);
            }
        }catch(Exception | LinkageError error){
            Log.w(TAG,"Egaliseur huit bandes non pris en charge",error);
            if(dynamic!=null){try{dynamic.release();}catch(Exception ignored){}dynamic=null;}
        }
        if(dynamic==null){
            try{fallback=new Equalizer(0,id);}
            catch(Exception | LinkageError error){
                status("Egaliseur natif indisponible sur cette sortie");
                Log.w(TAG,"Effet natif non disponible",error);
            }
        }
        apply();
    }

    void onMediaItemChanged(boolean isRadio){
        radio=isRadio;
        apply();
    }

    /** Recharge un réglage sauvé par le bridge UI, sans détruire la session. */
    void apply(){
        if(sessionId<=0)return;
        boolean enabled=false;
        boolean hifi=false;
        float[] gains=new float[8];
        try{
            JSONObject cfg=new JSONObject(prefs().getString("settings","{}"));
            enabled=cfg.optBoolean("enabled",true) && (!radio || cfg.optBoolean("radioEnabled",false));
            hifi=cfg.optBoolean("hifi",true);
            JSONArray arr=cfg.optJSONArray("gains");
            if(arr!=null && arr.length()==8){
                for(int i=0;i<8;i++)gains[i]=clamp((float)arr.optDouble(i,0f),-12f,12f);
            }
            if(dynamic!=null){
                if(enabled){
                    for(int i=0;i<8;i++)dynamic.setPreEqBandAllChannelsTo(i,
                            new DynamicsProcessing.EqBand(true,CUT_OFF_HZ[i],gains[i]));
                    float peak=0f;
                    for(float db:gains)peak=Math.max(peak,db);
                    dynamic.setInputGainAllChannelsTo(hifi ? -Math.min(13f,peak+(peak>0?1f:0f)) : 0f);
                }else dynamic.setInputGainAllChannelsTo(0f);
                dynamic.setEnabled(enabled);
                status(enabled?"EQ natif 8 bandes : actif (DynamicsProcessing)":"EQ natif désactivé");
            }else if(fallback!=null){
                if(enabled){
                    short count=fallback.getNumberOfBands();
                    short[] range=fallback.getBandLevelRange();
                    for(short band=0;band<count;band++){
                        float freq=fallback.getCenterFreq(band)/1000f;
                        float target=interpolate(gains,Math.max(freq,1f));
                        short mB=(short)Math.max(range[0],Math.min(range[1],Math.round(target*100f)));
                        fallback.setBandLevel(band,mB);
                    }
                }
                fallback.setEnabled(enabled);
                status(enabled?"EQ système actif (bandes matérielles variables)":"EQ natif désactivé");
            }else status("Egaliseur natif indisponible : flux direct sans égalisation");
        }catch(Exception | LinkageError failure){
            Log.e(TAG,"Réglage EQ ignoré sans interrompre le son",failure);
            closeEffect();
            status("EQ incompatible avec cette sortie : audio direct conservé");
        }
    }

    private static float clamp(float n,float lo,float hi){return Math.max(lo,Math.min(hi,n));}
    /** Convertit les 8 curseurs en bandes disponibles selon le téléphone. */
    private static float interpolate(float[] values,float frequency){
        if(frequency<=CUT_OFF_HZ[0])return values[0];
        if(frequency>=CUT_OFF_HZ[7])return values[7];
        for(int i=1;i<8;i++){
            if(frequency<=CUT_OFF_HZ[i]){
                float left=(float)Math.log(CUT_OFF_HZ[i-1]);
                float right=(float)Math.log(CUT_OFF_HZ[i]);
                float fraction=(float)((Math.log(frequency)-left)/(right-left));
                return values[i-1]+(values[i]-values[i-1])*fraction;
            }
        }
        return values[7];
    }
}
