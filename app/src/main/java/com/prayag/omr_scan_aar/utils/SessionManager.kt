package com.prayag.omr_scan_aar.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object SessionManager {
    private const val PREF_NAME = "OMR_SCANNER_PREFS"
    private const val KEY_ROLL_NUMBER = "roll_number"

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun setRollNumber(context: Context, rollNumber: String) {
        getPreferences(context).edit {
            putString(KEY_ROLL_NUMBER, rollNumber)
        }
    }

    fun getRollNumber(context: Context): String? {
        return getPreferences(context).getString(KEY_ROLL_NUMBER, null)
    }

    fun clearRollNumber(context: Context) {
        getPreferences(context).edit {
            remove(KEY_ROLL_NUMBER)
        }
    }

    fun clearAll(context: Context) {
        getPreferences(context).edit {
            clear()
        }
    }
}