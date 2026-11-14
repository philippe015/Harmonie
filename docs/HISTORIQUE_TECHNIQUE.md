# Harmonie Android v3.5.19 — HI-FI AUDIO PRO unifié

Le panneau indépendant « EFFETS AUDIO · ÉCHO & SPATIAL » a été retiré.
Ses commandes sont maintenant dans **HI-FI AUDIO PRO > Ambiance**. Ce panneau contient
huit profils sonores, trois corrections rapides Graves / Voix / Aigus, les boutons
**Enregistrer mon son**, **Rappeler mon son** et **Son direct**. Le réglage 432 Hz / Original
reste disponible. Pour l'écoute native Android/Media3, les corrections de tonalité
nécessitent un effet EQ pris en charge par le téléphone ; écho et largeur stéréo ne
modifient pas Media3. Version 3.5.19 (build 26).

Projet à compiler sous Manjaro : `bash compiler_manjaro.sh`.

---

# Harmonie Android v3.5.16 — LED Cyberpunk et effets supplémentaires

## Nouveautés 3.5.16 — LED Cyberpunk

**Correction** : avec Media3 natif (radio et musique Android), le moteur Web Audio ne reçoit pas le signal PCM. Auparavant les modes spectre, VU et néons affichaient la même animation d’ambiance. Chaque effet possède désormais sa propre animation visuelle, même en mode natif ou à l’arrêt (aperçu fixe).

**Effets** : spectre, VU-mètres, néons, halo circulaire, onde lumineuse, particules, pluie Matrix, pulsations, tunnel 3D, désactivé. **Palettes** : Cyberpunk cyan/magenta, arc-en-ciel, glace, feu, Matrix, plasma violet, coucher de soleil, or électrique.

Le menu **Style** recolore aussi le visualiseur ; le menu **Couleurs** permet de choisir une palette différente. Réglages mémorisés. Animations désactivées lorsque l’écran est masqué, jusqu’à 30 images/s pour limiter la consommation.

**Important** : les animations des pistes Media3 sont décoratives et ne reflètent pas le niveau sonore réellement mesuré. Le mode Web Audio conserve l’analyse du spectre si elle est disponible. Aucun effet LED ne modifie le signal audio ; Hi-Fi, 432 Hz, radios, playlists et Bluetooth restent inchangés.

Pour compiler sur Manjaro : `bash compiler_manjaro.sh`. Ne copiez pas les symboles d’invite `❯` ni les résultats de terminal dans les commandes.


Version Cyberpunk avec **Ma musique : Titres / Playlists / Gestion** et **Radios Internet : Explorer / Favoris**. Les listes, commandes, moteur Media3, commandes Bluetooth, pochettes, 432 Hz et égaliseur sont conservés.

Pour compiler sous Manjaro :

```bash
unzip -o ~/Téléchargements/Harmonie_Android_Python_Rust_v3_5_15_ONGLETS_NAVIGATION_Manjaro.zip -d ~/Projets
cd ~/Projets/Harmonie_Android_Python_Rust_v3
bash compiler_manjaro.sh
```

APK après compilation : `app/build/outputs/apk/debug/app-debug.apk`. Une installation par-dessus la précédente requiert la même clé de signature.

Voir `CHANGELOG_V3_5_15.md` et les anciens fichiers de suivi pour l'historique.

# Harmonie Android v3.5.14 — Correction de compilation Java

Cette version corrige la compilation de v3.5.13 : `reindexCoversAsync()` déclarait
deux variables locales nommées `updated`. La seconde est renommée en
`refreshedTrack` pour conserver le compteur de pochettes et la fusion de l’index.

Le pied de page affiche **VERSION 3.5.14 · BUILD 21**, l’identifiant Android
reste `fr.byphilou.harmonie`, et la fonction de réanalyse des pochettes est maintenue.
Toutes les fonctions musicales de v3.5.13 sont conservées.

```bash
unzip -o ~/Téléchargements/Harmonie_Android_Python_Rust_v3_5_14_CORRECTIF_JAVA_POCHETTES_Manjaro.zip -d ~/Projets
cd ~/Projets/Harmonie_Android_Python_Rust_v3
bash compiler_manjaro.sh
```

La compilation APK complète doit être validée sur Manjaro.

---

# Harmonie v3.5.13 — Pochettes après suppression/réimportation

La **lecture Media3, le mode 432 Hz et l'interface Cyberpunk sont conservés**.
Le nouveau bouton **« Réanalyser les pochettes »** se trouve sous les commandes de playlist dans **Ma musique**.
Il recherche les pochettes intégrées aux fichiers audio via le système Android (FLAC, MP3 et autres formats compatibles), y compris après réimportation ou import de dossier. Les images sont des JPG réduits conservés dans l'espace privé de l'application. Aucune image n'est téléchargée depuis Internet ; les fichiers sans pochette intégrée conservent une image générique.

```bash
unzip -o ~/Téléchargements/Harmonie_Android_Python_Rust_v3_5_13_POCHETTES_REIMPORT_Manjaro.zip -d ~/Projets
cd ~/Projets/Harmonie_Android_Python_Rust_v3
bash compiler_manjaro.sh
```

Ne désinstallez pas l'application existante avant d'avoir essayé l'installation en mise à jour (même signature requise). La compilation réelle nécessite Manjaro avec Android SDK/NDK ; elle n'a pas été exécutée ici.

---

# Harmonie Android v3.5.12 — Correction de file Media3

Voir `CHANGELOG_V3_5_11.md` pour les détails.

# Harmonie v3.5.10 — Numéro de version en bas de l’application

La version et le numéro de build de l’APK s’affichent dans le pied de page de l’interface Cyberpunk.

# IMPORTANT : Coupures de son (v3.5.6)

Si la piste continue d'avancer mais qu'aucun son ne sort : ouvrir **Diagnostic / son coupé** sous la barre de lecture, utiliser **Vérifier la sortie audio**, puis **Rétablir le son**. Le bouton ne garantit pas la réparation d'une déconnexion Bluetooth ou d'une priorité audio détenue par une autre application. Pour capter les erreurs Android, utilisez `bash scripts/diagnostic_coupure_son.sh` avec le téléphone branché et le débogage USB actif.

> **v3.5.5 — Playlists : tri par artiste.** Dans « Ma musique », le sélecteur « Trier la sélection » permet Ordre de la playlist, Artiste A → Z ou Z → A. Le tri est conservé au redémarrage et les touches Bluetooth Suivant/Précédent suivent la file Media3 triée. Voir `CHANGELOG_V3_5_5.md`.

> **v3.5.4 — Bluetooth voiture :** commandes du volant (lecture/pause, précédent, suivant) via Media3/AVRCP, métadonnées titre/artiste, et reprise du dernier morceau depuis une touche Bluetooth après arrêt du service. Voir `CHANGELOG_V3_5_4.md`.

> **Correctif v3.5.3 :** suppression du besoin de `androidx.core.content.ContextCompat` lors de la compilation Java. Voir `CHANGELOG_V3_5_3.md`.

# HARMONIE Android v3.5.5 — BLUETOOTH VOITURE · BIBLIOTHÈQUE · 432 Hz

**BY PHILOU/CYBERPUNK** — projet APK Android **Python + Rust + Java Media3**, à compiler sur Manjaro.

## Objectifs de v3.5, sans changer l'interface Cyberpunk

Cette livraison conserve **le style de l'interface Harmonie v3.4** (mêmes thèmes, LED, logos, pochettes anciennes, radios par pays/genre, playlists, paroles, égaliseur huit bandes, Hi-Fi Audio PRO). Un sélecteur compact de tri par artiste apparaît dans « Ma musique ». Le logo personnalisé de la v3.3 est conservé.

### Nouveaux comportements sur Android

- **Fichiers** : le bouton **+ Fichiers** ouvre le sélecteur Android (`ACTION_OPEN_DOCUMENT`) ; les autorisations de lecture sont conservées lorsque le fournisseur le permet. Les fichiers FLAC restent **à leur emplacement**, sans copie dans IndexedDB.
- **Dossiers** : le bouton **+ Dossier** utilise `ACTION_OPEN_DOCUMENT_TREE` ; l'index parcourt les sous-dossiers (maximum 1500 morceaux par sélection) et garde l'autorisation de l'arbre. Android 11+ ne permet pas de sélectionner certaines racines, notamment la racine du téléphone ou le dossier Téléchargements lui-même.
- **Bibliothèque** : liste des références enregistrée dans les préférences privées de l'application. Recherche et playlists utilisent l'interface existante. Supprimer un titre de la liste **ne supprime pas le fichier du téléphone**.
- **Lecture native Media3** : ExoPlayer dans `PlaybackService extends MediaSessionService`, donc indépendant de la WebView et du verrouillage de l'écran ; notification/contrôles Media3 sur l'écran verrouillé, prise en charge du focus audio et pause au retrait du casque selon l'appareil.
- **Radios** : stations sélectionnées dans l'onglet d'origine transmises au lecteur Media3 ; certains flux restent intrinsèquement incompatibles, géobloqués ou indisponibles. HLS `.m3u8` pris en charge.
- **432 Hz / Original** : les deux boutons existants restent visibles. Pour les titres locaux Media3, **432/440 (≈ 0,981818)** est appliqué à la *vitesse et à la hauteur*, tout comme dans la version HTML. Les radios restent toujours à leur hauteur et vitesse originales. **Le profil « Fidélité FLAC » sélectionne volontairement Original ; il reste possible de remettre ensuite 432 Hz.**
- **Compatibilité anciens morceaux** : les morceaux déjà enregistrés dans IndexedDB continuent à se lire via le moteur Web Audio de la v3.4 ; ils ne sont pas déplacés ou supprimés lors de la mise à jour. Le bouton **Sans copie** conserve également son comportement temporaire de la v3.4.

### Limites clairement connues

Les quatre profils Hi-Fi PRO, l'égaliseur et les effets écho/spatialisation sont conservés et restent opérationnels dans **l'ancien moteur Web Audio** ; le son des nouveaux fichiers et des radios **lus par Media3 ne traverse pas encore le DSP Web Audio**. Le raccordement de l'égaliseur/effets au moteur natif (avec Rust) est prévu pour **v3.6**. Le visualiseur LED natif peut donc afficher une animation d'ambiance plutôt qu'un spectre audio mesuré.

La compatibilité d'un fichier FLAC dépend toujours des décodeurs du téléphone. L'application ne garantit ni une sortie bit-perfect ni une reprise automatique après extinction forcée par le constructeur. La bibliothèque indexe les noms et chemins des fichiers natifs sans scanner leurs pochettes à l'import ; elle conserve les pochettes déjà présentes dans la base HTML. Si un fichier Android est déplacé, supprimé ou si son accès est révoqué, vous devrez le sélectionner à nouveau.

## Installation / compilation sous Manjaro

Conserver la même installation Android autant que possible pour ne pas perdre la bibliothèque. Ne pas désinstaller la version précédente pour installer celle-ci.

```bash
unzip -o ~/Téléchargements/Harmonie_Android_Python_Rust_v3_5_5_TRI_PAR_ARTISTE_Manjaro.zip -d ~/Projets
cd ~/Projets/Harmonie_Android_Python_Rust_v3
bash compiler_manjaro.sh
```

**Prérequis :** Python 3.14, JDK 17, Rust et cargo-ndk, SDK Android API 36, NDK 27c, Gradle 8.11.1 (le script prépare les outils non installés). Internet pour télécharger Media3/Gradle et les radios.

APK attendu **si compilation réussie** :

```
~/Projets/Harmonie_Android_Python_Rust_v3/app/build/outputs/apk/debug/app-debug.apk
```

Copie pratique :

```bash
cp app/build/outputs/apk/debug/app-debug.apk ~/Téléchargements/Harmonie_BY_PHILOU_v3.5.5.apk
```

Par USB, une fois le débogage activé :

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb logcat -s HarmonieNative:D AndroidRuntime:E python.stderr:E '*:S'
```

**Mise à jour :** l'identifiant `fr.byphilou.harmonie` est inchangé ; la version Android passe à `versionCode 11` / `versionName 3.5.4`. Une mise à jour par-dessus v3.4 demande la **même clé de signature** (habituellement la clé debug du même PC). Si Android refuse la signature, ne désinstallez pas l'application avant d'avoir sauvegardé les données.

## Vérifications

```bash
cd ~/Projets/Harmonie_Android_Python_Rust_v3
python -m unittest discover -s tests -v
bash -n compiler_manjaro.sh scripts/compiler_manjaro.sh
```

Les tests Python valident l'architecture et les fichiers livrés. Une simulation Chromium vérifie les boutons vers le pont Android, la sélection d'un fichier natif, la bascule 432/440 Hz et une radio. **La compilation Gradle, l'installation sur téléphone et la qualité de la lecture réelle ne sont pas testables dans l'environnement de génération : ces étapes restent à valider sur votre Manjaro et votre smartphone.**

## Programme prévu pour v3.6

Intégrer le traitement sonore dans la chaîne native (étudier Android AudioProcessor/Media3 et Rust), égaliseur huit bandes sans duplication de signal, préampli/limiteur de crête, écho/reverb/spatialisation natives, test d'écoute et contrôle de compatibilité mono/casque. **Ne pas promettre une amélioration physique de résolution sonore ni un véritable Dolby Atmos sans compatibilité matérielle.**

## Correctif 3.5.1 — Gradle et SDK Android

L'archive v3.5.0 échouait à `:app:checkDebugAarMetadata` car Media3 1.10.1 nécessite `compileSdk 36`, alors que le projet utilisait `compileSdk 35` et AGP 8.7.3. Cette archive corrige simultanément :

- `compileSdk 36` (le `targetSdk` reste 35) ;
- Android Gradle Plugin **8.10.1**, compatible API 36 ;
- Gradle **8.11.1** (version minimale AGP 8.10) ;
- téléchargement de `platforms;android-36` par le script Manjaro ;
- version Android 3.5.1 / code 8, pour distinguer le correctif.

Conserve le même identifiant `fr.byphilou.harmonie`, les radios, la bibliothèque native, la sélection 432 Hz et l'interface Cyberpunk.

Ce correctif est vérifié statiquement, mais la compilation Gradle réelle doit être vérifiée sous Manjaro.

### Prévisualiser les erreurs de compilation

```bash
cd ~/Projets/Harmonie_Android_Python_Rust_v3
bash compiler_manjaro.sh 2>&1 | tee ~/Téléchargements/harmonie_build_v351.log
```

Si `sdkmanager` affiche un avertissement XML v3/v4, mettez à jour les Android SDK Command-line Tools dans Android Studio. Un simple avertissement sans échec n'est pas la cause du problème de `compileSdk`.


## Correctif v3.5.2 — Lecture continue écran éteint

- **+ Fichiers / + Dossier** : les fichiers importés via le sélecteur natif sont remis à Media3 **dans une file de lecture complète**. Media3 passe automatiquement au prochain titre sans dépendre d'un minuteur JavaScript de WebView.
- Une recherche par nom réduit seulement les résultats affichés, **pas la file Media3**. Les filtres dossier et playlists personnalisées restent respectés.
- Les commandes « suivant/précédent » manipulent directement la file Media3 plutôt que la recréer.
- Sur erreur de lecture d'un titre illisible, Media3 tente de passer au suivant. Les erreurs sont visibles via `adb logcat -s HarmoniePlayback:I HarmonieNative:D AndroidRuntime:E '*:S'`.
- Le mode **432 Hz** conserve sa vitesse et hauteur réduites (`432/440`), y compris lors des transitions Media3.
- **Attention :** les morceaux importés auparavant dans la bibliothèque HTML/IndexedDB et les titres « Sans copie » continuent à utiliser la WebView. Réimportez-les via **+ Fichiers** ou **+ Dossier** pour qu'ils bénéficient de la lecture continue Android. Aucun fichier audio d'origine n'est modifié.
- Choisissez « Toutes les pistes » pour jouer tous les titres, ou une playlist complète. La répétition est désactivée par défaut : le lecteur s'arrête normalement à la fin de la file entière.
- Pour mettre à jour sans effacer les données, installer par-dessus avec la **même clé de signature** : `adb install -r app/build/outputs/apk/debug/app-debug.apk`.

**Vérification fonctionnelle recommandée :** importer trois pistes avec + Fichiers, lancer la première, verrouiller l'écran et vérifier que la deuxième démarre. Refaire l'essai en mode 432 Hz. La validation sur smartphone reste nécessaire.

### Diagnostiquer un échec de lecture continue (v3.5.2)

Le téléphone doit être branché avec le débogage USB activé. Exécuter après avoir reproduit le problème :

```bash
bash scripts/diagnostic_lecture_veille.sh
```

Ce script écrit `~/Téléchargements/harmonie_lecture_veille.log`. Rechercher `Lecture Media3: N titres`, puis `Transition automatique vers ...`. Si le journal annonce seulement **1 titre**, choisir **Toutes les pistes** et réimporter les morceaux avec **+ Fichiers** / **+ Dossier** (la bibliothèque HTML n'est pas une file native). Si plusieurs titres sont présents mais qu'un fichier échoue, vérifier les messages `Erreur de décodage/accès`.

## Commandes Bluetooth voiture (v3.5.4)

1. Associez le téléphone à l'autoradio et sélectionnez « audio multimédia » sur le téléphone.
2. Lancez un morceau depuis la bibliothèque **Android native** (« + Fichiers » ou « + Dossier ») ou une radio native.
3. Les touches lecture/pause, précédent et suivant de l'autoradio ou du volant utilisent Media3. Sur les stations radio, suivant/précédent n'a pas de playlist de stations.
4. Le titre/artiste/album sont publiés au système et peuvent être visibles sur l'écran Bluetooth (selon AVRCP/autoradio).
5. Verrouillez l'écran : le service Media3 continue la lecture et gère le passage au prochain titre.
6. Si le service s'arrête, la touche Play peut relancer la dernière file enregistrée (selon Android et l'autoradio, et si les URI existent toujours).
7. Le mode 432 Hz reste disponible pour les fichiers locaux ; la radio reste à vitesse d'origine.

**Les fichiers de l'ancienne bibliothèque HTML** ne sont pas contrôlés par la MediaSession native : réimportez-les avec le sélecteur Android pour profiter des commandes du volant. Testez uniquement véhicule stationné.

Diagnostic facultatif, téléphone relié au PC en ADB :

```bash
bash scripts/diagnostic_bluetooth_voiture.sh
```


## Correctif 3.5.7 — Diagnostic audio
Voir `CHANGELOG_V3_5_7.md`. Le bridge JavaScript lit un instantané diagnostique actualisé sur le main looper ; plus de lecture MediaController hors thread. La cause des coupures sonores doit être diagnostiquée avec les nouvelles mesures.

## v3.5.12 : Radio Hi-Fi Audio PRO
Radio Media3 : option EQ natif et 432 Hz / Original. Le diagnostic audio indique si l’effet Android est réellement disponible. Sur certaines sorties Bluetooth et certains constructeurs, la prise en charge native de l’égaliseur peut varier. Le 432 Hz radio est une conversion de hauteur relative, au tempo inchangé.


## v3.5.17 — LED synchronisées au rythme

Dans le panneau **VISUALISEUR LED**, activez **LED synchronisées au son (Android)**. Android demande alors l’autorisation audio (libellée « Microphone » sur certains systèmes). L’application ne lance aucune capture du microphone : elle analyse uniquement une FFT de faible résolution de la session Media3 de Harmonie. Cette fonctionnalité est facultative, et peut ne pas fonctionner sur certains appareils ou certaines sorties Bluetooth.

L’analyse cesse lorsque la fenêtre Harmonie est masquée ; la lecture audio en arrière-plan continue. Si l’analyse est indisponible, une animation décorative reste disponible. Les anciennes lectures HTML continuent d’utiliser l’analyse Web Audio existante.


## v3.5.18 — correctif Java LED
La sérialisation des bandes du visualiseur natif a été corrigée pour Android : l’appel `JSONArray.put` reçoit désormais un objet `Double`, évitant l’exception `JSONException` imposée par la surcharge primitive `double`. La synchronisation LED, Media3, le Bluetooth et l’option 432 Hz sont inchangés.

## Version 3.5.20 — Hi-Fi : réglages réellement reliés au moteur natif

Sur une radio native, déplacer un curseur rapide (Graves / Voix / Aigus)
active automatiquement l'égaliseur radio. Les réglages d'ambiance sont envoyés
à Media3 via une réverbération auxiliaire et un virtualiseur Android, lorsque
ces effets sont pris en charge par le téléphone ou la sortie audio.

L'écho du moteur Web Audio reste un délai à répétitions ; la réverbération
Android n'est pas le même effet. La largeur stéréo Web Audio est remplacée,
avec Media3, par un virtualiseur optionnel : ce dernier peut être ignoré,
notamment avec certaines sorties Bluetooth. Vérifier le volet Diagnostic/son
coupé > Égaliseur Android / Ambiance Android.
