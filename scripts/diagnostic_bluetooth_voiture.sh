#!/usr/bin/env bash
set -euo pipefail
if ! command -v adb >/dev/null 2>&1; then
  echo 'adb absent : installez android-tools sur Manjaro (sudo pacman -S android-tools)'
  exit 1
fi
adb devices
printf '
--- Session Harmonie ---
'
adb shell dumpsys media_session | grep -i -E -C 5 'fr.byphilou.harmonie|Harmonie' | head -100 || true
printf '
--- Informations audio Bluetooth ---
'
adb shell dumpsys audio | grep -i -E -m 25 'a2dp|bluetooth|media session' || true
printf '
--- Test en stationnement ---
'
echo 'Lancez un morceau natif dans Harmonie. Pour simuler les touches :'
echo '  adb shell input keyevent 85  # Play/Pause'
echo '  adb shell input keyevent 87  # Suivant'
echo '  adb shell input keyevent 88  # Précédent'
echo 'La touche peut être reçue par une autre application si Harmonie n’est pas la session active.'
