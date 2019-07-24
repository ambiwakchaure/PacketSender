package com.example.packetsender.other;

import android.util.Log;

import java.io.BufferedReader;

public class F {

    public static String returnStringData(BufferedReader br){

        String line;
        String str = "";
        try
        {

            while ((line = br.readLine()) != null) {
                Log.d("read line",line);
                str = str + line;
                str = str + "\r\n";
            }
        }
        catch (Exception e)
        {

        }
        return str;
    }
}
