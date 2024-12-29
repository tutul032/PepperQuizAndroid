package com.example.pepperquizsoft

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.aldebaran.qi.sdk.QiContext
import com.aldebaran.qi.sdk.QiSDK
import com.aldebaran.qi.sdk.RobotLifecycleCallbacks
import com.aldebaran.qi.sdk.builder.SayBuilder
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Socket
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity(), RobotLifecycleCallbacks {
    private var qiContext: QiContext? = null
    private var socket: Socket? = null
    private var reader: BufferedReader? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        QiSDK.register(this, this)

    }

    override fun onRobotFocusGained(qiContext: QiContext?) {
        // Start client in a background thread
        thread {
            try {
                val serverAddress = "192.168.0.110" // Replace with your PC's IP
                val serverPort = 12345
                socket = Socket(serverAddress, serverPort)

                // Setup reader for incoming messages
                reader = BufferedReader(InputStreamReader(socket!!.getInputStream()))
                Log.d("PepperClient", "Connected to server")

                // Start listening for commands
                try {
                    while (true) {
                        // Read command from the server
                        val command = reader?.readLine()
                        if (command != null) {
                            Log.d("PepperClient", "Received command: $command")
                            val say = SayBuilder.with(qiContext)
                                .withText("$command")
                                .build()
                            say.run()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("PepperClient", "Error reading commands: ${e.message}")
                }
            } catch (e: Exception) {
                Log.e("PepperClient", "Connection error: ${e.message}")
            }
        }
    }

    override fun onRobotFocusLost() {
        qiContext = null
    }

    override fun onRobotFocusRefused(reason: String?) {}


    override fun onDestroy() {
        super.onDestroy()
        try {
            socket?.close()
        } catch (e: Exception) {
            Log.e("PepperClient", "Error closing socket: ${e.message}")
        }
    }
}
