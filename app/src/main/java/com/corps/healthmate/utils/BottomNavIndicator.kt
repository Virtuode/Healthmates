package com.corps.healthmate.utils

import android.animation.ValueAnimator
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import androidx.core.animation.doOnEnd

class BottomNavIndicator(
    private val indicatorView: View,
    private val container: LinearLayout
) {
    private var currentPosition = 0
    private var itemWidth = 0f
    
    init {
        // Wait for layout to get item width
        container.post {
            itemWidth = (container.width / 5f) // 5 is number of menu items
            updateIndicatorPosition(0, animate = false)
        }
    }

    fun updateIndicatorPosition(position: Int, animate: Boolean = true) {
        if (!animate) {
            indicatorView.translationX = calculateTranslationX(position.toFloat())
            currentPosition = position
            return
        }

        ValueAnimator.ofFloat(currentPosition.toFloat(), position.toFloat()).apply {
            duration = 300L // Explicitly defining duration as Long
            interpolator = DecelerateInterpolator()

            addUpdateListener { animator ->
                indicatorView.translationX = calculateTranslationX(animator.animatedValue as Float)
            }

            doOnEnd { currentPosition = position }
            start()
        }
    }


    private fun calculateTranslationX(position: Float): Float {
        return position * itemWidth + (itemWidth - indicatorView.width) / 2
    }
}
