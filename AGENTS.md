# nubia-ly development

- Standalone project; do not merge its runtime into Toolbox.
- Initial support is NX809J only. Verify remote HEAD before editing; preserve dirty worktrees.
- Use test branches and draft PRs. The user tests APKs and authorizes merges explicitly.
- Device commands use Termux and su -c, never adb.
- Research baseline matches are not proof of customization safety.
- Do not ship unvalidated Quick Settings grid changes; prepare and verify recovery before any live test.
- Never disable Rescue Party, clear System UI data, replace stock APKs or edit registry files as a shortcut.
- Implement and test a scoped recovery contract before enabling the first cosmetic feature.
- Detailed area-prefixed commit messages; validation belongs in PR descriptions.
