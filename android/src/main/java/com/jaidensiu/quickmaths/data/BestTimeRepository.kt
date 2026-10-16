package com.jaidensiu.quickmaths.data

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.gameDataStore by preferencesDataStore(name = "game")

@Singleton
class BestTimeRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    // Application-lifetime scope: a best-time write must not be cancelled when the game screen's
    // ViewModel is cleared right after the final answer.
    private val scope = CoroutineScope(context = SupervisorJob() + Dispatchers.IO)

    val bestTimeMs: Flow<Long?> = context.gameDataStore.data
        .catch { error ->
            if (error is IOException) {
                Log.w(TAG, "Failed to read best time; treating as unset", error)
                emit(value = emptyPreferences())
            } else {
                throw error
            }
        }
        .map { preferences -> preferences[BEST_TIME_KEY] }

    /** Persists [timeMs] if it beats the stored best. Fire-and-forget; failures are logged. */
    fun recordTime(timeMs: Long) {
        scope.launch {
            try {
                context.gameDataStore.edit { preferences ->
                    val current = preferences[BEST_TIME_KEY]
                    if (current == null || timeMs < current) {
                        preferences[BEST_TIME_KEY] = timeMs
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.w(TAG, "Failed to persist best time ${timeMs}ms", error)
            }
        }
    }

    private companion object {
        const val TAG = "BestTimeRepository"
        val BEST_TIME_KEY = longPreferencesKey(name = "best_time_ms")
    }
}
