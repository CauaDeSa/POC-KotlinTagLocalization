package com.example.poc_kotlindevicelocalization

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class VPNActivity : ComponentActivity() {

    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var tagListener: ListenerRegistration
    private lateinit var tagId: String

    private var tagName by mutableStateOf("Tag não atribuída")
    private var locationText by mutableStateOf("Localização: aguardando...")
    private var trackingEnabled by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tagId = intent?.getStringExtra("tagId")?: return

        tagListener = firestore.collection("tags").document(tagId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                snapshot?.let {
                    tagName = it.getString("name") ?: "Tag não atribuída"
                    val lat = it.getDouble("latitude") ?: 0.0
                    val lon = it.getDouble("longitude") ?: 0.0
                    locationText = "Localização: $lat, $lon"
                    trackingEnabled = it.getBoolean("trackingEnabled") == true
                }
            }

        setContent {
            MaterialTheme {
                VPNLayout(
                    tagName = tagName,
                    location = locationText,
                    trackingEnabled = trackingEnabled,
                    onToggleTracking = {
                        toggleTracking()
                    }
                )
            }
        }
    }

    private fun toggleTracking(){
        trackingEnabled = !trackingEnabled
        firestore.collection("tags").document(tagId)
            .update("trackingEnabled", trackingEnabled)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }

        if (trackingEnabled) startService(
            Intent(this, LocationService::class.java).apply {
                putExtra("tagId", tagId)
            })
    }

    override fun onDestroy() {
        super.onDestroy()
        tagListener.remove()
    }
}

@Composable
fun VPNLayout(
    tagName: String,
    location: String,
    trackingEnabled: Boolean,
    onToggleTracking: () -> Unit
) {
    val spacerHeight = if (trackingEnabled) 240.dp else 264.dp

    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseIn),
            repeatMode = RepeatMode.Reverse
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = tagName,
            fontSize = 24.sp,
            color = Color.Black,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(spacerHeight))

        Icon(
            imageVector = if (trackingEnabled) Icons.Default.MyLocation else Icons.Default.LocationOff,
            contentDescription = "Ícone de Localização",
            tint = if (trackingEnabled) Color(0xFF2196F3) else Color.Gray,
            modifier = Modifier
                    .size(
                        if (trackingEnabled) 96.dp else 48.dp
                    )
                    .graphicsLayer(
                        scaleX = if (trackingEnabled) scale else 1f,
                        scaleY = if (trackingEnabled) scale else 1f)
        )

        Spacer(modifier = Modifier.height(spacerHeight))

        Button(
            onClick = onToggleTracking,
            modifier = Modifier
                .width(250.dp)
                .height(60.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (trackingEnabled) Color(0xFF4CAF50) else Color(0xFFF44336)
            )
        ) {
            Text(
                text = if (trackingEnabled) "Rastreamento Ativo" else "Ativar Rastreamento",
                fontSize = 18.sp,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.weight(0.3f))

        Text(
            text = location,
            fontSize = 18.sp,
            color = Color.DarkGray,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(bottom = 76.dp)
                .fillMaxWidth(),
        )
    }
}