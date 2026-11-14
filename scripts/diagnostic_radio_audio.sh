#!/usr/bin/env bash
set -euo pipefail
if ! command -v adb >/dev/null 2>&1; then
  echo 'ADB absent : sudo pacman -S android-tools' >&2
  exit 1
fi
if ! adb devices | grep -qE $'\tdevice$'; then
  echo 'Téléphone non autorisé : branchez l’USB, autorisez le débogage puis relancez.' >&2
  exit 1
fi
DEST="${HOME}/Téléchargements/harmonie_radio_audio_$(date '+%Y%m%d_%H%M%S').log"
mkdir -p "$(dirname "$DEST")"
adb logcat -d -v threadtime -s 'HarmonieNativeFX:V' 'HarmoniePlayback:V' 'HarmonieNative:V' '*:S' > "$DEST"
echo "Diagnostic enregistré : $DEST"
echo 'Testez EQ radio et 432 Hz puis relancez pour capturer le résultat.'
