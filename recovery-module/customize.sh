[ "$(getprop ro.product.model)" = NX809J ] || abort "NX809J only"
[ "$(getprop ro.build.version.sdk)" = 36 ] || abort "Android 16 only"
set_perm_recursive "$MODPATH" 0 0 0755 0644
for script in guardctl.sh action.sh service.sh uninstall.sh; do
    set_perm "$MODPATH/$script" 0 0 0755
done
ui_print "Probe only: no resources, properties, SELinux rules or hooks changed."
ui_print "Action stops the probe. Reboot starts with no permit."
