# VELA Native Android

Native Android application foundation for the VELA Volume Profile Bounce strategy.

## Current foundation
- Jetpack Compose Android application (no WebView and no application-owned server).
- XAUUSDm instrument label and selectable M1, M5, M15 and H1 candle intervals.
- M5 is the default interval.
- Strategy defaults transcribed from `VolumeProfile_Bounce_Strategy_v3.mq5`.
- Live order execution is disabled in this foundation.

## Build
Requires JDK 17, Android SDK 36 and Gradle 8.11.1.

```bash
gradle --no-daemon :app:assembleDebug
gradle --no-daemon :app:testDebugUnitTest
```

The current chart intentionally shows a disconnected state until the native Exness market-data client and secure credential storage are implemented. No synthetic prices are presented as broker data.
