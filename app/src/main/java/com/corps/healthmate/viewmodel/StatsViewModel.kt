package com.corps.healthmate.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class StatsViewModel : ViewModel() {
    private val _monthlyStats = MutableLiveData<Int>()
    val monthlyStats: LiveData<Int> get() = _monthlyStats

    private val _currentStats = MutableLiveData<Int>()
    val currentStats: LiveData<Int> get() = _currentStats

    // Initialize or update your stats here
    fun updateStats(monthly: Int, current: Int) {
        _monthlyStats.value = monthly
        _currentStats.value = current
    }
} 