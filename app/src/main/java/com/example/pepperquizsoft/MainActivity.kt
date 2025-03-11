package com.example.pepperquizsoft

import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.aldebaran.qi.Future
import com.aldebaran.qi.sdk.QiContext
import com.aldebaran.qi.sdk.QiSDK
import com.aldebaran.qi.sdk.RobotLifecycleCallbacks
import com.aldebaran.qi.sdk.`object`.actuation.Animation
import com.aldebaran.qi.sdk.builder.AnimateBuilder
import com.aldebaran.qi.sdk.builder.AnimationBuilder
import com.aldebaran.qi.sdk.builder.SayBuilder
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity(), RobotLifecycleCallbacks {
    private var qiContext: QiContext? = null
    private var socket: Socket? = null
    private var reader: BufferedReader? = null
    private var writer: PrintWriter? = null
    private lateinit var serverIpInput: EditText
    private lateinit var connectButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        QiSDK.register(this, this)
    }

    override fun onRobotFocusGained(qiContext: QiContext?) {
        this.qiContext = qiContext

        serverIpInput = findViewById(R.id.server_ip_input)
        connectButton = findViewById(R.id.connect_button)

        connectButton.setOnClickListener {
            val serverIP = serverIpInput.text.toString()
            thread { connectToServer(serverIP) }
        }
    }

    private fun connectToServer(serverIP: String) {
        try {
            val serverPort = 12345
            socket = Socket(serverIP, serverPort)
            reader = BufferedReader(InputStreamReader(socket!!.getInputStream()))
            writer = PrintWriter(socket!!.getOutputStream(), true)

            Log.d("PepperClient", "✅ Connected to server")

            while (true) {
                val command = reader?.readLine()
                command?.let { processCommand(it) }
            }
        } catch (e: Exception) {
            Log.e("PepperClient", "❌ Connection error: ${e.message}")
        }
    }

    private fun processCommand(command: String) {
        Log.d("PepperClient", "📩 Received command: $command")

        val moveCommands = listOf("move", "turn_left", "turn_right", "turn_left_10", "turn_right_10")
        val commandParts = command.split(" ", limit = 2)

        if (commandParts.isNotEmpty()) {
            val commandPrefix = commandParts[0]
            val commandDetails = if (commandParts.size > 1) commandParts[1] else ""

            when (commandPrefix) {
                in moveCommands -> handleMovement(commandPrefix)
                "voice" -> sayText(commandDetails)
                else -> handleTextCommand(command)
            }
        }
    }

    private fun handleMovement(command: String) {
        val animationResource = when (command) {
            "move" -> R.raw.trajectory_forward
            "turn_right" -> R.raw.trajectory_right_90_deg
            "turn_left" -> R.raw.trajectory_left_90_deg
            "turn_right_10" -> R.raw.trajectory_right_10_deg
            "turn_left_10" -> R.raw.trajectory_left_10_deg
            else -> null
        }

        animationResource?.let {
            val animation = AnimationBuilder.with(qiContext).withResources(it).build()
            val animate = AnimateBuilder.with(qiContext).withAnimation(animation).build()
            animate.run()
        }
    }

    private fun sayText(text: String) {
        val say = SayBuilder.with(qiContext).withText(text).build()
        say.run()
    }

    private fun handleTextCommand(command: String) {
        //sayText(command)

        // If the command is a question, trigger listening
        if (command.startsWith("voice_command")) {
            val textToSay = command.substringAfter("voice_command").trim()
            sayText(textToSay)
        }
        else if (command.startsWith("quiz_command")) {
            val textToSay = command.substringAfter("quiz_command").trim()
            sayText(textToSay)
            writer?.println("start listening")
        }
        else if (command.startsWith("negative_feedback_command")) {
            val textToSay = command.substringAfter("negative_feedback_command").trim().replace(Regex("^\\d+\\.\\s*"), "")
            val animations = listOf(
                R.raw.dizzy_a002, R.raw.furious_a001, R.raw.sad_a001, R.raw.sad_a003,
                R.raw.surprised_a001, R.raw.thinking_a002
            )

            val musicFiles = listOf(
                R.raw.negetive, R.raw.negetive_2, R.raw.negetive_3, R.raw.negative_4,
                R.raw.negative_5, R.raw.negative_6, R.raw.negative_7
            )

            val emptySay = SayBuilder.with(qiContext).withText("").build()

            val animation = AnimationBuilder.with(qiContext)
                .withResources(animations.random())
                .build()

            val animate = AnimateBuilder.with(qiContext)
                .withAnimation(animation)
                .build()

            val mediaPlayer = MediaPlayer.create(applicationContext, musicFiles.random())

            val animFuture: Future<Void> = emptySay.async().run()

            // Chain a lambda to the future.
            animFuture.thenCompose {
                mediaPlayer.start()
                animate.async().run()
            }.thenCompose {
                mediaPlayer.release()
                sayText(textToSay)
                emptySay.async().run()
            }
        }

        else if (command.startsWith("positive_feedback_command")) {
            val textToSay = command.substringAfter("positive_feedback_command").trim().replace(Regex("^\\d+\\.\\s*"), "")

            // Define animations and music resources
            val animations = listOf(
                R.raw.funny_a001, R.raw.nicereaction_a001, R.raw.both_hands_low_b001, R.raw.hello_a009,
                R.raw.salute_right_b001, R.raw.tickling_a002
            )

            val musicFiles = listOf(
                R.raw.positive, R.raw.positive_1, R.raw.positive_2, R.raw.positve_4, R.raw.positive_5,
                R.raw.positive_6, R.raw.positive_7, R.raw.positive_8
            )

            val emptySay = SayBuilder.with(qiContext).withText("").build()

            val animation = AnimationBuilder.with(qiContext)
                .withResources(animations.random())
                .build()

            val animate = AnimateBuilder.with(qiContext)
                .withAnimation(animation)
                .build()

            val mediaPlayer = MediaPlayer.create(applicationContext, musicFiles.random())

            val animFuture: Future<Void> = emptySay.async().run()

            // Chain a lambda to the future.
            animFuture.thenCompose {
                mediaPlayer.start()
                animate.async().run()
            }.thenCompose {
                mediaPlayer.release()
                sayText(textToSay)
                emptySay.async().run()
            }

        }

        else {
            Log.d("PepperClient", "🗣️ No listening triggered (Feedback detected)")
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
            Log.e("PepperClient", "❌ Error closing socket: ${e.message}")
        }
    }
}
