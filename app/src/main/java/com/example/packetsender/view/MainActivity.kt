package com.example.packetsender.view

import android.Manifest
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.location.LocationListener
import android.os.AsyncTask
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.ResultReceiver
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import com.example.packetsender.R
import com.example.packetsender.other.*
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GooglePlayServicesUtil
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.RuntimeExecutionException
import kotlinx.android.synthetic.main.activity_main.*
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.PrintWriter
import java.lang.Exception
import java.net.InetSocketAddress
import java.net.Socket

class MainActivity : AppCompatActivity(),
    OnMapReadyCallback,
    GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener,
    LocationListener {

    var fusedLocationClient: FusedLocationProviderClient? = null
    private lateinit var mMap: GoogleMap
    val REQUEST_CHECK_STATE = 12300 // any suitable ID

    companion object {
        var socket: Socket? = null
        var server_address: String? = null
        var format_type: String? = null
        var server_port: Int = 0
        var CNT: Int = 0
        var EMERGENCY_CNT: Int = 0
        var START_STOP_CNT: Int = 0
        var message = ""
        var imei = ""
        var packetType = ""
        var packetTypeForEmergency = ""
        //lateinit var progressDialog: ProgressDialog
        lateinit var context: Context
        lateinit var mHandlerHealthPacket: Handler
        lateinit var mHandlerNormalPacket: Handler


    }

    var lat = ""
    var lng = ""

    var currentlat = ""
    var currentlng = ""

    var drag_droplat = ""
    var drag_droplng = ""

    var manuallat = ""
    var manuallng = ""


    var latLongType = ""



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        context = this
        drag_droplat = "NA"
        drag_droplng = "NA"
        MyApplication.editor.putString(Constants.NAVIGATION, "navigated").commit()
        //set local data
        ipadress_edt.setText(MyApplication.prefs.getString(Constants.SERVER_ADDRESS, "13.127.103.21"))
        port_edt.setText(MyApplication.prefs.getString(Constants.SERVER_PORT, "9999"))
        setClickListner()

        //handler for packet sending
        mHandlerHealthPacket = Handler()
        mHandlerNormalPacket = Handler()
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as ScrollGoogleMap
        mapFragment.getMapAsync(this)
        mapFragment.setListener(object : ScrollGoogleMap.OnTouchListener {
            override fun onTouch() {
                map_scroll.requestDisallowInterceptTouchEvent(true)
            }
        })
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        if (checkPermission(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        ) {
            fusedLocationClient?.lastLocation?.addOnSuccessListener(this,
                { location: Location? ->
                    // Got last known location. In some rare
                    // situations this can be null.
                    if (location == null) {
                        // TODO, handle it
                    } else location.apply {
                        // Handle location object

                        Log.e("LOG", location.toString())
                        T.e("latitude : " + location.latitude)
                        T.e("longitude : " + location.longitude)
                    }
                })
        }

        val reqSetting = LocationRequest.create().apply {
            fastestInterval = 5000
            interval = 10000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            smallestDisplacement = 1.0f
        }

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(reqSetting)
        val client = LocationServices.getSettingsClient(this)
        client.checkLocationSettings(builder.build()).addOnCompleteListener { task ->
            try {
                val state: LocationSettingsStates = task.result!!.locationSettingsStates
                Log.e(
                    "LOG", "LocationSettings: \n" +
                            " GPS present: ${state.isGpsPresent} \n" +
                            " GPS usable: ${state.isGpsUsable} \n" +
                            " Location present: " +
                            "${state.isLocationPresent} \n" +
                            " Location usable: " +
                            "${state.isLocationUsable} \n" +
                            " Network Location present: " +
                            "${state.isNetworkLocationPresent} \n" +
                            " Network Location usable: " +
                            "${state.isNetworkLocationUsable} \n"
                )
            } catch (e: RuntimeExecutionException) {

                T.e("One : " + e)
                if (e.cause is ResolvableApiException)
                    (e.cause as ResolvableApiException).startResolutionForResult(
                        this@MainActivity,
                        REQUEST_CHECK_STATE
                    )
            }
        }
        val locationUpdates = object : LocationCallback() {
            override fun onLocationResult(lr: LocationResult) {
                Log.e("LOG", lr.toString())
                Log.e("LOG", "Newest Location: " + lr.locations.last())

                currentlat = "" + lr.locations.last().latitude
                currentlng = "" + lr.locations.last().longitude
                if (currentlat.length >= 10) {
                    currentlat = currentlat.substring(0, 9)
                }
                if (currentlng.length >= 10) {
                    currentlng = currentlng.substring(0, 9)
                }
                if (latLongType.equals("current")) {
                    lat_lng_tv.setText(currentlat + " , " + currentlng)
                    // do something with the new location...
                    changeMap(lr.locations.last().latitude, lr.locations.last().longitude)
                }

            }
        }
        fusedLocationClient?.requestLocationUpdates(
            reqSetting,
            locationUpdates,
            null /* Looper */
        )

    }


    //for Health Packet handler
    fun startHealthPacketHandler(){
        mIsRunningHealthPacket = true
        mStatusCheckerHealthPacket.run()
    }
    internal var mIsRunningHealthPacket: Boolean = false
    internal var mStatusCheckerHealthPacket : Runnable = object : Runnable{
        override fun run(){
            if(!mIsRunningHealthPacket){
                return//stop when told to stop
            }
            //call every 10 sec
            packetType = "health"
            //T.e("HANDLER_CALL : "+packetType)
            T.e("HANDLER_CALL : health handler running...")
            sendDataToServer(packetType)
            //do somethind here
            //T.e("HANDLER_CALL : "+"Running...")
            mHandlerHealthPacket.postDelayed(this,Constants.HEALTH_PACKET_DELAY)//2 min
        }
    }
    fun stopHealthPacketHandler(){
        mIsRunningHealthPacket = false
        mHandlerHealthPacket.removeCallbacks(mStatusCheckerHealthPacket)
    }
    //for normal packet handler
    fun startNormalPacketHandler(){
        mIsRunningNormalPacket = true
        mStatusCheckerNormalPacket.run()
    }
    internal var mIsRunningNormalPacket: Boolean = false
    internal var mStatusCheckerNormalPacket : Runnable = object : Runnable{
        override fun run() {
            if(!mIsRunningNormalPacket){
                return//stop when told to stop
            }

            if(START_STOP_CNT == 0 && packetTypeForEmergency.equals("")){

                if(mIsRunningNormalPacket){
                    stopNormalPacketHandler()
                }
                if(mIsRunningHealthPacket){
                    stopHealthPacketHandler()
                }
            }
            if(packetTypeForEmergency.equals("emergency"))
            {
                //call every 10 sec
                packetType = "emergency";
                packetTypeForEmergency = "emergency"
               // T.e("HANDLER_CALL : "+packetType)
                T.e("HANDLER_CALL : emergency handler running...")
                sendDataToServer(packetType)
            }
            else
            {
                if(packetType.equals("emergency"))
                {
                    //call every 10 sec
                    packetType = "emergency";
                    packetTypeForEmergency = "emergency"
                    //T.e("HANDLER_CALL : "+packetType)
                    T.e("HANDLER_CALL : emergency handler running...")
                    sendDataToServer(packetType)
                }
                else
                {
                    //call every 10 sec
                    //packetType = "normal";
                   // T.e("HANDLER_CALL : "+packetType)
                    T.e("HANDLER_CALL : normal handler running...")
                    sendDataToServer(packetType)
                    //sendDataToServer(packetTypeFlag)
                }
            }
            mHandlerNormalPacket.postDelayed(this,Constants.NORMAL_PACKET_DELAY)//10 sec
        }
    }
    fun stopNormalPacketHandler(){
        mIsRunningNormalPacket = false
        mHandlerNormalPacket.removeCallbacks(mStatusCheckerNormalPacket)
    }
    override fun onDestroy() {
        super.onDestroy()
        if(mIsRunningHealthPacket){
            stopHealthPacketHandler()
        }
        if(mIsRunningNormalPacket){
            stopNormalPacketHandler()
        }
    }
    private fun setClickListner() {

        emergency_btn.setOnClickListener {


            //handle state changed and colors of emergency button
            if(EMERGENCY_CNT == 0)
            {
                if (T.isNetworkAvailable())
                {
                    EMERGENCY_CNT = 1
                    emergency_btn.setText("Stop Emergency")
                    emergency_btn.setBackgroundColor(Color.parseColor("#ff0000"))
                    //send health packet
                    packetType = "emergency"
                    validateFields("emergency")//emergency
                }
                else
                {
                    T.e("Oops ! no internet connection")
                }
            }
            else
            {
                if(mIsRunningNormalPacket){
                    emergency_btn.setText("Start Emergency")
                    emergency_btn.setBackgroundColor(Color.parseColor("#339933"))
                    EMERGENCY_CNT = 0
                    stopNormalPacketHandler()
                }
            }

        }
        start_btn.setOnClickListener {

            if (T.isNetworkAvailable()) {
                //send health packet
                validateFields("health")
            } else {
                T.e("Oops ! no internet connection")
            }
        }
        stop_btn.setOnClickListener {

            if(mIsRunningHealthPacket){
                T.e("HANDLER_STOP : health handler")
                stopHealthPacketHandler()
            }
            if(mIsRunningNormalPacket){
                T.e("HANDLER_STOP : normal handler")
                stopNormalPacketHandler()
            }
            START_STOP_CNT = 0
            CNT = 0
            start_btn.setVisibility(View.VISIBLE)
            stop_btn.setVisibility(View.GONE)


        }
        connect_btn.setOnClickListener {
            if (T.isNetworkAvailable()) {
                validateFields("")
            } else {
                T.e("Oops ! no internet connection")
            }
        }
        format_type = "NA"
        radioGroup_format.setOnCheckedChangeListener(object : RadioGroup.OnCheckedChangeListener {
            override fun onCheckedChanged(group: RadioGroup?, checkedId: Int) {

                if (R.id.radioButton_dims == checkedId) {
                    format_type = "dims"
                } else if (R.id.radioButton_uttarakhand == checkedId) {
                    format_type = "uttarakhand"
                }
            }

        })
        packetType = "normal"
        radioGroup.setOnCheckedChangeListener(object : RadioGroup.OnCheckedChangeListener {
            override fun onCheckedChanged(group: RadioGroup?, checkedId: Int) {

                if (R.id.radioButton_login == checkedId) {
                    packetType = "login"
                } else if (R.id.radioButton_normal == checkedId) {
                    packetType = "normal"
                }  else if (R.id.radioButton_alarm == checkedId) {
                    packetType = "alarm"
                }
            }

        })
        latLongType = "NA"
        radioGroup_latlng.setOnCheckedChangeListener(object : RadioGroup.OnCheckedChangeListener {
            override fun onCheckedChanged(group: RadioGroup?, checkedId: Int) {

                if (R.id.radioButton_currlocation == checkedId) {
                    latLongType = "current"
                    lat_lng_li.setVisibility(View.GONE);
                } else if (R.id.radioButton_drag_drop == checkedId) {
                    latLongType = "drag_drop"
                    lat_lng_li.setVisibility(View.GONE);
                }else if (R.id.radioButton_manuallocation == checkedId) {
                    latLongType = "manual"
                    lat_lng_li.setVisibility(View.VISIBLE);
                }
            }
        })
    }

    private fun validateFields(packetTypeFlag : String) {

        //check format types empty
        if (!T.validateNA(main_layout, format_type!!, "Select format types")) {
            return
        }
        if (!T.validateEditext(main_layout, ipadress_edt, "Enter Server IP address")) {
            return
        }
        if (!T.validateEditext(main_layout, port_edt, "Enter Server Port")) {
            return
        }
        if (!T.validateEditext(main_layout, vendorid_edt, "Enter vendor id")) {
            return
        }
        if (!T.validateEditext(main_layout, imei_edt, "Enter IMEI")) {
            return
        }
        //check lat lng types empty
        if (!T.validateNA(main_layout, latLongType!!, "Select lat lng type")) {
            return
        }
        if (latLongType.equals("drag_drop"))
        {
            if (drag_droplat.equals("NA") || drag_droplng.equals("NA"))
            {
                T.s(main_layout, "Please tap on map to get manual lat long")
            }
            else
            {

                //send packet
                checkIfPacketEmergency(packetTypeFlag)
            }
        }
        else if(latLongType.equals("manual"))
        {
            //check manual lat long empty
            if (!T.validateEditext(main_layout, manual_lat_edt, "Enter manual latitude")) {
                return
            }
            if (!T.validateEditext(main_layout, manual_lng_edt, "Enter manual longitude")) {
                return
            }
            manuallat = manual_lat_edt.text.toString()
            manuallng = manual_lng_edt.text.toString()

            //send packet
            checkIfPacketEmergency(packetTypeFlag)
        }
        else
        {
            //send packet
            checkIfPacketEmergency(packetTypeFlag)
        }
    }
    fun checkIfPacketEmergency(packetTypeFlag : String){

        if(packetTypeFlag.equals("emergency"))
        {
            //stop all previous packet sending like health and normal
            CNT = 0
            packetTypeForEmergency = "emergency"
            start_btn.setVisibility(View.VISIBLE)
            stop_btn.setVisibility(View.GONE)
            if(mIsRunningHealthPacket){
                stopHealthPacketHandler()
            }
            if(mIsRunningNormalPacket){
                stopNormalPacketHandler()
            }
            //send emergency packet
            packetType = packetTypeFlag
            startNormalPacketHandler()
        }
        else
        {

            emergency_btn.setText("Start Emergency")
            emergency_btn.setBackgroundColor(Color.parseColor("#339933"))
            EMERGENCY_CNT = 0

            CNT = 0
            packetTypeForEmergency = ""
            start_btn.setVisibility(View.GONE)
            stop_btn.setVisibility(View.VISIBLE)
            if(mIsRunningHealthPacket){
                stopHealthPacketHandler()
            }
            if(mIsRunningNormalPacket){
                stopNormalPacketHandler()
            }
            START_STOP_CNT = 1
            packetType = packetTypeFlag
            startHealthPacketHandler()
        }
    }
    @Synchronized
    fun sendDataToServer(packetTypeFlag : String) {
        //get current date time minus 5.30 hr with this
        var timeStampArray = T.getSystemDateTime()!!.split(" ")
        var dateArray = timeStampArray.get(0).split("-")
        var timeArray = timeStampArray.get(1).split(":")
        var dateTimeString =
            dateArray.get(0) + "," + dateArray.get(1) + "," + dateArray.get(2) + "," + timeArray.get(0) + "," + timeArray.get(
                1
            ) + "," + timeArray.get(2)

        //check packet type
        if(packetTypeFlag.equals("emergency")){

            var vendor_id = vendorid_edt.text.toString().trim()
            server_address = ipadress_edt.text.toString()
            var portt = port_edt.text.toString()
            server_port = portt.toInt()
            imei = imei_edt.text.toString()

            if(format_type.equals("dims"))
            {
                message = "\$,EPB,EMR," + imei + ",NM," + dateTimeString + ",A,0" + lat + ",N,0" + lng + ",E,0570.9,000.4,000.00,G,0000000000,0000000000000,a06c5e78,*"
            }
            else if(format_type.equals("uttarakhand"))
            {
                message = "\$EPB," + vendor_id + ",EMR," + imei + ",NM,11072019095127,A,030.104912,N,078.302765,E,0357.9,000.4,000.00,G,0000000000,0000000000000*a547"
            }
            else
            {
                message = "emergency packet not found"
            }
            LongOperation(this).execute()
        }
        else
        {

            server_address = ipadress_edt.text.toString()
            var portt = port_edt.text.toString()
            server_port = portt.toInt()
            imei = imei_edt.text.toString()
            //save ip and port
            MyApplication.editor.putString(Constants.SERVER_ADDRESS, server_address).commit()
            MyApplication.editor.putString(Constants.SERVER_PORT, "" + server_port).commit()


            if (latLongType.equals("current")) {
                if (currentlat.length >= 10) {
                    lat = currentlat.substring(0, 9)
                } else {
                    lat = currentlat
                }
                if (currentlng.length >= 10) {
                    lng = currentlng
                } else {
                    lng = currentlng
                }

            }
            else if (latLongType.equals("manual")){
                lat = manuallat
                lng = manuallng
            }
            else
            {
                if (drag_droplat.length >= 10) {
                    lat = drag_droplat.substring(0, 9)
                } else {
                    lat = drag_droplat
                }
                if (drag_droplng.length >= 10) {
                    lng = drag_droplng.substring(0, 9)
                } else {
                    lng = drag_droplng
                }

            }
            var vendor_id = vendorid_edt.text.toString().trim()

            if (packetTypeFlag.equals("login"))//login
            {
                message = "$,01," + vendor_id + ",0.0.1," + imei + ",MH12AB1234,*"
            }
            else if (packetTypeFlag.equals("normal"))//normal
            {
                message =
                    "$,03," + vendor_id + ",0.0.1,TA,16,L," + imei + ",MH12AB1234,1," + dateTimeString + ",0" + lat + ",N,0" + lng + ",E,010.4,354.90,07,0571.7,01.90,01.00,IDEAIN,1,1,00.0,4.4,1,C,31,404,78,62fc,2a28,274d,6301,-056,2a27,62fc,-063,36a8,62f8,-066,2a29,62fc,-070,1000,00,000042,9d20b00f,*"
            }
            else if (packetTypeFlag.equals("health"))//health
            {
                message = "$,02," + vendor_id + ",0.0.1," + imei + ",100,30,00.0,00005,00600,0000,00,00.2,00.0,*"
            }
            else if (packetTypeFlag.equals("alarm"))//alarm
            {
                message =
                    "$,04," + vendor_id + ",0.0.1,IN,07,L," + imei + ",MH12AB1234,1," + dateTimeString + ",0" + lat + ",N,0" + lng + ",E,000.4,000.00,07,0570.4,02.00,01.10,IDEAIN,1,1,00.0,4.4,1,C,31,404,78,62fc,2a28,2a27,62fc,-059,2a29,62fc,-064,2851,62f8,-074,274d,6301,-074,1000,01,000034,a236dd7b,*"
            }

            LongOperation(this).execute()

        }

    }

    class LongOperation(context: MainActivity) : AsyncTask<String, Void, String>() {

        var context: MainActivity
        init {
            this.context = context
        }
        override fun onPreExecute() {


           /* progressDialog = ProgressDialog(context)
            progressDialog.setMessage("Sending, please wait...")
            progressDialog.setCancelable(false)
            progressDialog.show()*/

        }

        var str: String? = "waiting"
        override fun doInBackground(vararg params: String?): String {

            var address = InetSocketAddress(server_address, server_port)
            socket = Socket()
            var br: BufferedReader? = null
            try
            {
                socket!!.connect(address, 6000)
                socket!!.setSoTimeout(6000)
                var out = socket!!.getOutputStream()
                var output = PrintWriter(out)
                output.print(message)
                output.flush()
                br = BufferedReader(InputStreamReader(socket!!.getInputStream()))

                str = F.returnStringData(br)
                if (str != null) {
                    //T.e("HANDLER_CALL : "+"trying to print what was just read : "+str)
                    //Log.d("test", "trying to print what was just read")
                    println(str)
                }
                output.close()
                br!!.close()
                socket!!.close()

            }
            catch (e: Exception)
            {
                //T.e("HANDLER_CALL : packetType: "+packetType)
                packetType = "Failed to connect to "+server_address+":"+ server_port+" - Network is unreachable"
                T.e("HANDLER_CALL : "+"Exception : LongOperation() : "+e)
                //T.e("" + e)
            }
            return str!!

        }

        override fun onPostExecute(result: String?) {


            T.e("HANDLER_CNT : "+CNT)

            if(CNT == 0){
                CNT = 1

                if(!packetTypeForEmergency.equals("emergency"))
                {
                    //wait for 15 sec and send normal packet
                    context.fifteenMinDelayHere()
                }


            }
            if(packetType.equals("normal"))
            {
                T.e("PACKET_SENT : normal")
                //progressDialog.dismiss()
                T.t("Normal packet sent successfully")
            }
            else if (packetType.equals("login"))
            {
                T.e("PACKET_SENT : login")
                //progressDialog.dismiss()
                T.t("Login packet sent successfully")
            }
            else if (packetType.equals("alarm"))
            {
                T.e("PACKET_SENT : alarm")
                //progressDialog.dismiss()
                T.t("Alarm packet sent successfully")
            }
            else if (packetType.equals("health"))
            {
                T.e("PACKET_SENT : health")
                //progressDialog.dismiss()
                T.t("Health packet sent successfully")
            }
            else if (packetType.equals("emergency"))
            {
                T.e("PACKET_SENT : emergency")
                //progressDialog.dismiss()
                T.t("Emergency packet sent successfully")
            }
            else
            {
                T.e("PACKET_SENT : "+packetType)
                //progressDialog.dismiss()
                T.t(packetType)
            }


        }


    }

    fun fifteenMinDelayHere()
    {
        val mRunnable: Runnable
        val mHandler = Handler()
        mRunnable = Runnable {
            // TODO Auto-generated method stub
            //then send normal packet
            //T.e("HANDLER_CALL : "+"Time delay")
            if(radioButton_login.isChecked){
                packetType = "login"
            }
            else if(radioButton_normal.isChecked){
                packetType = "normal"

            }
            else if(radioButton_alarm.isChecked){
                packetType = "alarm"
            }

            startNormalPacketHandler()
        }
        mHandler.postDelayed(mRunnable, 15000)//15 sec
    }


    private fun changeMap(latitude: Double, longitude: Double) {


        Log.e("LOG", "latitude : " + latitude)
        Log.e("LOG", "longitude : " + longitude)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling

            return
        }

        if (mMap != null) {
            mMap.uiSettings.isZoomControlsEnabled = false
            val latLong: LatLng

            latLong = LatLng(latitude, longitude)
            val cameraPosition = CameraPosition.Builder().target(latLong).zoom(15f).build()
            mMap.isMyLocationEnabled = true
            mMap.uiSettings.isMyLocationButtonEnabled = true
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))


        } else {
            Toast.makeText(
                applicationContext,
                "Sorry! unable to create maps", Toast.LENGTH_SHORT
            )
                .show()
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode and 0xFFFF == REQUEST_CHECK_STATE) {
            Log.e("LOG", "Back from REQUEST_CHECK_STATE")

        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_ID -> {

            }
        }
    }

    val PERMISSION_ID = 42
    private fun checkPermission(vararg perm: String): Boolean {
        val havePermissions = perm.toList().all {
            ContextCompat.checkSelfPermission(this, it) ==
                    PackageManager.PERMISSION_GRANTED
        }
        if (!havePermissions) {
            if (perm.toList().any {
                    ActivityCompat.shouldShowRequestPermissionRationale(this, it)
                }
            ) {
                val dialog = AlertDialog.Builder(this)
                    .setTitle("Permission")
                    .setMessage("Permission needed!")
                    .setPositiveButton("OK", { id, v ->
                        ActivityCompat.requestPermissions(
                            this, perm, PERMISSION_ID
                        )
                    })
                    .setNegativeButton("No", { id, v -> })
                    .create()
                dialog.show()
            } else {
                ActivityCompat.requestPermissions(this, perm, PERMISSION_ID)
            }
            return false
        }
        return true
    }

    override fun onLocationChanged(location: Location?) {


        // T.e(""+location!!.latitude)

    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {

    }

    override fun onProviderEnabled(provider: String?) {

    }

    override fun onProviderDisabled(provider: String?) {

    }

    override fun onConnectionFailed(p0: ConnectionResult) {

    }

    override fun onConnected(bundle: Bundle?) {

    }

    override fun onConnectionSuspended(p0: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Add a marker in Sydney and move the camera
        val india = LatLng(20.593684, 78.962880)
        //mMap.moveCamera(CameraUpdateFactory.newLatLng(india))
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(india, 4f));

        mMap.setOnMapClickListener {
            //allPoints.add(it)
            mMap.clear()
            drag_droplat = "" + it.latitude
            drag_droplng = "" + it.longitude

            if (drag_droplat.length >= 10) {
                drag_droplat = drag_droplat.substring(0, 9)
            }
            if (drag_droplng.length >= 10) {
                drag_droplng = drag_droplng.substring(0, 9)
            }

            lat_lng_tv.setText("" + drag_droplat + " , " + drag_droplng)
            mMap.addMarker(MarkerOptions().position(it))
            // T.e("lat_long : "+it.latitude+""+it.longitude)
        }
    }
}
