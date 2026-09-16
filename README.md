# XREAL Canvas v0.1

Experimental Android/XREAL display-positioning spike.

## What this version tests

When an Android presentation display is available, XREAL Canvas opens a black
secondary-display canvas containing a 16:9 test window. The phone provides
live controls for:

- Scale
- X position
- Y position
- Center
- Reset

This version intentionally does **not** capture the phone screen yet. Its job is
to prove that a normal Android app can independently position and scale content
on the XREAL external display.

## Build an APK with GitHub Actions

This repository includes `.github/workflows/build-apk.yml`.

1. Put the **contents of this project** at the root of a GitHub repository.
   Do not upload this project as a single ZIP file inside the repository.
2. Open the repository's **Actions** tab.
3. Select **Build XREAL Canvas APK**.
4. If a build did not start automatically, choose **Run workflow**.
5. Open the completed run.
6. Under **Artifacts**, download `XREAL-Canvas-v0.1-debug`.
7. Unzip that artifact on the Android phone and install `app-debug.apk`.

Android may ask you to allow installs from your browser/files app.

## Hardware test

1. Connect the XREAL glasses directly to the Android phone.
2. Prefer Samsung **Extended** display mode for this experiment.
3. Launch XREAL Canvas.
4. The phone reports whether a presentation display was detected.
5. Look through the glasses for the XREAL CANVAS TEST WINDOW on a black field.
6. Move Scale, X, and Y and confirm whether the test window responds in real time.

## Notes

- Package: `com.xrealcanvas.app`
- minSdk: 29
- targetSdk / compileSdk: 35
- Android Gradle Plugin: 8.7.3
- Kotlin Android plugin: 2.0.21
- GitHub Actions uses Java 17 and Gradle 8.9.
