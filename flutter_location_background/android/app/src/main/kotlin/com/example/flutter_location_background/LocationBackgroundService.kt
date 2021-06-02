package com.example.flutter_location_background

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.gson.Gson
import io.flutter.Log
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import java.text.SimpleDateFormat
import java.util.*

class LocationBackgroundService : Service(), LocationListener {
    private lateinit var locationManager: LocationManager
    private lateinit var mSettingsClient: SettingsClient
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var mLocationRequest: LocationRequest
    private lateinit var mLocationSettingsRequest: LocationSettingsRequest

    private val gson = Gson()


    companion object {
        val channelName = "com.example.softsuave.locationUpdates"
        val SERVICE_CHANNEL = "location_update_service_channel"
        val FOREGROUND_SERVICE_ID = 10
        var START_LOCATION_SERVICE = "onStartService"
        var STOP_LOCATION_SERVICE = "onStopService"
        var MAKE_FOREGROUND = "onMakeForeground"
        var MAKE_BACKGROUND = "onMakeBackground"
        var isForground = false
        var serviceStaus = ServiceStatus.NOT_STARTED
        var channel: MethodChannel? = null
        var activity: MainActivity? = null
        var locationList = mutableListOf<String>()
        var locationLogList = mutableMapOf<String, LatLang>()
        const val TIME_STAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS"
        const val LOCATION_REQUEST_CODE = 3
        const val ON_LOG_DETAILS = "onLogDetails"
        const val GPS_REQUEST = 51

        fun startLocationUpdatesService(context: Context) {
            if (serviceStaus != ServiceStatus.STARTED) {
                val intent = Intent(context, LocationBackgroundService::class.java)
                intent.action = START_LOCATION_SERVICE
                context.startService(intent)
            }
        }

        fun stopLocationUpdatesService(context: Context) {
            if (serviceStaus == ServiceStatus.STARTED) {
                val intent = Intent(context, LocationBackgroundService::class.java)
                intent.action = STOP_LOCATION_SERVICE
                context.startService(intent)
            }
        }

        fun makeBackground(context: Context) {
            val intent = Intent(context, LocationBackgroundService::class.java)
            intent.action = MAKE_BACKGROUND
            context.startService(intent)
        }

        fun makeForeGround(context: Context) {
            val intent = Intent(context, LocationBackgroundService::class.java)
            intent.action = MAKE_FOREGROUND
            context.startService(intent)
        }

        fun registerChannel(flutterEngine: FlutterEngine, context: MainActivity) {
            activity = context
            channel = MethodChannel(flutterEngine.dartExecutor.binaryMessenger, channelName)
            channel?.setMethodCallHandler { call: MethodCall, result: MethodChannel.Result? ->
                if (call.method == "startLocationUpdates") {
                    startLocationUpdatesService(context)
                } else {
                    stopLocationUpdatesService(context)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        mLocationRequest = LocationRequest()
        mLocationRequest.interval = 3000 // 3 seconds
        mLocationRequest.fastestInterval = 3000 // 3 seconds
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        val builder = LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest)
        mLocationSettingsRequest = builder.build()
        builder.setAlwaysShow(true)
        locationManager =
                (this.getSystemService(Context.LOCATION_SERVICE) as LocationManager)
        mSettingsClient = LocationServices.getSettingsClient(this)
        val filter = IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION)
        filter.addAction(Intent.ACTION_PROVIDER_CHANGED)
        this.registerReceiver(gpsLocationReceiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.action?.let {
            when (it) {
                START_LOCATION_SERVICE -> {
                    locationLogList = mutableMapOf()
                    locationList = arrayListOf()
                    activity?.locationService = this
                    serviceStaus = ServiceStatus.STARTED
                    initializeLocationManager()
                }
                STOP_LOCATION_SERVICE -> {
                    locationLogList = mutableMapOf()
                    locationList = arrayListOf()
                    serviceStaus = ServiceStatus.NOT_STARTED
                    this.unregisterReceiver(gpsLocationReceiver)
                    stopService()
                }
                MAKE_FOREGROUND -> {
                    makeForground()
                }
                MAKE_BACKGROUND -> {
                    makeBackground()
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    fun initializeLocationManager() {
        if (!::fusedLocationClient.isInitialized) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        }
        try {
            if (ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_FINE_LOCATION
                    )
                    == PackageManager.PERMISSION_GRANTED
            ) {
                fusedLocationClient.requestLocationUpdates(
                        mLocationRequest,
                        locationCallback, Looper.getMainLooper()
                )
                if (::locationManager.isInitialized && (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
                ) {
                    fusedLocationClient?.lastLocation?.addOnCompleteListener {
                        it.result?.let { location ->
                            setLocationDetails(location)
                            //   println("location update from gps")
                        }

                    }

                } else {
                    checkGpsProvider()
                }
            } else {
                activity?.let { activity ->
                    activity?.requestLocationPermission()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    var locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            for (location in locationResult.locations) {
                setLocationDetails(location)
                // println("location update from location call back")

            }
        }
    }

    private fun setLocationDetails(lastLocation: Location?) {
        if (lastLocation == null)
            return
        val timeStamp = getTimeStamp()
        locationList.add("$timeStamp \n[${lastLocation.latitude} , ${lastLocation.longitude}]   ")
        //   locationLogList[timeStamp] = LatLang(lastLocation.latitude, lastLocation.longitude)
        channel?.invokeMethod(ON_LOG_DETAILS, gson.toJson(locationList))
        //  println("location update : ${locationLogList[timeStamp]} ${lastLocation.latitude} ${lastLocation.longitude}")
    }

    fun getTimeStamp(): String {
        val date = Calendar.getInstance()
        date.add(Calendar.DATE, 0)
        val formatter = SimpleDateFormat(TIME_STAMP_FORMAT, Locale.getDefault())
        formatter.timeZone = TimeZone.getDefault()
        return formatter.format(date.time)
    }


    override fun onBind(intent: Intent?): IBinder? {
        return MyBinder()
    }

    private fun makeForground() {
        if (serviceStaus != ServiceStatus.STARTED)
            return
        try {
            createNotificationChannel(
                    SERVICE_CHANNEL,
                    "Foreground Service"
            )
            val notificationIntent = Intent(this, MainActivity::class.java)
            notificationIntent.action = java.lang.Long.toString(System.currentTimeMillis())
            notificationIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            val pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    notificationIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
            )
            val notificationBuilder =
                    NotificationCompat.Builder(this, SERVICE_CHANNEL)
                            .setContentTitle("location updates")
                            .setSmallIcon(R.mipmap.ic_launcher)
                            .setContentIntent(pendingIntent)
                            .setAutoCancel(true)
                            .setOngoing(true)
                            .setSound(null)
                            .setPriority(Notification.PRIORITY_HIGH)
            startForeground(FOREGROUND_SERVICE_ID, notificationBuilder!!.build())
            isForground = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createNotificationChannel(channelId: String, name: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                    channelId, name,
                    NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.setSound(null, null)
            val manager = getSystemService(NotificationManager::class.java)
            if (manager?.getNotificationChannel(channelId) != channel)
                manager?.createNotificationChannel(channel)
        }
    }

    private fun makeBackground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            stopForeground(true)
        }
        isForground = false
    }

    private fun stopService() {
        if (::fusedLocationClient.isInitialized)
            fusedLocationClient.removeLocationUpdates(locationCallback)
        isForground = false
        channel?.invokeMethod(ON_LOG_DETAILS, "")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            stopForeground(true)
            stopSelf()
        }

    }

    fun checkGpsProvider() {
        val provider = Settings.Secure.getString(
                activity?.contentResolver,
                Settings.Secure.LOCATION_PROVIDERS_ALLOWED
        )
        if (!provider.contains("gps")) {
            turnGPSOn(object : OnGpsListener {
                override fun gpsStatus(isGPSEnable: Boolean) {
                    if (isGPSEnable)
                        initializeLocationManager()
                }

            })
        } else initializeLocationManager()
    }

    // method for turn on GPS
    fun turnGPSOn(gpsListener: OnGpsListener) {
        if (activity == null)
            return
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            mSettingsClient
                    .checkLocationSettings(mLocationSettingsRequest)
                    .addOnSuccessListener(
                            (activity as Activity)
                    ) { //  GPS is already enable, callback GPS status through listener
                        gpsListener.gpsStatus(true)
                    }
                    .addOnFailureListener(
                            activity as Activity
                    ) { e ->
                        val statusCode =
                                (e as ApiException).statusCode
                        when (statusCode) {
                            LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> try {
                                val rae = e as ResolvableApiException
                                rae.startResolutionForResult(
                                        activity as Activity,
                                        GPS_REQUEST
                                )
                            } catch (sie: IntentSender.SendIntentException) {
                                Log.i(
                                        ContentValues.TAG,
                                        "PendingIntent unable to execute request."
                                )
                            }
                            LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                                val errorMessage =
                                        "Location settings are inadequate, and cannot be " +
                                                "fixed here. Fix in Settings."
                                Log.e(
                                        ContentValues.TAG,
                                        errorMessage
                                )
                                Toast.makeText(
                                        this,
                                        errorMessage,
                                        Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
        } else gpsListener.gpsStatus(true)
    }

    interface OnGpsListener {
        fun gpsStatus(isGPSEnable: Boolean)
    }

    inner class MyBinder : Binder() {
        val service: LocationBackgroundService
            get() = this@LocationBackgroundService
    }


    data class LatLang(
            var latitude: Double = 0.0,
            var longitude: Double = 0.0
    )

    enum class ServiceStatus {
        STARTED,
        NOT_STARTED
    }

    override fun onLocationChanged(p0: Location) {
        initializeLocationManager()
    }

    private val gpsLocationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action ?: return) {
                LocationManager.PROVIDERS_CHANGED_ACTION -> {
                    locationManager =
                            (getSystemService(Context.LOCATION_SERVICE) as LocationManager)
                    if (::locationManager.isInitialized && (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)))
                        initializeLocationManager()
                }
            }
        }

    }
}