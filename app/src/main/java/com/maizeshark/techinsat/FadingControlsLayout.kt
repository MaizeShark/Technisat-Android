package com.maizeshark.techinsat

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout

class FadingControlsLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "FadingControlsLayout"
        private const val ANIMATION_DURATION_MS = 250L
    }

    private var isAnimating = false
    private var actualVisibility = View.VISIBLE  // <- ersetzt .visibility

    override fun setVisibility(visibility: Int) {
        Log.d(TAG, "Requested visibility=$visibility | current=$actualVisibility")

        if (visibility == actualVisibility) {
            Log.d(TAG, "Already in desired state. Ignoring.")
            return
        }

        if (isAnimating) {
            Log.d(TAG, "Animation in progress. Deferring to after animation.")
            // Merken wir uns einfach und lassen es nach der Animation anwenden
            post {
                setVisibility(visibility)
            }
            return
        }

        when {
            actualVisibility == View.VISIBLE && visibility == View.GONE -> animateOut()
            actualVisibility != View.VISIBLE && visibility == View.VISIBLE -> animateIn()
            else -> {
                super.setVisibility(visibility)
                actualVisibility = visibility
            }
        }
    }

    private fun animateIn() {
        Log.d(TAG, "Starting fade-in")
        isAnimating = true
        actualVisibility = View.VISIBLE

        alpha = 0f
        super.setVisibility(View.VISIBLE)

        animate()
            .alpha(1f)
            .setDuration(ANIMATION_DURATION_MS)
            .withEndAction {
                isAnimating = false
                Log.d(TAG, "Fade-in done")
            }
            .start()
    }

    private fun animateOut() {
        Log.d(TAG, "Starting fade-out")
        isAnimating = true

        animate()
            .alpha(0f)
            .setDuration(ANIMATION_DURATION_MS)
            .withEndAction {
                super.setVisibility(View.GONE)
                actualVisibility = View.GONE
                isAnimating = false
                Log.d(TAG, "Fade-out done")
            }
            .start()
    }
}
