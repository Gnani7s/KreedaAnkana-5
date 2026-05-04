# KreedaAnkana v4 — Bug Fix Release

## What was broken and how it's fixed

### 🔴 Bug 1: Rankings & Profile showing W:0 D:0 L:0 after posting scores

**Root cause (KreedaRepository.kt):**
The original `postResult()` called the Firestore batch counter update inside
`bgScope.launch { }` — fire-and-forget. If the app was slow or the coroutine
was delayed, the increment never reached Firestore, so team docs kept showing
`wins=0`. Also the local Room team cache was never updated, so offline the
stats were always 0.

**Fix:**
- `postResult()` now awaits the Firestore write synchronously (inside `tryFirebase`).
- A new private `updateLocalTeamCounters()` function immediately increments
  wins/draws/losses in the Room cache so the leaderboard and profile update
  **instantly** — even before Firestore responds.
- Team lookup in `postResult` (MainViewModel) is now **case-insensitive**, so
  "Svs" matches "svs" in the database, and the loser's counter is always found.

---

### 🔴 Bug 2: Calendar screen glitch (slots not showing / wrong date)

**Root cause (CalendarScreen.kt):**
The old screen called `viewModel.getSlotStatus()` which read from
`slots.value` — a snapshot that wasn't guaranteed to be fresh after
navigating away and back. The 7-day range was also too short.

**Fix:**
- CalendarScreen now derives slot status **directly** from the reactive
  `slots` StateFlow inside the composable, so it automatically recomposes
  whenever Firestore pushes new data.
- Extended day picker from 7 → **14 days**.
- Added booking indicators (orange dot) on days that have bookings.
- Added an "Upcoming Bookings" section at the bottom of the calendar.

---

### 🟡 Improvement: Leaderboard sorting

Rankings now sort by **points (wins×2 + draws×1)** descending, which is
the standard cricket/sports tournament tiebreaker, not just by raw wins.

---

## Files changed
- `data/repository/KreedaRepository.kt` — postResult sync + local counter update
- `viewmodel/MainViewModel.kt` — case-insensitive team lookup, points-based sort
- `ui/screens/CalendarScreen.kt` — reactive slot display, 14-day picker, booking dots

---

# 🏟️ Kreeda-Ankana
### Sports Ground & Match Organizer
> *"Turning Village Grounds into Organised Sports Hubs"*

---

## 📱 About

Kreeda-Ankana is an Android app (Jetpack Compose + Firebase + Room DB) that brings structure to village-level sports by enabling:
- **Ground Slot Booking** with real-time conflict prevention
- **Challenge Board** for inter-village match challenges
- **Score Wall** for posting and viewing match results
- **Leaderboard** ranked by team wins

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| UI | Jetpack Compose + Material Design 3 |
| Architecture | MVVM (ViewModel + StateFlow) |
| Backend | Firebase Firestore (real-time) |
| Local DB | Room DB (offline-first) |
| Auth | Firebase Auth (Phone OTP) |
| Notifications | Firebase Cloud Messaging (FCM) |
| DI | Hilt |
| Preferences | DataStore |

---

## ⚙️ Setup Instructions

### Step 1: Clone / Open Project
1. Open **Android Studio Hedgehog** or newer
2. Open the `KreedaAnkana` folder as a project
3. Wait for Gradle sync to complete

---

### Step 2: Create Firebase Project

1. Go to [https://console.firebase.google.com](https://console.firebase.google.com)
2. Click **Add project** → name it `KreedaAnkana`
3. Enable **Google Analytics** (optional)
4. Click **Continue** → **Create project**

---

### Step 3: Register Android App in Firebase

1. In your Firebase project, click **Add app** → Android icon
2. **Package name**: `com.kreedaankana`
3. **App nickname**: Kreeda-Ankana
4. Click **Register app**
5. **Download `google-services.json`**
6. Replace the placeholder file at:
   ```
   app/google-services.json
   ```
   with the downloaded file

---

### Step 4: Enable Firebase Services

In the Firebase Console:

#### Firestore Database
1. Left menu → **Firestore Database** → **Create database**
2. Choose **Start in test mode** (for development)
3. Select a region → **Enable**

#### Authentication (Optional - for production OTP)
1. Left menu → **Authentication** → **Get started**
2. **Sign-in method** tab → Enable **Phone**

#### Cloud Messaging
1. Left menu → **Cloud Messaging** → already enabled by default

---

### Step 5: Firestore Security Rules (Development)

In Firestore → Rules tab, use:
```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if true;  // Development only!
    }
  }
}
```

> ⚠️ For production, replace with proper user-based rules.

---

### Step 6: Build & Run

1. Connect an Android phone (API 26+) or start an emulator
2. Click **Run** ▶️ in Android Studio
3. The app will install and launch

---

## 📂 Project Structure

```
app/src/main/java/com/kreedaankana/
├── KreedaAnkanaApp.kt          # Hilt Application class
├── MainActivity.kt             # Entry point
├── KreedaMessagingService.kt   # FCM push notifications
│
├── model/
│   └── Models.kt               # Data classes (Team, Slot, Challenge, etc.)
│
├── data/
│   ├── local/
│   │   ├── KreedaDatabase.kt   # Room DB
│   │   ├── dao/Daos.kt         # DAO interfaces
│   │   └── entities/Entities.kt# Room entities
│   └── repository/
│       └── KreedaRepository.kt # Single source of truth
│
├── di/
│   └── AppModule.kt            # Hilt DI providers
│
├── navigation/
│   └── Screen.kt               # Navigation routes
│
├── viewmodel/
│   └── MainViewModel.kt        # Business logic + state
│
├── utils/
│   └── UserPreferences.kt      # DataStore preferences
│
└── ui/
    ├── theme/
    │   ├── Theme.kt             # Material3 sport theme (green/orange)
    │   └── Typography.kt        # Bold sporty fonts
    ├── components/
    │   └── Components.kt        # Reusable UI components
    └── screens/
        ├── OnboardingScreen.kt  # Team registration
        ├── HomeScreen.kt        # Bottom nav host
        ├── CalendarScreen.kt    # 7-day slot grid
        ├── BookSlotScreen.kt    # Slot booking with conflict check
        ├── ChallengeBoardScreen.kt
        ├── PostChallengeScreen.kt
        ├── ScoreWallScreen.kt   # Match results feed
        ├── LeaderboardScreen.kt # Village rankings
        └── LeaderboardScreen.kt # Profile + quick guide
```

---

## 🎮 App Flow

```
Launch
  │
  ├── First time → Onboarding (Register Team)
  │
  └── Registered → Home (Bottom Nav)
        ├── 📅 Calendar    → View slots → Book Slot
        ├── ⚔️  Challenges  → Browse → Accept / Post Challenge
        ├── 📊 Score Wall  → Results feed → Post Result
        ├── 🏆 Leaderboard → Rankings by wins
        └── 👤 Profile     → Team info + quick guide
```

---

## ✅ Success Criteria Met

| Requirement | Implementation |
|---|---|
| Double-booking prevented | Firebase Transaction atomically checks slot status before booking |
| Challenge Reply | Accept button in ChallengeCard updates Firestore + notifies via FCM |
| Bold & Sporty UI | Material3 with SportGreen (#2E7D32) + SportOrange (#E65100) palette |
| Offline mode | Room DB caches all data; Firestore offline persistence enabled |
| Real-time updates | Firestore `addSnapshotListener` on all collections |

---

## 🔧 Troubleshooting

| Issue | Fix |
|---|---|
| Gradle sync fails | Check internet connection; File → Invalidate Caches |
| `google-services.json` error | Re-download from Firebase Console and replace the file |
| App crashes on launch | Make sure Firestore is created in Firebase Console |
| Firestore permission denied | Set security rules to test mode (allow read, write: if true) |
| Build error: KSP | Ensure KSP version matches Kotlin version in `libs.versions.toml` |

---

## 📝 Academic Notes

- All Firebase usage complies with Google's free-tier terms
- App targets Android API 26+ (Android 8.0 Oreo and above)
- MVVM architecture with clean separation of concerns
- Offline-first design using Room DB as local mirror
- GenAI integration: AI caption suggestions in PostChallengeScreen (expandable to Gemini API)

---

*Made with ❤️ for village sports communities across India*
