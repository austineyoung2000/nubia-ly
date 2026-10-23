#!/system/bin/sh
# Single fixed namespace; never edit Vector internals, stock overlays or other modules.
set -eu
[ "$(id -u)" = 0 ] || { echo "STOP: root required"; exit 1; }
gp() { if [ -x /system/bin/getprop ]; then /system/bin/getprop "$1" 2>/dev/null; else getprop "$1" 2>/dev/null; fi; }
nx789j() { for p in ro.product.model ro.product.vendor.model ro.product.odm.model ro.product.system.model ro.product.product.model; do case "$(gp "$p")" in NX789J*|NX809J*) return 0 ;; esac; done; return 1; }
nx789j || { echo "STOP: NX789J or NX809J model only"; exit 1; }
case "$(gp ro.build.version.sdk)" in 35|36) ;; *) echo "STOP: Android 15 or 16 only"; exit 1 ;; esac
guard_dir=/data/local/tmp/nubia-ly-guard
state_dir=/data/adb/nubia_ly_guard
# Private durable enable identity; Action and uninstall remove only this fixed entry.
[ ! -L "$state_dir" ] || { echo "STOP: unsafe state directory"; exit 1; }
if [ -e "$state_dir" ]; then
    [ -d "$state_dir" ] && [ "$(stat -c %u:%a "$state_dir")" = 0:700 ] || exit 1
fi
command=${1:-status}
case "$command" in status|stop|probe|preview|clock|enable_clock|resume|resume_app) ;; *) echo "STOP: use status, stop, probe, preview or clock"; exit 1 ;; esac
# Stop the independent boot selection before touching cosmetic receipts.
boot_stop_failed=0
ui_stop_failed=0
if [ "$command" = stop ] && [ -f "${0%/*}/uictl.sh" ]; then
    sh "${0%/*}/uictl.sh" restore || ui_stop_failed=1
fi
if [ "$command" = stop ] && [ -f "${0%/*}/bootctl.sh" ]; then
    sh "${0%/*}/bootctl.sh" restore || boot_stop_failed=1
fi
# Refuse pre-existing symlinks and non-root/writable directories.
[ ! -L "$guard_dir" ] || { echo "STOP: unsafe guard directory"; exit 1; }
if [ ! -e "$guard_dir" ]; then
    [ "$command" != status ] || { echo "Guard absent; probe blocked"; exit 0; }
    (umask 022; mkdir "$guard_dir") || exit 1
fi
[ -d "$guard_dir" ] && [ "$(stat -c %u:%a "$guard_dir")" = 0:755 ] || {
    echo "STOP: guard directory must be root-owned mode 755"; exit 1;
}
case "$command" in
    stop)
        # unlink just our own fixed entry; never follow a permit symlink.
        [ ! -d "$guard_dir/probe" ] || { echo "STOP: unsafe probe entry"; exit 1; }
        [ ! -d "$guard_dir/preview" ] || { echo "STOP: unsafe preview entry"; exit 1; }
        [ ! -d "$guard_dir/clock" ] || { echo "STOP: unsafe clock entry"; exit 1; }
        [ ! -d "$guard_dir/enabled" ] || exit 1
        if [ -d "$state_dir" ]; then
            [ ! -d "$state_dir/clock_id" ] || exit 1
            rm -f "$state_dir/clock_id" || exit 1
        fi
        rm -f "$guard_dir/enabled" "$guard_dir/probe" "$guard_dir/preview" "$guard_dir/clock" || exit 1
        [ "$ui_stop_failed" = 0 ] || { echo "UI style restoration incomplete; retry Guard Action"; exit 1; }
        [ "$boot_stop_failed" = 0 ] || { echo "Cosmetic permits stopped; boot-animation stop unconfirmed. Retry Action."; exit 1; }
        echo "All nubia-ly permits stopped. Active preview restores on the next guard validation. Reboot discards in-memory overrides."
        ;;
    status)
        if [ -f "$guard_dir/enabled" ] && [ ! -L "$guard_dir/enabled" ]; then echo "Enabled clock authority present"; else echo "Clock authority disabled"; fi
        if [ -f "$guard_dir/probe" ] && [ ! -L "$guard_dir/probe" ]; then
            echo "Probe receipt present; app validates boot, expiry and permissions."
        else
            echo "No probe receipt; blocked"
        fi
        if [ -f "$guard_dir/preview" ] && [ ! -L "$guard_dir/preview" ]; then
            echo "Lifecycle permit present; app validates boot, expiry and permissions."
        else
            echo "No lifecycle permit; blocked"
        fi
        if [ -f "$guard_dir/clock" ] && [ ! -L "$guard_dir/clock" ]; then
            echo "Clock-color permit present; app validates boot, expiry and permissions."
        else
            echo "No clock-color permit; blocked"
        fi
        ;;
    resume_app)
        # A bounded boot-time nudge only. Never mint or replace enabled authority here.
        [ ! -e "${0%/*}/disable" ] && [ ! -e "${0%/*}/remove" ] || exit 0
        [ -f "$state_dir/clock_id" ] && [ ! -L "$state_dir/clock_id" ] || exit 0
        [ "$(stat -c %u:%a "$state_dir/clock_id")" = 0:600 ] || exit 1
        [ -f "$guard_dir/enabled" ] && [ ! -L "$guard_dir/enabled" ] || exit 0
        [ "$(stat -c %u:%a "$guard_dir/enabled")" = 0:644 ] || exit 1
        permit_id=$(cat "$state_dir/clock_id") || exit 1
        boot_id=$(cat /proc/sys/kernel/random/boot_id) || exit 1
        expected=$(printf 'nubia-ly-clock-enabled-v1\n%s\nclock-enabled\n%s' "$boot_id" "$permit_id")
        [ "$(cat "$guard_dir/enabled")" = "$expected" ] || exit 1
        case "$permit_id" in ""|*[!0-9a-f-]*) exit 1 ;; esac
        [ "${#permit_id}" = 36 ] || exit 1
        am broadcast --user 0 --include-stopped-packages --receiver-include-background \
            -a com.elitedarkkaiser.nubialy.RESUME_AFTER_UNLOCK \
            -n com.elitedarkkaiser.nubialy/.BootResumeReceiver >/dev/null 2>&1
        ;;
    enable_clock|resume)
        [ ! -e "${0%/*}/disable" ] && [ ! -e "${0%/*}/remove" ] || { echo "STOP: guard module disabled or pending removal"; exit 1; }
        if [ "$command" = enable_clock ]; then
            if [ ! -d "$state_dir" ]; then (umask 077; mkdir "$state_dir") || exit 1; fi
            [ ! -d "$state_dir/clock_id" ] || exit 1
            permit_id=$(cat /proc/sys/kernel/random/uuid) || exit 1
            temp=$(mktemp "$state_dir/.clock.XXXXXX") || exit 1
            trap 'rm -f "$temp"' EXIT HUP INT TERM
            printf '%s\n' "$permit_id" > "$temp" || exit 1
            chmod 600 "$temp"; mv -f "$temp" "$state_dir/clock_id" || exit 1
        else
            # Only previews are cleared on boot; durable enabled identity is explicit user state.
            for entry in probe preview clock; do [ ! -d "$guard_dir/$entry" ] || exit 1; done
            rm -f "$guard_dir/probe" "$guard_dir/preview" "$guard_dir/clock" || exit 1
            # A revoked durable identity must not be recreated on boot.
            rm -f "$guard_dir/enabled" || exit 1
            [ -f "$state_dir/clock_id" ] && [ ! -L "$state_dir/clock_id" ] || exit 0
            [ "$(stat -c %u:%a "$state_dir/clock_id")" = 0:600 ] || exit 1
            permit_id=$(cat "$state_dir/clock_id") || exit 1
        fi
        case "$permit_id" in ""|*[!0-9a-f-]*) exit 1 ;; esac
        [ "${#permit_id}" = 36 ] || exit 1
        boot_id=$(cat /proc/sys/kernel/random/boot_id) || exit 1
        case "$boot_id" in ""|*[!0-9a-f-]*) exit 1 ;; esac
        [ "${#boot_id}" = 36 ] || exit 1
        [ ! -d "$guard_dir/enabled" ] || exit 1
        temp=$(mktemp "$guard_dir/.enabled.XXXXXX") || exit 1
        trap 'rm -f "$temp"' EXIT HUP INT TERM
        printf 'nubia-ly-clock-enabled-v1\n%s\nclock-enabled\n%s\n' "$boot_id" "$permit_id" > "$temp" || exit 1
        chmod 644 "$temp"; mv -f "$temp" "$guard_dir/enabled" || exit 1
        echo "Clock enable authority ready. No expiry; app authorization required."
        if [ "$command" = resume ]; then
            nonce=$(cat /proc/sys/kernel/random/uuid) || exit 1
            read -r uptime ignored < /proc/uptime || exit 1
            deadline=$(( (${uptime%%.*} + 8) * 1000 ))
            am broadcast -a com.elitedarkkaiser.nubialy.PREVIEW_LIFECYCLE -p com.android.systemui \
                --es operation clock_resume --es nonce "$nonce" --el request_deadline_ms "$deadline" >/dev/null 2>&1 || true
        fi
        ;;
    probe|preview|clock)
        entry=$command
        # Remove previous receipt before trying anything; failures must leave no permit.
        [ ! -d "$guard_dir/$entry" ] || { echo "STOP: unsafe permit entry"; exit 1; }
        rm -f "$guard_dir/$entry" || exit 1
        boot_id=$(cat /proc/sys/kernel/random/boot_id) || exit 1
        case "$boot_id" in ""|*[!0-9a-f-]*) echo "STOP: invalid boot ID"; exit 1 ;; esac
        [ "${#boot_id}" = 36 ] || exit 1
        read -r uptime ignored < /proc/uptime || exit 1
        seconds=${uptime%%.*}
        case "$seconds" in ""|*[!0-9]*) exit 1 ;; esac
        ttl=60
        [ "$entry" != clock ] || ttl=30
        expires=$((seconds + ttl))
        temp=$(mktemp "$guard_dir/.probe.XXXXXX") || exit 1
        trap 'rm -f "$temp"' EXIT HUP INT TERM
        if [ "$entry" = preview ] || [ "$entry" = clock ]; then
            permit_id=$(cat /proc/sys/kernel/random/uuid) || exit 1
            case "$permit_id" in ""|*[!0-9a-f-]*) exit 1 ;; esac
            [ "${#permit_id}" = 36 ] || exit 1
            header=nubia-ly-preview-v1; purpose=lifecycle-only
            if [ "$entry" = clock ]; then header=nubia-ly-clock-v1; purpose=clock-color-only; fi
            printf '%s\n%s\n%s\n%s\n%s\n' "$header" "$boot_id" "$expires" "$purpose" "$permit_id" > "$temp" || exit 1
        else
            printf 'nubia-ly-probe-v1\n%s\n%s\nprobe-only\n' "$boot_id" "$expires" > "$temp" || exit 1
        fi
        chmod 0644 "$temp" || exit 1
        mv -f "$temp" "$guard_dir/$entry" || exit 1
        echo "$ttl-second $entry permit published. Explicit app Start required; no automatic preview."
        ;;
esac
