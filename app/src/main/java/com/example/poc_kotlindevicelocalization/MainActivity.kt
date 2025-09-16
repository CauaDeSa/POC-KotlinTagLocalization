package com.example.poc_kotlindevicelocalization

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val firestore = FirebaseFirestore.getInstance()

    private var currentTagId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                askUserInfo()
            } else {
                Toast.makeText(this, "Permissão de localização negada", Toast.LENGTH_SHORT).show()
            }
        }

    private fun askUserInfo() {
        val nameInput = EditText(this).apply { hint = "Nome da Tag" }
        val descInput = EditText(this).apply { hint = "Descrição" }
        val imageInput = EditText(this).apply { hint = "Image URI (opcional)" }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 16, 32, 16)
            addView(nameInput)
            addView(descInput)
            addView(imageInput)
        }

        AlertDialog.Builder(this)
            .setTitle("Configurar Tag")
            .setView(layout)
            .setPositiveButton("Salvar") { _, _ ->
                val name = nameInput.text.toString()
                val description = descInput.text.toString()
                val imageUri = imageInput.text.toString().ifEmpty { null }

                createTagInFirestore(name, description, imageUri)
            }
            .setCancelable(false)
            .show()
    }

    private fun createTagInFirestore(name: String, description: String, imageUri: String?) {
        val docId = UUID.randomUUID().toString()

        val tagData = hashMapOf(
            "id" to docId,
            "name" to name,
            "description" to description,
            "imageUri" to imageUri,
            "latitude" to 0.0,
            "longitude" to 0.0,
            "timestamp" to System.currentTimeMillis()
        )

        firestore.collection("tags")
            .document(docId)
            .set(tagData)
            .addOnSuccessListener {
                currentTagId = docId
                Toast.makeText(this, "Tag criada!", Toast.LENGTH_SHORT).show()
                startLocationUpdates()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao criar tag: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000
        ).build()

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        fusedLocationClient.requestLocationUpdates(locationRequest, object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    currentTagId?.let { tagId ->
                        val updates = mapOf(
                            "latitude" to location.latitude,
                            "longitude" to location.longitude,
                            "timestamp" to System.currentTimeMillis()
                        )
                        firestore.collection("tags")
                            .document(tagId)
                            .update(updates)
                            .addOnSuccessListener {
                                println("Localização atualizada: ${location.latitude}, ${location.longitude}")
                            }
                            .addOnFailureListener { e ->
                                println("Erro ao atualizar: ${e.message}")
                            }
                    }
                }
            }
        }, mainLooper)
    }
}