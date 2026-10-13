#!/system/bin/sh
# Single fixed namespace; never edit Vector internals, stock overlays or other modules.
set -eu
[ "$(id -u)" = 0 ] || { echo "STOP: root required"; exit 1; }
[ "$(getprop ro.product.model)" = NX809J ] || { echo "STOP: NX809J only"; exit 1; }
[ "$(getprop ro.build.version.sdk)" = 36 ] || { echo "STOP: Android 16 only"; exit 1; }
guard_dir=/data/local/tmp/nubia-ly-guard
command=${1:-status}
case "$command" in status|stop|probe|preview) ;; *) echo "STOP: use status, stop, probe or preview"; exit 1 ;; esac
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
        rm -f "$guard_dir/probe" "$guard_dir/preview" || exit 1
        echo "Probe and lifecycle permit stopped. No customization enabled. Reboot to discard any future in-memory hooks."
        ;;
    status)
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
        ;;
    probe|preview)
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
        expires=$((seconds + 60))
        temp=$(mktemp "$guard_dir/.probe.XXXXXX") || exit 1
        trap 'rm -f "$temp"' EXIT HUP INT TERM
        if [ "$entry" = preview ]; then
            permit_id=$(cat /proc/sys/kernel/random/uuid) || exit 1
            case "$permit_id" in ""|*[!0-9a-f-]*) exit 1 ;; esac
            [ "${#permit_id}" = 36 ] || exit 1
            printf 'nubia-ly-preview-v1\n%s\n%s\nlifecycle-only\n%s\n' "$boot_id" "$expires" "$permit_id" > "$temp" || exit 1
        else
            printf 'nubia-ly-probe-v1\n%s\n%s\nprobe-only\n' "$boot_id" "$expires" > "$temp" || exit 1
        fi
        chmod 0644 "$temp" || exit 1
        mv -f "$temp" "$guard_dir/$entry" || exit 1
        echo "60-second $entry permit published. No cosmetic permission granted."
        ;;
esac
