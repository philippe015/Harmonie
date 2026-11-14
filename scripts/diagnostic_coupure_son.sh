#!/usr/bin/env bash
set -euo pipefail
if ! command -v adb >/dev/null 2>&1; then echo 'adb absent : installez android-tools avec pacman'; exit 1; fi
if ! adb get-state 2>/dev/null | grep -qx device; then echo 'Branchez le téléphone en USB, activez le débogage USB et acceptez la connexion.'; exit 1; fi
OUT="${HOME}/Téléchargements/harmonie_audio_$(date +%Y%m%d_%H%M%S).log"
echo "Journaux Harmonie vers $OUT"
echo 'Lancez la lecture puis reproduisez la coupure. Ctrl+C pour arrêter.'
adb logcat -v time -s HarmoniePlayback:V HarmonieNative:V ExoPlayerImpl:W AudioTrack:W AudioFlinger:W AudioPolicyManager:W MediaCodec:W '*:S' | tee "$OUT"
