package com.shohiebsense.latitudelongitudeimpl

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity;
import android.view.Menu
import android.view.MenuItem
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationListener
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*

class MainActivity : AppCompatActivity(), GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
    LocationListener {

    var location: Location? = null
    var locationCallback = LocationCallback()
    lateinit var googleAPIClient : GoogleApiClient

    val PLAY_SERVICE_RESOLUTION_REQUEST = 9000
    lateinit var locationRequest : LocationRequest
    val UPDATE_INTERVAL = 5000L
    val FASTEST_INTERVAL = 5000L

    val permissionListToRequest = arrayListOf<String>()
    val permissionListRejected = arrayListOf<String>()
    val permissionList = arrayListOf<String>()

    val ALL_PERMISSIONS_RESULT = 1011



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION)
        permissionList.add(Manifest.permission.ACCESS_COARSE_LOCATION)



        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }
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


    override fun onStart() {
        super.onStart()
        googleAPIClient.connect()
    }

    override fun onResume() {
        super.onResume()
        if(!isPlayServiceInstalled()){
            text_location.text = "Plesae install Google Play Service"
        }
    }

    fun isPlayServiceInstalled() : Boolean {
        val apiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = apiAvailability.isGooglePlayServicesAvailable(this)

        if(resultCode != ConnectionResult.SUCCESS){
            if(apiAvailability.isUserResolvableError(resultCode)){
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICE_RESOLUTION_REQUEST)
            }
            else{
                finish()
            }
            return false
        }
        return true
    }

    override fun onPause() {
        super.onPause()
        if(googleAPIClient != null && googleAPIClient.isConnected){
            LocationServices.FusedLocationApi.removeLocationUpdates(googleAPIClient,this)
            googleAPIClient.disconnect()
        }
    }

    override fun onLocationChanged(location: Location?) {
        text_location.text = "Latitude ${location!!.latitude}\nLongitude: ${location!!.longitude}"
    }


    override fun onConnectionFailed(p0: ConnectionResult) {
    }

    fun isLocationPermissionGranted() : Boolean{
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)== PackageManager.PERMISSION_GRANTED

    }

    @SuppressLint("MissingPermission")
    override fun onConnected(bundle: Bundle?) {
        if(!isLocationPermissionGranted()) return

        location = LocationServices.getFusedLocationProviderClient(this).lastLocation.result!!
        if(location != null){
            text_location.text = "Latitude ${location!!.latitude}\nLongitude: ${location!!.longitude}"
        }
        startLocationUpdates()
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates(){
        locationRequest = LocationRequest()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = UPDATE_INTERVAL
        locationRequest.fastestInterval = FASTEST_INTERVAL

        if(isLocationPermissionGranted()){
            LocationServices.getFusedLocationProviderClient(this).requestLocationUpdates(locationRequest,
                locationCallback, null)
        }
    }

    override fun onConnectionSuspended(p0: Int) {
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when(requestCode){
            ALL_PERMISSIONS_RESULT -> {
                permissionListToRequest.forEach {
                    if(!hasPermission(it)){
                        permissionListRejected.add(it)
                    }
                }

                if(permissionListRejected.isNotEmpty()){
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                        if(shouldShowRequestPermissionRationale(permissionListRejected[0])){
                            AlertDialog.Builder(this)
                                .setMessage("This permssion is mandatory")
                                .setPositiveButton("Ok", object : DialogInterface.OnClickListener{
                                    override fun onClick(dialog: DialogInterface?, which: Int) {

                                        requestPermissions(permissionListRejected.toTypedArray(), ALL_PERMISSIONS_RESULT)
                                    }

                                })
                                .create().show()
                        }
                    }
                    return
                }

                if(googleAPIClient != null){
                    googleAPIClient.connect()
                }
            }
        }

    }
}
