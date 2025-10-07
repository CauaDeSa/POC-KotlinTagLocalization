package com.example.poc_kotlindevicelocalization

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var tagId: String
    private lateinit var firestore: FirebaseFirestore
    private var trackingEnabled = false
    private var tagListener: ListenerRegistration? = null
    private var locationCallback: LocationCallback? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        tagId = intent!!.getStringExtra("tagId")!!
        firestore = FirebaseFirestore.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            15000
        ).build()

        startForegroundService()
        observeTrackingStatus()

        return START_STICKY
    }

    private fun startForegroundService() {
        val channelId = "location_channel"
        val channelName = "Atualização de Localização"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notificationIntent = Intent(this, TagActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("tagId", tagId)
        }

        val pendingIntent = androidx.core.app.TaskStackBuilder.create(this).run {
            addNextIntentWithParentStack(notificationIntent)
            getPendingIntent(
                0,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Rastreamento ativo")
            .setContentText("Sua localização está sendo atualizada em segundo plano.")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(1, notification)
    }

    private fun observeTrackingStatus() {
        tagListener = firestore
            .collection("tags")
            .document(tagId)
            .addSnapshotListener { snapshot, _ ->
                val enabled = snapshot?.getBoolean("trackingEnabled") == true
                if (enabled != trackingEnabled) {
                    trackingEnabled = enabled
                    if (trackingEnabled && locationCallback == null) startLocationUpdates()
                    else {
                        stopLocationUpdates()
                        stopSelf()
                    }
                }
            }
    }

    private fun startLocationUpdates() {

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ){
            stopSelf()
            return
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                if (!trackingEnabled) {
                    stopLocationUpdates()
                    return
                }

                for (location in locationResult.locations) {
                    val updates = mapOf(
                        "latitude" to location.latitude,
                        "longitude" to location.longitude,
                        "lastUpdated" to System.currentTimeMillis()
                    )

                    firestore
                        .collection("tags")
                        .document(tagId)
                        .update(updates)
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback!!,
            mainLooper
        )
    }

    private fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
            locationCallback = null
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        tagListener?.remove()
    }
}