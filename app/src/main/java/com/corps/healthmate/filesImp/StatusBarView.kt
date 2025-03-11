package com.corps.healthmate.filesImp

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import com.corps.healthmate.R


class StatusBarView : LinearLayout {
    private var indicatorStepOne: View? = null
    private var indicatorStepTwo: View? = null
    private var indicatorStepThree: View? = null
    private var indicatorStepFour: View? = null

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context)
    }

    private fun init(context: Context) {
        LayoutInflater.from(context).inflate(R.layout.view_status_bar, this, true)

        indicatorStepOne = findViewById(R.id.step_one)
        indicatorStepTwo = findViewById(R.id.step_two)
        indicatorStepThree = findViewById(R.id.step_three)
        indicatorStepFour = findViewById(R.id.step_four)
    }

    private fun getColorCompat(colorRes: Int): Int {
        return context.getColor(colorRes)
    }

    fun setCurrentStep(currentStep: Int) {
        resetIndicators()

        when (currentStep) {
            1 -> indicatorStepOne!!.setBackgroundColor(getColorCompat(R.color.logo_color))
            2 -> {
                indicatorStepOne!!.setBackgroundColor(getColorCompat(R.color.logo_color))
                indicatorStepTwo!!.setBackgroundColor(getColorCompat(R.color.logo_color))
            }

            3 -> {
                indicatorStepOne!!.setBackgroundColor(getColorCompat(R.color.logo_color))
                indicatorStepTwo!!.setBackgroundColor(getColorCompat(R.color.logo_color))
                indicatorStepThree!!.setBackgroundColor(getColorCompat(R.color.logo_color))
            }

            4 -> {
                indicatorStepOne!!.setBackgroundColor(getColorCompat(R.color.logo_color))
                indicatorStepTwo!!.setBackgroundColor(getColorCompat(R.color.logo_color))
                indicatorStepThree!!.setBackgroundColor(getColorCompat(R.color.logo_color))
                indicatorStepFour!!.setBackgroundColor(getColorCompat(R.color.logo_color))
            }

            else -> {}
        }
    }

    private fun resetIndicators() {
        indicatorStepOne!!.setBackgroundColor(getColorCompat(android.R.color.darker_gray))
        indicatorStepTwo!!.setBackgroundColor(getColorCompat(android.R.color.darker_gray))
        indicatorStepThree!!.setBackgroundColor(getColorCompat(android.R.color.darker_gray))
        indicatorStepFour!!.setBackgroundColor(getColorCompat(android.R.color.darker_gray))
    }
}
