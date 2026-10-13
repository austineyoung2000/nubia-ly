# Vector integration

Framework: https://github.com/JingMatrix/Vector. Vector supports legacy Xposed and modern libxposed APIs. This first module uses the supported legacy API 82, fetched compile-only from the official Xposed Maven endpoint. The framework supplies it at runtime; its implementation is not bundled.

`assets/xposed_init` names `NubiaLyModule`; manifest metadata advertises the module and recommends only `com.android.systemui`. It checks package, main process, NX809J, Android 16 then logs module loading. System UI registers zero customization hooks and performs zero resource/settings writes. An app-only hook replaces ModuleStatus.isActive to report Vector activation. It does not certify System UI loading. No startup APK hashing is performed inside System UI; inspection occurs explicitly in the app.

For a future app/framework preference bridge, verify the selected Vector release interface before implementation. Do not claim framework activation from a root grant or an installed manager alone. No activation detector or remote settings bridge is implemented yet.

Phone tests after CI: install the app, verify the dashboard, run firmware/root inspection; optionally enable the module in Vector for System UI only and check the inert module-load message. Enabling requires the user's existing Vector/Zygisk setup; the app does not install or configure Vector.
