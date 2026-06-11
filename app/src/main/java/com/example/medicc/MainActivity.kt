package com.example.medicc  // change to match your package name

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

// ─ DATA ─

data class Hospital(
    val id: Int,
    val name: String,
    val city: String,
    val bloodNeeded: String,
    val isEmergency: Boolean
)

data class Drone(
    val id: String,
    val battery: Int,
    val signal: String,
    val status: String
)



// ── WEATHER STATE

// Represents the three possible states of a network call

sealed class WeatherState {
    object Loading : WeatherState()                    // API call in progress
    data class Success(val data: WeatherResponse) : WeatherState()  // got data
    data class Error(val message: String) : WeatherState()          // something failed
}

// ── VIEWMODEL ───

class MediCCViewModel : ViewModel() {
    //list of hospitals
    var hospitals by mutableStateOf(
        listOf(
            Hospital(1, "St. Mary Medical Center", "Istanbul",  "O-",  isEmergency = true),
            Hospital(2, "Ankara City Hospital",    "Ankara",    "A+",  isEmergency = false),
            Hospital(3, "Aegean Health Complex",   "Izmir",     "B+",  isEmergency = true),
            Hospital(4, "North General Hospital",  "Bursa",     "AB-", isEmergency = false),
            Hospital(5, "Central Clinic",          "Antalya",   "O+",  isEmergency = false),
        )
    )
        private set
//list of drones
    var drones by mutableStateOf(
        listOf(
            Drone("D001", battery = 85, signal = "Strong",   status = "In Transit"),
            Drone("D002", battery = 62, signal = "Strong",   status = "Standby"),
            Drone("D003", battery = 32, signal = "Moderate", status = "Returning"),
            Drone("D004", battery = 8,  signal = "Weak",     status = "Charging"),
            Drone("D005", battery = 91, signal = "Strong",   status = "In Transit"),
        )
    )
        private set

    var dispatchedHospitalIds by mutableStateOf(setOf<Int>())
        private set

    // Weather state starts as Loading so the UI shows a spinner immediately
    var weatherState by mutableStateOf<WeatherState>(WeatherState.Loading)
        private set

    // init block runs automatically when the ViewModel is first created
    // Perfect place to kick off the initial API call
    init {
        fetchWeather("Istanbul")
    }

    fun fetchWeather(city: String) {
        // viewModelScope is a coroutine scope tied to the ViewModel's lifecycle
        // When the ViewModel is destroyed, all coroutines inside it are cancelled
        viewModelScope.launch {
            weatherState = WeatherState.Loading
            try {
                // This is the actual API call — suspend means it waits here
                // without blocking the UI thread
                val response = WeatherApiClient.service.getWeather(
                    city = city,
                    apiKey = "b97b4db9582e75dfaae8fc678186db7b"
                )
                weatherState = WeatherState.Success(response)
            } catch (e: Exception) {
                // Network error, wrong API key, no internet — all caught here
                weatherState = WeatherState.Error("Could not load weather: ${e.message}")
            }
        }
    }

    fun dispatchDrone(hospitalId: Int) {
        dispatchedHospitalIds = dispatchedHospitalIds + hospitalId
        val standbyDrone = drones.firstOrNull { it.status == "Standby" }
        if (standbyDrone != null) {
            drones = drones.map { drone ->
                if (drone.id == standbyDrone.id) drone.copy(status = "In Transit")
                else drone
            }
        }
    }

    val activeMissions get() = drones.count { it.status == "In Transit" }
    val chargingCount  get() = drones.count { it.status == "Charging" }
    val emergencyCount get() = hospitals.count { it.isEmergency }
}

// ── BOTTOM NAV ───

sealed class BottomNavItem(val route: String, val label: String, val icon: String) {
    object Hospitals : BottomNavItem("hospitals", "Hospitals", "🏥")
    object Dashboard : BottomNavItem("dashboard", "Dashboard", "📡")
}

// ── ACTIVITY ────

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MediCCApp()
        }
    }
}

// ── ROOT ──

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediCCApp() {
    // viewModel() creates the ViewModel and keeps the same instance alive
    // across recompositions and screen rotations
    // This is the ONE place the ViewModel is created — everything else receives it
    val viewModel: MediCCViewModel = viewModel()

    val bottomNavController = rememberNavController()
    val currentRoute = bottomNavController.currentBackStackEntryAsState().value?.destination?.route
    val showBottomBar = !currentRoute.orEmpty().startsWith("hospital_detail/")

    Scaffold(
        containerColor = Color(0xFF0D1117),
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(
                    currentRoute = currentRoute,
                    onItemClick = { item ->
                        bottomNavController.navigate(item.route) {
                            popUpTo(bottomNavController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = bottomNavController,
            startDestination = "hospitals",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("hospitals") {
                // Pass the viewModel down to the hospitals flow
                HospitalsFlow(viewModel = viewModel)
            }
            composable("dashboard") {
                // Pass the viewModel down to the dashboard
                DashboardScreen(viewModel = viewModel)
            }
        }
    }
}

// ── BOTTOM NAV BAR ───

@Composable
fun BottomNavBar(currentRoute: String?, onItemClick: (BottomNavItem) -> Unit) {
    val items = listOf(BottomNavItem.Hospitals, BottomNavItem.Dashboard)
    NavigationBar(containerColor = Color(0xFF161B22)) {
        items.forEach { item ->
            val selected = currentRoute == item.route
            NavigationBarItem(
                selected = selected,
                onClick = { onItemClick(item) },
                icon = { Text(item.icon, fontSize = 20.sp) },
                label = {
                    Text(
                        text = item.label,
                        fontSize = 11.sp,
                        color = if (selected) Color(0xFF58A6FF) else Color(0xFF8B949E)
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF58A6FF),
                    indicatorColor = Color(0xFF1C2128)
                )
            )
        }
    }
}

// ── HOSPITALS FLOW ──

@Composable
fun HospitalsFlow(viewModel: MediCCViewModel) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "hospital_list") {
        composable("hospital_list") {
            HospitalListScreen(
                hospitals = viewModel.hospitals,
                onHospitalClick = { hospital ->
                    navController.navigate("hospital_detail/${hospital.id}")
                },
                onArchiveClick = {
                    navController.navigate("archive")  // ← add this
                }
            )
        }
        composable("hospital_detail/{hospitalId}") { backStackEntry ->
            val id = backStackEntry.arguments?.getString("hospitalId")?.toInt()
            val hospital = viewModel.hospitals.find { it.id == id }
            if (hospital != null) {
                HospitalDetailScreen(
                    hospital = hospital,
                    alreadyDispatched = viewModel.dispatchedHospitalIds.contains(hospital.id),
                    onDispatch = { viewModel.dispatchDrone(hospital.id) },
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable("archive") {
            ArchiveScreen(onBack = { navController.popBackStack() })
        }
    }
}

// ── HOSPITAL LIST SCREEN ───

@Composable
fun HospitalListScreen(hospitals: List<Hospital>, onHospitalClick: (Hospital) -> Unit, onArchiveClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "MediCC",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF58A6FF)
                )
                Text(
                    text = "Medical Drone Coordination",
                    fontSize = 14.sp,
                    color = Color(0xFF8B949E),
                    modifier = Modifier.padding(bottom = 20.dp)
                )
            }
            Box(
                modifier = Modifier
                    .background(Color(0xFF161B22), RoundedCornerShape(8.dp))
                    .clickable { onArchiveClick() }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text("📋 Archive", color = Color(0xFF58A6FF), fontSize = 13.sp)
            }
        }
        val emergencies = hospitals.count { it.isEmergency }
        if (emergencies > 0) {
            EmergencyBanner(count = emergencies)
            Spacer(modifier = Modifier.height(16.dp))
        }
        Text(
            text = "HOSPITALS (${hospitals.size})",
            fontSize = 12.sp,
            color = Color(0xFF8B949E),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(hospitals) { hospital ->
                HospitalCard(hospital = hospital, onClick = { onHospitalClick(hospital) })
            }
        }
    }
}

// ── SHARED COMPONENTS ──

@Composable
fun EmergencyBanner(count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF3D1A1A), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "🚨", fontSize = 18.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$count EMERGENCY REQUEST${if (count > 1) "S" else ""} ACTIVE",
            color = Color(0xFFFF6B6B),
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HospitalCard(hospital: Hospital, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (hospital.isEmergency) Color(0xFF3D1A1A) else Color(0xFF1C2128),
                        RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = hospital.bloodNeeded,
                    color = if (hospital.isEmergency) Color(0xFFFF6B6B) else Color(0xFF58A6FF),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = hospital.name,
                    color = Color(0xFFE6EDF3),
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp
                )
                Text(text = hospital.city, color = Color(0xFF8B949E), fontSize = 13.sp)
            }
            if (hospital.isEmergency) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFF3D1A1A), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(text = "URGENT", color = Color(0xFFFF6B6B), fontSize = 11.sp)
                }
            }
        }
    }
}

// ── HOSPITAL DETAIL SCREEN ───
@Composable
fun HospitalDetailScreen(
    hospital: Hospital,
    alreadyDispatched: Boolean,   // comes from ViewModel — did this hospital get a drone?
    onDispatch: () -> Unit,        // tells ViewModel to dispatch
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
            .padding(16.dp)
    ) {
        TextButton(onClick = onBack) {
            Text("← Back", color = Color(0xFF58A6FF))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = hospital.name,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFE6EDF3)
        )
        Text(
            text = hospital.city,
            color = Color(0xFF8B949E),
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
        )
        DetailRow("Blood Type Needed", hospital.bloodNeeded)
        DetailRow("Priority", if (hospital.isEmergency) "🚨 Emergency" else "Standard")
        DetailRow(
            "Drone Status",
            // Now reads from ViewModel state, not local state
            if (alreadyDispatched) "In Transit" else "Awaiting Dispatch"
        )
        DetailRow("Est. Delivery", "~22 minutes")
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            // Calls ViewModel function — does not touch data directly
            onClick = { if (!alreadyDispatched) onDispatch() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (alreadyDispatched) Color(0xFF238636) else Color(0xFF1F6FEB)
            )
        ) {
            Text(
                text = if (alreadyDispatched) "✓ Drone Dispatched" else "Dispatch Drone",
                modifier = Modifier.padding(8.dp)
            )
        }

        // Show this info box after dispatch 
        if (alreadyDispatched) {
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1C2128), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Text("Mission Active", color = Color(0xFF58A6FF), fontWeight = FontWeight.Bold)
                    Text(
                        "Drone assigned and en route. Check dashboard for live status.",
                        color = Color(0xFF8B949E),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color(0xFF8B949E), fontSize = 14.sp)
        Text(text = value, color = Color(0xFFE6EDF3), fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color(0xFF21262D))
    )
}

// ── DASHBOARD SCREEN ──────────────────────────────────────────────────────────

enum class DashboardFilter { ACTIVE, EMERGENCIES, CHARGING }

@Composable
fun DashboardScreen(viewModel: MediCCViewModel) {
    var activeFilter by remember { mutableStateOf<DashboardFilter?>(null) }

    val filteredDrones = when (activeFilter) {
        DashboardFilter.ACTIVE   -> viewModel.drones.filter { it.status == "In Transit" }
        DashboardFilter.CHARGING -> viewModel.drones.filter { it.status == "Charging" }
        else                     -> viewModel.drones
    }
    val filteredHospitals = when (activeFilter) {
        DashboardFilter.EMERGENCIES -> viewModel.hospitals.filter { it.isEmergency }
        else                        -> emptyList()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Mission Control", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF58A6FF))
            Text(
                "Live drone coordination dashboard",
                fontSize = 14.sp,
                color = Color(0xFF8B949E),
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    label = "ACTIVE",
                    value = "${viewModel.activeMissions}",
                    color = Color(0xFF58A6FF),
                    selected = activeFilter == DashboardFilter.ACTIVE,
                    onClick = {
                        activeFilter = if (activeFilter == DashboardFilter.ACTIVE) null
                        else DashboardFilter.ACTIVE
                    }
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    label = "EMERGENCIES",
                    value = "${viewModel.emergencyCount}",
                    color = Color(0xFFFF6B6B),
                    selected = activeFilter == DashboardFilter.EMERGENCIES,
                    onClick = {
                        activeFilter = if (activeFilter == DashboardFilter.EMERGENCIES) null
                        else DashboardFilter.EMERGENCIES
                    }
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    label = "CHARGING",
                    value = "${viewModel.chargingCount}",
                    color = Color(0xFFF0A500),
                    selected = activeFilter == DashboardFilter.CHARGING,
                    onClick = {
                        activeFilter = if (activeFilter == DashboardFilter.CHARGING) null
                        else DashboardFilter.CHARGING
                    }
                )
            }
        }


        item {
            when (val state = viewModel.weatherState) {

                // Still loading — show a spinner
                is WeatherState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF161B22), RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF58A6FF))
                    }
                }

                // Got data — show the weather card
                is WeatherState.Success -> {
                    WeatherCard(
                        weather = state.data,
                        onRefresh = { viewModel.fetchWeather("Istanbul") }
                    )
                }

                // Something went wrong — show error with retry button
                is WeatherState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF3D1A1A), RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Column {
                            Text("⚠ Weather unavailable", color = Color(0xFFFF6B6B), fontWeight = FontWeight.Bold)
                            Text(state.message, color = Color(0xFF8B949E), fontSize = 12.sp)
                            TextButton(onClick = { viewModel.fetchWeather("Istanbul") }) {
                                Text("Retry", color = Color(0xFF58A6FF))
                            }
                        }
                    }
                }
            }
        }


        if (activeFilter != null) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF161B22), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (activeFilter) {
                            DashboardFilter.ACTIVE      -> "🚁 Drones in transit"
                            DashboardFilter.EMERGENCIES -> "🚨 Emergency hospitals"
                            DashboardFilter.CHARGING    -> "🔋 Drones charging"
                            else -> ""
                        },
                        color = Color(0xFFE6EDF3),
                        fontSize = 13.sp
                    )
                    TextButton(onClick = { activeFilter = null }) {
                        Text("Clear", color = Color(0xFF58A6FF), fontSize = 12.sp)
                    }
                }
            }
        }



        if (activeFilter != DashboardFilter.EMERGENCIES) {
            item {
                Text(
                    text = if (activeFilter == null) "DRONE FLEET (${viewModel.drones.size})"
                    else "FILTERED DRONES (${filteredDrones.size})",
                    fontSize = 12.sp,
                    color = Color(0xFF8B949E)
                )
            }
            items(filteredDrones) { drone -> DroneCard(drone = drone) }
        }

        if (activeFilter == DashboardFilter.EMERGENCIES) {
            item {
                Text(
                    "EMERGENCY HOSPITALS (${filteredHospitals.size})",
                    fontSize = 12.sp,
                    color = Color(0xFF8B949E)
                )
            }
            items(filteredHospitals) { hospital ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF161B22), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color(0xFF3D1A1A), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(hospital.bloodNeeded, color = Color(0xFFFF6B6B), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(hospital.name, color = Color(0xFFE6EDF3), fontSize = 15.sp)
                            Text(hospital.city, color = Color(0xFF8B949E), fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

// ── DASHBOARD COMPONENTS ──────────────────────────────────────────────────────

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    color: Color,
    selected: Boolean = false,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .background(
                if (selected) color.copy(alpha = 0.15f) else Color(0xFF161B22),
                RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Column {
            Text(text = value, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = color)
            Text(text = label, fontSize = 10.sp, color = Color(0xFF8B949E))
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (selected) "tap to clear ↑" else "tap to filter",
                fontSize = 9.sp,
                color = color.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun DroneCard(drone: Drone) {
    val statusColor = when (drone.status) {
        "In Transit" -> Color(0xFF58A6FF)
        "Charging"   -> Color(0xFFF0A500)
        "Returning"  -> Color(0xFF8957E5)
        else         -> Color(0xFF8B949E)
    }
    val batteryColor = when {
        drone.battery >= 60 -> Color(0xFF238636)
        drone.battery >= 30 -> Color(0xFFF0A500)
        else                -> Color(0xFFFF6B6B)
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF161B22), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Drone ${drone.id}", color = Color(0xFFE6EDF3), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Box(
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(drone.status, color = statusColor, fontSize = 11.sp)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text("Battery  ${drone.battery}%", color = Color(0xFF8B949E), fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(Color(0xFF21262D), RoundedCornerShape(3.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(drone.battery / 100f)
                        .height(6.dp)
                        .background(batteryColor, RoundedCornerShape(3.dp))
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Signal: ${drone.signal}", color = Color(0xFF8B949E), fontSize = 12.sp)
        }
    }
}
@Composable
fun WeatherCard(weather: WeatherResponse, onRefresh: () -> Unit) {
    val windSpeed    = weather.wind.speed
    val condition    = weather.weather.firstOrNull()?.main ?: "Unknown"
    val description  = weather.weather.firstOrNull()?.description ?: ""
    val temp         = weather.main.temp.toInt()
    val humidity     = weather.main.humidity

    // Flight condition logic — drones shouldn't fly in high wind or rain
    val flightSafe = windSpeed < 10.0 && condition != "Rain" && condition != "Thunderstorm"

    val weatherEmoji = when (condition) {
        "Clear"        -> "☀️"
        "Clouds"       -> "☁️"
        "Rain"         -> "🌧️"
        "Thunderstorm" -> "⛈️"
        "Snow"         -> "❄️"
        "Mist", "Fog"  -> "🌫️"
        else           -> "🌡️"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF161B22), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Flight Weather · ${weather.name}",
                    color = Color(0xFF8B949E),
                    fontSize = 12.sp
                )
                TextButton(onClick = onRefresh) {
                    Text("↻ Refresh", color = Color(0xFF58A6FF), fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main weather info
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = weatherEmoji, fontSize = 36.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "$temp°C  ·  $description",
                        color = Color(0xFFE6EDF3),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Wind: ${windSpeed}m/s  ·  Humidity: ${humidity}%",
                        color = Color(0xFF8B949E),
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Flight condition badge — this is the key feature for MediCC
            Box(
                modifier = Modifier
                    .background(
                        if (flightSafe) Color(0xFF1A3D1A) else Color(0xFF3D1A1A),
                        RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (flightSafe) "✓ Conditions safe for drone flight"
                    else "✗ Conditions unsafe — drone dispatch paused",
                    color = if (flightSafe) Color(0xFF3FB950) else Color(0xFFFF6B6B),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
@Composable
fun ArchiveScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
            .padding(16.dp)
    ) {
        TextButton(onClick = onBack) {
            Text("← Back", color = Color(0xFF58A6FF))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Mission Archive",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF58A6FF)
        )
        Text(
            text = "Completed mission records",
            fontSize = 14.sp,
            color = Color(0xFF8B949E),
            modifier = Modifier.padding(top = 4.dp)
        )
        Spacer(modifier = Modifier.weight(1f))
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🚧", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Still working on it",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE6EDF3)
            )
            Text(
                text = "Mission history coming in the next update",
                fontSize = 14.sp,
                color = Color(0xFF8B949E),
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}