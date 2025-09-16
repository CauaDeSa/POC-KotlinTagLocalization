package com.example.poc_kotlindevicelocalization.models

data class Tag(
    val id: Int = 0,
    val name: String = "",
    val description: String = "",
    val imageUri: String? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)