#!/usr/bin/env bash
set -euo pipefail
if ! command -v adb >/dev/null 2>&1; then
  echo 'adb absent. Sur Manjaro : sudo pacman -S android-tools'
  exit 1
fi
adb get-state >/dev/null
OUT="${HOME}/Téléchargements/harmonie_hifi_native_$(date +%Y%m%d_%H%M%S).log"
{
  echo '=== Harmonie : effets système ==='
  echo 'Copiez aussi les lignes du volet « Diagnostic / son coupé ».'
  echo '==='; adb shell dumpsys media.audio_flinger 2>&1 | grep -Ei -C 3 'effect|session|reverb|virtual|dynamics' | head -250 || true
  echo '=== Erreurs et messages Harmonie ==='
  adb logcat -d -v time 2>&1 | grep -Ei 'HarmonieAmbience|HarmonieNativeFX|AudioTrack|AudioEffect|Visualizer' | tail -180 || true
} > "$OUT"
echo "Journal enregistré : $OUT"
