# Gym Tracker

An offline Android workout logger. No account and no cloud: everything is stored on your phone.

## Features

- **Plans**: build reusable workout plans with target sets and rep ranges, or import them from JSON.
- **Session logger**: log reps and weight per set, with hints from your last session, a rest timer, and a notification that keeps the session going in the background.
- **Exercise library**: 170+ built-in exercises tagged with primary and secondary muscles, plus your own custom ones.
- **Analytics**: a body heat map of training volume per muscle (by sets or tonnage), progress charts, personal records, and a consistency calendar.
- **History**: browse past sessions.
- **Backup**: export and import all data, or just your plans, as a file.

## Tech

Kotlin · Jetpack Compose · Material 3 · Room · DataStore. Min SDK 31 (Android 12).

## Build

```sh
./gradlew assembleDebug
./gradlew testDebugUnitTest
```
