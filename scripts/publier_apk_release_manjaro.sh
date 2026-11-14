#!/usr/bin/env bash
# Publie uniquement un APK compile sur cette machine.
# La signature debug peut differer de celle d'une installation precedente.
set -Eeuo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
REPO="philippe015/Harmonie"
TAG="v3.5.20"
ASSET="Harmonie-v3.5.20-android-arm64-debug-manjaro.apk"
APK="app/build/outputs/apk/debug/app-debug.apk"
fail() { printf '[ERREUR] %s\n' "$*" >&2; exit 1; }
command -v gh >/dev/null 2>&1 || fail "Installer GitHub CLI : sudo pacman -S github-cli"
gh auth status >/dev/null 2>&1 || fail "Se connecter : gh auth login"
USER="$(gh api user --jq '.login')" || fail "Impossible de verifier le compte GitHub"
[[ "$USER" == "philippe015" ]] || fail "Compte GitHub : $USER (attendu : philippe015)"
[[ -f app/build.gradle ]] || fail "Fichier app/build.gradle manquant"
grep -Eq "versionName[[:space:]]+['\"]3[.]5[.]20['\"]" app/build.gradle || fail "Ce projet n'est pas la version 3.5.20"
[[ -f CHANGELOG_V3_5_20.md ]] || fail "Notes de version absentes"
echo "[1/3] Compilation de l'APK"
bash compiler_manjaro.sh
[[ -s "$APK" ]] || fail "APK absent apres compilation"
unzip -t "$APK" >/dev/null || fail "APK ZIP invalide"
echo "[2/3] Preparation de l'APK"
mkdir -p distribution
cp "$APK" "distribution/$ASSET"
sha256sum "distribution/$ASSET" > "distribution/$ASSET.sha256"
cat "distribution/$ASSET.sha256"
echo "[3/3] Publication sur GitHub Releases"
if ! gh release view "$TAG" --repo "$REPO" >/dev/null 2>&1; then
  gh release create "$TAG" --repo "$REPO" \
    --title "Harmonie v3.5.20 — Hi-Fi Audio PRO (APK de test)" \
    --notes-file CHANGELOG_V3_5_20.md --prerelease
fi
gh release upload "$TAG" "distribution/$ASSET" "distribution/$ASSET.sha256" \
  --repo "$REPO" --clobber
echo "Publication : https://github.com/$REPO/releases/tag/$TAG"
echo "Attention : sauvegarder les donnees avant toute desinstallation (signature debug)."
