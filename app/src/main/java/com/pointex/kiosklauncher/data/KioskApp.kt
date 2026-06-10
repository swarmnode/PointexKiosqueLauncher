package com.pointex.kiosklauncher.data

import android.graphics.drawable.Drawable

/** A launchable application allowed inside the kiosk grid. */
data class KioskApp(
    val label: String,
    val packageName: String,
    val activityName: String,
    val icon: Drawable,
)
