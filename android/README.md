# Hubble Android

Android client for the Hubble application, built in Java using MVVM architecture and Retrofit to communicate with the Hubble Spring Boot backend.

---

## Project Structure

```
java/com/example/hubble/
│
├── data/
│   ├── api/
│   │   ├── ApiClient.java          ← Retrofit setup + base URL
│   │   └── ApiService.java         ← All Spring Boot endpoints
│   ├── model/
│   │   └── User.java               ← Mirrors Spring Boot JSON response
│   └── repository/
│       └── UserRepository.java     ← Single source of truth for data
│
├── view/
│   └── MainActivity.java           ← Displays data, handles user input
│
├── viewmodel/
│   └── MainViewModel.java          ← Manages UI state and talks to Repository
│
├── adapter/                        ← RecyclerView adapters for lists
│
├── base/
│   └── BaseActivity.java           ← Shared logic across all Activities
│
└── utils/
    ├── Constants.java              ← Base URL, shared keys
    ├── SessionManager.java         ← JWT token storage
    └── NetworkUtils.java           ← Internet connection check
```

---

## How Each Layer Works

### View (`view/`)
Activities and Fragments. Responsible only for displaying data and capturing user input. Contains no business logic. Observes ViewModel via LiveData and updates the UI automatically when data changes.

### ViewModel (`viewmodel/`)
The middleman between View and Repository. Holds and manages UI-related data. Survives screen rotations. Never touches the API directly — it only talks to the Repository.

### Repository (`data/repository/`)
The single source of truth. Decides where data comes from — the remote API or local cache. Handles success and error cases so the ViewModel stays clean.

### API (`data/api/`)
- `ApiClient.java` — sets up Retrofit with the Spring Boot base URL
- `ApiService.java` — defines all API endpoint interfaces

### Model (`data/model/`)
Plain Java objects (POJOs) that mirror the JSON responses from the Spring Boot backend.

---

## Data Flow

```
View (Activity)
     ↕  observes LiveData
ViewModel
     ↕  requests data
Repository
     ↕  calls endpoints
ApiService
     ↕  HTTP requests
Spring Boot Backend
```

---

## Tech Stack

| Library | Purpose |
|---|---|
| Retrofit | HTTP client for API calls |
| Gson | JSON parsing |
| LiveData | Observable data for UI updates |
| ViewModel | Lifecycle-aware UI state management |
| ConstraintLayout | UI layouts |

---

## Notes

- `10.0.2.2` is used as the base URL host when running on an Android emulator to reach `localhost` on your development machine
- JWT token is saved to SharedPreferences via `SessionManager` after login and attached to authenticated requests
- Add new screens by creating a subfolder inside `view/` with its Activity and a matching ViewModel inside `viewmodel/`
