# Page-scoped application

Repository About description:
Standalone NX809J/NX789J customization app inspired by Iconify, built for stock Nubia/REDMAGIC System UI with Vector integration.

Enable/Restore capture the visible section and page on the UI thread, then merge
only owned fields into the currently authorized active snapshot on the ordered
worker. Unapplied drafts never supply other fields. With no current authority,
the base is stock. The existing root guard and validation remain required.
Overview routes own their section; details own their displayed controls. Hidden
bitfields and QS layout options merge only the selected bits. Status-bar/QS icon
packs have separate fields. Legacy shared pack values migrate to both areas.
Clock color zero is an internal native-tint sentinel, not transparent user input;
opaque color pickers and temporary preview validation are unchanged.

Launcher overview/detail application follows the same merge rules. Dashboard
Emergency stop and global Restore retain their whole-app recovery behavior.
Boot animations already have their own explicit staging/restoration flow.

Tests cover every detail route, section composition, independent restore, native
clock color, separate packs, migration, launcher scopes and action navigation.
Physical RM10/RM11 testing is required before merging. This build includes the
RM10 Android 16 launcher fix from PR #36.
