from pathlib import Path
from zipfile import ZipFile, ZipInfo, ZIP_DEFLATED
root = Path(__file__).resolve().parents[1]
files = ["player_sha", "module.prop", "skip_mount", "bootctl.sh", "post-fs-data.sh", "customize.sh", "guardctl.sh", "action.sh", "service.sh", "uninstall.sh"]
out = root / "build" / "nubia-ly-guard-test.zip"
out.parent.mkdir(exist_ok=True)
with ZipFile(out, "w", ZIP_DEFLATED) as archive:
    for name in files:
        item = ZipInfo(name)
        item.create_system = 3
        item.external_attr = ((0o100755 if name.endswith(".sh") else 0o100644) << 16)
        archive.writestr(item, (root / "recovery-module" / name).read_bytes(), compress_type=ZIP_DEFLATED)
print(out)
