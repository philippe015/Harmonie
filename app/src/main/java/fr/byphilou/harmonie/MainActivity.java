package fr.byphilou.harmonie;

import android.app.Activity;
import android.Manifest;
import android.content.pm.PackageManager;
import android.content.ComponentName;
import android.os.Handler;
import android.os.Looper;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import com.google.common.util.concurrent.ListenableFuture;

import android.content.ClipData;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import android.webkit.ValueCallback;
import android.widget.Toast;

import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.HashSet;
import java.util.Set;

/** WebView native affichant exactement l'interface fournie par l'utilisateur. */
public class MainActivity extends Activity {
    private static final String TAG = "HarmonieNative";
    private static final int CHOOSE_FILES = 2101;
    private static final int CHOOSE_FOLDER = 2102;
    private static final int CHOOSE_NATIVE_FILES = 2103;
    private static final int PERMISSION_NATIVE_LED = 3107;
    private static final String PREFS = "native_library_v35";
    private final java.util.concurrent.atomic.AtomicBoolean coversReindexing = new java.util.concurrent.atomic.AtomicBoolean(false);
    private final Object libraryLock = new Object();
    private MediaController controller;
    private ListenableFuture<MediaController> controllerFuture;
    // Commandes HTML émises avant la liaison Media3: ne jamais les perdre silencieusement.
    private final List<Consumer<MediaController>> pendingMedia3Actions = new ArrayList<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private volatile String nativeSnapshot = "{\"ready\":false,\"playing\":false}";
    // WebView @JavascriptInterface est exécuté sur un thread de pont JavaScript, PAS sur le main looper.
    // Les appels MediaController (même ses getters) doivent rester sur le looper d'application.
    private volatile String audioDiagnosticSnapshot = "{\"ready\":false,\"message\":\"Diagnostic Android en préparation\"}";
    private long lastAudioDiagnosticUptimeMs = 0;
    private final Runnable snapshotTask = new Runnable() {
        @Override public void run() {
            refreshNativeSnapshot();
            if (android.os.SystemClock.uptimeMillis() - lastAudioDiagnosticUptimeMs >= 1500) {
                refreshAudioDiagnosticSnapshot();
            }
            mainHandler.postDelayed(this, 750);
        }
    };
    private static final int MAX_FOLDER_FILES = 1500;
    private static final String ORIGIN = "http://127.0.0.1:8767";

    private WebView webView;
    private ValueCallback<Uri[]> pendingFiles;
    private final Map<String, Uri> androidUris = new ConcurrentHashMap<>();

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        connectPlaybackService();
        mainHandler.post(snapshotTask);
        try {
            File webDir = new File(getFilesDir(), "harmonie_web");
            if (!webDir.exists() && !webDir.mkdirs()) throw new IOException("Impossible de créer le dossier interne");
            // Copie à chaque démarrage : les corrections HTML remplacent l'ancienne interface.
            try (InputStream in = getAssets().open("harmonie.html");
                 FileOutputStream out = new FileOutputStream(new File(webDir, "harmonie.html"))) {
                byte[] buffer = new byte[32768]; int n;
                while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
            }
            if (!Python.isStarted()) Python.start(new AndroidPlatform(this));
            String url = Python.getInstance().getModule("harmonie_server")
                .callAttr("start", webDir.getAbsolutePath(), getApplicationInfo().nativeLibraryDir).toString();
            initializeWebView();
            webView.loadUrl(url);
        } catch (Exception e) {
            Log.e(TAG, "Échec du démarrage", e);
            Toast.makeText(this, "Harmonie : " + e.getMessage(), Toast.LENGTH_LONG).show();
            android.widget.TextView error = new android.widget.TextView(this);
            error.setPadding(30, 30, 30, 30);
            error.setText("Échec de démarrage Harmonie\n\n" + e + "\n\nConsultez Logcat : " + TAG);
            setContentView(error);
        }
    }

    private void updateNativeLedVisibility(boolean visible) {
        if (getSharedPreferences(NativeBeatVisualizer.PREFS,MODE_PRIVATE).getBoolean("enabled",false)
                && checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED) return;
        if (controllerFuture == null) return;
        Intent intent = new Intent(this, PlaybackService.class);
        intent.setAction(NativeBeatVisualizer.ACTION_UPDATE);
        intent.putExtra("visible",visible);
        try { startService(intent); } catch(Exception e) { Log.w(TAG,"LED Media3 en attente",e); }
    }
    private void sendLedPermissionResult(boolean granted) {
        if(webView==null) return;
        webView.evaluateJavascript("window.HARMONIE_LED_PERMISSION && window.HARMONIE_LED_PERMISSION("+granted+")", null);
    }
    @Override protected void onResume() {
        super.onResume();
        if(getSharedPreferences(NativeBeatVisualizer.PREFS,MODE_PRIVATE).getBoolean("enabled",false))
            updateNativeLedVisibility(true);
    }
    @Override protected void onPause() {
        updateNativeLedVisibility(false); // No analysis when UI isn't visible; music continues via Media3.
        super.onPause();
    }
    @Override public void onRequestPermissionsResult(int code,String[] permissions,int[] results) {
        super.onRequestPermissionsResult(code,permissions,results);
        if(code!=PERMISSION_NATIVE_LED)return;
        boolean granted=results.length>0 && results[0]==PackageManager.PERMISSION_GRANTED;
        getSharedPreferences(NativeBeatVisualizer.PREFS,MODE_PRIVATE).edit()
                .putBoolean("enabled",granted).apply();
        if(granted)updateNativeLedVisibility(true);
        sendLedPermissionResult(granted);
        Toast.makeText(this,granted?"LED synchronisées au son activées":"LED : permission refusée, animations décoratives disponibles",Toast.LENGTH_LONG).show();
    }

    private void connectPlaybackService() {
        SessionToken token = new SessionToken(this, new ComponentName(this, PlaybackService.class));
        controllerFuture = new MediaController.Builder(this, token)
                .setApplicationLooper(Looper.getMainLooper()).buildAsync();
        controllerFuture.addListener(() -> {
            try {
                controller = controllerFuture.get();
                refreshNativeSnapshot(); refreshAudioDiagnosticSnapshot();
                // Les appels @JavascriptInterface peuvent précéder la liaison MediaController.
                // Réexécuter les commandes reçues dans leur ordre d'arrivée, sur le main looper.
                List<Consumer<MediaController>> queued = new ArrayList<>(pendingMedia3Actions);
                pendingMedia3Actions.clear();
                for (Consumer<MediaController> action : queued) runMedia3Action(action);
            } catch (Exception e) {
                pendingMedia3Actions.clear();
                Log.e(TAG, "Connexion Media3 impossible", e);
                Toast.makeText(this, "Lecteur Android inaccessible : " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }, command -> { mainHandler.post(command); });
    }

    private void noteAudioCommand(String operation) {
        getSharedPreferences("harmonie_bluetooth_car_v354",MODE_PRIVATE).edit()
                .putString("last_command",operation).apply();
        Log.i(TAG,"Commande Media3 : "+operation);
    }

    private void runMedia3Action(Consumer<MediaController> action) {
        if (controller == null) return;
        try {
            action.accept(controller);
            refreshNativeSnapshot(); refreshAudioDiagnosticSnapshot();
        } catch (Exception e) {
            Log.e(TAG, "Commande Media3 refusée", e);
            Toast.makeText(this, "Lecture : " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    private void withController(Consumer<MediaController> action) {
        runOnUiThread(() -> {
            if (controller == null) {
                // Ex. clic Lecture juste après l'ouverture : auparavant la commande était jetée.
                if (controllerFuture != null && !controllerFuture.isDone() && pendingMedia3Actions.size() < 32) {
                    pendingMedia3Actions.add(action);
                    Log.i(TAG, "Commande Media3 conservée en attente de connexion (" + pendingMedia3Actions.size() + ")");
                } else {
                    Log.w(TAG, "Lecteur Android non connecté : commande refusée");
                    Toast.makeText(this, "Lecteur Android indisponible : réessayez", Toast.LENGTH_SHORT).show();
                }
                return;
            }
            runMedia3Action(action);
        });
    }

    private void refreshNativeSnapshot() {
        if (controller == null) {
            nativeSnapshot = "{\"ready\":false,\"playing\":false,\"queueSize\":0,\"state\":1}";
            return;
        }
        try {
            JSONObject st = new JSONObject();
            st.put("ready", true);
            st.put("playing", controller.isPlaying());
            st.put("position", Math.max(0, controller.getCurrentPosition()) / 1000.0);
            long duration = controller.getDuration();
            st.put("duration", duration > 0 ? duration / 1000.0 : 0);
            MediaItem current = controller.getCurrentMediaItem();
            st.put("id", current != null ? current.mediaId : "");
            st.put("title", current != null && current.mediaMetadata.title != null ? current.mediaMetadata.title.toString() : "");
            st.put("artist", current != null && current.mediaMetadata.artist != null ? current.mediaMetadata.artist.toString() : "");
            st.put("carControls", true);
            st.put("state", controller.getPlaybackState());
            st.put("queueSize", controller.getMediaItemCount());
            st.put("queueIndex", controller.getCurrentMediaItemIndex());
            st.put("hasNext", controller.hasNextMediaItem());
            st.put("repeatMode", controller.getRepeatMode());
            st.put("shuffle", controller.getShuffleModeEnabled());
            st.put("playWhenReady", controller.getPlayWhenReady());
            st.put("speed", controller.getPlaybackParameters().speed);
            st.put("error", controller.getPlayerError() == null ? "" : String.valueOf(controller.getPlayerError().getMessage()));
            nativeSnapshot = st.toString();
        } catch (Exception ex) {
            Log.w(TAG, "Etat Media3 indisponible", ex);
            nativeSnapshot = "{\"ready\":false,\"playing\":false,\"queueSize\":0,\"state\":1}";
        }
    }

    /** Capture exclusivement sur le main looper de MediaController. Aucun getter Media3 depuis JS. */
    private void refreshAudioDiagnosticSnapshot() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            Log.e(TAG, "Diagnostic Media3 hors thread principal empêché");
            return;
        }
        audioDiagnosticSnapshot = collectAudioDiagnosticsOnMainThread();
        lastAudioDiagnosticUptimeMs = android.os.SystemClock.uptimeMillis();
    }

    private String collectAudioDiagnosticsOnMainThread() {
        JSONObject d=new JSONObject();
        try {
            d.put("ready", controller != null);
            d.put("sampleTimeMs", System.currentTimeMillis());
            AudioManager audio=(AudioManager)getSystemService(AUDIO_SERVICE);
            if(audio!=null){
                d.put("musicVolume",audio.getStreamVolume(AudioManager.STREAM_MUSIC));
                d.put("musicVolumeMax",audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
                d.put("musicMuted",audio.isStreamMute(AudioManager.STREAM_MUSIC));
                android.media.AudioDeviceInfo[] outputs=audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
                JSONArray devices=new JSONArray();
                for(android.media.AudioDeviceInfo dev:outputs){
                    // Liste des sorties offertes par Android ; pas forcément le routage actif.
                    String label=dev.getProductName()==null?"sortie "+dev.getType():dev.getProductName().toString();
                    devices.put(label);
                }
                d.put("devices",devices.join(", "));
            }
            android.content.SharedPreferences car=getSharedPreferences("harmonie_bluetooth_car_v354",MODE_PRIVATE);
            String saved=car.getString("last_queue","");
            int savedCount=0;
            if(!saved.isEmpty()){
                try {JSONArray rows=new JSONObject(saved).optJSONArray("items");
                    if(rows!=null)savedCount=rows.length();}catch(Exception ignored){}
            }
            d.put("savedQueueCount",savedCount);
            android.content.SharedPreferences fx=getSharedPreferences(NativeAudioEffects.PREFS,MODE_PRIVATE);
            d.put("nativeEqStatus",fx.getString("status","EQ en attente de session Android"));
            d.put("nativeEqSession",fx.getInt("audioSessionId",0));
            d.put("nativeAmbienceStatus",fx.getString("ambienceStatus","Ambiance Android en attente de session"));
            d.put("continuousPlayback",car.getBoolean("continuous_playback",true));
            d.put("lastEndDiagnostic",car.getString("last_end_diagnostic","aucune fin enregistrée"));
            d.put("lastEndAt",car.getLong("last_end_at",0));
            d.put("lastCommand",car.getString("last_command","aucune"));
            d.put("lastServiceEvent",car.getString("last_service_event","aucun"));
            d.put("lastServiceEventAt",car.getLong("last_service_event_at",0));
            if(controller!=null){
                d.put("playing",controller.isPlaying());
                d.put("playWhenReady",controller.getPlayWhenReady());
                d.put("playbackState",controller.getPlaybackState());
                d.put("suppressionReason",controller.getPlaybackSuppressionReason());
                d.put("playerVolume",controller.getVolume());
                d.put("playbackSpeed",controller.getPlaybackParameters().speed);
                d.put("playbackPitch",controller.getPlaybackParameters().pitch);
                d.put("mediaId",controller.getCurrentMediaItem()==null?"":controller.getCurrentMediaItem().mediaId);
                d.put("positionMs",controller.getCurrentPosition());
                d.put("mediaItemCount",controller.getMediaItemCount());
                d.put("playerError", controller.getPlayerError() == null ? "" : String.valueOf(controller.getPlayerError().getMessage()));
            }
        }catch(Exception ex){Log.e(TAG,"Diagnostic audio impossible",ex);try{d.put("error",ex.toString());}catch(Exception ignored){}}
        return d.toString();
    }

    /** Reprise manuelle d'une session disparue, depuis la dernière file valide.
     *  Le contrôleur et tous ses getters/setters sont utilisés sur le main looper.
     *  AUCUNE restauration automatique après pause ou interruption de priorité audio.
     */
    private boolean restoreSavedQueueOnMain(MediaController c, float volume) {
        try {
            android.content.SharedPreferences prefs=getSharedPreferences("harmonie_bluetooth_car_v354",MODE_PRIVATE);
            JSONObject data=new JSONObject(prefs.getString("last_queue",""));
            JSONArray rows=data.optJSONArray("items");
            if(rows==null || rows.length()==0)return false;
            List<MediaItem> items=new ArrayList<>();
            for(int i=0;i<rows.length();i++){
                JSONObject row=rows.optJSONObject(i);
                if(row==null)continue;
                String uriText=row.optString("uri","");
                Uri uri=Uri.parse(uriText);
                String scheme=uri.getScheme();
                if(!("content".equals(scheme)||"http".equals(scheme)||"https".equals(scheme)))continue;
                MediaMetadata.Builder meta=new MediaMetadata.Builder()
                        .setTitle(row.optString("title","Musique")).setIsPlayable(true);
                if(!row.optString("artist","").isEmpty())meta.setArtist(row.optString("artist"));
                if(!row.optString("album","").isEmpty())meta.setAlbumTitle(row.optString("album"));
                MediaItem.Builder item=new MediaItem.Builder().setUri(uri)
                        .setMediaId(row.optString("id",uriText)).setMediaMetadata(meta.build());
                if(!row.optString("mime","").isEmpty())item.setMimeType(row.optString("mime"));
                items.add(item.build());
            }
            if(items.isEmpty())return false;
            int index=Math.max(0,Math.min(items.size()-1,data.optInt("index",0)));
            long position=Math.max(0,prefs.getLong("last_position",data.optLong("position",0)));
            c.setMediaItems(items,index,position);
            c.setRepeatMode(data.optInt("repeat",Player.REPEAT_MODE_OFF));
            c.setShuffleModeEnabled(data.optBoolean("shuffle",false));
            float speed=(float)data.optDouble("speed",1.0);
            float pitch=(float)data.optDouble("pitch",1.0);
            if(speed<=0 || speed>2)speed=1f;
            if(pitch<=0 || pitch>2)pitch=1f;
            c.setPlaybackParameters(new PlaybackParameters(speed,pitch)); // 432 Hz conservé
            c.setVolume(Math.max(0f,Math.min(1f,volume)));
            c.prepare();c.play();
            Log.w(TAG,"Restauration MANUELLE file Media3 : "+items.size()+" titres, item="+index+", ms="+position);
            return true;
        }catch(Exception ex){Log.w(TAG,"Aucune file Media3 restaurable",ex);return false;}
    }

    private JSONArray loadNativeLibrary() {
        synchronized (libraryLock) {
            try {return new JSONArray(getSharedPreferences(PREFS, MODE_PRIVATE).getString("tracks", "[]"));}
            catch (Exception e) { return new JSONArray(); }
        }
    }
    private void saveNativeLibrary(JSONArray library) {
        synchronized (libraryLock) {
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString("tracks", library.toString()).apply();
        }
    }
    private void deliverNativeLibrary() {
        final String payload = loadNativeLibrary().toString();
        runOnUiThread(() -> {
            if (webView != null) webView.evaluateJavascript(
                "window.HARMONIE_NATIVE_LIBRARY && window.HARMONIE_NATIVE_LIBRARY(" + JSONObject.quote(payload) + ")", null);
        });
    }
    private boolean audioName(String name) {
        return name != null && name.toLowerCase(java.util.Locale.ROOT).matches(".*\\.(flac|mp3|wav|m4a|aac|ogg|oga|opus)$");
    }
    private JSONObject nativeTrack(Uri uri, String name, String path) throws Exception {
        JSONObject obj = new JSONObject();
        obj.put("id", "native:" + java.util.UUID.nameUUIDFromBytes(uri.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        obj.put("name", name); obj.put("path", path);
        obj.put("folder", path.contains("/") ? path.substring(0, path.lastIndexOf('/')) : "");
        obj.put("size", -1); obj.put("uri", uri.toString()); obj.put("native", true);
        // Métadonnées lisibles par l'autoradio via MediaSession/AVRCP.
        // Pour les dossiers volumineux, éviter de décoder tous les fichiers à l'import.
        String stem=name.replaceFirst("(?i)\\.(flac|mp3|wav|m4a|aac|ogg|oga|opus)$", "").trim();
        obj.put("title", stem.isEmpty()?name:stem);
        obj.put("artist", ""); obj.put("album", "");
        return obj;
    }
    private void readCarTags(JSONObject track, Uri uri) {
        // Seulement sur le fil d'import de fichiers : aucun blocage de l'interface.
        MediaMetadataRetriever retriever=new MediaMetadataRetriever();
        try {
            retriever.setDataSource(this, uri);
            String title=retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            String artist=retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            String album=retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
            if(title!=null&&!title.trim().isEmpty())track.put("title",title.trim());
            if(artist!=null&&!artist.trim().isEmpty())track.put("artist",artist.trim());
            if(album!=null&&!album.trim().isEmpty())track.put("album",album.trim());
            track.put("hasCover", extractEmbeddedCover(retriever, track.optString("id")));
            track.put("coverRevision", System.currentTimeMillis());
        } catch(Exception ex) {
            Log.d(TAG,"Tags absents ou illisibles : "+uri,ex);
        } finally { try{retriever.release();}catch(Exception ignored){} }
    }

    /** Cache persistant des pochettes embarquées : aucune image volumineuse en SharedPreferences. */
    private File coverFile(String id) {
        if (id == null || !id.matches("native:[0-9a-fA-F-]{36}")) return null;
        File dir = new File(getFilesDir(), "harmonie_covers");
        if (!dir.isDirectory() && !dir.mkdirs()) return null;
        return new File(dir, id.substring(7) + ".jpg");
    }

    /** Extrait une miniature JPG de l'image intégrée (FLAC/MP3/M4A) sans conserver le bitmap original. */
    private boolean extractEmbeddedCover(MediaMetadataRetriever retriever, String id) {
        File target = coverFile(id);
        if (target == null) return false;
        File temporary = new File(target.getParentFile(), target.getName() + ".tmp");
        try {
            byte[] bytes = retriever.getEmbeddedPicture();
            if (bytes == null || bytes.length == 0) {
                if (target.exists()) target.delete();
                return false;
            }
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(bytes, 0, bytes.length, bounds);
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return target.isFile();
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 1;
            while (Math.max(bounds.outWidth/options.inSampleSize, bounds.outHeight/options.inSampleSize) > 560)
                options.inSampleSize *= 2;
            Bitmap decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
            if (decoded == null) return target.isFile();
            int largest = Math.max(decoded.getWidth(), decoded.getHeight());
            float factor = Math.min(1f, 240f / largest);
            int width = Math.max(1, Math.round(decoded.getWidth() * factor));
            int height = Math.max(1, Math.round(decoded.getHeight() * factor));
            Bitmap scaled = Bitmap.createScaledBitmap(decoded, width, height, true);
            Bitmap opaque = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            Canvas canvas = new Canvas(opaque);
            canvas.drawColor(Color.BLACK);
            canvas.drawBitmap(scaled, 0, 0, null);
            try (FileOutputStream out = new FileOutputStream(temporary)) {
                if (!opaque.compress(Bitmap.CompressFormat.JPEG, 82, out)) throw new IOException("Compression JPEG impossible");
            } finally {
                opaque.recycle();
                if (scaled != decoded) scaled.recycle();
                decoded.recycle();
            }
            if (target.exists() && !target.delete()) Log.w(TAG, "Ancienne pochette non effacée : " + id);
            if (!temporary.renameTo(target)) throw new IOException("Cache pochette non enregistré");
            return true;
        } catch (Exception ex) {
            Log.w(TAG, "Pochette embarquée illisible : " + id, ex);
            return target.isFile();
        } finally {
            if (temporary.exists()) temporary.delete();
        }
    }

    /** Chargement à la demande des miniatures par WebView sans base64 dans l'index de la bibliothèque. */
    private WebResourceResponse nativeCoverResponse(String id) {
        File cached = coverFile(id);
        if (cached != null && cached.isFile()) {
            try {
                WebResourceResponse response = new WebResourceResponse("image/jpeg", null, new FileInputStream(cached));
                response.setStatusCodeAndReasonPhrase(200, "OK");
                response.setResponseHeaders(java.util.Collections.singletonMap("Cache-Control", "no-cache"));
                return response;
            } catch (IOException ex) { Log.w(TAG, "Pochette non lisible " + id, ex); }
        }
        return new WebResourceResponse("image/jpeg", null, 404, "Not Found", java.util.Collections.emptyMap(),
                new ByteArrayInputStream(new byte[0]));
    }

    /** Réanalyse les fichiers déjà indexés sans modifier ni supprimer une playlist. */
    private void reindexCoversAsync() {
        if (!coversReindexing.compareAndSet(false,true)) return;
        new Thread(() -> {
            int updated = 0, without = 0;
            try {
                JSONArray tracks = loadNativeLibrary();
                for (int i = 0; i < tracks.length(); i++) {
                    JSONObject t = tracks.optJSONObject(i);
                    if (t == null) continue;
                    String id = t.optString("id", "");
                    String uri = t.optString("uri", "");
                    if (!id.startsWith("native:") || uri.isEmpty()) continue;
                    readCarTags(t, Uri.parse(uri));
                    if (t.optBoolean("hasCover", false)) updated++; else without++;
                }
                // Réappliquer les corrections sur la version la plus récente de l'index.
                // Une importation simultanée ne doit jamais être écrasée par la réanalyse.
                synchronized (libraryLock) {
                    JSONArray latest = loadNativeLibrary();
                    java.util.Map<String, JSONObject> refreshed = new java.util.HashMap<>();
                    for (int i=0;i<tracks.length();i++) {
                        JSONObject t=tracks.optJSONObject(i);
                        if(t!=null)refreshed.put(t.optString("id"),t);
                    }
                    JSONArray merged = new JSONArray();
                    for (int i=0;i<latest.length();i++) {
                        JSONObject current=latest.optJSONObject(i);
                        if(current==null)continue;
                        JSONObject refreshedTrack=refreshed.get(current.optString("id"));
                        merged.put(refreshedTrack==null?current:refreshedTrack);
                    }
                    saveNativeLibrary(merged);
                }
                deliverNativeLibrary();
                final int a=updated,b=without;
                runOnUiThread(() -> Toast.makeText(this, a + " pochette(s) intégrée(s) retrouvée(s), " + b + " sans image.", Toast.LENGTH_LONG).show());
            } catch (Exception ex) { Log.e(TAG, "Réanalyse des pochettes impossible", ex); }
            finally { coversReindexing.set(false); }
        }, "harmonie-cover-reindex").start();
    }

    private MediaMetadata carMetadata(JSONObject track) {
        String title=track.optString("title","").trim();
        if(title.isEmpty())title=track.optString("name","Musique").replaceFirst("(?i)\\.(flac|mp3|wav|m4a|aac|ogg|oga|opus)$", "");
        MediaMetadata.Builder b=new MediaMetadata.Builder()
                .setTitle(title).setIsPlayable(true);
        String artist=track.optString("artist","").trim();
        String album=track.optString("album","").trim();
        if(!artist.isEmpty())b.setArtist(artist);
        if(!album.isEmpty())b.setAlbumTitle(album);
        File cover = coverFile(track.optString("id", ""));
        if (cover != null && cover.isFile()) b.setArtworkUri(Uri.fromFile(cover));
        return b.build();
    }

    private int mergeNativeLibrary(JSONArray entries) {
        synchronized (libraryLock) {
            JSONArray old = loadNativeLibrary();
            Set<String> ids = new HashSet<>();
            for (int i=0;i<old.length();i++) { JSONObject o=old.optJSONObject(i);if(o!=null)ids.add(o.optString("id")); }
            int added = 0;
            for (int i=0;i<entries.length();i++) {
                JSONObject row=entries.optJSONObject(i);
                if (row == null || ids.contains(row.optString("id"))) continue;
                old.put(row); ids.add(row.optString("id"));added++;
            }
            saveNativeLibrary(old); return added;
        }
    }
    private void importNativeFiles(Intent data) {
        new Thread(() -> {
            JSONArray list = new JSONArray();
            try {
                List<Uri> uris = new ArrayList<>();
                ClipData clip=data.getClipData();
                if(clip!=null)for(int i=0;i<clip.getItemCount();i++)uris.add(clip.getItemAt(i).getUri());
                else if(data.getData()!=null)uris.add(data.getData());
                for(Uri uri:uris) {
                    String name = null;
                    try(Cursor cursor=getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){
                        if(cursor!=null && cursor.moveToFirst())name=cursor.getString(0);
                    }
                    if(name==null)name=uri.getLastPathSegment();
                    if(!audioName(name))continue;
                    try{getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);}
                    catch(Exception e){Log.w(TAG,"Permission persistante non accordée: "+name,e);continue;}
                    JSONObject track=nativeTrack(uri,name,name);
                    readCarTags(track,uri);
                    list.put(track);
                }
                int count=mergeNativeLibrary(list);
                deliverNativeLibrary();
                runOnUiThread(()->Toast.makeText(this,count+" fichier(s) ajoutés à la bibliothèque native",Toast.LENGTH_LONG).show());
            }catch(Exception e){Log.e(TAG,"Importation native échouée",e);runOnUiThread(()->Toast.makeText(this,"Importation : "+e.getMessage(),Toast.LENGTH_LONG).show());}
        },"harmonie-native-import").start();
    }

    private void initializeWebView() {
        webView = new WebView(this);
        webView.setBackgroundColor(0xff0c1018);
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(false);       // Fichiers accessibles seulement via SAF.
        s.setAllowContentAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String scheme = uri.getScheme();
                if (!request.isForMainFrame()) return false;
                if (("http".equals(scheme) || "https".equals(scheme)) &&
                        ORIGIN.equals(scheme + "://" + uri.getAuthority())) return false;
                // Le bridge natif reste réservé à notre interface locale.
                try {
                    if ("https".equals(scheme) || "http".equals(scheme))
                        startActivity(new Intent(Intent.ACTION_VIEW, uri));
                } catch (Exception e) { Log.w(TAG, "Lien externe impossible", e); }
                return true;
            }
            @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String path = request.getUrl().getPath();
                if (!ORIGIN.equals(request.getUrl().getScheme() + "://" + request.getUrl().getAuthority())) return null;
                if (path == null) return null;
                if (path.startsWith("/__native_cover__/"))
                    return nativeCoverResponse(path.substring("/__native_cover__/".length()));
                if (!path.startsWith("/__native_file__/")) return null;
                String token = path.substring("/__native_file__/".length());
                Uri uri = androidUris.get(token);
                if (uri == null) return new WebResourceResponse("text/plain", "UTF-8", new ByteArrayInputStream(new byte[0]));
                try {
                    InputStream in = getContentResolver().openInputStream(uri);
                    if (in == null) throw new IOException("Flux vide");
                    String mime = getContentResolver().getType(uri);
                    if (mime == null) mime = "application/octet-stream";
                    WebResourceResponse response = new WebResourceResponse(mime, null, in);
                    response.setResponseHeaders(java.util.Collections.singletonMap("Cache-Control", "no-store"));
                    return response;
                } catch (Exception e) {
                    Log.e(TAG, "Document SAF non lisible", e);
                    return new WebResourceResponse("text/plain", "UTF-8", new ByteArrayInputStream(new byte[0]));
                }
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback,
                                                        FileChooserParams params) {
                if (pendingFiles != null) pendingFiles.onReceiveValue(null);
                pendingFiles = callback;
                try {
                    Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    // */* inclut les FLAC dont le fournisseur Android indique un MIME inconnu.
                    i.setType("*/*");
                    i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, params.getMode() == FileChooserParams.MODE_OPEN_MULTIPLE);
                    i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivityForResult(i, CHOOSE_FILES);
                } catch (Exception e) {
                    Log.e(TAG, "Sélecteur Android non disponible", e);
                    callback.onReceiveValue(null);pendingFiles = null;
                    Toast.makeText(MainActivity.this, "Sélecteur indisponible", Toast.LENGTH_LONG).show();
                }
                return true;
            }
            @Override public void onConsoleMessage(String message, int lineNumber, String sourceID) {
                Log.d(TAG, "JS " + lineNumber + ": " + message);
            }
        });
        webView.addJavascriptInterface(new FolderBridge(), "AndroidBridge");
        setContentView(webView);
    }

    private final class FolderBridge {
        /** Lu sur le thread WebView : aucune interaction avec MediaController. */
        @JavascriptInterface public String getAppVersionLabel() {
            try {
                android.content.pm.PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
                String name = info.versionName == null ? "inconnue" : info.versionName;
                long build = android.os.Build.VERSION.SDK_INT >= 28
                        ? info.getLongVersionCode() : info.versionCode;
                return "VERSION " + name + " · BUILD " + build;
            } catch (Exception ex) {
                Log.w(TAG, "Version APK indisponible", ex);
                return "VERSION 3.5.20 · BUILD 27";
            }
        }
        @JavascriptInterface public String getLibrary() {return loadNativeLibrary().toString();}
        @JavascriptInterface public void refreshNativeCovers() {reindexCoversAsync();}
        @JavascriptInterface public String getState() {return nativeSnapshot;}
        @JavascriptInterface public void chooseMusicFiles() {runOnUiThread(() -> {
            try { Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);intent.setType("*/*");
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                startActivityForResult(intent,CHOOSE_NATIVE_FILES);
            }catch(Exception e){Log.e(TAG,"Sélecteur impossible",e);}
        });}
        @JavascriptInterface public void chooseFolder() {runOnUiThread(() -> {
            try {Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                startActivityForResult(intent, CHOOSE_FOLDER);
            } catch (Exception e) {Log.e(TAG, "Sélection dossier impossible", e);}
        });}
        @JavascriptInterface public void removeNative(String id) {
            if (id == null || !id.startsWith("native:")) return;
            synchronized (libraryLock){JSONArray orig=loadNativeLibrary(),out=new JSONArray();
                for(int i=0;i<orig.length();i++){JSONObject t=orig.optJSONObject(i);if(t!=null&&!id.equals(t.optString("id")))out.put(t);}
                saveNativeLibrary(out);
            }
            deliverNativeLibrary();
        }
        @JavascriptInterface public void clearNative() {
            saveNativeLibrary(new JSONArray());
            // Les miniatures sont uniquement un cache : suppression sans toucher aux fichiers musique.
            File dir = new File(getFilesDir(), "harmonie_covers");
            File[] cached = dir.listFiles();
            if (cached != null) for (File file : cached) if (file.getName().endsWith(".jpg")) file.delete();
            getSharedPreferences("harmonie_bluetooth_car_v354",MODE_PRIVATE).edit()
                    .remove("last_queue").remove("last_position").apply();
            deliverNativeLibrary();
        }
        @JavascriptInterface public void playNative(String id, String queueJson, int frequency, float volume, int repeat, boolean shuffle){
            noteAudioCommand("playNative " + id);
            withController(c -> {
                JSONArray all=loadNativeLibrary();
                Map<String, JSONObject> byId=new java.util.HashMap<>();
                for(int i=0;i<all.length();i++){JSONObject t=all.optJSONObject(i);if(t!=null)byId.put(t.optString("id"),t);}
                List<MediaItem> items=new ArrayList<>();int index=-1;
                try{
                    JSONArray ids=new JSONArray(queueJson);
                    for(int i=0;i<ids.length();i++){
                        JSONObject t=byId.get(ids.optString(i));if(t==null)continue;
                        Uri uri=Uri.parse(t.optString("uri"));
                        MediaMetadata meta=carMetadata(t);
                        MediaItem it=new MediaItem.Builder().setUri(uri).setMediaId(t.optString("id")).setMediaMetadata(meta).build();
                        if(id.equals(t.optString("id")))index=items.size();
                        items.add(it);
                    }
                }catch(Exception ex){Log.e(TAG,"File d'attente native invalide",ex);}
                if(items.isEmpty() || index<0)throw new IllegalArgumentException("Fichier absent de la bibliothèque Android");
                // Important : Media3 reçoit la file entière AVANT la lecture.
                // Il passera au morceau suivant MÊME SI la WebView est suspendue.
                c.setMediaItems(items,index,0);
                c.setRepeatMode(repeat==1?Player.REPEAT_MODE_ONE:repeat==2?Player.REPEAT_MODE_ALL:Player.REPEAT_MODE_OFF);
                c.setShuffleModeEnabled(shuffle);
                c.setPlaybackParameters(nativeRate(frequency));
                c.setVolume(Math.max(0f,Math.min(1f,volume)));
                Log.i(TAG,"Lecture Media3: "+items.size()+" titres; index="+index+"; 432/440="+frequency);
                c.prepare();c.play();
            });
        }
        /** Les radios direct sont traitées à tempo original, avec pitch optionnel 432/440. */
        @JavascriptInterface public void playRadio(String url, String name, float volume, int frequency){
            noteAudioCommand("playRadio " + name);
            withController(c -> {
                Uri uri=Uri.parse(url);
                if(!("https".equals(uri.getScheme())||"http".equals(uri.getScheme())))throw new IllegalArgumentException("Flux radio invalide");
                MediaItem.Builder item=new MediaItem.Builder().setUri(uri).setMediaId("radio:"+url)
                   .setMediaMetadata(new MediaMetadata.Builder().setTitle(name).setArtist("Radio Internet")
                        .setIsPlayable(true).build());
                if(uri.getPath()!=null && uri.getPath().toLowerCase(java.util.Locale.ROOT).endsWith(".m3u8"))item.setMimeType(MimeTypes.APPLICATION_M3U8);
                c.setMediaItem(item.build());
                float relativePitch=frequency==432?432f/440f:1f;
                c.setPlaybackParameters(new PlaybackParameters(1f,relativePitch));
                c.setVolume(Math.max(0f,Math.min(1f,volume))); c.prepare(); c.play();
            });
        }
        /** Réordonne la file Media3 à chaud, sans changer le morceau en cours ni sa position.
         *  Les playlists restent stockées dans leur ordre original ; seule la file active bouge.
         */
        @JavascriptInterface public void reorderNativeQueue(String queueJson){withController(c->{
            try {
                JSONArray expected=new JSONArray(queueJson);
                int count=c.getMediaItemCount();
                if(count<2 || expected.length()!=count)return;
                List<String> currentIds=new ArrayList<>();
                for(int i=0;i<count;i++){
                    String mediaId=c.getMediaItemAt(i).mediaId;
                    if(!mediaId.startsWith("native:"))return; // Ne pas réordonner les radios.
                    currentIds.add(mediaId);
                }
                Set<String> requestedIds=new HashSet<>();
                for(int i=0;i<expected.length();i++)requestedIds.add(expected.getString(i));
                if(requestedIds.size()!=count || !requestedIds.equals(new HashSet<>(currentIds)))return;
                for(int i=0;i<count;i++){
                    String wanted=expected.getString(i);
                    int from=currentIds.indexOf(wanted);
                    if(from<0 || from==i)continue;
                    c.moveMediaItem(from,i); // Media3 conserve l'élément lu lors de son déplacement.
                    currentIds.remove(from);
                    currentIds.add(i,wanted);
                }
                Log.i(TAG,"File Media3 triée par artiste : "+count+" morceaux");
            }catch(Exception e){Log.w(TAG,"Tri de file native ignoré",e);}
        });}
        @JavascriptInterface public void nextNative(){withController(c->{
            if(c.hasNextMediaItem())c.seekToNextMediaItem();
            else if(c.getMediaItemCount()>1)c.seekTo(0,0); // Bouton suivant à la fin : retour au premier.
            if(c.getPlaybackState()==Player.STATE_ENDED){c.prepare();c.play();}
        });}
        @JavascriptInterface public void previousNative(){withController(c->{
            if(c.hasPreviousMediaItem())c.seekToPreviousMediaItem();
            else if(c.getMediaItemCount()>1)c.seekTo(c.getMediaItemCount()-1,0);
            if(c.getPlaybackState()==Player.STATE_ENDED){c.prepare();c.play();}
        });}
        // Important : ne PAS appeler MediaController ici. Le bridge WebView tourne hors main thread.
        /** L'égaliseur de radio est exécuté dans le MediaSessionService, et NON la WebView. */
        @JavascriptInterface public void setNativeAudioEffects(String json){
            if(json==null || json.length()>2048)return;
            try{
                JSONObject cfg=new JSONObject(json);
                JSONArray gains=cfg.optJSONArray("gains");
                if(gains==null || gains.length()!=8)return;
                for(int i=0;i<8;i++){
                    double db=gains.getDouble(i);
                    if(!Double.isFinite(db) || db< -12 || db>12)return;
                }
                JSONObject safe=new JSONObject();
                safe.put("enabled",cfg.optBoolean("enabled",false));
                safe.put("radioEnabled",cfg.optBoolean("radioEnabled",false));
                safe.put("hifi",cfg.optBoolean("hifi",false));
                safe.put("gains",gains);
                safe.put("ambienceOn",cfg.optBoolean("ambienceOn",false));
                safe.put("ambienceAmount",Math.max(0,Math.min(70,cfg.optInt("ambienceAmount",0))));
                safe.put("virtualizerOn",cfg.optBoolean("virtualizerOn",false));
                safe.put("virtualizerAmount",Math.max(0,Math.min(100,cfg.optInt("virtualizerAmount",0))));
                getSharedPreferences(NativeAudioEffects.PREFS,MODE_PRIVATE).edit()
                        .putString("settings",safe.toString()).apply();
                runOnUiThread(()->{
                    // Le service est créé pour les sessions actives, sans modifier la file.
                    if(controller==null || controller.getMediaItemCount()==0)return;
                    Intent update=new Intent(MainActivity.this,PlaybackService.class);
                    update.setAction(NativeAudioEffects.ACTION_UPDATE);
                    try{startService(update);}catch(Exception e){Log.w(TAG,"Mise à jour EQ différée",e);}
                });
            }catch(Exception e){Log.w(TAG,"Réglages EQ invalides ignorés",e);}
        }
        /** Read-only immutable frame; safe on WebView JavaScript bridge thread. */
        @JavascriptInterface public String getNativeLedFrame(){return NativeBeatVisualizer.getFrame();}
        @JavascriptInterface public boolean hasNativeLedPermission(){
            return checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED;
        }
        /** Explicit LED control: permission request is initiated ONLY by user's switch. */
        @JavascriptInterface public void setNativeLedCapture(boolean enabled){
            runOnUiThread(()->{
                if (enabled && checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},PERMISSION_NATIVE_LED);
                    return;
                }
                getSharedPreferences(NativeBeatVisualizer.PREFS,MODE_PRIVATE).edit()
                        .putBoolean("enabled",enabled).apply();
                updateNativeLedVisibility(enabled);
                sendLedPermissionResult(enabled);
            });
        }
        @JavascriptInterface public String getAudioDiagnostics(){return audioDiagnosticSnapshot;}
        @JavascriptInterface public void restoreSavedQueue(float volume){
            noteAudioCommand("restoreSavedQueue demandé par utilisateur");
            withController(c->{
                if(c.getMediaItemCount()>0){Log.i(TAG,"Restauration inutile : file déjà présente");return;}
                if(!restoreSavedQueueOnMain(c,volume)){
                    Toast.makeText(MainActivity.this,"Aucune file restaurable : sélectionnez un morceau",Toast.LENGTH_LONG).show();
                }
            });
        }
        /** Intervention MANUELLE : réinitialise le décodeur/sortie sans changer la file, le 432 Hz ou la position. */
        @JavascriptInterface public void recoverNativeAudio(){withController(c->{
            if(c.getMediaItemCount()<1)return;
            if(!c.getPlayWhenReady()){Log.i(TAG,"Réparation audio ignorée : lecteur en pause");return;}
            int index=c.getCurrentMediaItemIndex();
            if(index<0||index>=c.getMediaItemCount())return;
            long position=Math.max(0,c.getCurrentPosition());
            Log.w(TAG,"Récupération audio manuelle : item="+index+" pos="+position);
            c.stop(); // Libérer AudioTrack, puis reconstruire le pipeline audio à l'identique.
            c.seekTo(index,position);
            c.prepare();
            c.play();
        });}
        @JavascriptInterface public void pauseNative(){withController(c->c.pause());}
        @JavascriptInterface public void resumeNative(){withController(c->c.play());}
        @JavascriptInterface public void stopNative(){
            noteAudioCommand("stopNative depuis interface");
            withController(c->{
                c.stop();c.clearMediaItems();
                // Ne pas effacer l'historique à chaque changement de piste/radio.
                // La suppression volontaire de la bibliothèque utilise clearNative().
                Log.i(TAG,"File courante arrêtée ; dernière file valide préservée");
            });
        }
        @JavascriptInterface public void seekNative(long millis){withController(c->c.seekTo(Math.max(0,millis)));}
        @JavascriptInterface public void setFrequency(int hz){withController(c->{
            if(c.getCurrentMediaItem()!=null){
                if(c.getCurrentMediaItem().mediaId.startsWith("radio:"))
                    c.setPlaybackParameters(new PlaybackParameters(1f,hz==432?432f/440f:1f));
                else c.setPlaybackParameters(nativeRate(hz));
            }
        });}
        @JavascriptInterface public void setVolume(float level){withController(c->c.setVolume(Math.max(0f,Math.min(1f,level))));}
        /** Préférence lue par le service Media3 : elle fonctionne même sans Activity/WebView. */
        @JavascriptInterface public void setContinuousPlayback(boolean enabled){
            getSharedPreferences("harmonie_bluetooth_car_v354",MODE_PRIVATE).edit()
                    .putBoolean("continuous_playback",enabled).apply();
            Log.i(TAG,"Lecture continue Media3 = "+enabled);
        }
        @JavascriptInterface public void setShuffle(boolean enabled){withController(c->c.setShuffleModeEnabled(enabled));}
        @JavascriptInterface public void setRepeat(int mode){withController(c->c.setRepeatMode(mode==1?Player.REPEAT_MODE_ONE:mode==2?Player.REPEAT_MODE_ALL:Player.REPEAT_MODE_OFF));}
    }
    private PlaybackParameters nativeRate(int frequency){
        float factor=frequency==432?432f/440f:1f;
        return new PlaybackParameters(factor,factor); // Changes both speed and pitch, like WebAudio v10.
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CHOOSE_FILES) {
            if (pendingFiles == null) return;
            ValueCallback<Uri[]> callback = pendingFiles;
            pendingFiles = null;
            if (resultCode != RESULT_OK || data == null) {callback.onReceiveValue(null);return;}
            List<Uri> result = new ArrayList<>();
            ClipData clips = data.getClipData();
            if (clips != null) {
                for (int i = 0; i < clips.getItemCount(); i++) result.add(clips.getItemAt(i).getUri());
            } else if (data.getData() != null) result.add(data.getData());
            callback.onReceiveValue(result.isEmpty() ? null : result.toArray(new Uri[0]));
        } else if (requestCode == CHOOSE_NATIVE_FILES && resultCode == RESULT_OK && data != null) {
            importNativeFiles(data);
        } else if (requestCode == CHOOSE_FOLDER && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri root = data.getData();
            try {getContentResolver().takePersistableUriPermission(root, Intent.FLAG_GRANT_READ_URI_PERMISSION);}
            catch (Exception ignored) { }
            new Thread(() -> deliverFolderFiles(root), "harmonie-folder").start();
        }
    }

    private void deliverFolderFiles(Uri root) {
        try {
            JSONArray files = new JSONArray();
            String docId = DocumentsContract.getTreeDocumentId(root);
            walkTree(root, docId, "", files, 0);
            int added=mergeNativeLibrary(files);
            deliverNativeLibrary();
            // Importation rapide des URI, extraction des pochettes ensuite sur un thread dédié.
            if(added>0)reindexCoversAsync();
            runOnUiThread(() -> Toast.makeText(this, added + " morceau(x) indexés sans copie (pochettes en cours)",Toast.LENGTH_LONG).show());
        } catch (Exception e) {
            Log.e(TAG, "Parcours du dossier impossible", e);
            runOnUiThread(() -> Toast.makeText(this, "Dossier inaccessible : " + e.getMessage(), Toast.LENGTH_LONG).show());
        }
    }

    private void walkTree(Uri treeUri, String docId, String relative, JSONArray output, int depth) throws Exception {
        if (depth > 15 || output.length() >= MAX_FOLDER_FILES) return;
        Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(treeUri,docId);
        String[] cols={DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE};
        try(Cursor cursor=getContentResolver().query(children,cols,null,null,null)){
            if(cursor==null)return;
            List<String[]> entries=new ArrayList<>();
            while(cursor.moveToNext())entries.add(new String[]{cursor.getString(0),cursor.getString(1),cursor.getString(2)});
            for(String[] entry:entries){
                if(output.length()>=MAX_FOLDER_FILES)break;
                String child=entry[0],name=entry[1],mime=entry[2];
                if(name==null||child==null)continue;
                if(DocumentsContract.Document.MIME_TYPE_DIR.equals(mime)){
                    walkTree(treeUri,child,relative+name+"/",output,depth+1);continue;
                }
                if(!audioName(name))continue;
                Uri uri=DocumentsContract.buildDocumentUriUsingTree(treeUri,child);
                output.put(nativeTrack(uri,name,relative+name));
            }
        }
    }

    @Override public void onBackPressed() {
        // Retour dans le visualiseur : le fermer, sinon quitter l'application.
        if (webView == null) {super.onBackPressed();return;}
        webView.evaluateJavascript("!!(document.getElementById('stageOverlay') && !document.getElementById('stageOverlay').hidden)", result -> {
            if ("true".equals(result)) webView.evaluateJavascript("document.getElementById('stageClose').click()", null);
            else finish();
        });
    }
    @Override protected void onDestroy() {
        mainHandler.removeCallbacks(snapshotTask);
        pendingMedia3Actions.clear();
        if (controllerFuture != null) MediaController.releaseFuture(controllerFuture);
        controller = null;
        if (pendingFiles != null) {pendingFiles.onReceiveValue(null);pendingFiles = null;}
        if (webView != null) {webView.destroy();webView = null;}
        super.onDestroy();
    }
}
