package com.example.ime

import android.content.ClipboardManager
import android.content.Context
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.view.ContextThemeWrapper
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.MyClipSyncApp
import com.example.R
import com.example.clipboard.ClipboardCaptureManager
import com.example.data.ClipEntity
import com.example.data.ClipRepository
import com.example.data.SyncPreferences
import com.example.di.DefaultAppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Android InputMethodService implementing clipboard capture and clipboard history panel.
 * As an active IME, this service integrates with public ClipboardManager APIs and provides
 * a Gboard-like clipboard insertion UI.
 */
class MyClipSyncImeService : InputMethodService() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private var observeClipsJob: Job? = null

    private lateinit var clipboardCaptureManager: ClipboardCaptureManager
    private lateinit var clipRepository: ClipRepository
    private lateinit var syncPreferences: SyncPreferences

    private lateinit var adapter: ClipImeAdapter
    private var rvClips: RecyclerView? = null
    private var layoutEmpty: LinearLayout? = null

    override fun onCreate() {
        super.onCreate()

        val app = application as? MyClipSyncApp
        val container = app?.container ?: DefaultAppContainer(applicationContext)
        clipboardCaptureManager = container.clipboardCaptureManager
        clipRepository = container.clipRepository
        syncPreferences = container.syncPreferences
    }

    override fun onCreateInputView(): View {
        val view = layoutInflater.inflate(R.layout.ime_clipboard_view, null)

        val btnSwitchKeyboard = view.findViewById<ImageButton>(R.id.btn_switch_keyboard)
        val btnClose = view.findViewById<ImageButton>(R.id.btn_close_ime)
        rvClips = view.findViewById(R.id.rv_ime_clips)
        layoutEmpty = view.findViewById(R.id.layout_ime_empty)

        btnSwitchKeyboard.setOnClickListener {
            switchKeyboard()
        }

        btnClose.setOnClickListener {
            requestHideSelf(0)
        }

        adapter = ClipImeAdapter(
            onItemClick = { clip ->
                pasteClip(clip)
            },
            onItemLongClick = { clip, anchorView ->
                showClipOptions(clip, anchorView)
            },
            onPinClick = { clip ->
                togglePin(clip)
            },
            onDeleteClick = { clip ->
                deleteClip(clip)
            }
        )

        rvClips?.layoutManager = LinearLayoutManager(this)
        rvClips?.adapter = adapter

        return view
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)

        // As an active IME in the foreground, read primary clip safely
        checkAndCapturePrimaryClip()

        // Start observing clips reactively
        startObservingClips()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        observeClipsJob?.cancel()
        super.onFinishInputView(finishingInput)
    }

    private fun checkAndCapturePrimaryClip() {
        serviceScope.launch(Dispatchers.IO) {
            try {
                clipboardCaptureManager.handlePrimaryClipChanged()
            } catch (_: Exception) {
                // Defensive against background security exceptions or dead binders
            }
        }
    }

    private fun startObservingClips() {
        observeClipsJob?.cancel()
        observeClipsJob = serviceScope.launch {
            val expiryHours = syncPreferences.imeExpiryHours
            val expiryDurationMs = if (expiryHours > 0) {
                expiryHours * 3600_000L
            } else {
                365L * 24 * 3600_000L // Effectively infinite if 0
            }

            clipRepository.getImeClips(expiryDurationMs = expiryDurationMs, limit = 50)
                .collect { clips ->
                    adapter.submitList(clips)
                    if (clips.isEmpty()) {
                        layoutEmpty?.visibility = View.VISIBLE
                        rvClips?.visibility = View.GONE
                    } else {
                        layoutEmpty?.visibility = View.GONE
                        rvClips?.visibility = View.VISIBLE
                    }
                }
        }
    }

    private fun pasteClip(clip: ClipEntity) {
        val ic = currentInputConnection
        if (ic != null) {
            ic.commitText(clip.text, 1)
        } else {
            // Fallback: Copy to clipboard if no input connection is active
            serviceScope.launch {
                clipboardCaptureManager.copyToClipboardWithoutBroadcast(clip.text, clip.sensitive)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MyClipSyncImeService, R.string.ime_copied, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun togglePin(clip: ClipEntity) {
        serviceScope.launch(Dispatchers.IO) {
            clipRepository.setPinned(clip.id, !clip.pinned)
        }
    }

    private fun deleteClip(clip: ClipEntity) {
        serviceScope.launch(Dispatchers.IO) {
            clipRepository.deleteClip(clip.id)
        }
    }

    private fun showClipOptions(clip: ClipEntity, anchorView: View) {
        try {
            val themedContext = ContextThemeWrapper(this, android.R.style.Theme_DeviceDefault_Settings)
            val popup = PopupMenu(themedContext, anchorView)
            popup.menu.add(0, 1, 0, "Paste")
            popup.menu.add(0, 2, 0, if (clip.pinned) getString(R.string.ime_unpinned) else getString(R.string.ime_pinned))
            popup.menu.add(0, 3, 0, getString(R.string.ime_copy_to_clip))
            popup.menu.add(0, 4, 0, getString(R.string.ime_send_to_pc))
            popup.menu.add(0, 5, 0, getString(R.string.ime_delete))

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> {
                        pasteClip(clip)
                        true
                    }
                    2 -> {
                        togglePin(clip)
                        true
                    }
                    3 -> {
                        serviceScope.launch {
                            clipboardCaptureManager.copyToClipboardWithoutBroadcast(clip.text, clip.sensitive)
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@MyClipSyncImeService, R.string.ime_copied, Toast.LENGTH_SHORT).show()
                            }
                        }
                        true
                    }
                    4 -> {
                        serviceScope.launch(Dispatchers.IO) {
                            clipboardCaptureManager.captureLocalClip(clip.text, clip.sensitive, clip.pinned)
                        }
                        true
                    }
                    5 -> {
                        deleteClip(clip)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        } catch (_: Exception) {
            // In case themed popup fails on unusual OEM device, default to paste
            pasteClip(clip)
        }
    }

    private fun switchKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (!switchToPreviousInputMethod()) {
                imm?.showInputMethodPicker()
            }
        } else {
            imm?.showInputMethodPicker()
        }
    }

    override fun onDestroy() {
        observeClipsJob?.cancel()
        serviceJob.cancel()
        super.onDestroy()
    }
}
