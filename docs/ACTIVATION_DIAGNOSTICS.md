# Activation failure diagnostic test

Fingerprint speed is tested and merged. The reported screenshot shows activation failing in the status-bar/surface stage, but the existing message discards nested exception details. It does not establish the underlying cause.

This test APK preserves the bounded exception cause chain in Help & diagnostics and labels status-bar hook installation, surface activation, status-bar refresh, individual surface backends, and surface refresh. Existing rollback and failure latches are unchanged. This is a diagnostic build, not a claimed fix.

Install and reboot to reload Vector. Apply the affected page. If it fails again, share the first part of Help & diagnostics beginning “Styling activation failed.” That line now contains the original failure instead of only the wrapper. Guard 22 remains unchanged.
