package com.corps.healthmate.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Button
import androidx.fragment.app.Fragment
import com.corps.healthmate.R
import com.corps.healthmate.utils.NetworkUtils

class NoInternetFragment : Fragment() {

    private var retryCallback: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_no_internet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Add bounce animation to the retry button
        val bounceAnimation = AnimationUtils.loadAnimation(context, R.anim.bounce_animation)
        view.findViewById<Button>(R.id.retryButton).apply {
            startAnimation(bounceAnimation)
            setOnClickListener {
                if (NetworkUtils.isNetworkAvailable(requireContext())) {
                    retryCallback?.invoke()
                }
            }
        }
    }

    fun setRetryCallback(callback: () -> Unit) {
        retryCallback = callback
    }

    companion object {
        fun newInstance(retryCallback: () -> Unit): NoInternetFragment {
            return NoInternetFragment().apply {
                setRetryCallback(retryCallback)
            }
        }
    }
}
