"""Install exact 3.3.6 recipe/tag resources for compatibility checks without development-remapping its class tweaker."""
import hashlib
from pathlib import Path
import urllib.request
import zipfile

target = Path("build/integration-fixtures/farmersdelight-3.3.6.jar")
target.parent.mkdir(parents=True, exist_ok=True)
urllib.request.urlretrieve("https://cdn.modrinth.com/data/7vxePowz/versions/wbVXT4Ua/FarmersDelight-1.21.1-3.3.6%2Brefabricated.jar", target)
assert hashlib.sha1(target.read_bytes()).hexdigest() == "6d75a472d56c7cf0e9abc7e654861076e8eff389"
with zipfile.ZipFile(target) as archive:
    names = ["data/farmersdelight/recipe/integration/create/filling/chocolate_pie.json",
             "data/c/tags/fluid/chocolate.json"]
    for name in names:
        path = Path("run/server/world/datapacks/probe_high") / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(archive.read(name))
