# AGENTS.md

Context, conventions, and actionable technical instructions for AI coding agents working on the **app-finance** repository.

---

## 1. Project Overview

**app-finance** is a modern native Android personal finance and credit tracking application built with **Kotlin** and **Jetpack Compose** (Material 3). It follows a local-first offline architecture powered by a local **SQLite** engine paired with cloud synchronization via the **Turso (libSQL)** HTTP Pipeline API.

### Key Technologies & Frameworks
- **Language**: Kotlin 2.0.0 (targeting JVM 11 / Android SDK 35)
- **UI Toolkit**: Jetpack Compose with Material 3 & Cupertino-styled design components
- **Navigation**: Jetpack Compose Navigation (`androidx.navigation:navigation-compose:2.8.2`)
- **State & Architecture**: Clean Architecture + MVVM/MVI principles using Kotlin Coroutines, `StateFlow`, and Unidirectional Data Flow (UDF)
- **Local Database**: Native Android SQLite (`SQLiteOpenHelper`)
- **Cloud Synchronization**: Turso (libSQL) HTTP pipeline integration via OkHttp 4.12.0 and Gson 2.10.1
- **Build System**: Gradle 8.9+ with Kotlin DSL (`build.gradle.kts`, `settings.gradle.kts`, Version Catalog in `gradle/libs.versions.toml`)
- **Target Platform**: Android API 24 (minSdk) to API 35 (compileSdk & targetSdk)

---

## 2. Environment & Tooling Setup

### Prerequisites
- **JDK 17+**: Required by Android Gradle Plugin (AGP) 8.9.x.
  - *Windows note*: If default `java` is JDK 11, ensure `JAVA_HOME` points to JDK 17 (e.g., `C:\Program Files\Java\jdk-17`).
- **Android SDK**: API 35 SDK platforms and build-tools installed.
- **SDK Path**: Defined in `local.properties`:
  ```properties
  sdk.dir=C:\\Users\\<USER>\\AppData\\Local\\Android\\Sdk
  ```

### Quick Environment Verification Commands

**PowerShell (Windows)**:
```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
.\gradlew.bat --version
```

**Bash / Linux / macOS**:
```bash
export JAVA_HOME="/path/to/jdk-17"
./gradlew --version
```

---

## 3. Development Workflow & Build Commands

Always execute commands through the Gradle wrapper (`.\gradlew.bat` on Windows or `./gradlew` on Unix).

### Core Build Tasks

| Action | Windows PowerShell | Bash / Linux / macOS | Output Location |
| :--- | :--- | :--- | :--- |
| **Clean Build** | `.\gradlew.bat clean` | `./gradlew clean` | `build/`, `app/build/` |
| **Assemble Debug APK** | `.\gradlew.bat assembleDebug` | `./gradlew assembleDebug` | `app/build/outputs/apk/debug/app-debug.apk` |
| **Assemble Release APK** | `.\gradlew.bat assembleRelease` | `./gradlew assembleRelease` | `app/build/outputs/apk/release/app-release-unsigned.apk` |
| **Run Lint Checks** | `.\gradlew.bat lintDebug` | `./gradlew lintDebug` | `app/build/reports/lint-results-debug.html` |
| **Compile Kotlin Only** | `.\gradlew.bat compileDebugKotlin` | `./gradlew compileDebugKotlin` | `app/build/intermediates/javac/` |

---

## 4. Testing Instructions

### Executing Tests

- **Run all local unit tests**:
  ```powershell
  $env:JAVA_HOME = "C:\Program Files\Java\jdk-17"; .\gradlew.bat testDebugUnitTest
  ```
- **Run a single test class**:
  ```powershell
  .\gradlew.bat testDebugUnitTest --tests "com.gtc.app_finance.ExampleUnitTest"
  ```
- **Run a specific test method**:
  ```powershell
  .\gradlew.bat testDebugUnitTest --tests "com.gtc.app_finance.ExampleUnitTest.addition_isCorrect"
  ```
- **Run connected instrumentation tests** *(requires running Android emulator or connected device via ADB)*:
  ```powershell
  .\gradlew.bat connectedDebugAndroidTest
  ```

### Test Structure & Guidelines
- **Unit Tests (`app/src/test/java/`)**:
  - Focus on Repositories, ViewModels, business logic, financial summary calculations, and data mappers (`toDomain()`, `fromDomain()`).
  - Unit tests run directly on the host JVM (Temurin / OpenJDK 17).
  - Fast feedback loop; no Android device needed.
- **Instrumented Tests (`app/src/androidTest/java/`)**:
  - Focus on Android-specific components, Compose UI interactions, and SQLite database migrations/queries.

---

## 5. Architecture & Codebase Layout

```
app/src/main/
├── AndroidManifest.xml
├── java/com/gtc/app_finance/
│   ├── FinanceApplication.kt            # Application class with Koin DI container setup
│   ├── MainActivity.kt                  # Activity entry point with Compose host
│   ├── di/                              # Dependency Injection modules (AppModules.kt)
│   ├── data/
│   │   ├── dao/                         # Data Access Objects for SQLite
│   │   │   ├── CreditDao.kt
│   │   │   ├── PaymentDao.kt
│   │   │   └── TransactionDao.kt
│   │   ├── database/                    # Database setup, helpers & remote sync
│   │   │   ├── AppDatabase.kt           # SQLite database wrapper
│   │   │   ├── DatabaseConfig.kt        # Database credentials and configuration model
│   │   │   ├── TursoConfigProvider.kt   # Dynamic token provider & failover manager
│   │   │   ├── TursoDatabaseHelper.kt   # SQLiteOpenHelper schema & table DDL
│   │   │   └── TursoSyncClient.kt       # Remote HTTP libSQL pipeline sync
│   │   ├── entity/                      # Database entities (SQLite row representations)
│   │   │   ├── CreditEntity.kt
│   │   │   ├── PaymentEntity.kt
│   │   │   └── TransactionEntity.kt
│   │   └── repository/
│   │       └── FinanceRepository.kt     # Unified data layer combining local + remote
│   ├── domain/
│   │   └── model/                       # Domain models used by UI & ViewModels
│   │       ├── Credit.kt
│   │       ├── DatabaseDiagnostic.kt
│   │       ├── FinancialSummary.kt
│   │       ├── Payment.kt
│   │       └── Transaction.kt
│   └── ui/
│       ├── components/                  # Reusable UI widgets & BottomSheets
│       │   ├── AddCreditSheet.kt
│       │   ├── AddPaymentSheet.kt
│       │   ├── AddTransactionSheet.kt
│       │   ├── CreditCardItem.kt
│       │   ├── CupertinoCard.kt
│       │   ├── CupertinoSegmentedControl.kt
│       │   ├── CupertinoTabBar.kt
│       │   └── DatabaseDiagnosticSheet.kt
│       ├── main/                        # Shell UI & navigation graph
│       │   ├── MainScreen.kt            # Scaffold with BottomBar & content
│       │   ├── Navigation.kt            # NavHost and screen routing
│       │   └── SimpleViewModelFactory.kt# Factory for ViewModel instantiation
│       ├── screens/                     # Feature screens & corresponding ViewModels
│       │   ├── analytics/
│       │   ├── credits/
│       │   ├── dashboard/
│       │   └── transactions/
│       ├── theme/                       # Color palette, Shapes, Typography & Theme
│       └── utils/
│           └── CurrencyFormatter.kt     # Financial amount formatting (COP/USD)
```

---

## 6. Code Style & Implementation Guidelines

### Kotlin Conventions
- Strictly adhere to official Kotlin style conventions (`kotlin.code.style=official`).
- Keep code idiomatic: prefer immutability (`val`), expressions over statements, data classes, and sealed interfaces.
- Avoid passing raw `Context` deep into business logic or Compose hierarchies; pass repository dependencies or callbacks.

### Jetpack Compose Standards
- **Stateless Composables**: Separate stateful screen containers from stateless presentation composables.
- **State Hoisting**: Pass events up via lambda callbacks (e.g., `onDismiss: () -> Unit`, `onConfirm: (Transaction) -> Unit`) and state down via parameters.
- **Modifier Guidelines**: Always expose `modifier: Modifier = Modifier` as the first optional parameter on custom UI components.
- **Theme Usage**: Always read styles and colors from `MaterialTheme.colorScheme` and `MaterialTheme.typography` instead of hardcoding raw values.

### State & Concurrency Guidelines
- Use Kotlin Coroutines and Kotlin `StateFlow` for observable UI state.
- Perform all disk I/O (SQLite queries) and network calls (Turso Cloud HTTP pipeline) on `Dispatchers.IO` using `withContext(Dispatchers.IO)`.
- Never block the UI thread (`Dispatchers.Main`).

### Database & Turso Sync Protocol
- **Local First**: Every write (transaction, credit, payment) is first saved to the local SQLite database via the appropriate DAO.
- **Remote Pipeline Sync**: Following a successful local write, `FinanceRepository` dispatches the equivalent SQL statement to Turso Cloud using `TursoSyncClient.executeQuery()`.
- **Failover / Resiliency**: `TursoSyncClient` detects HTTP 401 unauthorized errors and automatically switches between the primary and backup auth tokens managed dynamically by `TursoConfigProvider`.

---

## 7. Version Catalog & Dependency Management

All external dependencies and plugins are managed in `gradle/libs.versions.toml`:
- **Libraries**:
  - Compose BOM: `androidx.compose:compose-bom:2024.09.00`
  - Core & Lifecycle: `lifecycle-runtime-ktx:2.8.6`, `lifecycle-viewmodel-compose:2.8.6`
  - Navigation: `androidx.navigation:navigation-compose:2.8.2`
  - Dependency Injection: `io.insert-koin:koin-android:3.5.6`, `io.insert-koin:koin-androidx-compose:3.5.6`
  - Networking: `com.squareup.okhttp3:okhttp:4.12.0`
  - JSON serialization: `com.google.code.gson:gson:2.10.1`
- **Adding new dependencies**: Add the version and library definition to `gradle/libs.versions.toml` and reference it via `alias(libs.<name>)` in `app/build.gradle.kts`. Do not hardcode version strings in `build.gradle.kts`.

---

## 8. Pull Request & Pre-Commit Verification Checklist

Before creating a commit or submitting a pull request, every agent must verify:

1. **Lint Verification**:
   ```powershell
   .\gradlew.bat lintDebug
   ```
   Ensure no new lint errors or missing resource warnings are introduced.

2. **Unit Tests**:
   ```powershell
   .\gradlew.bat testDebugUnitTest
   ```
   All tests must execute and pass without failure.

3. **APK Assembly**:
   ```powershell
   .\gradlew.bat assembleDebug
   ```
   Verify that compilation succeeds and the APK packages cleanly.

4. **Commit Format**:
   Follow concise imperative commit messages:
   - `feat: add transaction category filter`
   - `fix: handle edge case in remaining credit calculation`
   - `refactor: extract Cupertino segmented control component`
   - `test: add unit tests for FinanceRepository summary calculation`
