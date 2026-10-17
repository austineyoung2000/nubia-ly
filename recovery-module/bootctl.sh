#!/system/bin/sh
# One systemless file bind only. Never write /system, remount partitions or mount directories.
set -eu
MODDIR=${0%/*}
state_dir=/data/adb/nubia_ly_guard
boot_dir=$state_dir/boot
player=/system/lib64/libbootanimation.so
target=/system/media/bootanimation.zip
# Filled only after inspecting the owner's exact player and stock archive.
player_sha=8a254699242729e1af694850415b60a8c98201fae8f3aec5c17ff188d2e12d55
[ "$(id -u)" = 0 ] || exit 1
gp() { if [ -x /system/bin/getprop ]; then /system/bin/getprop "$1" 2>/dev/null; else getprop "$1" 2>/dev/null; fi; }
nx789j() { for p in ro.product.model ro.product.vendor.model ro.product.odm.model ro.product.system.model ro.product.product.model; do case "$(gp "$p")" in NX789J*|NX809J*) return 0 ;; esac; done; return 1; }
nx789j || exit 1
case "$(gp ro.build.version.sdk)" in 35|36) ;; *) exit 1 ;; esac
command=${1:-status}
case "$command" in status|restore|mount|confirm|stage_rising|stage_cyberpunk|stage_valorant|stage_google|stage_google_monet) ;; *) exit 1 ;; esac
safe_dir() { [ ! -L "$1" ] && [ -d "$1" ] && [ "$(stat -c %u:%a "$1")" = 0:700 ]; }
safe_file() { [ ! -L "$1" ] && [ -f "$1" ] && [ "$(stat -c %u:%a "$1")" = "0:$2" ]; }
[ ! -L "$state_dir" ] && [ ! -L "$boot_dir" ] || exit 1
if [ -e "$state_dir" ]; then safe_dir "$state_dir" || exit 1; fi
if [ -e "$boot_dir" ]; then safe_dir "$boot_dir" || exit 1; fi
identity() {
    case "$1" in
        rising) bytes=10068709; hash=79e5c5ec55373224dd935ca77102e3d474f4f93fb8b194c48915f18c0ffb2e92 ;;
        cyberpunk) bytes=23773485; hash=7187c3ac7d8319ab160084a73d247f69daf59d49ab230eba7cec405882191514 ;;
        valorant) bytes=7223104; hash=24ac9b2cb306a3946f84af28fcd848313b127152d6e4cbf36432d00225d83104 ;;
        google) bytes=8404191; hash=c8a96eac5fb156784bcdf53e981bcd1074b38a8ac85396b6082980a12e6a4b28 ;;
        google_monet) bytes=3725631; hash=051ba247fbbf9d152edb6ff9f0ef19f3af6a231739cb7f461e5363a7fa12f8d7 ;;
        *) return 1 ;;
    esac
}
safe_target_path() {
    case "$1" in
        /system/media/bootanimation.zip) return 0 ;;
        /system/media/*/bootanimation.zip)
            path_variant=${1#/system/media/}; path_variant=${path_variant%/bootanimation.zip}
            case "$path_variant" in ""|*[!A-Za-z0-9_-]*) return 1 ;; esac
            [ "${#path_variant}" -le 32 ]; return $? ;;
        *) return 1 ;;
    esac
}
profile() {
    [ ! -e "$MODDIR/disable" ] && [ ! -e "$MODDIR/remove" ] && [ -d /data/adb/ksu ] || return 1
    [ -f "$player" ] && [ ! -L "$player" ] || return 1
    live_sha=$(sha256sum "/proc/1/root$player" | cut -d ' ' -f 1)
    sha_ok=false
    [ "$live_sha" = "$player_sha" ] && sha_ok=true
    if [ "$sha_ok" = false ] && [ -f "$MODDIR/player_sha" ] && [ ! -L "$MODDIR/player_sha" ]; then
        while IFS= read -r allow; do
            allow=$(printf '%s' "$allow" | tr -d ' \t\r')
            case "$allow" in ""|\#*) continue ;; esac
            case "$allow" in ANY|any) sha_ok=true; break ;; esac
            [ "$live_sha" = "$allow" ] && { sha_ok=true; break; }
        done < "$MODDIR/player_sha"
    fi
    [ "$sha_ok" = true ] || return 1
    # Inspected findBootAnimationFile: readable custom path wins, then fans mode,
    # then /system/media/<persist.vendor.custom.variant.id>/bootanimation.zip.
    # Support only the normal/carrier branch, never arbitrary custom/ROM property paths.
    custom=$(getprop persist.sys.customanim.boot)
    if [ -n "$custom" ] && [ -f "$custom" ]; then return 1; fi
    case "$(getprop persist.vendor.sys.fansanim.boot)" in ""|0) ;; *) return 1 ;; esac
    case "$(getprop sys.init.userspace_reboot.in_progress)" in ""|0|false) ;; *) return 1 ;; esac
    variant=$(getprop persist.vendor.custom.variant.id)
    case "$variant" in *[!A-Za-z0-9_-]*) return 1 ;; esac
    [ "${#variant}" -le 32 ] || return 1
    target=/system/media/bootanimation.zip
    if [ -n "$variant" ]; then target=/system/media/$variant/bootanimation.zip; fi
    if [ ! -e "$target" ] && [ ! -L "$target" ]; then
        [ ! -e "/proc/1/root$target" ] && [ ! -L "/proc/1/root$target" ] || return 1
        # The inspected player falls through when the carrier candidate is absent.
        # Support only the verified default fallback, with no competing archive.
        [ -z "$(getprop ro.product.bootanim.file)" ] || return 1
        case "$(getprop ro.boot.theme)" in ""|0) ;; *) return 1 ;; esac
        for fallback_dir in /apex/com.android.bootanimation/etc /oem/media /product/media /system/media; do
            for fallback_name in bootanimation.zip bootanimation-dark.zip; do
                candidate=$fallback_dir/$fallback_name
                [ "$candidate" = /system/media/bootanimation.zip ] && continue
                [ ! -e "$candidate" ] && [ ! -L "$candidate" ] && [ ! -e "/proc/1/root$candidate" ] || return 1
            done
        done
        target=/system/media/bootanimation.zip
    fi
    # Persisted vendor properties may load after post-fs-data. Use the stage-time
    # verified target when early boot has no variant value yet.
    if [ "$command" = mount ] && [ -z "$variant" ] && safe_file "$boot_dir/baseline" 600; then
        read -r baseline_path baseline_hash < "$boot_dir/baseline"
        target=$baseline_path
        case "$target" in
            /system/media/bootanimation.zip) ;;
            /system/media/*/bootanimation.zip)
                target_variant=${target#/system/media/}; target_variant=${target_variant%/bootanimation.zip}
                case "$target_variant" in ""|*[!A-Za-z0-9_-]*) return 1 ;; esac
                [ "${#target_variant}" -le 32 ] || return 1 ;;
            *) return 1 ;;
        esac
    fi
    safe_target_path "$target" || return 1
    [ -f "$target" ] && [ -f "/proc/1/root$target" ] && [ ! -L "$target" ] && [ "$(readlink -f "$target")" = "$target" ] || return 1

}
boot_id=$(cat /proc/sys/kernel/random/boot_id)
case "$boot_id" in ""|*[!0-9a-f-]*) exit 1 ;; esac
[ "${#boot_id}" = 36 ] || exit 1
selected=stock
if [ "$command" != restore ] && [ -e "$boot_dir/selection" ]; then safe_file "$boot_dir/selection" 600 || exit 1; selected=$(cat "$boot_dir/selection"); identity "$selected" || exit 1; fi
if [ "$command" = status ]; then
    compatible=blocked; if profile; then compatible=ready; fi
    mounted=stock
    if safe_file "$boot_dir/mounted" 600; then
        read -r receipt_boot receipt_id receipt_inode receipt_target < "$boot_dir/mounted"
        if ! safe_target_path "$receipt_target"; then receipt_target=$target; fi
        if [ "$receipt_boot" = "$boot_id" ] && [ "$(stat -c %d:%i "/proc/1/root$receipt_target" 2>/dev/null)" = "$receipt_inode" ]; then mounted=$receipt_id; fi
    fi
    result=none
    if safe_file "$boot_dir/result" 600; then result=$(cat "$boot_dir/result"); fi
    case "$result" in none|staged|restored|mounted|confirmed|mount_failed|rolled_back|unverified) ;; *) result=unknown ;; esac
    printf 'nubia-ly-boot-v1\nprofile=%s\nselection=%s\nmounted=%s\nresult=%s\n' "$compatible" "$selected" "$mounted" "$result"
    exit 0
fi
if [ "$command" = restore ] && [ ! -d "$boot_dir" ]; then exit 0; fi
if [ "$command" != restore ]; then profile || { echo 'Boot animation blocked: player/profile or module state is not verified.'; exit 1; }; fi
if [ ! -d "$state_dir" ]; then (umask 077; mkdir "$state_dir"); fi
if [ ! -d "$boot_dir" ]; then (umask 077; mkdir "$boot_dir"); fi
# Remove only our fixed lock marker from a prior boot. Unknown lock contents fail closed.
if [ "$command" = mount ] && [ "$(getprop sys.boot_completed)" != 1 ] && [ -d "$boot_dir/lock" ] && [ ! -L "$boot_dir/lock" ]; then
    if safe_file "$boot_dir/lock/boot" 600 && [ "$(cat "$boot_dir/lock/boot")" != "$boot_id" ]; then rm -f "$boot_dir/lock/boot"; fi
    if [ ! -f "$boot_dir/lock/boot" ]; then rm -f "$boot_dir/lock/pid"; fi
    rmdir "$boot_dir/lock" 2>/dev/null || true
fi
[ ! -L "$boot_dir/lock" ] || exit 1
if [ -e "$boot_dir/lock" ]; then safe_dir "$boot_dir/lock" || exit 1; fi
if safe_file "$boot_dir/lock/pid" 600; then
    lock_pid=$(cat "$boot_dir/lock/pid")
    case "$lock_pid" in ""|*[!0-9]*) exit 1 ;; esac
    [ "${#lock_pid}" -le 10 ] || exit 1
    if ! kill -0 "$lock_pid" 2>/dev/null; then rm -f "$boot_dir/lock/pid" "$boot_dir/lock/boot"; rmdir "$boot_dir/lock" 2>/dev/null || true; fi
fi
(umask 077; mkdir "$boot_dir/lock") || { echo 'Boot animation operation busy; stop was not confirmed. Retry status/restore.'; exit 1; }
printf '%s\n' "$boot_id" > "$boot_dir/lock/boot"; chmod 600 "$boot_dir/lock/boot"
printf '%s\n' "$$" > "$boot_dir/lock/pid"; chmod 600 "$boot_dir/lock/pid"
temp=
asset_temp=
trap 'if [ -n "$temp" ]; then rm -f "$temp"; fi; if [ -n "$asset_temp" ]; then rm -f "$asset_temp"; fi; rm -f "$boot_dir/lock/boot" "$boot_dir/lock/pid"; rmdir "$boot_dir/lock" 2>/dev/null || true' EXIT HUP INT TERM
if [ "$command" = restore ]; then
    for entry in selection attempt payload.zip; do [ ! -d "$boot_dir/$entry" ] || exit 1; done
    rm -f "$boot_dir/selection" "$boot_dir/attempt" "$boot_dir/payload.zip"
    [ ! -d "$boot_dir/result" ] || exit 1
    temp=$(mktemp "$boot_dir/.boot.XXXXXX"); printf 'restored\n' > "$temp"; chmod 600 "$temp"; mv -f "$temp" "$boot_dir/result"; temp=
    echo 'Stock selected for next boot. Existing bind, if any, disappears on reboot.'
    exit 0
fi
publish() { [ ! -d "$boot_dir/$1" ] || return 1; temp=$(mktemp "$boot_dir/.boot.XXXXXX"); printf '%s\n' "$2" > "$temp"; chmod 600 "$temp"; mv -f "$temp" "$boot_dir/$1"; temp=; }
deadline_ok() {
    case "${2:-}" in ""|*[!0-9]*) return 1 ;; esac
    # App passes a bounded monotonic deadline in whole seconds, never a shell expression.
    [ "${#2}" -le 10 ] || return 1
    read -r uptime ignored < /proc/uptime
    [ "$2" -gt "${uptime%%.*}" ] && [ "$2" -le "$(( ${uptime%%.*} + 20 ))" ]
}
case "$command" in
    stage_*)
        deadline_ok "$@" || exit 1
        stage_target=$target
        choice=${command#stage_}; identity "$choice"
        temp=$(mktemp "$boot_dir/.boot.XXXXXX")
        staged=/data/user/0/com.elitedarkkaiser.nubialy/files/boot-stage.zip
        [ -f "$staged" ] && [ ! -L "$staged" ] || exit 1
        resolved=$(readlink -f "$staged")
        case "$resolved" in /data/user/0/com.elitedarkkaiser.nubialy/files/boot-stage.zip|/data/data/com.elitedarkkaiser.nubialy/files/boot-stage.zip) ;; *) exit 1 ;; esac
        [ "$(stat -c %s "$staged")" = "$bytes" ] || exit 1
        head -c "$((bytes + 1))" "$staged" > "$temp"
        [ "$(stat -c %s "$temp")" = "$bytes" ] && [ "$(sha256sum "$temp" | cut -d ' ' -f 1)" = "$hash" ] || exit 1
        deadline_ok "$@" || exit 1
        [ ! -d "$boot_dir/payload.zip" ] || exit 1
        # Record the underlying original only when it is not already our own live bind.
        own_bind=false
        if safe_file "$boot_dir/mounted" 600 && safe_file "$boot_dir/baseline" 600; then
            read -r receipt_boot receipt_id receipt_inode receipt_target < "$boot_dir/mounted"
            read -r baseline_path baseline_hash < "$boot_dir/baseline"
            if [ "$receipt_boot" = "$boot_id" ] && [ "$baseline_path" = "$target" ] && [ "$(stat -c %d:%i "/proc/1/root$target")" = "$receipt_inode" ]; then own_bind=true; fi
        fi
        if [ "$own_bind" = false ]; then
            if awk -v p="$target" '$5==p {found=1} END {exit !found}' /proc/1/mountinfo; then exit 1; fi
            [ "$(stat -c %u "/proc/1/root$target")" = 0 ] || exit 1
            original_hash=$(sha256sum "/proc/1/root$target" | cut -d ' ' -f 1)
            # Publish baseline without disturbing the in-progress ZIP temp file.
            asset_temp=$temp; temp=$(mktemp "$boot_dir/.boot.XXXXXX")
            printf '%s %s\n' "$target" "$original_hash" > "$temp"; chmod 600 "$temp"
            [ ! -d "$boot_dir/baseline" ] || exit 1; mv -f "$temp" "$boot_dir/baseline"; temp=$asset_temp
        fi
        deadline_ok "$@" || exit 1
        profile && [ "$stage_target" = "$target" ] || exit 1
        chmod 644 "$temp"; mv -f "$temp" "$boot_dir/payload.zip"; temp=; asset_temp=
        if [ "$(getprop sys.boot_completed)" = 1 ]; then [ ! -d "$boot_dir/attempt" ] || exit 1; rm -f "$boot_dir/attempt"; fi
        publish selection "$choice"
        publish result staged
        echo 'Selection verified and staged for next boot.'
        ;;
    mount)
        [ "$(getprop sys.boot_completed)" != 1 ] || exit 1
        [ "$selected" != stock ] || exit 0
        # A previous boot that never completed disarms this attempt before any mount.
        if [ -e "$boot_dir/attempt" ]; then
            safe_file "$boot_dir/attempt" 600 || exit 1
            previous=$(cat "$boot_dir/attempt")
            if [ "$previous" != "$boot_id" ]; then rm -f "$boot_dir/selection" "$boot_dir/attempt"; publish result rolled_back; echo 'Incomplete prior boot: animation disarmed.'; exit 0; fi
            exit 0
        fi
        safe_file "$boot_dir/payload.zip" 644 || exit 1
        identity "$selected"
        [ "$(stat -c %s "$boot_dir/payload.zip")" = "$bytes" ] && [ "$(sha256sum "$boot_dir/payload.zip" | cut -d ' ' -f 1)" = "$hash" ] || exit 1
        safe_file "$boot_dir/baseline" 600 || exit 1
        read -r baseline_path baseline_hash < "$boot_dir/baseline"
        [ "$baseline_path" = "$target" ] && [ "$(sha256sum "/proc/1/root$target" | cut -d ' ' -f 1)" = "$baseline_hash" ] || exit 1
        if awk -v p="$target" '$5==p {found=1} END {exit !found}' /proc/1/mountinfo; then exit 1; fi
        context=$(ls -Zd "/proc/1/root$target" | awk '{print $1}')
        case "$context" in u:object_r:*:s0) ;; *) exit 1 ;; esac
        chcon "$context" "$boot_dir/payload.zip" || exit 1
        chmod 644 "$boot_dir/payload.zip"
        publish attempt "$boot_id"
        # KernelSU-documented source tag; failure leaves stock, with no fallback mount.
        if ! mount -o bind,dev=KSU "$boot_dir/payload.zip" "$target"; then rm -f "$boot_dir/attempt"; publish result mount_failed; exit 1; fi
        # Prove visibility in PID 1's namespace; never fall back to broader mounts.
        if [ "$(stat -c %d:%i "/proc/1/root$target")" != "$(stat -c %d:%i "$boot_dir/payload.zip")" ]; then
            rm -f "$boot_dir/selection"; publish result unverified; exit 1
        fi
        publish mounted "$boot_id $selected $(stat -c %d:%i "/proc/1/root$target") $target"
        publish result mounted
        ;;
    confirm)
        [ "$(getprop sys.boot_completed)" = 1 ] || exit 1
        safe_file "$boot_dir/attempt" 600 && [ "$(cat "$boot_dir/attempt")" = "$boot_id" ] || exit 0
        safe_file "$boot_dir/mounted" 600 || exit 1
        read -r receipt_boot receipt_id receipt_inode receipt_target < "$boot_dir/mounted"
        [ "$receipt_boot" = "$boot_id" ] && [ "$(stat -c %d:%i "/proc/1/root$target")" = "$receipt_inode" ] || exit 1
        rm -f "$boot_dir/attempt"
        publish result confirmed
        ;;
esac
