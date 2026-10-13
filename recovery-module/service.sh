#!/system/bin/sh
MODDIR=${0%/*}
# Only ephemeral test receipts expire on boot; durable enabled identity is explicit user state.
sh "$MODDIR/guardctl.sh" resume
