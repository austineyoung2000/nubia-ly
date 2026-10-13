#!/system/bin/sh
MODDIR=${0%/*}
exec sh "$MODDIR/guardctl.sh" stop
