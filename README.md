MediCC — Medical Drone Coordination Center

An Android application for coordinating medical blood delivery drones between blood banks and hospitals in real time.

Built with Kotlin and Jetpack Compose following MVVM architecture.


Features
Hospital Request System — lists all hospitals with pending blood requests and emergency priority flags
Drone Dispatch — dispatch a drone to a hospital directly from the detail screen
Mission Dashboard — live overview of active missions, emergencies, and charging drones
Drone Fleet Monitor — battery level indicators, signal strength, and real-time status for each drone
Live Weather Integration — real-time wind speed, temperature, humidity, and flight safety evaluation via OpenWeatherMap API
Flight Safety Gate — automatically flags unsafe conditions (wind > 10m/s, rain, thunderstorm) and pauses dispatch
Filterable Dashboard — tap any stat card to filter the drone fleet by status
Mission Archive — coming in next update



Architecture

MediCC
├── Data Layer
│   ├── Hospital data class
│   ├── Drone data class
│   └── WeatherResponse data class
├── ViewModel Layer
│   └── MediCCViewModel — single source of truth for all app state
└── UI Layer
    ├── HospitalListScreen
    ├── HospitalDetailScreen
    ├── DashboardScreen
    ├── ArchiveScreen
    └── Shared Components (DroneCard, StatCard, WeatherCard, EmergencyBanner)


How to Run


Clone the repository


bashgit clone https://github.com/HasanOzay/MediCC.git


Open in Android Studio Giraffe or later
Add your OpenWeatherMap API key in MediCCViewModel.kt:


kotlinapiKey = "YOUR_API_KEY_HERE"


Get a free API key at openweathermap.org
Run on a physical device or emulator (API 24+)



Project Structure

app/src/main/java/com/example/medicc/
├── MainActivity.kt       — all screens, components, ViewModel, navigation
└── WeatherApi.kt         — Retrofit setup, API interface, data classes


What I Learned


Declarative UI with Jetpack Compose — component-based architecture where each composable has one job
MVVM architecture — separating UI from business logic using a shared ViewModel
Jetpack Navigation — nested NavHost for independent tab navigation flows
REST API integration — Retrofit + Gson for live weather data with Loading/Success/Error state handling
Coroutines — non-blocking network calls using viewModelScope and suspend functions
Android lifecycle — how ViewModel survives screen rotation while Compose state does not



Roadmap


 Mission archive with full delivery history
 Google Maps SDK integration for live drone tracking
 WebSocket connection for real-time drone telemetry
 Push notifications for emergency requests
 Multi-city weather support along flight path



Author

Hasan Özay


GitHub: @YOUR_USERNAME
Email: hasanozay0405@gmail.com
Location: Düzce, Türkiye
