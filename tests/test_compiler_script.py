"""Vérifie le problème Cargo-ndk signalé sur Manjaro."""
import pathlib
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[1]


class BuildScriptTests(unittest.TestCase):
    def test_cargo_ndk_started_from_manifest_directory(self):
        code = (ROOT / "scripts" / "compiler_manjaro.sh").read_text()
        self.assertIn('cd "$PROJECT/rust-core"', code)
        self.assertIn('cargo ndk -t arm64-v8a -o "$PROJECT/app/src/main/jniLibs" build --release', code)
        self.assertNotIn('build --release --manifest-path rust-core/Cargo.toml', code)

    def test_manifest_exists(self):
        self.assertTrue((ROOT / "rust-core" / "Cargo.toml").is_file())


if __name__ == "__main__":
    unittest.main()
