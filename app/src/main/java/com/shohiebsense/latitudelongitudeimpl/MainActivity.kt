package com.shohiebsense.latitudelongitudeimpl

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity;
import android.view.Menu
import android.view.MenuItem
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import java.text.DateFormat
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {

    val permissionList = arrayListOf<String>()

    object RequestCode {
        const val PERMISSIONS_REQUEST_CODE = 34
        const val SETTINGS_REQUEST_CODE = 0x1
        const val UPDATE_INTERVAL_IN_MILLISECONDS : Long = 1000
        const val FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS: Long = UPDATE_INTERVAL_IN_MILLISECONDS / 2

        const val KEY_REQUESTING_LOCATION_UPDATES = "requesting-location-updates"
        const val KEY_LOCATION = "location"
        const val KEY_LAST_UPDATED_TIME_STRING = "last-updated-time-string"
    }

    lateinit var fusedLocationClient : FusedLocationProviderClient
    lateinit var settingsClient : SettingsClient
    lateinit var locationRequest: LocationRequest
    lateinit var locationSettingsRequest : LocationSettingsRequest
    lateinit var locationCallback : LocationCallback
    var currentLocation : Location? = null


    //latitude NS
    var latitudeLabel = ""
    //longitude WE
    var longitudeLabel = ""
    var lastUpdateTimeLabel = ""

    var isRequestingLocationUpdates = false

    var lastUpdateTime = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)


        latitudeLabel = getString(R.string.latitude_label)
        longitudeLabel = getString(R.string.longitude_label)
        lastUpdateTimeLabel = getString(R.string.last_update_time_label)

        updateValuesFromBundle(savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        settingsClient = LocationServices.getSettingsClient(this)

        createLocationCallback()
        createLocationRequest()
        buildLocationSettingsRequest()

        permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION)
        permissionList.add(Manifest.permission.ACCESS_COARSE_LOCATION)


        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }

        handleButtons()
    }

    fun updateValuesFromBundle(savedInstanceState: Bundle?){
        if(savedInstanceState != null){
            if(savedInstanceState.keySet().contains(RequestCode.KEY_REQUESTING_LOCATION_UPDATES)){
                isRequestingLocationUpdates = savedInstanceState.getBoolean(
                    RequestCode.KEY_REQUESTING_LOCATION_UPDATES
                )
            }

            if(savedInstanceState.keySet().contains(RequestCode.KEY_LOCATION)){
                currentLocation = savedInstanceState.getParcelable(RequestCode.KEY_LOCATION)
            }

            if(savedInstanceState.keySet().contains(RequestCode.KEY_LAST_UPDATED_TIME_STRING)){
                lastUpdateTime = savedInstanceState.getString(RequestCode.KEY_LAST_UPDATED_TIME_STRING)
            }

            updateUI()
        }
    }

    fun updateUI(){
        setButtonEnabledState()
        updateLocationUI()
    }

    fun setButtonEnabledState(){
        if(isRequestingLocationUpdates){
            start_updates_button.isEnabled = false
            stop_updates_button.isEnabled = true
        }
        else{
            start_updates_button.isEnabled = true
            stop_updates_button.isEnabled = false
        }
    }

    fun updateLocationUI(){
        if(currentLocation != null){
            latitude_text.text = String.format(Locale.ENGLISH, "%s: %f", latitudeLabel, currentLocation!!.latitude)
            latitude_NS_notation_text.text = convertLatitudeNotation()
            longitude_text.text = String.format(Locale.ENGLISH, "%s: %f", longitudeLabel, currentLocation!!.longitude)
            longitude_WE_notation_text.text = convertLongitudeToNotation()
            last_update_time_text.text = String.format(Locale.ENGLISH, "%s: %s",lastUpdateTimeLabel, lastUpdateTime)
        }
    }


    fun createLocationCallback(){
        locationCallback = object: LocationCallback(){
            override fun onLocationResult(locationResult: LocationResult?) {
                super.onLocationResult(locationResult)

                currentLocation = locationResult?.lastLocation
                lastUpdateTime = DateFormat.getTimeInstance().format(Date())
                updateLocationUI()
            }
        }
    }

    fun createLocationRequest(){
        locationRequest = LocationRequest()

        locationRequest.interval = RequestCode.UPDATE_INTERVAL_IN_MILLISECONDS
        locationRequest.fastestInterval = RequestCode.FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS
        locationRequest.priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY

    }

   fun buildLocationSettingsRequest(){
       val builder = LocationSettingsRequest.Builder()
       builder.addLocationRequest(locationRequest)
       locationSettingsRequest = builder.build()
   }

    fun convertLatitudeNotation() : String{
        var latitudeStr = currentLocation!!.latitude.toString()
        if(latitudeStr.contains("-")){
            latitudeStr.replace("-","")
            latitudeStr += latitudeStr + "S"
        }
        else{
            latitudeStr += "N"
        }
        return latitudeStr
    }

    fun convertLongitudeToNotation() : String{
        var longitudeStr = currentLocation!!.longitude.toString()
        if(longitudeStr.contains("-")) {
            longitudeStr.replace("-","")
            longitudeStr += "S"
        }
        else{
            longitudeStr += "N"
        }
        return longitudeStr
    }

    fun permissionsToRequest(permissionList: ArrayList<String>) : ArrayList<String>{
        val resultList = arrayListOf<String>()
        permissionList.forEach {
            if(!hasPermission(it)){
                resultList.add(it)
            }
        }
        return resultList
    }

    fun hasPermission(permission : String) : Boolean {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when(requestCode){
            Activity.RESULT_OK -> {
                //user agreed
            }
            Activity.RESULT_CANCELED -> {
                isRequestingLocationUpdates = false
                updateUI()
            }
        }
    }

    fun handleButtons(){
        start_updates_button.setOnClickListener {
            if(!isRequestingLocationUpdates){
                isRequestingLocationUpdates = true
                setButtonEnabledState()
                startLocationUpdates()
            }
        }

        stop_updates_button.setOnClickListener {
            stopLocationUpdtes()
        }
    }



    fun stopLocationUpdtes(){
        if(!isRequestingLocationUpdates){
            //updates never requested
        }

        fusedLocationClient.removeLocationUpdates(locationCallback)
            .addOnCompleteListener {
                isRequestingLocationUpdates = false
                setButtonEnabledState()
            }
    }


    override fun onResume() {
        super.onResume()

        if(isRequestingLocationUpdates && arePermissionsGranted()){
            startLocationUpdates()
        }
        else if(!arePermissionsGranted()){
            requestPermissions()
        }

        updateUI()
    }

    fun arePermissionsGranted() : Boolean{
        val permissionState = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        return permissionState == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions(){
        val shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(this,
            Manifest.permission.ACCESS_FINE_LOCATION)

        if(shouldProvideRationale){
            //show rationale
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                RequestCode.PERMISSIONS_REQUEST_CODE)
        }
        else{
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), RequestCode.PERMISSIONS_REQUEST_CODE)
        }
    }


    fun isLocationPermissionGranted() : Boolean{
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)== PackageManager.PERMISSION_GRANTED

    }


    @SuppressLint("MissingPermission")
    fun startLocationUpdates(){
        settingsClient.checkLocationSettings(locationSettingsRequest)
            .addOnSuccessListener {
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper())
                updateUI()
            }
            .addOnFailureListener {
                val statusCode = (it as ApiException).statusCode
                when(statusCode){
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                        //location settings not satisfied
                        try{
                            val resolvableApiException = it as ResolvableApiException
                            resolvableApiException.startResolutionForResult(this, RequestCode.SETTINGS_REQUEST_CODE)
                        }catch (sendIntentException : IntentSender.SendIntentException){

                        }
                    }

                    LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                        val errorMessage = "Location Setttings inadequate"
                        isRequestingLocationUpdates = false
                    }
                }
                updateUI()
            }
    }



    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if(requestCode == RequestCode.PERMISSIONS_REQUEST_CODE){
            if(grantResults.isEmpty()){


                //cancelled
            }
            else if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                if(isRequestingLocationUpdates){
                    startLocationUpdates()
                }
            }
            else{
                //denied
                val intent = Intent()
                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                val uri = Uri.fromParts(
                    "package",
                    BuildConfig.APPLICATION_ID, null
                )
                intent.data = uri
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
            }

        }

    }
}
