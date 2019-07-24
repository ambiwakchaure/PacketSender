package com.example.packetsender.other

import android.app.Application
import android.content.Context
import android.content.SharedPreferences

class MyApplication : Application(){

    companion object{
        lateinit var context: Context
        lateinit var prefs : SharedPreferences
        lateinit var editor : SharedPreferences.Editor
    }
    override fun onCreate() {
        super.onCreate()
        context = applicationContext

        //shared preferences
        prefs = getSharedPreferences(Constants.PREF_KEY,0)
        editor = prefs.edit()
        editor.commit()
    }

}