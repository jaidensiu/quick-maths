package com.jaidensiu.quickmaths.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.gameDataStore by preferencesDataStore(name = "game")

@Singleton
class BestTimeRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    val bestTimeMs: Flow<Long?> = context.gameDataStore.data
        .catch { error ->
            if (error is IOException) {
                emit(value = emptyPreferences())
            } else {
                throw error
            }
        }
        .map { preferences -> preferences[BEST_TIME_KEY] }

    suspend fun submitTime(timeMs: Long) {
        context.gameDataStore.edit { preferences ->
            val current = preferences[BEST_TIME_KEY]
            if (current == null || timeMs < current) {
                preferences[BEST_TIME_KEY] = timeMs
            }
        }
    }

    private companion object {
        val BEST_TIME_KEY = longPreferencesKey(name = "best_time_ms")
    }
}
