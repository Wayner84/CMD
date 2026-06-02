# CMD — Coordinate Measuring Dreams

CMD is a daily-use Android toolbox for CMM programming ideas and utilities.

This repository is intended to evolve iteratively: when a useful CMM/metrology idea appears, it can become another app function behind the home screen button list.

## Branding

The app uses a dark-mode-friendly, muted pink Townsend Precision Labs-inspired vector mark. The clean SVG source is in:

- `assets/townsend-precision-labs-logo.svg`

The Android launcher/app icon is the matching vector drawable:

- `app/src/main/res/drawable/ic_cmd_logo.xml`

## First function: IJK Angle Visualiser

The initial screen lets you visualise a direction vector used in CMM programming:

- Dark mode, muted pink theme.
- Home screen with function buttons.
- IJK visualiser with red/green/blue origin triad.
- Direction arrow updated live from angle sliders or typed values.
- Manual entry for XY angle, Z/elevation angle, and I/J/K vector values.
- Touch-drag 3D view rotation so the vector can be inspected from any direction.

## Build

```bash
./gradlew assembleDebug
```

The CI workflow installs an Android SDK on GitHub Actions and builds the debug APK.

## Licence

MIT.
