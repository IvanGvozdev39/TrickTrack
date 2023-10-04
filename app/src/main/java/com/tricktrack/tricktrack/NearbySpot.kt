package com.tricktrack.tricktrack

data class NearbySpot(
    val title: String,
    val latitude: Double,
    val longitude: Double,
    val type: String,
    val distance: Float
)