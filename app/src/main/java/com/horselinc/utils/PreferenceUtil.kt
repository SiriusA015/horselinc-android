package com.horselinc.utils

import android.content.Context
import com.horselinc.R

@Suppress("UNCHECKED_CAST")
class PreferenceUtil constructor(context: Context) {
    private val preferences = context.getSharedPreferences(context.getString(R.string.app_name), 0)

    fun put(key: String, value: Any): PreferenceUtil {
        val e = preferences.edit()
        try {
            when (value) {
                is String -> e.putString(key, value)
                is Boolean -> e.putBoolean(key, value)
                is Int -> e.putInt(key, value)
                is Long -> e.putLong(key, value)
                is Float -> e.putFloat(key, value)
                is Set<*> -> e.putStringSet(key, value as Set<String>)
                else -> throw ClassCastException(value.javaClass.name + " is not allowed type of object.")
            }
        } finally {
            e.apply()
        }

        return this
    }

    fun <T> get(key: String, defaultValue: T): T {
        return when (defaultValue) {
            is String -> preferences.getString(key, defaultValue) as T
            is Boolean -> preferences.getBoolean(key, defaultValue) as T
            is Int -> preferences.getInt(key, defaultValue) as T
            is Long -> preferences.getLong(key, defaultValue) as T
            is Float -> preferences.getFloat(key, defaultValue) as T
            is Set<*> -> preferences.getStringSet(key, defaultValue as Set<String>) as T
            else -> throw ClassCastException(defaultValue.toString() + " is not allowed type of object.")
        }
    }

    fun getAll(): Map<String, *> {
        return preferences.all
    }

    fun contains(key: String): Boolean {
        return preferences.contains(key)
    }

    fun clear() {
        preferences.edit().clear().apply()
    }

    fun remove(key: String) {
        preferences.edit().remove(key).apply()
    }
}