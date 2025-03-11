package com.corps.healthmate.adapters

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class SurveyPagerAdapter(fragmentManager: androidx.fragment.app.FragmentActivity, private val fragments: List<Fragment>) :
    FragmentStateAdapter(fragmentManager) {

    override fun getItemCount(): Int {
        return fragments.size
    }

    override fun createFragment(position: Int): Fragment {
        return fragments[position]
    }

}
