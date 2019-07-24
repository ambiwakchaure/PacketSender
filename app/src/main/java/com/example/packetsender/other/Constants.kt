package com.example.packetsender.other

interface Constants {

    companion object{
        var PREF_KEY = "packet_sender"
        var SERVER_ADDRESS = "server_address"
        var SERVER_PORT = "server_port"
        var NAVIGATION = "navigation"

        var NORMAL_PACKET_DELAY : Long = 10000
        var HEALTH_PACKET_DELAY : Long = 120000
    }
}