from pathlib import Path
import re

ROOT=Path(__file__).resolve().parents[1]

def test_executor_no_androidx_core_requirement():
    java=(ROOT/'app/src/main/java/fr/byphilou/harmonie/MainActivity.java').read_text()
    assert 'ContextCompat' not in java
    assert 'mainHandler.post(command)' in java
    assert 'controllerFuture.addListener' in java

def test_gradle_version_and_native_queue():
    gradle=(ROOT/'app/build.gradle').read_text()
    assert "versionName '3.5.20'" in gradle
    assert 'versionCode 27' in gradle
    assert 'compileSdk 36' in gradle
    activity=(ROOT/'app/src/main/java/fr/byphilou/harmonie/MainActivity.java').read_text()
    assert 'setMediaItems(items,index,0)' in activity
    assert '432f/440f' in activity
