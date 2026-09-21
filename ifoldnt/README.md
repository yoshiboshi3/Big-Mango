# iFoldn't v0.1

A deliberately silly Galaxy Z Fold utility: fade one half of the inner screen while unfolding, plus optional 45-degree corner bevels on both displays.

## Install

Install `iFoldnt-v0.1-debug.apk` on the Galaxy Z Fold 7, open it, grant **Display over other apps**, then enable the desired effect.

Defaults: Witchcraft Auto (Right fallback), 65% maximum darkness, fade from 90° to 178°, bevel preset **Stupid**.

### Witchcraft Auto

Tap **Calibrate Witchcraft** and record two movements:
1. Hold LEFT; move RIGHT.
2. Hold RIGHT; move LEFT.

Manual Left and Right remain available if the Fold's public sensors do not distinguish the two motions reliably.

## Safety / privacy

- No internet permission
- No camera
- No storage access
- No Accessibility service
- Overlay is non-focusable and non-touchable
- Android 12+ overlay opacity is dynamically capped below the system maximum obscuring opacity for pass-through touches
- One overlay window is used for both fade and bevels so their opacities cannot stack into Android's untrusted-touch block threshold
- Overlay is removed when locked/screen-off, disabled, permission is missing, geometry is unavailable, or the service stops

Package: `com.ifoldnt` · minSdk 30 · targetSdk 33 · compileSdk 35
