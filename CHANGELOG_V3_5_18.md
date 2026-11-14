# Harmonie Android 3.5.18 — correctif de compilation Java des LED synchronisées

- Correction de `NativeBeatVisualizer.java`, ligne de sérialisation des 30 bandes FFT : `JSONArray.put(double)` impose un traitement de `JSONException` sur Android. Utilisation explicite de `Double.valueOf(...)`, transmise à `JSONArray.put(Object)` afin de conserver une valeur JSON numérique sans exception vérifiée.
- Aucun changement des analyses FFT, des effets LED, du moteur Media3, des playlists, des radios, du Bluetooth ou du mode 432 Hz.
- Version Android `3.5.18`, `versionCode 25`; même `applicationId` pour mise à jour si la signature correspond.
- Exécuter `bash compiler_manjaro.sh`. La compilation complète de l’APK doit être validée sous Manjaro.
