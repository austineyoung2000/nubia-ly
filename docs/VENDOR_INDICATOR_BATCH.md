# Vendor indicator batch: 0.1.23

Install the signed update over the current app and reboot so Vector loads the new hooks. NX809J only; existing guard needs no update.

## Cleaned-up editor

Under **Load individual system status icons**, the fan, pump and refresh-rate hide checkboxes now share one list with the loaded alarm and other system indicators. The separate pump switch is removed and its saved choice is preserved. Open **Fan, pump & refresh-rate appearance** only when needed; the six sliders start collapsed. Reloading the live icon inventory removes old controls and rebuilds the list without changing saved selections.

## New features together

| Indicator | Hide | Size | Opacity |
|---|---|---|---|
| Cooling fan | New separate option | 80–120% | 25–100% |
| Cooling pump | Existing option, moved into list | 80–120% | 25–100% |
| Refresh rate | New separate option | 80–120% | 25–100% |

Size and opacity use 5% steps relative to stock scaling/animation; 100% keeps stock. Hidden indicators remain hidden even when sized or faded. Zero-valued internal defaults retain the previous appearance. Choices apply only after Enable, and wake/reboot uses the enabled snapshot rather than newer drafts. Restore stock, guard Action and Emergency stop release all overrides. Reset icons & battery selections clears these drafts too.

These targets were statically verified in the uploaded NX809J SystemUI_MFV.apk: RedMagicFunctionIconFeature's fan/pump/refresh subclasses resolve cooling_fan_view, liquid_cooling_view and refresh_rate_view within PhoneStatusBarView. Identification uses package-qualified resource names, not numeric IDs. No vendor feature model, cooling hardware, cooling settings, refresh-rate settings or Quick Settings tile state is changed.

## One combined phone test

1. Check the grouped hide options and collapsed appearance section. If pump hiding was already enabled, confirm the choice remains checked. Load the live icon list twice and check for duplicates or lost selections.
2. With fan, pump and refresh-rate indicators visible, pick different size/opacity values for each—for example fan 120%/50%, pump 80%/75%, refresh rate 110%/25%. Enable and confirm each indicator follows its own setting. Keep your existing clock/mobile service/notification/battery styles active alongside them.
3. Hide fan and refresh rate; Enable. They should disappear independently of the pump. Hide all three and verify hardware operation and normal tile controls still work. Turn cooling on/off through your normal controls and check that hidden icons do not reappear.
4. Switch apps, shade and lock/unlock. Confirm style remains steady and size/opacity do not accumulate. Draft edits alone should not change the applied style.
5. Restore stock with cooling on and check the appropriate indicators return; repeat with cooling off and check they stay absent. Re-enable, reboot, then test guard Action or Emergency stop to restore the entire style.

Phone results pending. Report unexpected icon changes, duplicate list entries, callback failures or incomplete restoration. No timed session is required.

## Automated validation

Production backend tests cover independent vendor sizing/opacity, latest stock updates, repeated refresh, independent and combined hiding, subtree/package exclusion, detach and restoration. The existing apply/restore failure fences remain tested. Preferences cover identity-bound frozen selections and legacy defaults; unit checks cover all 144 size/opacity combinations and malformed values. Existing clock/date, inventory, root-call and recovery checks remain in CI.
