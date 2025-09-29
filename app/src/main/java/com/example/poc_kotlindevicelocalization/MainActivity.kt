package com.example.poc_kotlindevicelocalization

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity() {

    private val PERMISSION_REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkPermissions()

        goToQRActivity()
    }

    private fun checkPermissions() {
        val fineLocation = Manifest.permission.ACCESS_FINE_LOCATION
        val permissionsToRequest = mutableListOf<String>()

        if (ActivityCompat.checkSelfPermission(this, fineLocation) != PackageManager.PERMISSION_GRANTED)
            permissionsToRequest.add(fineLocation)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val backgroundLocation = Manifest.permission.ACCESS_BACKGROUND_LOCATION

            if (ActivityCompat.checkSelfPermission(this, backgroundLocation) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, fineLocation) == PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(backgroundLocation)
            }
        }

        if (permissionsToRequest.isNotEmpty())
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), PERMISSION_REQUEST_CODE)
    }

    private fun goToQRActivity() {
        val intent = Intent(this, QRActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            goToQRActivity()
        }
    }
}