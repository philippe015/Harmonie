#!/usr/bin/env bash
set -Eeuo pipefail
cd "$(dirname "$0")/.."
PROJECT="$PWD"
export PATH="$HOME/.cargo/bin:$PATH"

fail(){ echo "[ERREUR] $*" >&2; exit 1; }
command -v java >/dev/null || fail "Installer jdk17-openjdk (sudo pacman -S jdk17-openjdk)"
command -v python3.14 >/dev/null || fail "Python 3.14 requis sur Manjaro pour Chaquopy : installer/activer python3.14"
command -v cargo >/dev/null || fail "Rust/Cargo absent : installer rustup puis rustup default stable"
command -v rustup >/dev/null || fail "rustup absent : sudo pacman -S rustup ; rustup default stable"

if [[ -d /usr/lib/jvm/java-17-openjdk ]]; then export JAVA_HOME=/usr/lib/jvm/java-17-openjdk; export PATH="$JAVA_HOME/bin:$PATH"; fi
export ANDROID_HOME="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}}"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
[[ -d "$ANDROID_HOME" ]] || fail "Android SDK absent. Installer Android Studio, SDK Platform 36 et NDK 27.2.12479018 dans $ANDROID_HOME"

SDKMANAGER="$(find "$ANDROID_HOME/cmdline-tools" -name sdkmanager -type f 2>/dev/null | head -n 1 || true)"
if [[ -n "$SDKMANAGER" ]]; then
  echo 'Installation/vérification des outils Android…'
  "$SDKMANAGER" --sdk_root="$ANDROID_HOME" 'platforms;android-36' 'build-tools;35.0.0' 'ndk;27.2.12479018' || fail "Erreur SDK Manager : accepter les licences avec sdkmanager --licenses"
fi
export ANDROID_NDK_HOME="${ANDROID_NDK_HOME:-$ANDROID_HOME/ndk/27.2.12479018}"
[[ -f "$ANDROID_NDK_HOME/build/cmake/android.toolchain.cmake" ]] || fail "NDK absent dans $ANDROID_NDK_HOME"
[[ -f "$ANDROID_HOME/platforms/android-36/android.jar" ]] || fail "Android API 36 introuvable. Installer : sdkmanager 'platforms;android-36'"

rustup target add aarch64-linux-android
if ! command -v cargo-ndk >/dev/null 2>&1; then
  echo 'Installation cargo-ndk…'; cargo install cargo-ndk --locked
fi

echo '[VÉRIFICATION] Tests Rust sur Manjaro avant Android'
(
  cd "$PROJECT/rust-core"
  cargo test --lib
) || fail "Code Rust invalide : corriger le code dans rust-core/src/lib.rs"

mkdir -p "$PROJECT/app/src/main/jniLibs"
echo '[1/3] Compilation Rust pour Android arm64-v8a'
# cargo-ndk (v4+) commence par `cargo metadata` dans son répertoire courant.
# `--manifest-path rust-core/Cargo.toml` transmis à `build` ne suffit pas :
# il faut lancer cargo-ndk DEPUIS le dossier qui contient Cargo.toml.
[[ -f "$PROJECT/rust-core/Cargo.toml" ]] || fail "Cargo.toml absent dans rust-core/ (archive incomplète ?)"
(
  cd "$PROJECT/rust-core"
  cargo ndk -t arm64-v8a -o "$PROJECT/app/src/main/jniLibs" build --release
) || fail "Compilation Rust échouée : voir les dernières lignes ci-dessus"
[[ -s "$PROJECT/app/src/main/jniLibs/arm64-v8a/libharmonie_core.so" ]] || fail "Bibliothèque Rust non générée"

echo '[2/3] Préparation Gradle 8.11.1 (AGP 8.10.1 / Android API 36)'
GRADLE_DIR="$HOME/.cache/harmonie-tools/gradle-8.11.1"
if command -v gradle >/dev/null 2>&1 && gradle --version 2>/dev/null | grep -qE "^Gradle 8\.11\.1$"; then
  GRADLE=gradle
else
  if [[ ! -x "$GRADLE_DIR/bin/gradle" ]]; then
    mkdir -p "$(dirname "$GRADLE_DIR")"
    archive="$HOME/.cache/harmonie-tools/gradle-8.11.1-bin.zip"
    if [[ ! -f "$archive" ]]; then
      command -v curl >/dev/null || fail 'Installer curl'
      curl -fL --retry 3 'https://services.gradle.org/distributions/gradle-8.11.1-bin.zip' -o "$archive"
    fi
    unzip -oq "$archive" -d "$(dirname "$GRADLE_DIR")" || fail "Archive Gradle incorrecte. Supprimer $archive puis réessayer"
  fi
  GRADLE="$GRADLE_DIR/bin/gradle"
fi

printf '\n[3/3] Construction APK Android (Gradle + Chaquopy)\n'
"$GRADLE" --no-daemon --stacktrace assembleDebug
APK="$PROJECT/app/build/outputs/apk/debug/app-debug.apk"
[[ -s "$APK" ]] || fail "Gradle n'a pas généré $APK"
echo "APK généré : $APK"
command -v adb >/dev/null 2>&1 && echo 'Installation téléphone (USB) : adb install -r app/build/outputs/apk/debug/app-debug.apk'
