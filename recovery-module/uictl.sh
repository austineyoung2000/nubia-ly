#!/system/bin/sh
# Own overlays only. This test feature resets on reboot; existing OEM/Monet overlays stay selected.
set -eu
[ "$(id -u)" = 0 ] || exit 1
MODDIR=${0%/*}
STATE=/data/adb/nubia_ly_guard/ui_style
PREFIX=com.elitedarkkaiser.nubialy.uistyle
THEMES="espresso snow vivid pacleggers amalgamation illusions immensity nights solarized"
TARGETS="framework vendor settings systemui"
valid() { case " $THEMES " in *" $1 "*) return 0;; *) return 1;; esac; }
[ ! -L "${STATE%/*}" ] && [ ! -L "$STATE" ] || exit 1
if [ -e "${STATE%/*}" ]; then [ "$(stat -c %u:%a "${STATE%/*}")" = 0:700 ] || exit 1; fi
(umask 077; mkdir -p "$STATE")
[ "$(stat -c %u:%a "$STATE")" = 0:700 ] || exit 1
for entry in lock current pending; do [ ! -L "$STATE/$entry" ] || exit 1; done
op=${1:-status}
case "$op" in preview_*|confirm) [ ! -e "$MODDIR/disable" ] && [ ! -e "$MODDIR/remove" ] || { echo "Guard disabled"; exit 1; };; esac
case "$op" in status|restore|confirm|boot|expire) ;; preview_*) valid "${op#preview_}" || exit 1;; *) exit 1;; esac
# Serialize shell operations. A killed caller leaves a detectable stale lock, never a replay.
tries=0
while [ -d "$STATE/lock" ] && [ "$tries" -lt 10 ]; do tries=$((tries + 1)); sleep 1; done
if ! mkdir "$STATE/lock" 2>/dev/null; then
    [ -f "$STATE/lock/pid" ] || { echo "Theme lock incomplete; retry status"; exit 1; }
    if [ -f "$STATE/lock/pid" ]; then read -r owner < "$STATE/lock/pid"; case "$owner" in ""|*[!0-9]*) exit 1;; esac; kill -0 "$owner" 2>/dev/null && { echo "Theme operation still running"; exit 1; }; fi
    rm -f "$STATE/lock/pid"; rmdir "$STATE/lock" || exit 1; mkdir "$STATE/lock"
fi
echo $$ > "$STATE/lock/pid"
trap 'rm -f "$STATE/lock/pid"; rmdir "$STATE/lock"' EXIT
list() { cmd overlay list --user 0; }
current=stock
if [ -f "$STATE/current" ]; then read -r current < "$STATE/current"; valid "$current" || current=stock; fi
clear_all() {
    result=0
    listing=$(list) || return 1
    for theme in $THEMES; do for target in $TARGETS; do
        package="$PREFIX.$theme.$target"
        if printf '%s\n' "$listing" | grep -Fqx "[x] $package"; then cmd overlay disable --user 0 "$package" >/dev/null || result=1; fi
    done; done
    listing=$(list) || return 1
    for theme in $THEMES; do for target in $TARGETS; do
        printf '%s\n' "$listing" | grep -Fqx "[x] $PREFIX.$theme.$target" && result=1
    done; done
    return "$result"
}
enable() {
    for target in $TARGETS; do
        package="$PREFIX.$1.$target"
        cmd overlay enable --user 0 "$package" >/dev/null || return 1
        cmd overlay set-priority --user 0 "$package" highest >/dev/null || return 1
    done
    listing=$(list) || return 1
    for target in $TARGETS; do printf '%s\n' "$listing" | grep -Fqx "[x] $PREFIX.$1.$target" || return 1; done
    cmd overlay lookup --user 0 android android:color/system_surface_dark >/dev/null || return 1
    cmd overlay lookup --user 0 androidzte androidzte:color/zte_materialColorSurface >/dev/null || return 1
    cmd overlay lookup --user 0 com.android.settings com.android.settings:color/mfvc_card_bg_color >/dev/null || return 1
    cmd overlay lookup --user 0 com.android.systemui com.android.systemui:color/volume_dialog_background_color >/dev/null || return 1
}
preflight() {
    [ "$(getprop ro.product.model)" = NX809J ] && [ "$(getprop ro.build.version.sdk)" = 36 ] || { echo "Inspected NX809J Android 16 profile required"; return 1; }
    [ "$(getprop ro.build.fingerprint)" = "$(cat "$MODDIR/ui-fingerprint")" ] || { echo "Firmware differs from inspected UI profile"; return 1; }
    listing=$(list) || return 1
    for target in $TARGETS; do printf '%s\n' "$listing" | grep -Fq "$PREFIX.espresso.$target" || { echo "Install this test guard and reboot once to register UI overlays"; return 1; }; done
}
case "$op" in
 status)
    preflight
    echo "UI style: $current"
    [ ! -f "$STATE/pending" ] || echo "Preview active: Keep within 90 seconds or automatic stock restoration follows."
    echo "Test styles reset on reboot. Guard Action restores all four targets."
    ;;
 restore|boot)
    # Cancellation is recorded before disabling so an old timer cannot affect a newer preview.
    rm -f "$STATE/pending"
    clear_all || { echo "UI style restoration incomplete; retry Restore or Guard Action"; exit 1; }
    rm -f "$STATE/current"; echo "UI style restored across framework, Nubia framework, Settings and System UI"
    ;;
 expire)
    [ -f "$STATE/pending" ] || exit 0
    read -r token < "$STATE/pending"; [ "$token" = "${2:-}" ] || exit 0
    clear_all || { echo "Automatic UI restoration incomplete"; exit 1; }
    rm -f "$STATE/pending" "$STATE/current"
    ;;
 confirm)
    [ -f "$STATE/pending" ] || { echo "No theme preview to keep"; exit 1; }
    # A completed preview must still own all targets before being retained.
    valid "$current" || exit 1
    listing=$(list) || exit 1
    for target in $TARGETS; do printf '%s\n' "$listing" | grep -Fqx "[x] $PREFIX.$current.$target" || { echo "Preview ownership incomplete; restore first"; exit 1; }; done
    rm -f "$STATE/pending"; echo "UI style kept for this boot: $current"
    ;;
 preview_*)
    preflight
    # Verify native binaries before making any resource change, including package updates on the same ROM.
    while read -r expected file; do
        [ -f "$file" ] && [ "$(sha256sum "$file" | cut -d ' ' -f1)" = "$expected" ] || { echo "Native UI APK differs: $file"; exit 1; }
    done < "$MODDIR/ui-native-sha256"
    selection=${op#preview_}
    had_pending=0; [ ! -f "$STATE/pending" ] || had_pending=1
    token=$(cat /proc/sys/kernel/random/uuid)
    echo "$token" > "$STATE/pending"
    # Arm before the first resource change: a killed app/root caller cannot strand a partial apply.
    nohup sh -c 'sleep 90; sh "$1" expire "$2"' ui-style "$MODDIR/uictl.sh" "$token" >/dev/null 2>&1 </dev/null &
    clear_all || exit 1
    if ! enable "$selection"; then
        clear_all || { echo "Partial UI restoration pending"; exit 1; }
        if valid "$current"; then enable "$current" || { clear_all || true; rm -f "$STATE/current"; exit 1; }; fi
        [ "$had_pending" = 1 ] || rm -f "$STATE/pending"
        echo "Theme activation failed; check restoration before retrying"; exit 1
    fi
    echo "$selection" > "$STATE/current"
    echo "Preview applied to all four targets: $selection. Check Settings and System UI, then press Keep within 90 seconds."
    ;;
esac
