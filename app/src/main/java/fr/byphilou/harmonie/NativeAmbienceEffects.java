package fr.byphilou.harmonie;

import android.content.Context;
import android.media.audiofx.PresetReverb;
import android.media.audiofx.Virtualizer;
import android.util.Log;
import androidx.media3.common.AuxEffectInfo;
import androidx.media3.exoplayer.ExoPlayer;
import org.json.JSONObject;

/**
 * Effets facultatifs de la session ExoPlayer : la réverbération Android est un
 * effet AUX (pas un délai/écho répétitif Web Audio) ; la virtualisation dépend
 * du routeur/casque. Aucune erreur ici ne doit arrêter la lecture.
 * Doit être appelé uniquement depuis le Looper principal du PlaybackService.
 */
final class NativeAmbienceEffects {
    private static final String TAG="HarmonieAmbience";
    private final Context context;
    private final ExoPlayer player;
    private int sessionId=0;
    private PresetReverb reverb;
    private Virtualizer virtualizer;
    private String reverbStatus="inactif";
    private String virtualStatus="inactif";

    NativeAmbienceEffects(Context context,ExoPlayer player){
        this.context=context.getApplicationContext();this.player=player;
    }
    private void status(){
        String value="Réverbération : "+reverbStatus+" · Espace stéréo : "+virtualStatus;
        context.getSharedPreferences(NativeAudioEffects.PREFS,Context.MODE_PRIVATE)
            .edit().putString("ambienceStatus",value).apply();
        Log.i(TAG,value);
    }
    private void detachReverb(){
        try{player.setAuxEffectInfo(new AuxEffectInfo(AuxEffectInfo.NO_AUX_EFFECT_ID,0f));}
        catch(Exception | LinkageError e){Log.w(TAG,"Détachement réverbération",e);}
    }
    private void closeVirtualizer(){
        if(virtualizer!=null){try{virtualizer.setEnabled(false);virtualizer.release();}
            catch(Exception | LinkageError e){Log.w(TAG,"Libération virtualizer",e);}virtualizer=null;}
    }
    void onSessionChanged(int id){
        if(id==sessionId)return;
        closeVirtualizer();sessionId=id;
        apply();
    }
    void apply(){
        JSONObject cfg;
        try{cfg=new JSONObject(context.getSharedPreferences(NativeAudioEffects.PREFS,Context.MODE_PRIVATE)
            .getString("settings","{}"));}
        catch(Exception e){reverbStatus="réglages invalides";virtualStatus="réglages invalides";status();return;}
        boolean echo=cfg.optBoolean("ambienceOn",false);
        int echoAmount=Math.max(0,Math.min(70,cfg.optInt("ambienceAmount",0)));
        boolean stereo=cfg.optBoolean("virtualizerOn",false);
        int stereoAmount=Math.max(0,Math.min(100,cfg.optInt("virtualizerAmount",0)));
        // Effet AUX de réverbération : créé uniquement quand demandé, sans
        // altérer la sortie principale en cas d'indisponibilité.
        if(echo && echoAmount>0){
            try{
                if(reverb==null)reverb=new PresetReverb(0,0);
                short preset=echoAmount<20?PresetReverb.PRESET_SMALLROOM:
                    echoAmount<45?PresetReverb.PRESET_MEDIUMROOM:PresetReverb.PRESET_LARGEROOM;
                reverb.setPreset(preset);
                reverb.setEnabled(true);
                player.setAuxEffectInfo(new AuxEffectInfo(reverb.getId(),
                    Math.min(0.75f,echoAmount/100f)));
                reverbStatus="demandée (réverbération Android, pas écho à répétitions)";
            }catch(Exception | LinkageError e){
                detachReverb();
                if(reverb!=null){try{reverb.release();}catch(Exception ignored){}reverb=null;}
                reverbStatus="indisponible sur cette sortie";Log.w(TAG,"Réverbération indisponible",e);
            }
        }else{
            detachReverb();
            if(reverb!=null){try{reverb.release();}catch(Exception ignored){}reverb=null;}
            reverbStatus="désactivée";
        }
        if(stereo && stereoAmount>0 && sessionId>0){
            try{
                if(virtualizer==null)virtualizer=new Virtualizer(0,sessionId);
                virtualizer.setStrength((short)Math.min(1000,stereoAmount*10));
                int result=virtualizer.setEnabled(true);
                virtualStatus=result==0 && virtualizer.getEnabled()
                    ?"activée (virtualisation Android, sortie dépendante)"
                    :"non activée par Android";
            }catch(Exception | LinkageError e){
                closeVirtualizer();virtualStatus="indisponible sur cette sortie";
                Log.w(TAG,"Virtualisation indisponible",e);
            }
        }else{
            if(virtualizer!=null)try{virtualizer.setEnabled(false);}catch(Exception ignored){}
            virtualStatus=stereo && sessionId<=0?"en attente de session":"désactivé";
        }
        status();
    }
    void release(){
        detachReverb();
        if(reverb!=null){try{reverb.release();}catch(Exception ignored){}reverb=null;}
        closeVirtualizer();sessionId=0;
        reverbStatus="arrêtée";virtualStatus="arrêté";status();
    }
}
