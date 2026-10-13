# Vector integration

Framework: https://github.com/JingMatrix/Vector. This module uses legacy Xposed API 82, compile-only from the official Xposed Maven endpoint; Vector supplies it at runtime. No framework implementation is bundled.

The System UI entry checks package, main process, NX809J and Android 16, then logs loading. One read-only Application.attach diagnostic hook registers a signature-protected guard-check receiver. It registers zero cosmetic hooks and writes no resources or settings.

An app-only hook replaces ModuleStatus.isActive to report Vector activation inside nubia-ly. This does not certify System UI loading or health. No root grant or manager switch is used as activation evidence.

The emergency-stop latch defaults to stopped and cannot be armed. A remote preference bridge and independently tested boot recovery remain pending.

Phone check: enable nubia-ly, select System UI in Vector, reboot and check app activation. Verify the separate System UI module-load log. Save emergency stop, reopen and verify it remains stopped. No visual changes are expected.
