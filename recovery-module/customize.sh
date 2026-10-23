gp() { if [ -x /system/bin/getprop ]; then /system/bin/getprop "$1" 2>/dev/null; else getprop "$1" 2>/dev/null; fi; }
nx789j() { for p in ro.product.model ro.product.vendor.model ro.product.odm.model ro.product.system.model ro.product.product.model; do case "$(gp "$p")" in NX789J*|NX809J*) return 0 ;; esac; done; return 1; }
ui_print "- Installer sees: model='$(gp ro.product.model)' vendor='$(gp ro.product.vendor.model)' sdk='$(gp ro.build.version.sdk)'"
nx789j || abort "NX789J or NX809J model only - see the model values above"
case "$(gp ro.build.version.sdk)" in 35|36) ;; *) abort "Android 15 or 16 only - see the sdk value above" ;; esac
set_perm_recursive "$MODPATH" 0 0 0755 0644
for overlay in "$MODPATH"/system/product/overlay/nubialy_ui_*.apk; do
    [ ! -f "$overlay" ] || { rm -f "$MODPATH/skip_mount"; break; }
done
for script in uictl.sh guardctl.sh bootctl.sh post-fs-data.sh action.sh service.sh uninstall.sh; do
    set_perm "$MODPATH/$script" 0 0 0755
done
ui_print "Independent recovery guard with saved-feature resume after first unlock. Installation enables no new features."
ui_print "Action revokes all nubia-ly permits. Only explicitly saved enabled clock authority resumes after reboot."

ui_print "Optional boot animations use one verified systemless file bind. Stock files remain untouched."
ui_print "Action disarms boot animation for the next boot; any current bind ends on reboot."
ui_print " "
ui_print "NX789J Android 15 and inspected Android 16 player hashes are included."
ui_print "Unknown firmware players are skipped; updates require inspection and an exact profile."
