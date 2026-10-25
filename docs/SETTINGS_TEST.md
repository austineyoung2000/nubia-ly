# Settings appearance test

Separate Settings editor and Vector scope: `com.android.settings`. Tested source target: NX809J Android 16, Settings version 16.0.010.2512231129, SHA-256 `ede2ad99467767acca35c7a8e7aff5d56a82f25f74a736c2b99b80f9da689480`.

## Setup and controls

Install the signed test APK, enable Settings in nubia-ly's Vector scope and reboot once. Open nubia-ly → Settings app. Choose REDMAGIC Stock or RisingOS, then Apply. Color themes: Follow overall UI, Light, Dark and Custom. Custom colors cover page/card backgrounds, title/summary text and accents. Card styles use the actual shared QS, power and volume effects; Follow uses the currently applied style. Sizing covers card corners, minimum row height, icons, text and group spacing.

RisingOS adapts the main Settings preference list. It keeps native visible category entries and their `Preference.performClick` routes. Native category grouping is preserved; ungrouped lists are split into groups of three. Battery/storage cards use current battery broadcasts and data-volume capacity; temperature is the device battery sensor. Storage uses decimal GB. Profile opens the native user settings. Bottom search launches the inspected native search provider. Native subpages retain their controls and receive preference card/text sizing where supported. Compose/special custom pages are not universally covered.

## Phone checks

- Switch between both layouts. Native Wi-Fi, Bluetooth, apps, display, security and REDMAGIC-specific rows must still open their original pages.
- In RisingOS, check live battery/temperature, storage and native search. Scroll the full category list and return from a subpage.
- Test Light, Dark and Custom, all three style families, and sizing with both normal and enlarged system fonts.
- Restore: original homepage and search return; original row colors, text sizes and backgrounds return. Reopen Settings and check that unrelated nubia-ly areas keep their selections.
- Emergency stop remains authoritative. Disabling only Settings scope in Vector and restarting Settings gives the native application.

## Recovery contract

No system APK replacement, database edits, Settings data wipes or disabling recovery protections. An authenticated broker method verifies both calling package and actual installed UID (shared system UID alone is insufficient). Its startup budget is independent of System UI, launcher and other processes. The existing durable stop latch and 15-second bounded permit apply. Nonce/version/deadline validation, request timeouts and expiry restore mutated views and remove injected content. While enabled and the screen is on, lightweight renewal keeps the bounded permit warm before reopening. Screen off pauses renewal; expiry still restores native views. A successful APK hash is cached against source path, length, modification time and firmware fingerprint in the process owner’s private cache. The signature-protected Restore request immediately removes styling and invalidates pending reads, without waiting for a new broker permit. Drafts cannot enable runtime styling. A mismatched APK or device keeps stock appearance.

This is a phone-test build; the reference layout is recreated from the supplied photos. Subpage coverage and vendor view lifecycle require device validation before merge.

Lifecycle regression: open About phone and Back repeatedly; rotate/recreate the homepage; detach the custom view; recreate the native list; switch Stock/RisingOS repeatedly; then Restore. The original list must remain measurable and return to its original parent/index/layout parameters and visibility even if the custom view detached. The adapter now uses a tracked FrameLayout host with the original list INVISIBLE rather than removing it from layout with GONE.
