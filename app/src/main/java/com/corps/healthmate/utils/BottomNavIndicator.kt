package com.corps.healthmate.utils

import android.animation.ValueAnimator
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout

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
            val translationX = calculateTranslationX(position.toFloat())
            indicatorView.translationX = translationX
            currentPosition = position
            return
        }

        val startPosition = currentPosition
        val endPosition = position
        
        ValueAnimator.ofFloat(startPosition.toFloat(), endPosition.toFloat()).apply {
            duration = 300
            interpolator = DecelerateInterpolator()
            
            addUpdateListener { animator ->
                val value = animator.animatedValue as Float
                val translationX = calculateTranslationX(value)
                indicatorView.translationX = translationX
            }
            
            start()
        }
        
        currentPosition = position
    }

    private fun calculateTranslationX(position: Float): Float {
        return position * itemWidth + (itemWidth - indicatorView.width) / 2
    }
}
