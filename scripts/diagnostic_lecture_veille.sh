#!/usr/bin/env bash
set -euo pipefail
if ! command -v adb >/dev/null 2>&1; then
  echo "adb introuvable. Sur Manjaro : sudo pacman -S android-tools" >&2
  exit 1
fi
if ! adb get-state 2>/dev/null | grep -qx device; then
  echo "Téléphone non détecté par adb. Activer le débogage USB et autoriser cet ordinateur." >&2
  exit 1
fi
out="${1:-$HOME/Téléchargements/harmonie_lecture_veille.log}"
adb logcat -d -v time -s 'HarmoniePlayback:V' 'HarmonieNative:V' 'AndroidRuntime:E' '*:S' > "$out"
echo "Journal : $out"
echo 'Transitions et file Media3 :'
grep -E 'Transition automatique|Lecture Media3:|fin file index=|lecture continue native|Reprise complète|service détruit|timeline:|Erreur de décodage|FATAL EXCEPTION' "$out" | tail -n 50 || true
