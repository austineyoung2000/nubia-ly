# Status-bar authority recovery test (APK 91)

The reported styling loss did not recover automatically: the tester manually pressed Enable before sending the enabled screenshot. The screenshots alone do not establish the original cause.

Code inspection found two reproducible contention paths: the shared broker can reply BUSY while another scoped client is reading, and a runtime diagnostic can overlap an existing read. Previously either was treated as a missing permit and permanently stopped renewal.

Persistent styling now retries transient contention within its existing health deadline, without extending authority. If that deadline expires, the backend restores stock and the runtime attempts fresh startup authorization after 2, 5 and 10 seconds. Successful authorization must match the previously enabled identity. Each request still passes the broker's root receipt, recovery state and startup checks. Three unsuccessful attempts exhaust the automatic retry budget until a later wake or explicit Enable.

Explicit stop cancels retries and fences late callbacks. A blocked, malformed, mismatched or invalid authorization stops styling without retry. Temporary previews do not automatically resume. Backend failures retain stock restoration. No root access is granted by this retry path.

`tests/test_preview_recovery.py` executes production PreviewRuntime with deterministic Android fixtures: shared BUSY, overlapping reads, unavailable broker, unchanged deadline, expiry restoration, fresh resume, blocked/bad-nonce responses, bounded retries and explicit-stop cancellation. Existing sleep tests cover temporary previews and surface failures.

This is a phone-test build on the existing temporary launcher compatibility branch. The launcher APK identity bypass remains a test-only change and this branch must not be merged as-is. Recovery guard 0.1.17 is unchanged. Device confirmation of this reported failure remains pending.
