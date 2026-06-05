# CMD — Coordinate Measuring Dreams

CMD is a daily-use Android toolbox for CMM programming ideas and utilities.

This repository is intended to evolve iteratively: when a useful CMM/metrology idea appears, it can become another app function behind the home screen button list.

## Branding

The app uses a dark-mode-friendly, muted pink Townsend Precision Labs-inspired vector mark. The clean SVG source is in:

- `assets/townsend-precision-labs-logo.svg`

The Android launcher/app icon is the matching vector drawable:

- `app/src/main/res/drawable/ic_cmd_logo.xml`

## Functions

### IJK Angle Visualiser

Visualise a direction vector used in CMM programming:

- Dark mode, muted pink theme.
- Home screen with function buttons.
- IJK visualiser with red/green/blue origin triad.
- Direction arrow updated live from angle sliders or typed values.
- Manual entry for XY angle, Z/elevation angle, and I/J/K vector values.
- Touch-drag 3D view rotation so the vector can be inspected from any direction.

### Model Measure

Import a simple model and take quick point-to-point measurements:

- Opens OBJ/STL files through Android's document picker.
- Supports OBJ vertices/faces, including slash-separated face tokens and quad/ngon fan triangulation.
- Supports ASCII STL and binary STL.
- Shows a dependency-free custom Canvas wireframe viewer.
- Auto-fits the model and lets you drag to orbit around it.
- Shows vertex/triangle count and X/Y/Z bounding-box dimensions.
- Tap one visible vertex for A, tap another for B, then see distance and ΔX/ΔY/ΔZ.

Note: OBJ/STL files do not reliably declare units, so dimensions are shown as model units. For CMM use, treat them as mm/inch according to the source model.

## Build

```bash
./gradlew assembleDebug
```

The CI workflow installs an Android SDK on GitHub Actions and builds the debug APK.

## Licence

MIT.
