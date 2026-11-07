# PetCare Log

Android app (Kotlin, XML + ViewBinding, Material 3) to track your pets, using Firebase Authentication
(email/password + Google) and Firebase Realtime Database. Data lives under `users/{uid}`.

## Open in Android Studio
1. File > Open and choose this `PetCareLog` folder; let Gradle sync (JDK 17, AGP 8.13.0, Kotlin 2.2.20, Gradle 8.14.3).
2. `local.properties` points to `/Users/zalio/Library/Android/sdk`; change `sdk.dir` if needed.
3. Run on a device/emulator with Google Play services, or build with `./gradlew assembleDebug`
   (output: `app/build/outputs/apk/debug/app-debug.apk`; a copy is `PetCareLog-debug.apk`).

## Connect your real Firebase project
The bundled `app/google-services.json` is a **placeholder**; the app builds but login will not work until replaced.
1. Create a project in the [Firebase console](https://console.firebase.google.com).
2. Add an Android app with package name `com.petcare.log`.
3. Get your debug SHA-1: `./gradlew signingReport` (use the `debug` variant's SHA-1) and add it under Project settings > Your apps.
4. Authentication > Sign-in method: enable **Email/Password** and **Google**.
5. Build > Realtime Database > Create database; open the Rules tab and paste the contents of `database.rules.json`.
6. Download `google-services.json` (after adding the SHA-1 and enabling Google, so it contains the web OAuth client) into `app/`, replacing the placeholder.
7. Rebuild: `./gradlew assembleDebug`.

If your database is not in us-central1, make sure `firebase_url` in `google-services.json` is correct.
