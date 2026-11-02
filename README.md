# SpendWise

Offline-first personal finance tracker for Android (MVP).

Track income and expenses, organize them into predefined categories, set a monthly budget, and view category-wise spending analytics. All data stays on the device.

## Stack

- Kotlin, Jetpack Compose, Material 3
- MVVM + Repository
- Room, DataStore, Hilt, Navigation Compose, Coroutines/Flow

## Package

- `applicationId` / namespace: `com.dins.spendwise`
- minSdk 26, targetSdk 37

## Setup

1. Open the project in Android Studio (recent AGP 9-compatible IDE).
2. Let Gradle sync (JDK 17+; project targets built-in Kotlin via AGP 9).
3. Run the `app` configuration on an emulator or device (API 26+).

If command-line Gradle hits SSL/`PKIX` errors on Windows, the project already sets
`-Djavax.net.ssl.trustStoreType=Windows-ROOT` in `gradle.properties`.

Command-line debug install:

```bash
./gradlew :app:installDebug
```

On Windows:

```bat
gradlew.bat :app:installDebug
```

## Tests

Unit tests (money, budgets, filters, validation):

```bash
./gradlew :app:testDebugUnitTest
```

Instrumented DAO tests (device/emulator required):

```bash
./gradlew :app:connectedDebugAndroidTest
```

## Implemented MVP checklist

- [x] Dashboard with month navigation, income/expense/net, budget progress, recent transactions
- [x] Add / edit / delete transactions with validation
- [x] Predefined income and expense categories (seeded)
- [x] Transaction history with search + month/type/category filters
- [x] Monthly budget set/edit and utilization thresholds
- [x] Analytics donut chart + accessible category list
- [x] Settings: theme (system/light/dark), INR note, About, privacy, version
- [x] Room offline persistence; money as Long paise; LocalDate as epoch day
- [x] Unit tests for core calculations/validation; DAO instrumented tests

## Known limitations

- No custom categories, cloud sync, login, bank/UPI/SMS import, or multi-currency conversion
- INR display only
- Room uses destructive migration for this pre-release schema (replace with real migrations before Play production updates)
- No biometric lock, CSV export, or recurring transactions

## Signed release AAB

1. Create a keystore (once), keep it private — never commit it:

```bash
keytool -genkey -v -keystore spendwise-upload.jks -keyalg RSA -keysize 2048 -validity 10000 -alias spendwise
```

2. Create `keystore.properties` in the project root (gitignored) or configure signing in Android Studio:

```properties
storePassword=YOUR_STORE_PASSWORD
keyPassword=YOUR_KEY_PASSWORD
keyAlias=spendwise
storeFile=../path/to/spendwise-upload.jks
```

3. Wire `signingConfigs` in `app/build.gradle.kts` (or use Android Studio Build → Generate Signed Bundle).

4. Generate the bundle:

```bash
./gradlew :app:bundleRelease
```

Output:

`app/build/outputs/bundle/release/app-release.aab`

## Play Console notes

- App name suggestion: SpendWise – Expense Tracker
- Declare local-only data in Data safety; no account, no shared data collection in MVP
- Publish a privacy policy URL that matches the in-app privacy copy (local storage only)
