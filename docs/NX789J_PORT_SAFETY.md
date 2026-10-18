# NX789J port integration

The community NX789J port is retained with two safety corrections before merging.

* Boot-animation players require an exact model/SDK/SHA-256 profile. Wildcard and
  unscoped entries are ignored. The original NX809J Android 16 hash stays pinned.
  The NX789J Android 15 hash is the one supplied by the port author; an NX789J
  Android 16 player hash has not been supplied, so that optional boot-animation
  path remains blocked until its player is inspected. Other supported cosmetics
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
