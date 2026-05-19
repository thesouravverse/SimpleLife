# DayQuest

A minimal life-diary app. Each day is a page. Tap **+**, add a task,
check it off → green tick + **+10 XP**. Unchecked tasks at end of day → **−5 XP**.
Total XP unlocks badges: **Sprout → Elite → Master → Epic → Legend → Mythic → Mythical Glory**.

Swipe arrows or pick a date to flip through previous pages.

## Stack
- Kotlin + Jetpack Compose + Material 3
- MVVM (StateFlow) + Hilt
- Room (local DB, zero-network)
- WorkManager (daily penalty job at 23:59)
- Android 8.0+ (minSdk 26, targetSdk 35)

## Build
Pushes to `main` trigger GitHub Actions to produce a debug APK artifact.
Tags `v*` build a signed release `.aab`.
