# NutriWise — Clinical Nutrition & AI Food Forensic Auditor

NutriWise is an advanced Android application built with Jetpack Compose that empowers users to make clinically informed dietary decisions. By scanning packaged food labels, the app forensically audits ingredient lists, nutrition tables, and hidden additives (such as INS/E-numbers and seed oils) against personalized health profiles, chronic conditions, and metabolic targets.

---

## Key Features

* **Dual Scanning Engine:** Combines Google ML Kit Barcode Scanning and Optical Character Recognition (OCR) to parse product labels and ingredients in real time.
* **AI Clinical Forensic Audits:** Leverages Gemini (`gemini-3.6-flash`) to reconstruct missing nutrient data and audit formulations against declared health conditions (e.g., Diabetes, Hypertension, Cancer directives, Celiac/Gluten Sensitivity, GBS, Fatty Liver).
* **Personalized Health Watchlists:** Dynamic tracking of chronic diseases, food allergies, and custom clinical directives with live BMI and fitness goal calibration (Cut, Bulk, Clean Eating).
* **Smart Swaps & Indian Market Alternatives:** Suggests verified, cleaner product alternatives accessible on quick-commerce platforms (Blinkit, Zepto, Instamart).
* **Community Feed:** A social network for health-conscious users featuring verified reviews, product ratings, comments, and real-time like/dislike interactions.

---

## Tech Stack & Architecture

* **UI Framework:** Jetpack Compose (Material 3) with dynamic dark/light theme support
* **Language:** Kotlin & Kotlin Coroutines / StateFlow
* **Backend & Auth:** Firebase Authentication (Email/Password & Google Sign-In), Firebase Realtime Database, Firebase Storage
* **Computer Vision / OCR:** Google ML Kit (Text Recognition & Barcode Scanning), CameraX
* **AI & Networking:** Gemini REST API via OkHttp3, Retrofit2, Moshi
* **Image Loading:** Coil Compose

---

## Setup & Configuration Guide

### 1. Prerequisites

* Android Studio Ladybug (or newer)
* JDK 11 or higher
* Android SDK (API 24 minimum, target API 35)
* A registered Firebase project

---

### 2. Adding `google-services.json`

To enable Firebase Authentication and Realtime Database, you must supply your project's configuration file:

1. Go to the [Firebase Console](https://console.firebase.google.com/).
2. Select your project (or create a new one named `NutriWise`).
3. Click **Add App** and select the **Android** platform icon.
4. Enter the Android package name:
```text
com.example.nutriwise

```


5. *(Important for Google Sign-In)* Generate and enter your debug SHA-1 fingerprint:
* In Android Studio, open the **Gradle** tab on the right side.
* Navigate to `NutriWise > Tasks > android > signingReport` and double-click it.
* Copy the `SHA-1` key printed in the console and paste it into the Firebase setup field.


6. Click **Register App** and download the resulting `google-services.json` file.
7. Switch the project explorer view in Android Studio from **Android** to **Project**.
8. Move the downloaded `google-services.json` file into the app module directory:
```text
NutriWise/
└── app/
    ├── google-services.json   <-- Place it directly here
    ├── build.gradle.kts
    └── src/

```



---

### 3. Firebase Console Configuration

1. **Authentication:**
* Go to **Build > Authentication > Sign-in method**.
* Enable **Email/Password**.
* Enable **Google**. Under the Web SDK configuration, copy the **Web client ID** and verify it matches the string resource `default_web_client_id` in your Android project (`res/values/strings.xml`).


2. **Realtime Database:**
* Go to **Build > Realtime Database > Create Database**.
* Set your location and initialize in test or locked mode.
* Apply secure rules allowing authenticated users to manage their data:
```json
{
  "rules": {
    "users": {
      "$uid": {
        ".read": "auth != null",
        ".write": "auth != null && auth.uid == $uid"
      }
    },
    "posts": {
      ".read": "auth != null",
      ".write": "auth != null"
    }
  }
}

```





---

### 4. Configuring the Gemini API Key

1. Generate an API key from [Google AI Studio](https://aistudio.google.com/).
2. Secure the key inside your project's `local.properties` file:
```properties
GEMINI_API_KEY=your_actual_api_key_here

```


3. Pass the key into `AiExplanationService` via `BuildConfig` or secret managers to prevent committing credentials to version control.

---

### 5. Building and Running

1. In Android Studio, select **File > Sync Project with Gradle Files**.
2. Run **Build > Clean Project**, followed by **Build > Rebuild Project**.
3. Select an emulator or physical device running Android 7.0 (API 24) or higher.
4. Click **Run ('app')** (`Shift + F10`).
