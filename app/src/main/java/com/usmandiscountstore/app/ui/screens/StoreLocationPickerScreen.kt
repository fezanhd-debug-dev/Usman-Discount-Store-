package com.usmandiscountstore.app.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.preference.PreferenceManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.usmandiscountstore.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import java.util.Locale

/**
 * Map-based store location picker.
 * Admin taps on map → pin drops → address auto-fetches → Save.
 */
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetTextI18n")
@Composable
fun StoreLocationPickerScreen(
    initialLat: Double,
    initialLon: Double,
    onBack: () -> Unit,
    onLocationPicked: (lat: Double, lon: Double, address: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // osmdroid config (required)
    LaunchedEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName
        Configuration.getInstance().load(
            context,
            PreferenceManager.getDefaultSharedPreferences(context)
        )
    }

    var selectedLat by remember { mutableDoubleStateOf(initialLat) }
    var selectedLon by remember { mutableDoubleStateOf(initialLon) }
    var addressText by remember { mutableStateOf("") }
    var isGeocoding by remember { mutableStateOf(false) }
    var mapView by remember { mutableStateOf<MapView?>(null) }

    var hasLocationPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
        )
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { r -> hasLocationPerm = r[Manifest.permission.ACCESS_FINE_LOCATION] == true }

    // Reverse geocode
    suspend fun reverseGeocode(lat: Double, lon: Double): String {
        return try {
            val geocoder = Geocoder(context, Locale.ENGLISH)
            val addresses: List<Address>? = geocoder.getFromLocation(lat, lon, 1)
            if (addresses.isNullOrEmpty()) {
                "%.5f, %.5f".format(lat, lon)
            } else {
                val a = addresses[0]
                val parts = mutableListOf<String>()
                if (!a.thoroughfare.isNullOrBlank()) parts.add(a.thoroughfare)
                if (!a.locality.isNullOrBlank()) parts.add(a.locality)
                if (!a.subAdminArea.isNullOrBlank()) parts.add(a.subAdminArea)
                if (!a.adminArea.isNullOrBlank()) parts.add(a.adminArea)
                if (!a.countryName.isNullOrBlank()) parts.add(a.countryName)
                if (parts.isEmpty()) {
                    a.getAddressLine(0) ?: "%.5f, %.5f".format(lat, lon)
                } else {
                    parts.joinToString(", ")
                }
            }
        } catch (e: Exception) {
            "%.5f, %.5f".format(lat, lon)
        }
    }

    // Auto-fetch address when location changes
    LaunchedEffect(selectedLat, selectedLon) {
        isGeocoding = true
        addressText = withContext(Dispatchers.IO) { reverseGeocode(selectedLat, selectedLon) }
        isGeocoding = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Store Location", fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Tap on map to set pin", fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (!hasLocationPerm) {
                            permLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        } else {
                            // Jump to current location
                            mapView?.let { mv ->
                                val lm = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
                                try {
                                    val loc = lm.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
                                        ?: lm.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
                                    loc?.let {
                                        mv.controller.setCenter(GeoPoint(it.latitude, it.longitude))
                                        mv.controller.setZoom(18.0)
                                        selectedLat = it.latitude
                                        selectedLon = it.longitude
                                    }
                                } catch (_: SecurityException) {}
                            }
                        }
                    }) {
                        Icon(Icons.Default.MyLocation, "My Location", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandGreen)
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = Color.White
            ) {
                Column(Modifier.padding(16.dp)) {
                    // Selected address card
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = BrandGreenLight),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, null, tint = BrandGreen)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("SELECTED LOCATION", fontSize = 10.sp,
                                    color = TextGray, fontWeight = FontWeight.Bold)
                                if (isGeocoding) {
                                    Text("Fetching address...", fontSize = 12.sp, color = BrandGreen)
                                } else {
                                    Text(
                                        addressText.ifBlank { "%.5f, %.5f".format(selectedLat, selectedLon) },
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextDark
                                    )
                                }
                                Text(
                                    "Lat: %.5f  |  Lon: %.5f".format(selectedLat, selectedLon),
                                    fontSize = 10.sp,
                                    color = TextGray
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Button(
                        onClick = {
                            onLocationPicked(selectedLat, selectedLon, addressText)
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                        enabled = !isGeocoding
                    ) {
                        Icon(Icons.Default.Check, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Set This Location", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        },
        containerColor = BackgroundLight
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(17.0)
                        controller.setCenter(GeoPoint(initialLat, initialLon))

                        // Pin marker
                        val marker = Marker(this).apply {
                            position = GeoPoint(initialLat, initialLon)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            title = "Store"
                        }
                        overlays.add(marker)

                        // Tap on map
                        val eventsReceiver = object : MapEventsReceiver {
                            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                                p?.let {
                                    selectedLat = it.latitude
                                    selectedLon = it.longitude
                                    marker.position = it
                                    invalidate()
                                }
                                return true
                            }
                            override fun longPressHelper(p: GeoPoint?): Boolean {
                                return false
                            }
                        }
                        overlays.add(MapEventsOverlay(eventsReceiver))

                        mapView = this
                    }
                }
            )

            // Top overlay hint
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp)
                    .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("Tap anywhere on the map to place the pin",
                    fontSize = 11.sp, color = TextDark, fontWeight = FontWeight.Medium)
            }
        }
    }
}
