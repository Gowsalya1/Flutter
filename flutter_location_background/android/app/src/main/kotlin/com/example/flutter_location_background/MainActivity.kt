package com.example.flutter_location_background

import android.Manifest
import android.content.pm.PackageManager
import androidx.annotation.NonNull
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugins.GeneratedPluginRegistrant

class MainActivity : FlutterActivity() {

    lateinit var locationService: LocationBackgroundService
    override fun configureFlutterEngine(@NonNull flutterEngine: FlutterEngine) {
        GeneratedPluginRegistrant.registerWith(flutterEngine);
        LocationBackgroundService.registerChannel(flutterEngine, this)
    }

    override fun onResume() {
        super.onResume()
        if (LocationBackgroundService.serviceStaus == LocationBackgroundService.ServiceStatus.STARTED)
            LocationBackgroundService.makeBackground(context)
    }

    override fun onPause() {
        super.onPause()
        if (LocationBackgroundService.serviceStaus == LocationBackgroundService.ServiceStatus.STARTED)
            LocationBackgroundService.makeForeGround(context)
    }


    fun requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                )
        ) {
            ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    LocationBackgroundService.LOCATION_REQUEST_CODE
            )
        } else {
            ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    LocationBackgroundService.LOCATION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LocationBackgroundService.LOCATION_REQUEST_CODE -> {
                try {
                    if (grantResults.isNotEmpty()
                            && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    ) {
                        if (ContextCompat.checkSelfPermission(
                                        this,
                                        Manifest.permission.ACCESS_FINE_LOCATION
                                )
                                == PackageManager.PERMISSION_GRANTED
                        ) {
                            if (::locationService.isInitialized) {
                                locationService.checkGpsProvider()
                            }
                        }
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

        }
    }
}
