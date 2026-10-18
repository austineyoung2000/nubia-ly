# NX789J port integration

The community NX789J port is retained with two safety corrections before merging.

* Boot-animation players require an exact model/SDK/SHA-256 profile. Wildcard and
  unscoped entries are ignored. The original NX809J Android 16 hash stays pinned.
  The NX789J Android 15 hash is the one supplied by the port author; the NX789J
  Android 16 player supplied by the tester has now been statically inspected and
  its exact hash is included. On-device playback still requires tester confirmation. Other supported cosmetics
  are unaffected. The profile file is included in the packaged guard module.
* Root receipt reads have one five-second deadline covering both process exit
  and bounded output collection. A hung root prompt or a pipe held open after
  process exit cannot indefinitely block the calling guard worker. Cleanup runs
  independently so closing a blocked stream cannot extend the caller deadline.
  Receipt metadata, canonical path, size, boot identity and permit checks remain
  mandatory; transport failure grants no authority.

Regression tests execute the production transport against a stalled process and
an exited process with an open pipe, and check oversize/nonzero/interrupt behavior.
The production shell loader is exercised against exact matching profiles, wrong
model/SDK, wildcard/malformed entries and unsafe allowlist permissions, alongside
existing mount/rollback/restore and unchanged-stock tests.

Guard module version: 0.1.16 (21). Existing launcher support remains pinned to the
NX809J launcher APK and is not expanded to NX789J by this port.

## NX789J Android 16 player inspection (2026-10-18)

Tester archive: RM10-bootanimation-player.tar.gz. Firmware fingerprint:
`nubia/NX789J/NX789J:16/BQ2A.250705.001-BP2A.250605.031.A3/20260227.161747:user/release-keys`.
Model/device NX789J, SDK 36. Library 134776 bytes, SHA-256
`58b9eec99405c00242ca10cf9bb40a40b386c3c219f875b91ac28f1860dd1761`,
ELF build ID `3df83e520828f2950951ba8c4a706533`. Executable 52072 bytes, SHA-256
`e3a819e9e3d26e27c627700b9f82851a156c7c0db8ac8fce25d6ecb10c790efe`.
Both computed hashes match the supplied manifest. Proprietary binaries are not bundled.

AArch64 disassembly of findBootAnimationFile (0xf9cc) confirms the readable
custom-animation branch (access R_OK at 0xfbdc, selection at 0xfc24), normal
fallback construction, and later vendor selection through getAnimationFiles
(0x16a44, called at 0x10008). The vendor function chooses fans animation when
its initialized flag is set; otherwise it builds /system/media/ + the
persist.vendor.custom.variant.id + /bootanimation.zip for normal boot.
findBootAnimationFileInternal (0x1055c) selects the first readable candidate
and leaves the earlier selection unchanged when none is readable. This matches
the existing loader's supported normal/carrier path and conservative fallback
checks; custom, fans and userspace-reboot paths remain excluded.

This is static compatibility evidence, not a physical boot test. No stock
animation ZIP or live property snapshot was supplied. Runtime target existence,
canonical path and baseline checks remain mandatory; rollback is unchanged.
