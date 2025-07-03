package com.maizeshark.techinsat

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.DefaultTimeBar
import androidx.media3.ui.PlayerView

@UnstableApi
class MainActivity : AppCompatActivity() {

    private lateinit var playerView: PlayerView
    private var player: ExoPlayer? = null

    private lateinit var customRewindButton: ImageButton
    private lateinit var customPlayPauseButton: ImageButton
    private lateinit var customForwardButton: ImageButton
    private lateinit var fadingControls: FadingControlsLayout
    private lateinit var timeBar: DefaultTimeBar

    private lateinit var tvElapsedTime: TextView
    private lateinit var tvRemainingTime: TextView

    private val mainHandler = Handler(Looper.getMainLooper())
    private val timeUpdateHandler = Handler(Looper.getMainLooper())
    private val timeUpdateInterval = 500L

    private var playbackPosition = 0L
    private var currentItem = 0
    private var playWhenReady = true

    private val VIDEO_URL = "https://storage.googleapis.com/exoplayer-test-media-0/BigBuckBunny_320x180.mp4"

    private val autoHideDelayMs = 5000L
    private val autoHideRunnable = Runnable {
        hideControls()
    }

    // GEÄNDERT: Die Logik hier ist jetzt der Schlüssel zur Lösung!
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // Bei jeder Tastenaktion (die hier ankommt) die Steuerung anzeigen und Timer neustarten
        if (event.action == KeyEvent.ACTION_DOWN) {
            showControls()
        }

        // DER ENTSCHEIDENDE TEIL: Priorität für die TimeBar
        val focusedView = currentFocus
        if (focusedView == timeBar) {
            // Wenn die TimeBar den Fokus hat, soll sie das Event zuerst bekommen.
            // Links/Rechts wird dann zum feinen Spulen (Scrubbing) verwendet.
            // Wenn sie es nicht verarbeiten kann (z.B. bei DPAD_UP), gibt sie false zurück.
            if (timeBar.dispatchKeyEvent(event)) {
                // Die TimeBar hat das Event verarbeitet, wir sind fertig.
                return true
            }
        }

        // Wenn die TimeBar NICHT im Fokus ist ODER das Event NICHT verarbeitet hat:
        // Leite es an den PlayerView für Standardaktionen (15s Vor/Zurück) weiter.
        // `super.dispatchKeyEvent(event)` kümmert sich um die Klicks auf die Buttons.
        return playerView.dispatchKeyEvent(event) || super.dispatchKeyEvent(event)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        playerView = findViewById(R.id.player_view)
        playerView.requestFocus()
    }

    private fun initializePlayer() {
        player = ExoPlayer.Builder(this)
            .build()
            .also { exoPlayer ->
                playerView.player = exoPlayer
                exoPlayer.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        updatePlayPauseButtonIcon(isPlaying)
                    }
                })

                val mediaItem = MediaItem.fromUri(VIDEO_URL)
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.playWhenReady = playWhenReady
                exoPlayer.seekTo(currentItem, playbackPosition)
                exoPlayer.prepare()
            }
        timeUpdateHandler.post(updateTimeRunnable)
        setupCustomControls()
    }

    private fun setupCustomControls() {
        fadingControls = playerView.findViewById(R.id.custom_controls_root)
        customRewindButton = playerView.findViewById(R.id.custom_rew)
        customPlayPauseButton = playerView.findViewById(R.id.custom_play_pause)
        customForwardButton = playerView.findViewById(R.id.custom_ffwd)
        tvElapsedTime = playerView.findViewById(R.id.tv_elapsed_time)
        tvRemainingTime = playerView.findViewById(R.id.tv_remaining_time)
        timeBar = playerView.findViewById(R.id.exo_progress)

        customPlayPauseButton.setOnClickListener {
            player?.playWhenReady = !(player?.playWhenReady ?: true)
            restartAutoHideTimer()
        }
        customRewindButton.setOnClickListener {
            player?.seekBack()
            restartAutoHideTimer()
        }
        customForwardButton.setOnClickListener {
            player?.seekForward()
            restartAutoHideTimer()
        }

        playerView.setOnClickListener {
            showControls()
        }

        restartAutoHideTimer()
    }

    // Die onKeyDown-Methode ist nicht mehr zwingend notwendig, da dispatchKeyEvent das jetzt abdeckt,
    // aber sie schadet auch nicht als Fallback.
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (fadingControls.visibility != View.VISIBLE) {
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_UP,
                KeyEvent.KEYCODE_DPAD_DOWN,
                KeyEvent.KEYCODE_DPAD_LEFT,
                KeyEvent.KEYCODE_DPAD_RIGHT,
                KeyEvent.KEYCODE_DPAD_CENTER,
                KeyEvent.KEYCODE_ENTER -> {
                    showControls()
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onBackPressed() {
        if (fadingControls.visibility == View.VISIBLE) {
            hideControls()
        } else {
            super.onBackPressed()
        }
    }

    private fun showControls() {
        if (fadingControls.visibility == View.VISIBLE) {
            restartAutoHideTimer()
            return
        }
        fadingControls.visibility = View.VISIBLE
        // Fokus auf Play/Pause-Button setzen, wenn Steuerung erscheint
        customPlayPauseButton.requestFocus()
        restartAutoHideTimer()
    }

    private fun hideControls() {
        fadingControls.visibility = View.GONE
        cancelAutoHideTimer()
    }

    private fun restartAutoHideTimer() {
        cancelAutoHideTimer()
        mainHandler.postDelayed(autoHideRunnable, autoHideDelayMs)
    }

    private fun cancelAutoHideTimer() {
        mainHandler.removeCallbacks(autoHideRunnable)
    }

    // --- Restlicher Code bleibt unverändert ---

    private fun updatePlayPauseButtonIcon(isPlaying: Boolean) {
        val drawableId = if (isPlaying) R.drawable.pause else R.drawable.play
        customPlayPauseButton.setImageDrawable(ContextCompat.getDrawable(this, drawableId))
    }

    private fun releasePlayer() {
        player?.let { exoPlayer ->
            playbackPosition = exoPlayer.currentPosition
            currentItem = exoPlayer.currentMediaItemIndex
            playWhenReady = exoPlayer.playWhenReady
            exoPlayer.release()
        }
        player = null
        timeUpdateHandler.removeCallbacks(updateTimeRunnable)
    }

    private fun hideSystemUi() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, playerView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onStart() {
        super.onStart()
        initializePlayer()
    }

    override fun onResume() {
        super.onResume()
        hideSystemUi()
        if (player == null) {
            initializePlayer()
        }
    }

    override fun onPause() {
        super.onPause()
        releasePlayer()
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }

    private val updateTimeRunnable = object : Runnable {
        override fun run() {
            updatePlaybackTimes()
            timeUpdateHandler.postDelayed(this, timeUpdateInterval)
        }
    }

    private fun updatePlaybackTimes() {
        val exoPlayer = player ?: return
        val position = exoPlayer.currentPosition
        val duration = exoPlayer.duration
        if (duration <= 0) return
        tvElapsedTime.text = formatTime(position)
        tvRemainingTime.text = "-${formatTime(duration - position)}"
    }

    private fun formatTime(millis: Long): String {
        val totalSeconds = millis / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%d:%02d", minutes, seconds)
    }
}