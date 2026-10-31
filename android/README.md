# VELA Native Android

Native Android client for the VELA Volume Profile Bounce strategy. The app does not embed the web app, use a WebView, or require a VELA-owned backend.

## Native features
- Jetpack Compose interface for XAUUSDm.
- M1, M5, M15 and H1 interval selection; M5 is the default.
- Native signed Exness Trader API GET requests for access-point discovery, instrument conditions, API limits and candle history.
- Bid/ask candle history and signed WebSocket bid/ask tick subscription.
- Candlestick chart rendered natively with a live bid marker; historical candles refresh every 15 seconds.
- Strategy signal evaluation is gated once per newly opened selected-timeframe candle.
- MQL5 volume-profile binning, POC/VAH/VAL expansion, SMA trend filter, Wilder ATR, fixed SL/TP calculations and spread-aware ATR trailing-stop ratchet.
- Live order execution is intentionally unavailable and remains OFF. This branch has no order placement method.

## Credential security
- API key identifier, account ID, gateway URL and Ed25519 private key are encrypted together with AES-GCM.
- The AES key is generated and retained by Android Keystore; only ciphertext is stored in private app preferences.
- Private key field is masked. Credentials are signed and sent directly from the Android device over HTTPS.
- Android backup is disabled for this app. Clearing credentials removes the encrypted preference value.
- Never paste a private key into logs, issues or chat messages.

Supported private-key input: 32-byte seed as 64 hex characters, 64-byte secret as 128 hex characters (the first 32 bytes are used as the seed), or PKCS#8 PEM.

## Exness market data
- The candle request uses the selected API interval and price_type=both.
- The app reads the account-specific max_candles_per_request from the Exness limits endpoint instead of assuming the limit.
- The tick WebSocket handshake is signed as GET; the stream subscribes only to the configured instrument.
- Candle history refreshes every 15 seconds. Tick delivery is the live price source; Exness may throttle tick updates.
- No broker credentials or live API calls are part of CI. Account-level connectivity must be verified on a device with a user-provided Exness API key.

## Strategy parity notes
The engine ports the EA's calculations and read-only entry decision. It does not place or modify broker positions. Tick-driven trailing-stop calculation is exposed as a pure function for validation; it is not attached to an active broker position. Real-device parity and broker-specific instrument point precision still require verification with an authorized account.

## Build and test
Requires JDK 17, Android SDK 36 and Gradle 8.11.1.

    gradle --no-daemon :app:assembleDebug
    gradle --no-daemon :app:testDebugUnitTest

GitHub Actions builds the debug APK and runs local unit tests. The APK is not considered verified until the workflow and artifact are both checked.
