# HARMONIE — BY PHILOU / CYBERPUNK

**Lecteur musical Android open source** : fichiers FLAC et autres formats compatibles, radios Internet, bibliothèque, playlists, interface Cyberpunk et visualisations LED.

**Version des sources : 3.5.20 (build 27)** · **État : projet communautaire en développement, à compiler et tester sur appareil réel.**

> Harmonie est développé pour Android depuis Manjaro Linux. Les fonctions disponibles peuvent varier selon les décodeurs et les effets audio du téléphone. Ce dépôt contient les **sources**, pas un APK certifié ni une garantie de fonctionnement sur tous les appareils.

## Fonctionnalités

- **Ma musique** : onglets Titres / Playlists / Gestion, tri par artiste, indexation de dossiers et récupération des pochettes intégrées lorsque disponibles.
- **Radios Internet** : navigation et favoris, lecture des flux compatibles via Media3.
- **Lecture en arrière-plan** : service Android Media3 avec commandes d'écran verrouillé et Bluetooth/autoradio pris en charge par le système.
- **Hi-Fi Audio PRO** : égaliseur, profils, réglages Graves/Voix/Aigus et ambiances selon le moteur audio et les capacités de l'appareil.
- **432 Hz / Original** : transposition relative au réglage 440 Hz ; cette option *ne garantit pas* un accordage physique 432 Hz sur un enregistrement ou une station.
- **LED Cyberpunk** : animations configurables et visualisation du son lorsque l'API Android Visualizer est accessible.

## Architecture

| Composant | Rôle |
|---|---|
| `app/src/main/assets/harmonie.html` | Interface Cyberpunk HTML/CSS/JavaScript avec Web Audio pour les anciens imports |
| `app/src/main/java/fr/byphilou/harmonie/` | Activité Android, Media3, bibliothèque, visualisation et effets natifs |
| `app/src/main/python/harmonie_server.py` | Serveur local 127.0.0.1 pour l'interface et les données radio |
| `rust-core/` | Bibliothèque Rust et fonctions utilitaires ; **pas** encore le DSP principal de Media3 |
| `scripts/` | Compilation et diagnostics Manjaro |
| `tests/` | Tests automatisés de non-régression et contrôles statiques |

**Limites connues :** certains effets audio sont propres à Web Audio, d'autres aux effets Android natifs ; leur disponibilité dépend du téléphone, du Bluetooth et de la session audio. Les radios dépendent de services/flux tiers. L'application n'est pas certifiée bit-perfect. Les tests de sources ne remplacent pas un test APK réel.

## Compiler sous Manjaro

Prérequis : **JDK 17**, Python **3.14** (Chaquopy), Rust et rustup, Android SDK **API 36**, Android NDK **27.2.12479018**, curl, git et accès Internet pour les dépendances. Le script prépare Gradle **8.11.1** et `cargo-ndk`.

```bash
cd Harmonie
bash compiler_manjaro.sh
```

Résultat attendu si compilation réussie : `app/build/outputs/apk/debug/app-debug.apk`.

**Mise à jour d'un APK existant :** utiliser la **même clé de signature** que l'APK déjà installé. Ne désinstallez pas votre version précédente avant d'avoir sauvegardé vos données.

### Tests légers (sans Android SDK)

```bash
python -m unittest discover -s tests -p 'test_*.py' -v
bash -n compiler_manjaro.sh scripts/*.sh
```

La compilation Java/Gradle complète et les essais réels (Bluetooth, LED synchronisées, radios, changement de morceau écran verrouillé) doivent être effectués sur téléphone.

## Participer au développement

Les contributions du public sont bienvenues : corrections de bugs, simplification de l'interface, performances, accessibilité, traduction, tests sur d'autres téléphones, documentation et nouvelles fonctionnalités.

1. Consultez les **Issues** pour signaler un défaut ou proposer une amélioration.
2. Créez un **fork**, une branche dédiée, puis une **Pull Request** décrivant votre modification et vos essais.
3. Ne publiez jamais vos clés privées, fichiers musicaux personnels, jetons API, données privées ni des pochettes sous droits que vous n'avez pas le droit de redistribuer.
4. Pour un bug Android, joignez la version, la version Android, le modèle du téléphone, les étapes de reproduction et les lignes d'erreur pertinentes (sans informations personnelles).

Voir [CONTRIBUTING.md](CONTRIBUTING.md), [SECURITY.md](SECURITY.md) et [docs/ROADMAP.md](docs/ROADMAP.md).

## Confidentialité et tiers

La bibliothèque musicale reste locale au téléphone, mais les **radios Internet** et leurs annuaires sont des services externes et peuvent recevoir des requêtes réseau. Android peut demander l'autorisation audio nécessaire à l'API `Visualizer` : cela n'ajoute pas d'enregistrement du microphone dans ce projet. Vérifiez les autorisations et la politique de confidentialité avant toute distribution commerciale.

## Licence

Code distribué sous **MIT** (voir [LICENSE](LICENSE)). Les marques, logos de radios, pochettes d'albums et autres contenus tiers ne sont pas automatiquement couverts par cette licence.

**Projet communautaire BY PHILOU / CYBERPUNK.**
