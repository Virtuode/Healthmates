package com.corps.healthmate.interfaces

interface SurveyDataProvider {
    fun getSurveyData(): Map<String, Any?>
    fun isDataValid(): Boolean
    fun loadExistingData(data: Map<String, Any?>)
} 