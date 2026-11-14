# Harmonie Android v3.5.3 — Correction de compilation Java

## Erreur corrigée

La compilation de v3.5.2 s’arrêtait sur :
`package androidx.core.content does not exist` et
`cannot find symbol ContextCompat` dans `MainActivity.java`.

La correction utilise `mainHandler.post(command)` comme exécuteur de retour
sur le thread principal Android à la place de `ContextCompat.getMainExecutor`.
Cette approche ne nécessite pas d’ajouter `androidx.core`.

## Conservé
- Interface Harmonie Cyberpunk et logo.
- Service Media3 et file de lecture native complète écran verrouillé.
- Option 432 Hz / Original (432/440).
- Bibliothèque persistante et radios.
- Les quatre profils Hi-Fi Audio PRO.

## Compilation sur Manjaro

```bash
unzip -o ~/Téléchargements/Harmonie_Android_Python_Rust_v3_5_3_CORRECTIF_JAVA_Manjaro.zip -d ~/Projets
cd ~/Projets/Harmonie_Android_Python_Rust_v3
bash compiler_manjaro.sh
```

APK attendu après compilation réussie :
`app/build/outputs/apk/debug/app-debug.apk`

Attention : tests de compilation complète sur téléphone non réalisés ici.
Conserver la même signature pour mettre à jour l’application installée.
