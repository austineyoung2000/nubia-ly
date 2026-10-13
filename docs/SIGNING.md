# Permanent APK signing

A dedicated RSA 4096 signing key for `com.elitedarkkaiser.nubialy` is stored in this repository's Actions secrets. It is independent of Toolbox. The private key and passwords are never committed. `SIGNING_CERT_SHA256.txt` contains only the public certificate digest.

CI uses the same persistent key for the test/debug and release Gradle configurations. It verifies the signed test APK certificate against the public digest before publishing `nubia-ly-signed-test-apk`. Partial secret configuration or a mismatched certificate fails the build. A secretless build is explicitly named `nubia-ly-ephemeral-debug-apk`, with an update-compatibility warning; this supports untrusted fork CI without sharing keys.

The previous 0.1.0/0.1.1 ephemeral debug APK cannot update to this certificate. Uninstall only nubia-ly once, install the permanent-key APK, re-enable its Vector scope, then reboot. This removes local nubia-ly preferences. Future test APKs signed by this key should install as updates. Debug APKs remain debuggable; permanent signing does not make a test build a production release.

Keep the private signing backup offline and private. It contains the keystore and its passwords; anyone holding it can sign APKs as this app. Never attach it to a GitHub issue, commit it, or upload it to a public repository. Do not rotate or regenerate the key for routine builds. GitHub secrets cannot be downloaded as a backup later.

Secret names: `RELEASE_KEYSTORE_BASE64`, `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD`. No new GitHub credential or repository permission is required for signing.
