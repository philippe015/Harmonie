package fr.byphilou.harmonie;

import android.content.Intent;
import android.os.Bundle;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import org.json.JSONObject;
import org.json.JSONArray;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.PlaybackParameters;
import android.util.Log;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.annotation.Nullable;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;

/** Media3 owns playback outside the WebView lifecycle (screen lock, headset, task switch). */
public final class PlaybackService extends MediaSessionService {
    private ExoPlayer player;
    private MediaSession session;
    private NativeAudioEffects nativeEffects;
    private NativeAmbienceEffects nativeAmbience;
    private NativeBeatVisualizer beatVisualizer;
    private int errorsInARow = 0;
    private static final String CAR_PREFS="harmonie_bluetooth_car_v354";
    private static final String QUEUE_KEY="last_queue";
    private static final String CONTINUOUS_KEY="continuous_playback";
    private boolean continuousRestartPending=false;
    /** La lecture continue reste pilotée par Media3 même si la WebView est endormie. */
    private boolean continuousEnabled(){
        return getSharedPreferences(CAR_PREFS,MODE_PRIVATE).getBoolean(CONTINUOUS_KEY,true);
    }
    private void onNativePlaylistEnded(){
        if(player==null)return;
        int count=player.getMediaItemCount();
        int index=player.getCurrentMediaItemIndex();
        MediaItem current=player.getCurrentMediaItem();
        String id=current==null?"":current.mediaId;
        boolean wanted=player.getPlayWhenReady();
        boolean eligible=wanted && count>0 && id.startsWith("native:")
                && player.getRepeatMode()==Player.REPEAT_MODE_OFF && continuousEnabled();
        String report="fin file index="+index+"/"+count+" lectureDemandée="+wanted
                +" continu="+continuousEnabled()+" piste="+id;
        recordServiceEvent(report);
        getSharedPreferences(CAR_PREFS,MODE_PRIVATE).edit()
                .putString("last_end_diagnostic",report)
                .putLong("last_end_at",System.currentTimeMillis()).apply();
        if(!eligible || continuousRestartPending)return;
        // Le callback Media3 n'attend PAS la WebView pour relancer la file.
        // Poster sur le Looper du lecteur évite une modification de timeline dans le callback.
        continuousRestartPending=true;
        saveHandler.post(() -> {
            continuousRestartPending=false;
            if(player==null || player.getPlaybackState()!=Player.STATE_ENDED ||
                    !player.getPlayWhenReady() || !continuousEnabled() ||
                    player.getMediaItemCount()==0 || player.getRepeatMode()!=Player.REPEAT_MODE_OFF)return;
            MediaItem last=player.getCurrentMediaItem();
            if(last==null || !last.mediaId.startsWith("native:"))return;
            int next=player.hasNextMediaItem()?player.getNextMediaItemIndex():0;
            if(next==C.INDEX_UNSET)next=0;
            recordServiceEvent("lecture continue native : piste "+next+" / "+player.getMediaItemCount());
            player.seekTo(next,0L);
            player.prepare();
            player.play();
        });
    }

    private final Handler saveHandler=new Handler(Looper.getMainLooper());
    private final Runnable checkpoint=new Runnable() {
        @Override public void run() {
            savePlaybackProgress();
            saveHandler.postDelayed(this,15000);
        }
    };

    /** Restaure un service Media3 même si l'Activity/WebView n'existe plus. */
    private MediaSession.MediaItemsWithStartPosition restoreCarQueue(boolean forPlayback) {
        List<MediaItem> items=new ArrayList<>();
        int index=0;
        long position=0;
        try {
            String saved=getSharedPreferences(CAR_PREFS,MODE_PRIVATE).getString(QUEUE_KEY,"");
            if(saved.isEmpty())return new MediaSession.MediaItemsWithStartPosition(
                    Collections.emptyList(),C.INDEX_UNSET,C.TIME_UNSET);
            JSONObject data=new JSONObject(saved);
            JSONArray rows=data.optJSONArray("items");
            if(rows==null)return new MediaSession.MediaItemsWithStartPosition(
                    Collections.emptyList(),C.INDEX_UNSET,C.TIME_UNSET);
            for(int i=0;i<rows.length();i++){
                JSONObject row=rows.optJSONObject(i);
                if(row==null)continue;
                String src=row.optString("uri","");
                Uri uri=Uri.parse(src);
                String scheme=uri.getScheme();
                if(!("content".equals(scheme)||"http".equals(scheme)||"https".equals(scheme)))continue;
                MediaMetadata.Builder mb=new MediaMetadata.Builder()
                        .setTitle(row.optString("title","Musique")).setIsPlayable(true);
                if(!row.optString("artist","").isEmpty())mb.setArtist(row.optString("artist"));
                if(!row.optString("album","").isEmpty())mb.setAlbumTitle(row.optString("album"));
                MediaItem.Builder item=new MediaItem.Builder()
                        .setUri(uri).setMediaId(row.optString("id",src)).setMediaMetadata(mb.build());
                String mime=row.optString("mime","");
                if(!mime.isEmpty())item.setMimeType(mime);
                items.add(item.build());
            }
            if(items.isEmpty())return new MediaSession.MediaItemsWithStartPosition(
                    Collections.emptyList(),C.INDEX_UNSET,C.TIME_UNSET);
            index=Math.max(0,Math.min(items.size()-1,data.optInt("index",0)));
            position=Math.max(0,getSharedPreferences(CAR_PREFS,MODE_PRIVATE)
                    .getLong("last_position",data.optLong("position",0)));
            if(forPlayback){
                player.setRepeatMode(data.optInt("repeat",Player.REPEAT_MODE_OFF));
                player.setShuffleModeEnabled(data.optBoolean("shuffle",false));
                float speed=(float)data.optDouble("speed",1);
                float pitch=(float)data.optDouble("pitch",1);
                if(speed<=0||speed>2)speed=1;
                if(pitch<=0||pitch>2)pitch=1;
                player.setPlaybackParameters(new PlaybackParameters(speed,pitch));
            }
        } catch(Exception ex){Log.e("HarmoniePlayback","Reprise Bluetooth indisponible",ex);}
        // IMPORTANT: le service doit restituer la file COMPLÈTE même pour l'aperçu
        // de reprise (isForPlayback=false). Une file d'une seule piste empêchait
        // le passage automatique après l'éveil du service en arrière-plan.
        Log.i("HarmoniePlayback","Reprise complète : "+items.size()+" pistes index="+index
                +" lecture demandée="+forPlayback);
        return new MediaSession.MediaItemsWithStartPosition(items,index,position);
    }

    private void recordServiceEvent(String event) {
        getSharedPreferences(CAR_PREFS,MODE_PRIVATE).edit()
                .putString("last_service_event",event)
                .putLong("last_service_event_at",System.currentTimeMillis()).apply();
        Log.i("HarmoniePlayback", event);
    }

    private void savePlaybackProgress() {
        // Un service recréé peut être momentanément vide. Ne pas écraser la position sauvegardée.
        if(player==null || player.getMediaItemCount()==0)return;
        getSharedPreferences(CAR_PREFS,MODE_PRIVATE).edit()
                .putLong("last_position",Math.max(0,player.getCurrentPosition())).apply();
    }

    private void savePlaybackState() {
        if(player==null)return;
        try{
            int count=player.getMediaItemCount();
            // Ne PAS effacer la dernière file valide sur un état vide transitoire.
            // Seul stopNative/clearNative (action explicite) efface cette sauvegarde.
            if(count<1)return;
            JSONObject data=new JSONObject();
            data.put("version",1);
            data.put("index",Math.max(0,player.getCurrentMediaItemIndex()));
            data.put("position",Math.max(0,player.getCurrentPosition()));
            data.put("repeat",player.getRepeatMode());
            data.put("shuffle",player.getShuffleModeEnabled());
            data.put("speed",player.getPlaybackParameters().speed);
            data.put("pitch",player.getPlaybackParameters().pitch);
            JSONArray rows=new JSONArray();
            for(int i=0;i<count;i++){
                MediaItem item=player.getMediaItemAt(i);
                if(item.localConfiguration==null)continue;
                JSONObject row=new JSONObject();
                row.put("uri",item.localConfiguration.uri.toString());
                row.put("id",item.mediaId);
                row.put("title",item.mediaMetadata.title==null?"":item.mediaMetadata.title.toString());
                row.put("artist",item.mediaMetadata.artist==null?"":item.mediaMetadata.artist.toString());
                row.put("album",item.mediaMetadata.albumTitle==null?"":item.mediaMetadata.albumTitle.toString());
                if(item.localConfiguration.mimeType!=null)row.put("mime",item.localConfiguration.mimeType);
                rows.put(row);
            }
            data.put("items",rows);
            getSharedPreferences(CAR_PREFS,MODE_PRIVATE).edit()
                    .putString(QUEUE_KEY,data.toString())
                    .putLong("last_position",Math.max(0,player.getCurrentPosition())).apply();
        }catch(Exception ex){Log.w("HarmoniePlayback","Sauvegarde file Bluetooth impossible",ex);}
    }


    @Override public void onCreate() {
        super.onCreate();
        getSharedPreferences(CAR_PREFS,MODE_PRIVATE).edit()
                .putLong("service_created_at",System.currentTimeMillis()).apply();
        recordServiceEvent("service créé");
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build();
        nativeEffects = new NativeAudioEffects(this);
        beatVisualizer = new NativeBeatVisualizer(this);
        player = new ExoPlayer.Builder(this).build();
        player.setAudioAttributes(attributes, true); // Android audio focus and interruptions.
        player.setHandleAudioBecomingNoisy(true);
        nativeAmbience = new NativeAmbienceEffects(this,player);
        // Erreurs de sortie AudioTrack : Media3 sait parfois les réparer lui-même.
        // Ne pas redémarrer en boucle sur ce seul signal ; journaliser pour le diagnostic.
        player.addAnalyticsListener(new androidx.media3.exoplayer.analytics.AnalyticsListener() {
            @Override public void onAudioSinkError(
                    androidx.media3.exoplayer.analytics.AnalyticsListener.EventTime eventTime,
                    Exception audioSinkError) {
                Log.e("HarmoniePlayback", "Erreur AudioTrack / sortie audio", audioSinkError);
            }
        });     // Disconnecting headphones pauses music.
        // The native player must own the COMPLETE queue. No JS "ended" timer is
        // needed (or dependable) while the screen is locked.
        player.addListener(new Player.Listener() {
            @Override public void onAudioSessionIdChanged(int audioSessionId) {
                nativeEffects.onSessionChanged(audioSessionId);
                nativeAmbience.onSessionChanged(audioSessionId);
                beatVisualizer.onSessionChanged(audioSessionId);
            }
            @Override public void onEvents(Player p, Player.Events events){
                if (events.contains(Player.EVENT_IS_PLAYING_CHANGED)) beatVisualizer.onPlayingChanged(p.isPlaying());
                if(events.contains(Player.EVENT_TIMELINE_CHANGED)){
                    recordServiceEvent("timeline: " + p.getMediaItemCount() +
                            " titre(s), état=" + p.getPlaybackState() +
                            ", lectureDemandée=" + p.getPlayWhenReady());
                }
                // Reconstruire une grosse bibliothèque uniquement si la file ou ses options changent.
                if(events.contains(Player.EVENT_TIMELINE_CHANGED)
                        ||events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION)
                        ||events.contains(Player.EVENT_REPEAT_MODE_CHANGED)
                        ||events.contains(Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED)
                        ||events.contains(Player.EVENT_PLAYBACK_PARAMETERS_CHANGED))savePlaybackState();
                else if(events.contains(Player.EVENT_POSITION_DISCONTINUITY)
                        ||events.contains(Player.EVENT_PLAY_WHEN_READY_CHANGED))savePlaybackProgress();
            }
            @Override public void onMediaItemTransition(@Nullable MediaItem item, int reason) {
                nativeEffects.onMediaItemChanged(item!=null && item.mediaId.startsWith("radio:"));
                Log.i("HarmoniePlayback", "Transition automatique vers " +
                        (item != null ? item.mediaId : "aucun") +
                        " | raison=" + reason + " | file=" + player.getMediaItemCount());
            }
            @Override public void onPlayerError(PlaybackException error) {
                Log.e("HarmoniePlayback", "Erreur de décodage/accès; titre=" +
                        player.getCurrentMediaItemIndex() + " / " + player.getMediaItemCount(), error);
                // Un fichier inaccessible/corrompu ne doit pas condamner toute
                // la playlist. Interrompre la boucle si plusieurs titres échouent.
                errorsInARow++;
                if (player.getMediaItemCount() > 1 &&
                        errorsInARow <= player.getMediaItemCount() &&
                        player.hasNextMediaItem()) {
                    int from = player.getCurrentMediaItemIndex();
                    player.seekToNextMediaItem();
                    if (from != player.getCurrentMediaItemIndex()) {
                        player.prepare();
                        player.play();
                    }
                }
            }
            @Override public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_READY) errorsInARow = 0;
                if (playbackState == Player.STATE_ENDED) {
                    // La fin d'un morceau au milieu d'une file est gérée par ExoPlayer.
                    // La fin de la file peut boucler sur le premier titre si activé.
                    onNativePlaylistEnded();
                }
            }
        });
        session = new MediaSession.Builder(this, player)
            .setCallback(new MediaSession.Callback() {
                // Laisser Media3 interpréter automatiquement PLAY/PAUSE/NEXT/PREV de
                // l'autoradio, du volant et des casques Bluetooth (AVRCP).
                @Override public ListenableFuture<MediaSession.MediaItemsWithStartPosition> onPlaybackResumption(
                        MediaSession mediaSession,MediaSession.ControllerInfo controller,boolean isForPlayback) {
                    return Futures.immediateFuture(restoreCarQueue(isForPlayback));
                }
            }).build();
        nativeEffects.onSessionChanged(player.getAudioSessionId());
        nativeAmbience.onSessionChanged(player.getAudioSessionId());
        beatVisualizer.onSessionChanged(player.getAudioSessionId());
        beatVisualizer.onPlayingChanged(player.isPlaying());
        saveHandler.postDelayed(checkpoint,15000);
    }

    /** Mise à jour depuis l'interface, persistée avant l'envoi. Ne détruit pas la playlist. */
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        int result = super.onStartCommand(intent,flags,startId);
        if(intent!=null && NativeBeatVisualizer.ACTION_UPDATE.equals(intent.getAction())
                && beatVisualizer != null){
            boolean requested=getSharedPreferences(NativeBeatVisualizer.PREFS,MODE_PRIVATE).getBoolean("enabled",false);
            beatVisualizer.configure(requested,intent.getBooleanExtra("visible",false));
        }
        if(intent!=null && NativeAudioEffects.ACTION_UPDATE.equals(intent.getAction())
                && nativeEffects!=null){
            MediaItem item=player==null?null:player.getCurrentMediaItem();
            nativeEffects.onMediaItemChanged(item!=null && item.mediaId.startsWith("radio:"));
            if(nativeAmbience!=null)nativeAmbience.apply();
        }
        return result;
    }

    @Override public MediaSession onGetSession(MediaSession.ControllerInfo controllerInfo) {
        return session;
    }

    // Do not stop an ongoing session when the app window is dismissed.
    @Override public void onTaskRemoved(@Nullable Intent rootIntent) {
        recordServiceEvent("tâche Android retirée, lectureEnCours="+isPlaybackOngoing());
        if (!isPlaybackOngoing()) pauseAllPlayersAndStopSelf();
    }

    @Override public void onDestroy() {
        recordServiceEvent("service détruit, file="+(player==null?0:player.getMediaItemCount()));
        saveHandler.removeCallbacks(checkpoint);
        savePlaybackState();
        if (session != null) { session.release(); session = null; }
        if(nativeAmbience!=null){nativeAmbience.release();nativeAmbience=null;}
        if(nativeEffects!=null){nativeEffects.release();nativeEffects=null;}
        if(beatVisualizer!=null){beatVisualizer.release();beatVisualizer=null;}
        if (player != null) { player.release(); player = null; }
        super.onDestroy();
    }
}
