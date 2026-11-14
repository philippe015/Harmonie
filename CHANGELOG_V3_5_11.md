# Harmonie v3.5.11 — Corriger les coupures au passage au titre suivant

## Cause retrouvée dans le code v3.5.10
Lorsqu'une notification de titre Media3 était reflétée vers la WebView, `readNativeState()` appelait `stopOldForLoad()` qui, selon l'état HTML, exécutait `stopNative`. Cette commande arrêtait le lecteur et supprimait la file courante **et la sauvegarde**. La restauration automatique du dernier titre dans `init()` pouvait aussi interrompre une session existante. Le diagnostic avec file=0 et sauvegarde=0 est compatible avec ce défaut, mais une interruption système ne peut être exclue sans journal système.

## Modifications
- `stopOldForLoad(false)` dans les retours d'état du service : ne déconnecte que les éléments Web Audio ; Media3 reste intact.
- `selectTrack(last,false)` ne coupe plus Media3 lors de la restauration de l'interface.
- `stopNative()` n'efface plus l'historique de la file. Seul « Effacer la bibliothèque » supprime ce dernier état.
- Une commande JavaScript arrivée avant la connexion MediaController est mise en attente puis jouée sur le thread principal (jusqu'à 32 commandes), au lieu d'être silencieusement jetée.
- Le checkpoint n'écrase plus la position sauvegardée lorsqu'un service nouvellement recréé est vide.
- Version visible en bas : 3.5.11 / build 18.
- Radio, Bluetooth autoradio, lecture écran éteint, tri artiste et 432 Hz conservés.

## Installation Manjaro
```bash
unzip -o ~/Téléchargements/Harmonie_Android_Python_Rust_v3_5_11_CORRECTIF_FILE_AUTOMATIQUE_Manjaro.zip -d ~/Projets
cd ~/Projets/Harmonie_Android_Python_Rust_v3
bash compiler_manjaro.sh
```

## Validation
```bash
python -m unittest discover -s tests -v
bash -n compiler_manjaro.sh scripts/compiler_manjaro.sh
```
Reproduire sur le smartphone avec 3 FLAC importés **via + Fichiers / + Dossier**, écran éteint, puis tester la transition. La compilation Gradle et l'exécution Android n'ont pas été vérifiées dans l'environnement de génération.

Si la coupure revient : `bash scripts/diagnostic_coupure_son.sh` via ADB. Le journal doit inclure les tags `HarmonieNative`, `HarmoniePlayback` et `AndroidRuntime`.
