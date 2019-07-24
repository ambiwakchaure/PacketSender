package com.example.packetsender.view

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import com.example.packetsender.R
import com.example.packetsender.other.Constants
import com.example.packetsender.other.MyApplication
import kotlinx.android.synthetic.main.activity_splash.*
import java.lang.Exception

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)


        var navigation = MyApplication.prefs.getString(Constants.NAVIGATION,"0")
        if(navigation.equals("0"))
        {
            //check location setting
            checkLocationEnableOrNot()
            //check location permission
            if (checkPermission(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION))
            {

            }
        }
        else
        {
            startActivity(Intent(this,MainActivity::class.java))
            finish()
        }


        //call main activity
        next_btn.setOnClickListener {
            startActivity(Intent(this,MainActivity::class.java))
            finish()
        }
    }

    private fun checkLocationEnableOrNot() {

        var lm = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        var gps_enabled = false
        var network_enabled = false

        try
        {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            if(!gps_enabled && !network_enabled)
            {
                // Initialize a new instance of
                val builder = AlertDialog.Builder(this)
                // Set the alert dialog title
                builder.setTitle("Location Setting")
                builder.setCancelable(false)
                // Display a message on alert dialog
                builder.setMessage("Location not enabled tap on Open location setting to enable location.")
                // Set a positive button and its click listener on alert dialog
                builder.setPositiveButton("Open location setting"){dialog, which ->
                    dialog.dismiss()
                    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }
                // Display a negative button on alert dialog
                builder.setNegativeButton("Close app"){dialog,which ->
                    dialog.dismiss()
                    finish()
                }
                // Finally, make the alert dialog using builder
                val dialog: AlertDialog = builder.create()
                dialog.show()
            }
        }
        catch (e : Exception)
        {

        }
    }

    val PERMISSION_ID = 42
    private fun checkPermission(vararg perm:String) : Boolean {
        val havePermissions = perm.toList().all {
            ContextCompat.checkSelfPermission(this,it) ==
                    PackageManager.PERMISSION_GRANTED
        }
        if (!havePermissions) {
            if(perm.toList().any {
                    ActivityCompat.
                        shouldShowRequestPermissionRationale(this, it)}
            ) {
                val dialog = AlertDialog.Builder(this)
                    .setTitle("Permission")
                    .setMessage("Permission needed!")
                    .setPositiveButton("OK", {id, v ->
                        ActivityCompat.requestPermissions(
                            this, perm, PERMISSION_ID)
                    })
                    .setNegativeButton("No", {id, v -> })
                    .create()
                dialog.show()
            } else {
                ActivityCompat.requestPermissions(this, perm, PERMISSION_ID)
            }
            return false
        }
        return true
    }
}
