package com.example.poc_kotlindevicelocalization

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.journeyapps.barcodescanner.BarcodeEncoder
import java.util.UUID

class QRActivity : AppCompatActivity() {

    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var currentTagId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        createTag()

        setContent {
            MaterialTheme {
                QRScreen(bitmap = createBitMap()!!)
            }
        }
    }

    private fun createTag() {
        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val sessionId = prefs.getString("SESSION_ID", null)

        currentTagId = sessionId ?: UUID.randomUUID().toString()

        val tagData = hashMapOf(
            "latitude" to 0.0,
            "longitude" to 0.0,
            "lastUpdated" to System.currentTimeMillis(),
            "trackingEnabled" to false
        )

        firestore.collection("tags").document(currentTagId)
            .set(tagData)
            .addOnSuccessListener {
                firestore.collection("tags").document(currentTagId)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) return@addSnapshotListener

                        if (snapshot != null && snapshot.exists()) {
                            val nome = snapshot.getString("name")
                            val description = snapshot.getString("description")

                            if (!nome.isNullOrBlank() && !description.isNullOrBlank()) {
                                val intent = Intent(this, TagActivity::class.java)
                                intent.putExtra("tagId", currentTagId)
                                startActivity(intent)
                                finish()
                            }
                        }
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao criar tag: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun createBitMap(): Bitmap? {
        try {
            val barcodeEncoder = BarcodeEncoder()
            return barcodeEncoder.encodeBitmap(
                currentTagId, BarcodeFormat.QR_CODE, 300, 300
            )
        } catch (e: WriterException) {
            e.printStackTrace()
        }

        return null
    }
}

@Composable
fun QRScreen(
    bitmap: Bitmap
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Escaneie o QRCode para registrar a tag",
            fontSize = 24.sp,
            color = Color.Black,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "QR Code",
            modifier = Modifier
                .width(300.dp)
                .height(300.dp),
        )
    }
}