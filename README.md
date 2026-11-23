# Daily Expenses

A small, offline personal expense tracker built with Kotlin and the Android SDK.
Runs on Android 6.0 (API 23) or newer.

## Features

- Add a description and amount for the selected day (today by default).
- Select any date to review its expenses and exact daily total.
- Delete an expense after confirmation.
- Expenses persist across app restarts in a private SQLite database.
- No account, internet permission, analytics, or cloud service. Database and
  settings are excluded from cloud backup and device transfer.

The currency is taken from the device region on first launch and remembered.
Amounts use two decimal places; enter digits with a dot or comma decimal
separator and no grouping separators. The maximum expense is 999,999,999.99.
Changing the device region does not reinterpret previously recorded expenses.
Uninstalling the app or clearing its storage permanently deletes all expenses.

## Build and run

Open this directory in Android Studio, let Gradle sync, and run the `app` module
on a device or emulator. Alternatively, with JDK 17 and Android SDK 35 installed,
set `ANDROID_HOME` to your SDK location (or set `sdk.dir` in an untracked
`local.properties` file), then run:

```sh
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest :app:lintDebug
```

The APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.
The first build requires access to Google Maven and Maven Central.

## Manual verification

1. Add `Lunch` for `12.50` and `Bus` for `2.25`; confirm the total is `14.75`.
2. Try an empty description, zero/negative amount, and more than two decimal
   places; confirm invalid input is not saved.
3. Choose another date, add an expense, and return to today; confirm each day
   has its own history and total.
4. Close/reopen the app and verify the records remain, including in airplane mode.
5. Cancel deletion, then confirm deletion; verify the total updates only after
   confirmation.
6. Rotate while editing to verify the draft survives. Rotate immediately after
   saving to verify the submitted expense is not offered for saving again.