package com.maizeshark.techinsat

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.MediaController // Import MediaController
import android.widget.ProgressBar
import android.widget.Toast
import android.widget.VideoView
import androidx.fragment.app.FragmentActivity

class MainActivity : FragmentActivity() {

    private lateinit var videoView: VideoView
    private lateinit var progressBar: ProgressBar
    private var mediaController: MediaController? = null // Store MediaController instance

    companion object {
        private const val TAG = "MainActivity"
        private const val VIDEO_URL = "https://abhiandroid.com/ui/wp-content/uploads/2016/04/videoviewtestingvideo.mp4"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        videoView = findViewById(R.id.Film)
        progressBar = findViewById(R.id.seekBar)

        progressBar.visibility = View.VISIBLE

        // Setup MediaController
        if (mediaController == null) {
            mediaController = MediaController(this)
            mediaController?.setAnchorView(videoView) // Anchor to the VideoView
        }
        videoView.setMediaController(mediaController)

        val videoUri = Uri.parse(VIDEO_URL)
        videoView.setVideoURI(videoUri)

        videoView.setOnPreparedListener { mp ->
            Log.d(TAG, "Video prepared. Starting playback.")
            progressBar.visibility = View.GONE
            mp.start()
        }

        videoView.setOnErrorListener { mp, what, extra ->
            progressBar.visibility = View.GONE
            Log.e(TAG, "Error during video playback. What: $what, Extra: $extra")
            Toast.makeText(this, "Error playing video", Toast.LENGTH_LONG).show()
            true
        }

        videoView.setOnCompletionListener {
            Log.d(TAG, "Video playback completed.")
            Toast.makeText(this, "Video finished", Toast.LENGTH_SHORT).show()
        }

        // Start playing after setting URI and listeners
        // videoView.start() // This can be removed if you start it in onPrepared
    }

    // It's good practice to release resources when the activity is destroyed
    override fun onStop() {
        super.onStop()
        // Stop playback and release resources to prevent leaks if the activity is stopped
        if (videoView.isPlaying) {
            videoView.stopPlayback()
        }
    }
}