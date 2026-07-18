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
- Auto-fits the model and lets you drag right to orbit right.
- Supports pinch-to-zoom and two-finger pan.
- Shows vertex/triangle count and X/Y/Z bounding-box dimensions.
- Tap one visible vertex for A, tap another for B, then see distance and ΔX/ΔY/ΔZ.

Note: OBJ/STL files do not reliably declare units, so dimensions are shown as model units. For CMM use, treat them as mm/inch according to the source model.

### Drawing Ballooning

Create an editable inspection balloon table from an engineering drawing image:

- Home-screen Drawing Ballooning tool.
- Take a drawing photo or load a drawing image, then pan and pinch-zoom around it.
- Add vertical and horizontal grid lines and enter column/row labels before dimensioning starts.
- Grid lines stay visible at lower opacity while ballooning.
- Add dimension boxes over drawing callouts; CMD adds sequential circled balloon numbers starting at 1.
- Automatically assigns a grid reference from the selected box position.
- Separate Drawing/Edit and Table tabs keep mark-up and editable records apart.
- Table records include number, dimension text/manual OCR field, +tol, -tol, result, grid reference, and GD&T toggle/type notes.
- All table fields are editable so OCR/manual guesses can be corrected. Full automatic OCR and auto tolerance parsing are planned follow-up improvements.

### ISO Tolerance Tables

Quickly check ISO 286 limits and fits:

- Home-screen tool for hole or shaft tolerances.
- Enter nominal size in mm and a designation such as `H7`, `g6`, `JS11`, or `za12`.
- Supports hole positions A, B, C, CD, D, E, EF, F, FG, G, H, JS, K, M, N, P, R, S, T, U, V, X, Y, Z, ZA, ZB, ZC where ISO table rows exist.
- Supports shaft positions a, b, c, cd, d, e, ef, f, fg, g, h, js, k, m, n, p, r, s, t, u, v, x, y, z, za, zb, zc where ISO table rows exist.
- Supports IT01, IT0, and IT1–IT18.
- Shows tolerance width, upper/lower deviations, and minimum/maximum size.
- Warns rather than inventing a value when a size/grade/position is outside the embedded ISO 286 table range.

### Trigonometry Calculator

Right-triangle shop calculator with a visual diagram:

- Enter any two usable values from opposite, adjacent, hypotenuse, and angle A.
- Calculates missing sides, both acute angles, and area.
- Updates a scaled triangle diagram for a quick visual check.

### Probe Angle Calculator

Estimate the maximum probing angle before the stylus shaft/stem becomes the limiting contact instead of the probe ball:

- Inputs probe ball diameter, shaft/stem diameter, and usable length from ball centre.
- Outputs max angle from surface normal and equivalent angle from the surface.
- Uses `asin((probe diameter - shaft diameter) / 2 / usable length)` with a clear assumption note in the app.

## Build

```bash
./gradlew assembleDebug
```

The CI workflow installs an Android SDK on GitHub Actions and builds the debug APK.

## Licence

MIT.
