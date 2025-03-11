package com.corps.healthmate.activities


import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList

import android.graphics.Typeface
import android.location.Location
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable

import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale

import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.corps.healthmate.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.activity.compose.setContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color

import com.corps.healthmate.ui.theme.HealthMateTheme
import kotlinx.coroutines.delay

import androidx.compose.foundation.layout.PaddingValues

import androidx.compose.material.icons.Icons

import androidx.compose.material3.Icon

import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.Card
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalPolice
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ElderlyWoman
import androidx.compose.material.icons.filled.EmergencyShare
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Typography


class EmergencyHandlerActivity : ComponentActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: Location? = null

    // Simplified Emergency Numbers with priority
    private val emergencyNumbers = mapOf(
        "Critical" to mapOf(
            "National Emergency" to "112",
            "Police" to "100",
            "Fire" to "101",
            "Ambulance" to "102",
            "Road Accident" to "1073"
        ),
        "Medical" to mapOf(
            "Ambulance" to "102",
            "Mental Care" to "1800-599-0019"
        ),
        "Support" to mapOf(
            "Women Helpline" to "1091",
            "Women Domestic Abuse" to "181",
            "Senior Citizens" to "1091",
            "Child Helpline" to "1098"
        )
    )

    private val requiredPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.CALL_PHONE,
        Manifest.permission.SEND_SMS
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        checkAndRequestPermissions()

        setContent {

            HealthMateTheme {
                EmergencyScreen(
                    onSosClick = { handleSosPress() },
                    onEmergencyNumberClick = { number -> initiateEmergencyCall(number) }
                )
            }
        }
    }

    @Composable
    fun EmergencyScreen(
        onSosClick: () -> Unit,
        onEmergencyNumberClick: (String) -> Unit
    ) {
        var isAnimating by remember { mutableStateOf(true) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(top = 24.dp, bottom = 16.dp)
            ) {
                // SOS Button Section
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Enhanced Pulse Effect
                    PulseEffect(isAnimating = isAnimating)

                    // Improved SOS Button
                    Button(
                        onClick = { onSosClick() },
                        modifier = Modifier
                            .size(140.dp)
                            .clip(CircleShape)
                            .shadow(
                                elevation = 8.dp,
                                shape = CircleShape,
                                spotColor = MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                            ),
                        colors = ButtonDefaults.buttonColors( // Fix: Change buttonElevation to colors
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation( // Fix: Use elevation here
                            defaultElevation = 8.dp,
                            pressedElevation = 4.dp,
                            hoveredElevation = 6.dp,
                            focusedElevation = 6.dp
                        ),
                        contentPadding = PaddingValues(0.dp)
                    )
                    {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "SOS",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.sp
                                ),
                                color = Color.White
                            )
                            Text(
                                text = "Emergency",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }

                // Emergency Categories Section
                Text(
                    text = "Emergency Services",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    color = MaterialTheme.colorScheme.onBackground
                )

                EmergencyCategories(onEmergencyNumberClick)
            }
        }
    }

    @Composable
    fun PulseEffect(isAnimating: Boolean) {
        val circles = listOf(
            remember { Animatable(0f) },
            remember { Animatable(0f) },
            remember { Animatable(0f) }
        )

        circles.forEachIndexed { index, animatable ->
            LaunchedEffect(key1 = true) {
                delay(index * 500L)
                animatable.animateTo(
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2000),
                        repeatMode = RepeatMode.Restart
                    )
                )
            }
        }

        circles.forEach { animatable ->
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(1f + (animatable.value * 2))
                    .alpha(1f - animatable.value)
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 1f - animatable.value),
                        shape = CircleShape
                    )
            )
        }
    }

    @Composable
    fun EmergencyCategories(onEmergencyNumberClick: (String) -> Unit) {
        emergencyNumbers.forEach { (category, numbers) ->
            EmergencyCategory(
                category = category,
                numbers = numbers,
                onNumberClick = onEmergencyNumberClick
            )
        }
    }

    @Composable
    fun EmergencyCategory(
        category: String,
        numbers: Map<String, String>,
        onNumberClick: (String) -> Unit
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(16.dp))
                .shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(16.dp),
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                ),
            backgroundColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .animateContentSize()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                getCategoryColor(category).copy(alpha = 0.1f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getCategoryIcon(category),
                            contentDescription = null,
                            tint = getCategoryColor(category),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Text(
                        text = category,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp
                        ),
                        modifier = Modifier.padding(start = 12.dp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                numbers.forEach { (service, number) ->
                    EmergencyButton(
                        service = service,
                        number = number,
                        onClick = { onNumberClick(number) }
                    )
                }
            }
        }
    }

    @Composable
    fun EmergencyButton(
        service: String,
        number: String,
        onClick: () -> Unit
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .height(56.dp),
            colors = ButtonDefaults.buttonColors( // Use Material 2 ButtonDefaults
                containerColor = getServiceColor(service).copy(alpha = 0.1f), // Change `containerColor` to `backgroundColor`
                contentColor = getServiceColor(service)
            ),
            shape = RoundedCornerShape(12.dp)
        )
        {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = getServiceIcon(service),
                        contentDescription = null,
                        tint = getServiceColor(service)
                    )
                    Text(
                        text = service,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
                Text(
                    text = number,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }

    private fun getServiceColor(service: String): Color {
        return when {
            service.contains("National Emergency") -> Color(0xFFE53935) // Red
            service.contains("Police") -> Color(0xFF1E88E5) // Blue
            service.contains("Fire") -> Color(0xFFFF5722) // Deep Orange
            service.contains("Ambulance") -> Color(0xFFE53935) // Red
            service.contains("Road Accident") -> Color(0xFFFF9800) // Orange
            service.contains("Mental Care") -> Color(0xFF7B1FA2) // Purple
            service.contains("Women") -> Color(0xFFD81B60) // Pink
            service.contains("Senior") -> Color(0xFF00897B) // Teal
            service.contains("Child") -> Color(0xFF43A047) // Green
            else -> Color(0xFF546E7A) // Blue Grey
        }
    }

    private fun getCategoryIcon(category: String): ImageVector {
        return when (category) {
            "Critical" -> Icons.Filled.Warning
            "Medical" -> Icons.Filled.LocalHospital
            "Support" -> Icons.Filled.SupportAgent
            else -> Icons.Filled.Emergency
        }
    }

    private fun getCategoryColor(category: String): Color {
        return when (category) {
            "Critical" -> Color(0xFFE53935)
            "Medical" -> Color(0xFF1E88E5)
            "Support" -> Color(0xFF43A047)
            else -> Color(0xFF546E7A)
        }
    }

    private fun getServiceIcon(service: String): ImageVector {
        return when {
            service.contains("National Emergency") -> Icons.Filled.Emergency
            service.contains("Ambulance") -> Icons.Filled.LocalHospital
            service.contains("Police") -> Icons.Filled.LocalPolice
            service.contains("Fire") -> Icons.Filled.LocalFireDepartment
            service.contains("Road Accident") -> Icons.Filled.DirectionsCar
            service.contains("Mental Care") -> Icons.Filled.Psychology
            service.contains("Women") -> Icons.Filled.Person
            service.contains("Senior") -> Icons.Filled.ElderlyWoman
            service.contains("Child") -> Icons.Filled.ChildCare
            else -> Icons.Filled.Call
        }
    }

    private fun checkAndRequestPermissions() {
        val notGrantedPermissions = requiredPermissions.filter {
            ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (notGrantedPermissions.isNotEmpty()) {
            MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialog_Rounded)
                .setTitle("Emergency Permissions Required")
                .setMessage("To ensure quick emergency response, we need access to your location and phone. This helps us contact emergency services faster when needed.")
                .setPositiveButton("Grant Access") { _, _ ->
                    ActivityCompat.requestPermissions(
                        this,
                        notGrantedPermissions.toTypedArray(),
                        PERMISSIONS_REQUEST_CODE
                    )
                }
                .setNegativeButton("Not Now", null)
                .show()
        }
    }

    private fun handleSosPress() {
        if (currentLocation != null) {
            sendSOSMessage()
        } else {
            getLastLocation { location ->
                currentLocation = location
                sendSOSMessage()
            }
        }
    }

    private fun sendSOSMessage() {
        currentLocation?.let { location ->
            val message = buildSOSMessage(location)
            // Show sending animation
            showSendingAnimation {
                // Send SMS to emergency contacts
                sendEmergencyMessage(message)
                // Call emergency number
                initiateEmergencyCall("112")
            }
        }
    }

    private fun buildSOSMessage(location: Location): String {
        return """
            EMERGENCY SOS!
            Location: https://www.google.com/maps?q=${location.latitude},${location.longitude}
            Medical Info: ${getMedicalInfo()}
            Please send help immediately!
        """.trimIndent()
    }

    private fun showSendingAnimation(onComplete: () -> Unit) {
        // Implement sending animation
    }

    private fun getLastLocation(onLocationReceived: (Location) -> Unit) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
            == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    location?.let { onLocationReceived(it) }
                }
        }
    }

    private fun sendEmergencyMessage(message: String) {
        // Implement SMS sending logic here
        // You'll need to handle SMS permissions and sending
    }

    private fun initiateEmergencyCall(number: String) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) 
            == PackageManager.PERMISSION_GRANTED) {
            val intent = Intent(Intent.ACTION_CALL)
            intent.data = Uri.parse("tel:$number")
            startActivity(intent)
        }
    }

    private fun getMedicalInfo(): String {
        // Implement this to return relevant medical information
        return "No known medical conditions"
    }

    companion object {
        private const val PERMISSIONS_REQUEST_CODE = 100
        private const val LOCATION_UPDATE_INTERVAL = 10000L
    }
}