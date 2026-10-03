package com.github.premtechworks.synqvia.ime

import android.content.Context
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.view.ContextThemeWrapper
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.premtechworks.synqvia.SynqviaApp
import com.github.premtechworks.synqvia.R
import com.github.premtechworks.synqvia.clipboard.ClipboardCaptureManager
import com.github.premtechworks.synqvia.data.ClipEntity
import com.github.premtechworks.synqvia.data.ClipRepository
import com.github.premtechworks.synqvia.data.SyncPreferences
import com.github.premtechworks.synqvia.di.DefaultAppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Android InputMethodService implementing QWERTY typing, symbols, and clipboard history panel.
 * As an active IME, this service allows full typing while seamlessly providing clipboard
 * insertion from a toggled panel that maintains identical height.
 */
class SynqviaImeService : InputMethodService() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private var observeClipsJob: Job? = null

    private lateinit var clipboardCaptureManager: ClipboardCaptureManager
    private lateinit var clipRepository: ClipRepository
    private lateinit var syncPreferences: SyncPreferences

    private lateinit var adapter: ClipImeAdapter
    private var rvClips: RecyclerView? = null
    private var layoutEmpty: LinearLayout? = null
    private var keyboardView: SynqviaKeyboardView? = null
    private var currentEditorInfo: EditorInfo? = null
    private var filterPinnedOnly: Boolean = false

    override fun onCreate() {
        super.onCreate()

        val app = application as? SynqviaApp
        val container = app?.container ?: DefaultAppContainer(applicationContext)
        clipboardCaptureManager = container.clipboardCaptureManager
        clipRepository = container.clipRepository
        syncPreferences = container.syncPreferences

        serviceScope.launch {
            syncPreferences.themeModeFlow.collect {
                window?.window?.let { win -> applyWindowNavBar(win) }
                if (isInputViewShown) {
                    setInputView(onCreateInputView())
                }
            }
        }
    }

    fun applyWindowNavBar(win: android.view.Window) {
        val themeMode = try { syncPreferences.themeMode } catch (_: Exception) { "system" }
        val palette = KeyboardPalette.resolve(this, themeMode)
        win.navigationBarColor = palette.keyboardBg
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            win.isNavigationBarContrastEnforced = false
        }
        WindowCompat.getInsetsController(win, win.decorView).isAppearanceLightNavigationBars = !palette.isDark
    }

    override fun onConfigureWindow(win: android.view.Window, isFullscreen: Boolean, isCandidatesOnly: Boolean) {
        super.onConfigureWindow(win, isFullscreen, isCandidatesOnly)
        applyWindowNavBar(win)
    }

    override fun onWindowShown() {
        super.onWindowShown()
        window?.window?.let { applyWindowNavBar(it) }
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        window?.window?.let { applyWindowNavBar(it) }
        val themeMode = try { syncPreferences.themeMode } catch (_: Exception) { "system" }
        if (themeMode == "system") {
            setInputView(onCreateInputView())
        }
    }

    override fun onCreateInputView(): View {
        val themeMode = try { syncPreferences.themeMode } catch (_: Exception) { "system" }
        val palette = KeyboardPalette.resolve(this, themeMode)
        val themedContext = ContextThemeWrapper(this, palette.themeResId)
        val kbView = SynqviaKeyboardView(themedContext)
        keyboardView = kbView

        window?.window?.let { applyWindowNavBar(it) }

        ViewCompat.setOnApplyWindowInsetsListener(kbView) { view, windowInsets ->
            val navBarInsets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
            view.setPadding(
                view.paddingLeft,
                view.paddingTop,
                view.paddingRight,
                navBarInsets.bottom
            )
            windowInsets
        }

        val themedInflater = android.view.LayoutInflater.from(themedContext)
        val clipboardView = themedInflater.inflate(
            R.layout.ime_clipboard_view,
            kbView.layoutClipboardContainer,
            true
        )
        rvClips = clipboardView.findViewById(R.id.rv_ime_clips)
        layoutEmpty = clipboardView.findViewById(R.id.layout_ime_empty)

        kbView.actionListener = object : KeyboardActionListener {
            override fun onText(text: String) {
                currentInputConnection?.commitText(text, 1)
            }

            override fun onBackspace() {
                handleBackspace()
            }

            override fun onBackspaceRepeat() {
                handleBackspace()
            }

            override fun onEnter() {
                handleEnter()
            }

            override fun onShiftChanged(state: ShiftState) {}

            override fun onLayerChanged(layer: KeyboardLayer) {}

            override fun onToggleClipboard() {
                filterPinnedOnly = false
                if (kbView.isClipboardActive) {
                    startObservingClips()
                }
            }

            override fun onToolbar2() {
                filterPinnedOnly = kbView.isPinnedFilterActive
                if (kbView.isClipboardActive) {
                    startObservingClips()
                }
            }

            override fun onOpenKeyboardPicker() {
                switchKeyboard()
            }
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

        return kbView
    }

    override fun onComputeInsets(outInsets: Insets) {
        super.onComputeInsets(outInsets)
        val inputView = keyboardView ?: return
        val loc = IntArray(2)
        inputView.getLocationInWindow(loc)
        val inputTop = loc[1]
        outInsets.contentTopInsets = inputTop
        outInsets.visibleTopInsets = inputTop
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        currentEditorInfo = info

        val kb = keyboardView
        if (kb != null && info != null) {
            val isNumeric = KeyboardState.isNumericInput(info.inputType)
            val isPassword = KeyboardState.isPasswordInput(info.inputType)
            kb.isPasswordMode = isPassword
            kb.setLayer(if (isNumeric) KeyboardLayer.SYMBOLS_1 else KeyboardLayer.LETTERS)
            kb.enterAction = KeyboardState.resolveEnterAction(info.imeOptions)

            val capsMode = currentInputConnection?.getCursorCapsMode(info.inputType) ?: 0
            kb.applyAutoCaps(capsMode)
            kb.setClipboardPanelActive(false)
        }

        // Foreground sanctioned clipboard capture
        checkAndCapturePrimaryClip()

        // Start observing clips reactively
        startObservingClips()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        observeClipsJob?.cancel()
        super.onFinishInputView(finishingInput)
    }

    private fun handleBackspace() {
        val ic = currentInputConnection ?: return
        val selected = ic.getSelectedText(0)
        if (!selected.isNullOrEmpty()) {
            ic.commitText("", 1)
        } else {
            val before = ic.getTextBeforeCursor(2, 0)
            val count = KeyboardState.calculateBackspaceDeleteCount(before)
            if (count > 0) {
                ic.deleteSurroundingText(count, 0)
            }
        }
    }

    private fun handleEnter() {
        val ic = currentInputConnection ?: return
        val info = currentEditorInfo
        val action = info?.imeOptions?.and(EditorInfo.IME_MASK_ACTION) ?: EditorInfo.IME_ACTION_NONE
        val noEnterAction = (info?.imeOptions?.and(EditorInfo.IME_FLAG_NO_ENTER_ACTION) ?: 0) != 0
        if (!noEnterAction && (action == EditorInfo.IME_ACTION_SEND ||
                    action == EditorInfo.IME_ACTION_GO ||
                    action == EditorInfo.IME_ACTION_SEARCH ||
                    action == EditorInfo.IME_ACTION_DONE ||
                    action == EditorInfo.IME_ACTION_NEXT)
        ) {
            ic.performEditorAction(action)
        } else {
            ic.commitText("\n", 1)
        }
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
                    val displayedClips = if (filterPinnedOnly) clips.filter { it.pinned } else clips
                    adapter.submitList(displayedClips)
                    if (displayedClips.isEmpty()) {
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
                    Toast.makeText(this@SynqviaImeService, R.string.ime_copied, Toast.LENGTH_SHORT).show()
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
                                Toast.makeText(this@SynqviaImeService, R.string.ime_copied, Toast.LENGTH_SHORT).show()
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
