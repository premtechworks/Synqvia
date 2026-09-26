package com.example.di

import android.content.Context
import com.example.clipboard.ClipboardCaptureManager
import com.example.clipboard.DefaultSensitiveClassifier
import com.example.data.AppDatabase
import com.example.data.ClipRepository
import com.example.data.SyncPreferences
import com.example.ime.AndroidDefaultImeDetector
import com.example.ime.DefaultImeDetector
import kotlinx.coroutines.Dispatchers

interface AppContainer {
    val syncPreferences: SyncPreferences
    val appDatabase: AppDatabase
    val clipRepository: ClipRepository
    val clipboardCaptureManager: ClipboardCaptureManager
    val defaultImeDetector: DefaultImeDetector
}

class DefaultAppContainer(private val context: Context) : AppContainer {
    override val syncPreferences: SyncPreferences by lazy {
        SyncPreferences(context)
    }

    override val appDatabase: AppDatabase by lazy {
        AppDatabase.getInstance(context)
    }

    override val clipRepository: ClipRepository by lazy {
        ClipRepository(
            clipDao = appDatabase.clipDao(),
            syncPreferences = syncPreferences,
            ioDispatcher = Dispatchers.IO
        )
    }

    override val defaultImeDetector: DefaultImeDetector by lazy {
        AndroidDefaultImeDetector(context)
    }

    override val clipboardCaptureManager: ClipboardCaptureManager by lazy {
        ClipboardCaptureManager(
            context = context,
            clipRepository = clipRepository,
            syncPreferences = syncPreferences,
            sensitiveClassifier = DefaultSensitiveClassifier(),
            defaultImeDetector = defaultImeDetector,
            ioDispatcher = Dispatchers.IO,
            mainDispatcher = Dispatchers.Main
        )
    }
}
