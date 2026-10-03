package com.github.premtechworks.synqvia.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.premtechworks.synqvia.R
import com.github.premtechworks.synqvia.data.ClipEntity
import com.github.premtechworks.synqvia.ime.ClipImeAdapter
import com.github.premtechworks.synqvia.ime.KeyboardActionListener
import com.github.premtechworks.synqvia.ime.KeyboardLayer
import com.github.premtechworks.synqvia.ime.KeyboardState
import com.github.premtechworks.synqvia.ime.ShiftState
import com.github.premtechworks.synqvia.ime.SynqviaKeyboardView
import com.github.premtechworks.synqvia.ui.MainViewModel
import com.github.premtechworks.synqvia.ui.components.SynqviaCard
import com.github.premtechworks.synqvia.ui.components.SynqviaScreen
import com.github.premtechworks.synqvia.ui.components.SynqviaTopBar
import androidx.core.content.ContextCompat
import com.github.premtechworks.synqvia.ui.components.SynqviaLightDarkPreview
import com.github.premtechworks.synqvia.ui.components.SynqviaSwitch
import com.github.premtechworks.synqvia.ui.theme.SynqviaTheme
import com.github.premtechworks.synqvia.ui.theme.SynqviaType

@Composable
fun ImeSettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDefaultIme by viewModel.isDefaultIme.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val imm = remember {
        context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
    }
    var isImeEnabled by remember {
        mutableStateOf(
            imm?.enabledInputMethodList?.any { it.packageName == context.packageName } == true
        )
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshImeStatus()
                isImeEnabled = imm?.enabledInputMethodList?.any { it.packageName == context.packageName } == true
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    ImeSettingsContent(
        isDefaultIme = isDefaultIme,
        isImeEnabled = isImeEnabled,
        onBack = onBack,
        onToggleDefault = {
            if (!isImeEnabled) {
                try {
                    context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
                } catch (_: Exception) {}
            } else {
                imm?.showInputMethodPicker()
            }
        },
        modifier = modifier
    )
}

@Composable
fun ImeSettingsContent(
    isDefaultIme: Boolean,
    isImeEnabled: Boolean,
    onBack: () -> Unit,
    onToggleDefault: () -> Unit,
    modifier: Modifier = Modifier
) {
    SynqviaScreen(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
        ) {
            SynqviaTopBar(
                title = "Clipboard Keyboard (IME)",
                onBack = onBack
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Card 1: Use Synqvia Keyboard Switch
            SynqviaCard(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                padding = 16.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_use_synqvia_keyboard")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val colors = SynqviaTheme.colors
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Use Synqvia Keyboard",
                            style = SynqviaType.Headline,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = when {
                                isDefaultIme -> "Active default keyboard • Instant clipboard capture enabled"
                                isImeEnabled -> "Keyboard enabled • Tap to set as default"
                                else -> "Tap to enable Synqvia Keyboard in system settings"
                            },
                            style = SynqviaType.Caption,
                            color = colors.textSecondary
                        )
                    }

                    SynqviaSwitch(
                        checked = isDefaultIme,
                        onCheckedChange = { onToggleDefault() },
                        modifier = Modifier.testTag("switch_use_synqvia_keyboard")
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Card 2: Interactive Preview
            SynqviaCard(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                padding = 16.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_ime_preview")
            ) {
                val colors = SynqviaTheme.colors
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Preview",
                        style = SynqviaType.Headline,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Try typing below to preview the keyboard and clipboard toolbar.",
                        style = SynqviaType.Caption,
                        color = colors.textSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Embedded Android View preview
                    AndroidView(
                        modifier = Modifier.fillMaxWidth(),
                        factory = { ctx ->
                            buildInteractivePreviewView(ctx)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private fun buildInteractivePreviewView(context: Context): View {
    val container = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    // Preview EditText (system keyboard suppressed)
    val editText = EditText(context).apply {
        hint = "Try typing here…"
        setHintTextColor(ContextCompat.getColor(context, R.color.ime_key_hint))
        setTextColor(ContextCompat.getColor(context, R.color.ime_key_text))
        textSize = 14f
        setPadding(32, 28, 32, 28)
        inputType = InputType.TYPE_CLASS_TEXT
        background = GradientDrawable().apply {
            setColor(ContextCompat.getColor(context, R.color.ime_key_letter_background))
            cornerRadius = 24f
            setStroke(2, ContextCompat.getColor(context, R.color.ime_card_outline))
        }
        showSoftInputOnFocus = false
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = 32
        }
    }
    container.addView(editText)

    // Synqvia Keyboard View
    val kbView = SynqviaKeyboardView(context).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }

    // Inflate sample clipboard panel inside preview
    val clipboardView = View.inflate(context, R.layout.ime_clipboard_view, kbView.layoutClipboardContainer)
    val rvClips = clipboardView.findViewById<RecyclerView>(R.id.rv_ime_clips)
    val layoutEmpty = clipboardView.findViewById<View>(R.id.layout_ime_empty)

    var previewClips = listOf(
        ClipEntity(
            id = "1",
            text = "https://github.com/premtechworks/synqvia",
            src = "pc",
            direction = "remote",
            ts = System.currentTimeMillis() - 120_000L,
            pinned = false,
            sensitive = false
        ),
        ClipEntity(
            id = "2",
            text = "sudo systemctl restart bluetooth",
            src = "phone",
            direction = "local",
            ts = System.currentTimeMillis() - 900_000L,
            pinned = false,
            sensitive = false
        ),
        ClipEntity(
            id = "3",
            text = "Meeting passcode: 849-210",
            src = "pc",
            direction = "remote",
            ts = System.currentTimeMillis() - 3600_000L,
            pinned = true,
            sensitive = false
        )
    )

    lateinit var adapter: ClipImeAdapter
    adapter = ClipImeAdapter(
        onItemClick = { clip ->
            val curText = editText.text?.toString() ?: ""
            val start = editText.selectionStart.coerceAtLeast(0)
            val end = editText.selectionEnd.coerceAtLeast(0)
            val newText = StringBuilder(curText).replace(start, end, clip.text).toString()
            editText.setText(newText)
            editText.setSelection((start + clip.text.length).coerceAtMost(newText.length))
        },
        onItemLongClick = { _, _ -> },
        onPinClick = { clip ->
            previewClips = previewClips.map {
                if (it.id == clip.id) it.copy(pinned = !it.pinned) else it
            }
            val displayed = if (kbView.isPinnedFilterActive) previewClips.filter { it.pinned } else previewClips
            adapter.submitList(displayed)
        },
        onDeleteClick = { clip ->
            previewClips = previewClips.filter { it.id != clip.id }
            val displayed = if (kbView.isPinnedFilterActive) previewClips.filter { it.pinned } else previewClips
            adapter.submitList(displayed)
            layoutEmpty.visibility = if (displayed.isEmpty()) View.VISIBLE else View.GONE
        }
    )

    rvClips.layoutManager = LinearLayoutManager(context)
    rvClips.adapter = adapter
    adapter.submitList(previewClips)

    kbView.actionListener = object : KeyboardActionListener {
        override fun onText(text: String) {
            val curText = editText.text?.toString() ?: ""
            val start = editText.selectionStart.coerceAtLeast(0)
            val end = editText.selectionEnd.coerceAtLeast(0)
            val newText = StringBuilder(curText).replace(start, end, text).toString()
            editText.setText(newText)
            editText.setSelection((start + text.length).coerceAtMost(newText.length))
        }

        override fun onBackspace() {
            handlePreviewBackspace(editText)
        }

        override fun onBackspaceRepeat() {
            handlePreviewBackspace(editText)
        }

        override fun onEnter() {
            val curText = editText.text?.toString() ?: ""
            val start = editText.selectionStart.coerceAtLeast(0)
            val end = editText.selectionEnd.coerceAtLeast(0)
            val newText = StringBuilder(curText).replace(start, end, "\n").toString()
            editText.setText(newText)
            editText.setSelection((start + 1).coerceAtMost(newText.length))
        }

        override fun onShiftChanged(state: ShiftState) {}

        override fun onLayerChanged(layer: KeyboardLayer) {}

        override fun onToggleClipboard() {
            val displayed = if (kbView.isPinnedFilterActive) previewClips.filter { it.pinned } else previewClips
            adapter.submitList(displayed)
            layoutEmpty.visibility = if (displayed.isEmpty()) View.VISIBLE else View.GONE
        }

        override fun onToolbar2() {
            val displayed = if (kbView.isPinnedFilterActive) previewClips.filter { it.pinned } else previewClips
            adapter.submitList(displayed)
            layoutEmpty.visibility = if (displayed.isEmpty()) View.VISIBLE else View.GONE
        }

        override fun onOpenKeyboardPicker() {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.showInputMethodPicker()
        }
    }

    container.addView(kbView)
    return container
}

private fun handlePreviewBackspace(editText: EditText) {
    val curText = editText.text?.toString() ?: return
    val start = editText.selectionStart
    val end = editText.selectionEnd
    if (start != end) {
        val s = minOf(start, end).coerceAtLeast(0)
        val e = maxOf(start, end).coerceAtMost(curText.length)
        val newText = StringBuilder(curText).delete(s, e).toString()
        editText.setText(newText)
        editText.setSelection(s)
    } else if (start > 0) {
        val before = curText.subSequence(0, start)
        val count = KeyboardState.calculateBackspaceDeleteCount(before)
        val newText = StringBuilder(curText).delete(start - count, start).toString()
        editText.setText(newText)
        editText.setSelection(start - count)
    }
}

@SynqviaLightDarkPreview
@Composable
private fun ImeSettingsContentPreview() {
    SynqviaTheme {
        ImeSettingsContent(
            isDefaultIme = true,
            isImeEnabled = true,
            onBack = {},
            onToggleDefault = {}
        )
    }
}
