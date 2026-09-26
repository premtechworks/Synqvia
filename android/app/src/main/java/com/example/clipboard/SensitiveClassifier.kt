package com.example.clipboard

import android.content.ClipData
import android.content.ClipDescription
import android.os.Build

interface SensitiveClassifier {
    fun isSensitive(clipData: ClipData?): Boolean
    fun isSensitive(description: ClipDescription?): Boolean
}

class DefaultSensitiveClassifier : SensitiveClassifier {

    override fun isSensitive(clipData: ClipData?): Boolean {
        if (clipData == null) return false
        return isSensitive(clipData.description)
    }

    override fun isSensitive(description: ClipDescription?): Boolean {
        if (description == null) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val extras = description.extras ?: return false
            return extras.getBoolean(ClipDescription.EXTRA_IS_SENSITIVE, false)
        }
        return false
    }
}
