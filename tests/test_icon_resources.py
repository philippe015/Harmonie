from pathlib import Path
from xml.etree import ElementTree as ET
from PIL import Image

root = Path(__file__).resolve().parents[1]
res = root / "app/src/main/res"
manifest = ET.parse(root / "app/src/main/AndroidManifest.xml").getroot()
app = manifest.find("application")
ns = "{http://schemas.android.com/apk/res/android}"
assert app.attrib[ns + "icon"] == "@mipmap/ic_launcher"
assert app.attrib[ns + "roundIcon"] == "@mipmap/ic_launcher_round"
for density, size in [("mdpi",48),("hdpi",72),("xhdpi",96),("xxhdpi",144),("xxxhdpi",192)]:
    for name in ("ic_launcher.png", "ic_launcher_round.png"):
        with Image.open(res / ("mipmap-" + density) / name) as im:
            assert im.size == (size,size)
for name in ("ic_launcher.xml", "ic_launcher_round.xml"):
    r = ET.parse(res / "mipmap-anydpi-v26" / name).getroot()
    assert r.tag == "adaptive-icon"
for name in ("ic_launcher_foreground.png", "ic_launcher_monochrome.png"):
    with Image.open(res / "drawable-nodpi" / name) as im:
        assert im.size == (432,432) and im.mode == "RGBA"
print("Icônes Android, ressources et manifeste : OK")
