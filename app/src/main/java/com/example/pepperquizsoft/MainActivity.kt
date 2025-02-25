package com.example.pepperquizsoft

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.aldebaran.qi.sdk.QiContext
import com.aldebaran.qi.sdk.QiSDK
import com.aldebaran.qi.sdk.RobotLifecycleCallbacks
import com.aldebaran.qi.sdk.`object`.actuation.Animation
import com.aldebaran.qi.sdk.builder.AnimateBuilder
import com.aldebaran.qi.sdk.builder.AnimationBuilder
import com.aldebaran.qi.sdk.builder.SayBuilder
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Socket
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity(), RobotLifecycleCallbacks {
    private var qiContext: QiContext? = null
    private var socket: Socket? = null
    private var reader: BufferedReader? = null
    private lateinit var serverIpInput: EditText
    private lateinit var connectButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        QiSDK.register(this, this)

    }

    override fun onRobotFocusGained(qiContext: QiContext?) {

        serverIpInput = findViewById(R.id.server_ip_input)
        connectButton = findViewById(R.id.connect_button)
        connectButton.setOnClickListener {

            val serverIP = serverIpInput.text.toString()

            thread {
                try {
                    //val serverIP = "192.168.0.110" // Replace with your PC's IP
                    val serverPort = 12345
                    socket = Socket(serverIP, serverPort)

                    // Setup reader for incoming messages
                    reader = BufferedReader(InputStreamReader(socket!!.getInputStream()))
                    Log.d("PepperClient", "Connected to server")

                    // Start listening for commands
                    try {
                        while (true) {
                            // Read command from the server
                            val command = reader?.readLine()

                            if (command == "move") {
                                val animation: Animation = AnimationBuilder.with(qiContext) // Create the builder with the context.
                                    .withResources(R.raw.trajectory_forward) // Set the animation resource.
                                    .build()
                                val animate = AnimateBuilder.with(qiContext) // Create the builder with the context.
                                    .withAnimation(animation) // Set the animation.
                                    .build() // Build the animate action.
                                animate.run()
                            }

                            else if (command == "turn_right") {
                                val animation: Animation = AnimationBuilder.with(qiContext) // Create the builder with the context.
                                    .withResources(R.raw.trajectory_right) // Set the animation resource.
                                    .build()
                                val animate = AnimateBuilder.with(qiContext) // Create the builder with the context.
                                    .withAnimation(animation) // Set the animation.
                                    .build() // Build the animate action.
                                animate.run()
                            }

                            else if (command == "turn_left") {
                                val animation: Animation = AnimationBuilder.with(qiContext) // Create the builder with the context.
                                    .withResources(R.raw.trajectory_left) // Set the animation resource.
                                    .build()
                                val animate = AnimateBuilder.with(qiContext) // Create the builder with the context.
                                    .withAnimation(animation) // Set the animation.
                                    .build() // Build the animate action.
                                animate.run()

                            }
                            else {
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



           // Start client in a background thread

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
