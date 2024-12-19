package com.example.myapplication2

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.SurfaceView
import android.widget.FrameLayout
import android.widget.Toast

import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import io.agora.rtc2.video.VideoCanvas

class MainActivity : AppCompatActivity() {
    // Fill in the App ID obtained from the Agora Console
    private val myAppId = "e9344614aec24e6989b102a177047e2e"
    // Fill in the channel name
    private val channelName = "demo2"
    // Fill in the temporary token generated from Agora Console
    private val token = "659dffb23ada47ba83734aa603b02c68"

    private var mRtcEngine: RtcEngine? = null

    private val mRtcEventHandler = object : IRtcEngineEventHandler() {
        // Callback when successfully joining the channel
        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            runOnUiThread {
                Toast.makeText(this@MainActivity, "Join channel success", Toast.LENGTH_SHORT).show()
            }
        }

        // Callback when a remote user or host joins the current channel
        override fun onUserJoined(uid: Int, elapsed: Int) {
            runOnUiThread {
                // When a remote user joins the channel, display the remote video stream for the specified uid
                setupRemoteVideo(uid)
            }
        }

        // Callback when a remote user or host leaves the current channel
        override fun onUserOffline(uid: Int, reason: Int) {
            runOnUiThread {
                Toast.makeText(this@MainActivity, "User offline: $uid", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initializeAndJoinChannel() {
        try {
            // Create an RtcEngineConfig instance and configure it
            val config = RtcEngineConfig().apply {
                mContext = applicationContext
                mAppId = myAppId
                mEventHandler = mRtcEventHandler
            }
            // Create and initialize an RtcEngine instance
            mRtcEngine = RtcEngine.create(config)
        } catch (e: Exception) {
            throw RuntimeException("Check the error.")
        }
        // Enable the video module
        mRtcEngine?.enableVideo()

        // Enable local preview
        mRtcEngine?.startPreview()

        // Create a SurfaceView object and make it a child object of FrameLayout
        val container = findViewById<FrameLayout>(R.id.local_video_view_container)
        val surfaceView = SurfaceView(applicationContext)
        container.addView(surfaceView)
        // Pass the SurfaceView object to the SDK and set the local view
        mRtcEngine?.setupLocalVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_FIT, 0))

        // Create an instance of ChannelMediaOptions and configure it
        val options = ChannelMediaOptions().apply {
            // Set the user role to BROADCASTER or AUDIENCE according to the scenario
            clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
            // In the video calling scenario, set the channel profile to CHANNEL_PROFILE_COMMUNICATION
            channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
        }

        // Join the channel using a temporary token and channel name, setting uid to 0 means the engine will randomly generate a username
        // The onJoinChannelSuccess callback will be triggered upon success
        mRtcEngine?.joinChannel(token, channelName, 0, options)
    }

    private fun setupRemoteVideo(uid: Int) {
        val container = findViewById<FrameLayout>(R.id.remote_video_view_container)
        val surfaceView = SurfaceView(applicationContext).apply {
            setZOrderMediaOverlay(true)
        }
        container.addView(surfaceView)
        // Pass the SurfaceView object to the SDK and set the remote view
        mRtcEngine?.setupRemoteVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_FIT, uid))
    }

    private val permissionReqId = 22

    // Obtain recording, camera and other permissions required to implement real-time audio and video interaction
    private fun getRequiredPermissions(): Array<String> {
        // Determine the permissions required when targetSDKVersion is 31 or above
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.RECORD_AUDIO, // Recording permission
                Manifest.permission.CAMERA, // Camera permission
                Manifest.permission.READ_PHONE_STATE, // Permission to read phone status
                Manifest.permission.BLUETOOTH_CONNECT // Bluetooth connection permission
            )
        } else {
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA
            )
        }
    }

    private fun checkPermissions(): Boolean {
        for (permission in getRequiredPermissions()) {
            val permissionCheck = ContextCompat.checkSelfPermission(this, permission)
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    // System permission request callback
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (checkPermissions()) {
            initializeAndJoinChannel()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // If authorized, initialize RtcEngine and join the channel
        if (checkPermissions()) {
            initializeAndJoinChannel()
        } else {
            ActivityCompat.requestPermissions(this, getRequiredPermissions(), permissionReqId)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Stop local video preview
        mRtcEngine?.stopPreview()
        // Leave the channel
        mRtcEngine?.leaveChannel()
    }
}