"""Check real datagen output, including units, conditions, and repeated-run stability."""
import argparse
import hashlib
import json
from pathlib import Path

root = Path("src/generated/resources")
recipes = root / "data/create/recipe"
def read(name):
    return json.loads((recipes / name).read_text())

chocolate = read("mixing/chocolate.json")
milk = next(i for i in chocolate["ingredients"] if i.get("type") == "fluid_tag")
assert milk["amount"] == 20250, milk
assert chocolate["results"] == [{"amount": 20250, "id": {"fluid": "create:chocolate"}}], chocolate
mud = read("mixing/mud_by_mixing.json")
assert any(i.get("fabric:type") == "create:block_tag_ingredient" and
           i.get("tag") == "minecraft:convertable_to_mud" for i in mud["ingredients"]), mud
for path in recipes.rglob("*.json"):
    value = json.loads(path.read_text())
    if "/compat/" in path.as_posix():
        assert value.get("fabric:load_conditions"), f"Missing compat condition: {path}"
    assert "processingTime" not in value and "heatRequirement" not in value, path
assert (root / "data/create/loot_table/blocks/schematicannon.json").is_file()
assert (root / "data/create/tags/block/wrench_pickup.json").is_file()
files = {p.relative_to(root).as_posix(): hashlib.sha256(p.read_bytes()).hexdigest()
         for p in root.rglob("*") if p.is_file() and ".cache" not in p.parts}
parser = argparse.ArgumentParser()
parser.add_argument("--snapshot", type=Path)
parser.add_argument("--compare", type=Path)
args = parser.parse_args()
if args.snapshot:
    args.snapshot.write_text(json.dumps(files, sort_keys=True))
if args.compare:
    before = json.loads(args.compare.read_text())
    changed = sorted(k for k in set(before) | set(files) if before.get(k) != files.get(k))
    assert not changed, f"Non-deterministic generated resources: {changed}"
print(f"Validated {len(list(recipes.rglob('*.json')))} recipes and {len(files)} resources")
