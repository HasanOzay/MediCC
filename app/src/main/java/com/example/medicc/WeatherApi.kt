package com.example.medicc

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// ── DATA CLASSES ──────────────────────────────────────────────────────────────

// These match the JSON structure OpenWeatherMap sends back
// Gson reads the JSON and fills these classes automatically

data class WeatherResponse(
    val weather: List<WeatherDescription>,  // e.g. "Clear", "Rain"
    val main: MainData,                     // temperature, humidity
    val wind: WindData,                     // speed and direction
    val name: String                        // city name
)

data class WeatherDescription(
    val main: String,           // "Clear", "Clouds", "Rain"
    val description: String     // "clear sky", "light rain"
)

data class MainData(
    val temp: Double,           // temperature in Kelvin by default
    val humidity: Int           // humidity percentage
)

data class WindData(
    val speed: Double           // wind speed in metres per second
)

// ── API INTERFACE ─────────────────────────────────────────────────────────────

// This interface describes the API endpoint as a Kotlin function
// Retrofit reads this and builds the actual HTTP call for you
interface WeatherApiService {

    // @GET means this is an HTTP GET request
    // "weather" is the endpoint path — full URL becomes:
    // https://api.openweathermap.org/data/2.5/weather
    @GET("weather")
    suspend fun getWeather(           // suspend = this is a coroutine function
        @Query("q") city: String,     // ?q=Istanbul
        @Query("appid") apiKey: String, // &appid=YOUR_KEY
        @Query("units") units: String = "metric"  // &units=metric → Celsius
    ): WeatherResponse
}

// ── RETROFIT INSTANCE ─────────────────────────────────────────────────────────

// This object creates one shared Retrofit instance for the whole app
// "object" in Kotlin is a singleton — only one instance ever exists
object WeatherApiClient {
    private const val BASE_URL = "https://api.openweathermap.org/data/2.5/"

    // Retrofit builder — sets the base URL and JSON parser
    val service: WeatherApiService = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create()) // converts JSON → data class
        .build()
        .create(WeatherApiService::class.java)
}