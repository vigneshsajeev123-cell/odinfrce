package com.memoryuniverse.core.media

import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * ContentObserver for Android MediaStore.
 * Detects new photos taken or deleted while the app is active, debouncing rapid shots.
 */
class MediaStoreObserver(
    private val scope: CoroutineScope,
    private val onMediaStoreChanged: () -> Unit
) : ContentObserver(Handler(Looper.getMainLooper())) {

    private var debounceJob: Job? = null
    private val debounceDelayMs = 1500L

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        super.onChange(selfChange, uri)
        // Debounce camera burst triggers
        debounceJob?.cancel()
        debounceJob = scope.launch(Dispatchers.IO) {
            delay(debounceDelayMs)
            onMediaStoreChanged()
        }
    }
}
