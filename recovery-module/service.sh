#!/system/bin/sh
MODDIR=${0%/*}
# Only ephemeral test receipts expire on boot; durable enabled identity is explicit user state.
sh "$MODDIR/guardctl.sh" resume

# Boot success is independent of app/CE unlock and of cosmetic enable authority.
(
    attempts=0
    while [ "$attempts" -lt 60 ]; do
        [ ! -e "$MODDIR/disable" ] && [ ! -e "$MODDIR/remove" ] || exit 0
        if [ "$(getprop sys.boot_completed)" = 1 ]; then
            sh "$MODDIR/bootctl.sh" confirm
            exit 0
        fi
        attempts=$((attempts + 1)); sleep 2
    done
    # Conservative rollback for the next boot; never force a reboot or unmount live files.
    sh "$MODDIR/bootctl.sh" restore
) >/dev/null 2>&1 &

# CE preferences become available after first unlock. BOOT_COMPLETED reaches the app
# then; bounded nudges also wake a previously force-stopped app, without an Activity.
[ -f /data/adb/nubia_ly_guard/clock_id ] || exit 0
(
    attempts=0
    while [ "$attempts" -lt 180 ]; do
        [ ! -e "$MODDIR/disable" ] && [ ! -e "$MODDIR/remove" ] || exit 0
        [ -f /data/adb/nubia_ly_guard/clock_id ] || exit 0
        if [ "$(getprop sys.boot_completed)" = 1 ] && [ "$(am get-started-user-state 0 2>/dev/null)" = RUNNING_UNLOCKED ]; then
            # Three bounded nudges cover injected services becoming ready after unlock.
            nudge=0
            while [ "$nudge" -lt 3 ]; do
                [ ! -e "$MODDIR/disable" ] && [ ! -e "$MODDIR/remove" ] || exit 0
                [ -f /data/adb/nubia_ly_guard/clock_id ] || exit 0
                sh "$MODDIR/guardctl.sh" resume_app
                nudge=$((nudge + 1))
                [ "$nudge" -ge 3 ] || sleep 10
            done
            exit 0
        fi
        attempts=$((attempts + 1))
        sleep 10
    done
) >/dev/null 2>&1 &
