# Fresh Debug Rebuild

Base: upstream master `8351a570`, mirrored by `src` without local product changes.
Development branch: `feat/fresh-setup-rime-sync`.
Package: `com.osfans.trime.debug.fresh`, displayed as `Trime Fresh (Debug)`.
This package does not inherit the old debug application's preferences or runtime data.

## Features

- Standard (`tongwenfeng.trime`) theme: vertical gap 8, height 230/180 (portrait/landscape).
- Candidate base size 22sp; portrait one-code-point candidates 21sp, two/three 23sp.
- Landscape keeps 22sp and adapts the number of visible candidates to available width.
- Backspace held for 600ms clears composition only when composition existed at touch-down.
- A consumed composition hold does not repeat deletion into the host application.
- With no composition, existing repeat-delete behavior is unchanged.
- Long-hold vibration uses the existing enabled key-press vibration setting.

## Original Import

Use the storage page in the setup wizard. Android requires the initial source and
destination grants: select original Trime's `files` root, then primary storage `/rime`.
Create that folder in the picker if necessary. This is `/storage/emulated/0/rime`,
not a removable card or an application-private directory.

The original app must have initialized its files first. The import combines shared
and user configuration sources; user sources take precedence. Live `.userdb`, compiled
`build`, installation identity, sync snapshots and backup directories are not copied.
They are not portable configuration files. The original app is never modified.

The standard-theme overrides are merged into a custom YAML patch. Replaced files
are backed up, deployment occurs against the app runtime copy, and external mode is
committed after success. Normal installation/deployment failures attempt rollback.
This is not a crash-proof multi-file transaction across process death or power loss.

## Rime Ice Update

The cloud-download entry in the keyboard's more menu downloads the official nightly
`full.zip`, verifies GitHub's SHA-256 digest, stages it, backs up replaced files and
deploys it. It uses the already authorized external directory when configured;
otherwise it uses app storage. No additional file picker is opened.

Custom YAML patches, existing `default.yaml`, custom phrases, user options and user
databases are preserved. Updating does not force a different active schema. When
installing Rime Ice for the first time, enable it in the schema list, then select it.
Backups use timestamped `.bak` names in the external tree and backup directories in
the app's external files directory. Keep external backups before uninstalling.

## Build And Verification

The Windows build prepares real assets from Git's symlink placeholder files before
generating checksums. It does not rewrite the source checkout. Kotlin incremental
compilation is disabled to avoid stale interface delegates after branch changes.
Native per-entry INFO logs are suppressed; warnings/errors remain available.

```powershell
. E:/trime-build-env/trime-env.ps1
$env:BUILD_ABI='arm64-v8a,x86_64'
gradle.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --console=plain
```

Validated on Android 15 x86_64 emulator: original v3.3.12 import through both SAF
grants, `/rime` persistence, keyboard startup, standard theme, composition-only hold,
normal repeat-delete, online Rime Ice update, enabling/selecting Rime Ice, Chinese
candidates, direct English commit via the mode button, return to Chinese via held
space, and adaptive landscape candidates. Unit suite: 240 tests, zero failures.

Samsung hardware, physical vibration feel, arbitrary third-party themes, revoked
permissions during a write and power-loss recovery have not been device-tested.
The old phone crash's root cause cannot be established without its logs.
