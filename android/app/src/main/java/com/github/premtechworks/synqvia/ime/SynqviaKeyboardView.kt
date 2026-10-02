package com.github.premtechworks.synqvia.ime

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.github.premtechworks.synqvia.R

/**
 * Custom View implementing full QWERTY, Symbols 1 and Symbols 2 layers,
 * Shift / Caps Lock management, Backspace hold-to-repeat, Key preview popup bubbles,
 * Enter action rendering, and a 44dp toolbar with Clipboard history panel toggling.
 */
class SynqviaKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val interTypeface by lazy {
        try {
            ResourcesCompat.getFont(context, R.font.inter_variable)
        } catch (_: Exception) {
            null
        }
    }

    private fun applyInterFont(
        textView: TextView,
        sizeSp: Float,
        weight: Int = 400,
        opsz: Float = sizeSp.coerceIn(14f, 32f)
    ) {
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
        interTypeface?.let { tf ->
            textView.typeface = tf
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                textView.fontVariationSettings = "'wght' $weight, 'opsz' ${opsz.toInt()}"
            } catch (_: Exception) {}
        }
    }

    var actionListener: KeyboardActionListener? = null

    val stateMachine = KeyboardState()

    var isClipboardActive: Boolean = false
        private set

    var isPinnedFilterActive: Boolean = false
        private set

    var isPasswordMode: Boolean = false

    var enterAction: EnterAction = EnterAction.NEWLINE
        set(value) {
            field = value
            updateEnterKeyIcon()
        }

    // View references
    val layoutClipboardContainer: FrameLayout
    private val layoutKeysContainer: LinearLayout
    private val btnClipboard: ImageButton
    private val btnPinned: ImageButton
    private val btnPicker: ImageButton

    // Enter and Shift view refs
    private var ivEnterKey: ImageView? = null
    private var ivShiftKey: ImageView? = null
    private val letterKeyViews = mutableListOf<TextView>()

    // Preview popup
    private val previewPopup: PopupWindow
    private val tvPreviewChar: TextView

    // Backspace repeating handler
    private val repeatHandler = Handler(Looper.getMainLooper())
    private var backspaceStartTime = 0L
    private val backspaceRepeatRunnable = object : Runnable {
        override fun run() {
            actionListener?.onBackspaceRepeat()
            performHaptic()
            val elapsed = System.currentTimeMillis() - backspaceStartTime
            val interval = if (elapsed > 1500L) 25L else 50L
            repeatHandler.postDelayed(this, interval)
        }
    }

    init {
        orientation = VERTICAL
        LayoutInflater.from(context).inflate(R.layout.ime_keyboard_view, this, true)

        layoutKeysContainer = findViewById(R.id.layout_keys_container)
        layoutClipboardContainer = findViewById(R.id.layout_clipboard_container)
        btnClipboard = findViewById(R.id.btn_ime_clipboard)
        btnPinned = findViewById(R.id.btn_ime_pinned)
        btnPicker = findViewById(R.id.btn_ime_picker)

        findViewById<TextView?>(R.id.tv_ime_brand)?.let {
            applyInterFont(it, 15f, weight = 600, opsz = 15f)
        }

        // Setup Key Preview Popup
        val previewView = LayoutInflater.from(context).inflate(R.layout.ime_key_preview, null)
        tvPreviewChar = previewView.findViewById(R.id.tv_preview_char)
        applyInterFont(tvPreviewChar, 22f, weight = 400, opsz = 22f)
        val previewW = resources.getDimensionPixelSize(R.dimen.ime_preview_bubble_width)
        val previewH = resources.getDimensionPixelSize(R.dimen.ime_preview_bubble_height)
        previewPopup = PopupWindow(previewView, previewW, previewH).apply {
            isTouchable = false
            isOutsideTouchable = false
        }

        updateContentHeights()
        setupToolbar()
        renderCurrentLayer()
    }

    private fun setupToolbar() {
        btnClipboard.setOnClickListener {
            performHaptic()
            setClipboardPanelActive(!isClipboardActive)
            actionListener?.onToggleClipboard()
        }

        btnPinned.setOnClickListener {
            performHaptic()
            if (!isClipboardActive) {
                isPinnedFilterActive = true
                setClipboardPanelActive(true)
            } else {
                isPinnedFilterActive = !isPinnedFilterActive
                updateToolbarButtons()
            }
            actionListener?.onToolbar2()
        }

        btnPicker.setOnClickListener {
            performHaptic()
            actionListener?.onOpenKeyboardPicker()
        }

        updateToolbarButtons()
    }

    fun setClipboardPanelActive(active: Boolean) {
        isClipboardActive = active
        if (!active) {
            isPinnedFilterActive = false
        }
        val targetIn = if (active) layoutClipboardContainer else layoutKeysContainer
        val targetOut = if (active) layoutKeysContainer else layoutClipboardContainer

        if (!isAttachedToWindow || !android.animation.ValueAnimator.areAnimatorsEnabled()) {
            targetIn.alpha = 1f
            targetIn.visibility = View.VISIBLE
            targetOut.alpha = 0f
            targetOut.visibility = View.GONE
            updateToolbarButtons()
            return
        }

        targetOut.animate().cancel()
        targetIn.animate().cancel()

        targetIn.alpha = 0f
        targetIn.visibility = View.VISIBLE
        targetIn.animate().alpha(1f).setDuration(150L).start()

        targetOut.animate().alpha(0f).setDuration(150L).withEndAction {
            targetOut.visibility = View.GONE
        }.start()

        updateToolbarButtons()
    }

    private fun toggleClipboardPanel(pinnedOnly: Boolean) {
        if (isClipboardActive && isPinnedFilterActive == pinnedOnly) {
            setClipboardPanelActive(false)
        } else {
            isPinnedFilterActive = pinnedOnly
            setClipboardPanelActive(true)
        }
    }

    private fun updateToolbarButtons() {
        if (isClipboardActive) {
            btnClipboard.setBackgroundResource(R.drawable.bg_ime_toolbar_button_active)
            btnClipboard.setColorFilter(ContextCompat.getColor(context, R.color.ime_toolbar_btn_active_icon))
        } else {
            btnClipboard.setBackgroundResource(R.drawable.bg_ime_toolbar_button)
            btnClipboard.setColorFilter(ContextCompat.getColor(context, R.color.ime_toolbar_btn_inactive_icon))
        }

        if (isClipboardActive && isPinnedFilterActive) {
            btnPinned.setBackgroundResource(R.drawable.bg_ime_toolbar_button_active)
            btnPinned.setColorFilter(ContextCompat.getColor(context, R.color.ime_toolbar_btn_active_icon))
        } else {
            btnPinned.setBackgroundResource(R.drawable.bg_ime_toolbar_button)
            btnPinned.setColorFilter(ContextCompat.getColor(context, R.color.ime_toolbar_btn_inactive_icon))
        }

        btnPicker.setBackgroundResource(R.drawable.bg_ime_toolbar_button)
        btnPicker.setColorFilter(ContextCompat.getColor(context, R.color.ime_toolbar_btn_inactive_icon))
    }

    fun setLayer(layer: KeyboardLayer) {
        stateMachine.setLayer(layer)
        actionListener?.onLayerChanged(layer)
        renderCurrentLayer()
    }

    fun applyAutoCaps(capsMode: Int) {
        val newState = stateMachine.applyAutoCaps(capsMode)
        updateShiftUI(newState)
        actionListener?.onShiftChanged(newState)
    }

    private fun performHaptic() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            } else {
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            }
        } catch (_: Exception) {}
    }

    fun getKeyHeight(): Int {
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        return resources.getDimensionPixelSize(
            if (isLandscape) R.dimen.ime_key_height_landscape else R.dimen.ime_key_height
        )
    }

    fun computeContentHeight(): Int {
        val keyH = getKeyHeight()
        val rowGap = resources.getDimensionPixelSize(R.dimen.ime_key_gap_v)
        val density = resources.displayMetrics.density
        val topPadding = (8 * density).toInt()
        val bottomPadding = (8 * density).toInt()
        return (4 * keyH) + (3 * rowGap) + topPadding + bottomPadding
    }

    fun updateContentHeights() {
        val contentH = computeContentHeight()
        val contentLayout = findViewById<View>(R.id.layout_ime_content)
        contentLayout?.layoutParams?.height = contentH
        layoutClipboardContainer.layoutParams?.height = contentH
        layoutKeysContainer.layoutParams?.height = contentH
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        updateContentHeights()
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        updateContentHeights()
        renderCurrentLayer()
    }

    private fun renderCurrentLayer() {
        layoutKeysContainer.removeAllViews()
        letterKeyViews.clear()
        ivShiftKey = null
        ivEnterKey = null

        when (stateMachine.layer) {
            KeyboardLayer.LETTERS -> renderLettersLayer()
            KeyboardLayer.SYMBOLS_1 -> renderSymbols1Layer()
            KeyboardLayer.SYMBOLS_2 -> renderSymbols2Layer()
        }
    }

    private fun createRowLayout(isLastRow: Boolean = false): LinearLayout {
        val rowGap = resources.getDimensionPixelSize(R.dimen.ime_key_gap_v)
        return LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, getKeyHeight()).apply {
                bottomMargin = if (isLastRow) 0 else rowGap
            }
        }
    }

    // -------------------------------------------------------------
    // LETTERS LAYER
    // -------------------------------------------------------------
    private fun renderLettersLayer() {
        val hGap = resources.getDimensionPixelSize(R.dimen.ime_key_gap_h)
        val isUpper = stateMachine.shiftState != ShiftState.OFF

        // Row 1: q w e r t y u i o p (with 1..0 hints)
        val row1 = createRowLayout()
        val row1Chars = listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
        val row1Hints = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
        for (i in row1Chars.indices) {
            val keyView = createLetterKeyWithHint(
                char = if (isUpper) row1Chars[i].uppercase() else row1Chars[i],
                hint = row1Hints[i],
                weight = 1.0f,
                marginEnd = if (i < row1Chars.size - 1) hGap else 0
            )
            row1.addView(keyView)
        }
        layoutKeysContainer.addView(row1)

        // Row 2: a s d f g h j k l (0.5 inset)
        val row2 = createRowLayout()
        val row2Chars = listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
        row2.addView(View(context).apply { layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, 0.5f) })
        for (i in row2Chars.indices) {
            val keyView = createSimpleLetterKey(
                char = if (isUpper) row2Chars[i].uppercase() else row2Chars[i],
                weight = 1.0f,
                marginEnd = if (i < row2Chars.size - 1) hGap else 0
            )
            row2.addView(keyView)
        }
        row2.addView(View(context).apply { layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, 0.5f) })
        layoutKeysContainer.addView(row2)

        // Row 3: Shift (1.5x), z x c v b n m, Backspace (1.5x)
        val row3 = createRowLayout()
        val shiftKey = createShiftKey(weight = 1.5f, marginEnd = hGap)
        row3.addView(shiftKey)

        val row3Chars = listOf("z", "x", "c", "v", "b", "n", "m")
        for (i in row3Chars.indices) {
            val keyView = createSimpleLetterKey(
                char = if (isUpper) row3Chars[i].uppercase() else row3Chars[i],
                weight = 1.0f,
                marginEnd = hGap
            )
            row3.addView(keyView)
        }

        val backspaceKey = createBackspaceKey(weight = 1.5f)
        row3.addView(backspaceKey)
        layoutKeysContainer.addView(row3)

        // Row 4: ?123 (1.5x), comma (1.0x), Space (flex), period (1.0x), Enter (1.5x)
        val row4 = createRowLayout(isLastRow = true)
        val symKey = createFunctionTextKey("?123", weight = 1.5f, marginEnd = hGap) {
            setLayer(KeyboardLayer.SYMBOLS_1)
        }
        row4.addView(symKey)

        val commaKey = createSimpleLetterKey(",", weight = 1.0f, marginEnd = hGap)
        row4.addView(commaKey)

        val spaceKey = createSpaceKey(weight = 4.5f, marginEnd = hGap)
        row4.addView(spaceKey)

        val dotKey = createSimpleLetterKey(".", weight = 1.0f, marginEnd = hGap)
        row4.addView(dotKey)

        val enterKey = createEnterKey(weight = 1.5f)
        row4.addView(enterKey)
        layoutKeysContainer.addView(row4)

        updateShiftUI(stateMachine.shiftState)
    }

    // -------------------------------------------------------------
    // SYMBOLS 1 LAYER
    // -------------------------------------------------------------
    private fun renderSymbols1Layer() {
        val hGap = resources.getDimensionPixelSize(R.dimen.ime_key_gap_h)

        // Row 1: 1 2 3 4 5 6 7 8 9 0
        val row1 = createRowLayout()
        val r1 = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
        for (i in r1.indices) {
            row1.addView(createSimpleLetterKey(r1[i], 1.0f, if (i < r1.size - 1) hGap else 0))
        }
        layoutKeysContainer.addView(row1)

        // Row 2: @ # $ % & - + ( )
        val row2 = createRowLayout()
        val r2 = listOf("@", "#", "$", "%", "&", "-", "+", "(", ")")
        row2.addView(View(context).apply { layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, 0.5f) })
        for (i in r2.indices) {
            row2.addView(createSimpleLetterKey(r2[i], 1.0f, if (i < r2.size - 1) hGap else 0))
        }
        row2.addView(View(context).apply { layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, 0.5f) })
        layoutKeysContainer.addView(row2)

        // Row 3: =< (1.5x), * " ' : ; ! ? (1.0x), Backspace (1.5x)
        val row3 = createRowLayout()
        val sym2Key = createFunctionTextKey("=\\<", 1.5f, marginEnd = hGap) {
            setLayer(KeyboardLayer.SYMBOLS_2)
        }
        row3.addView(sym2Key)
        val r3 = listOf("*", "\"", "'", ":", ";", "!", "?")
        for (ch in r3) {
            row3.addView(createSimpleLetterKey(ch, 1.0f, marginEnd = hGap))
        }
        row3.addView(createBackspaceKey(1.5f))
        layoutKeysContainer.addView(row3)

        // Row 4: ABC (1.5x), comma, Space, period, Enter
        val row4 = createRowLayout(isLastRow = true)
        val abcKey = createFunctionTextKey("ABC", 1.5f, marginEnd = hGap) {
            setLayer(KeyboardLayer.LETTERS)
        }
        row4.addView(abcKey)
        row4.addView(createSimpleLetterKey(",", 1.0f, marginEnd = hGap))
        row4.addView(createSpaceKey(4.5f, marginEnd = hGap))
        row4.addView(createSimpleLetterKey(".", 1.0f, marginEnd = hGap))
        row4.addView(createEnterKey(1.5f))
        layoutKeysContainer.addView(row4)
    }

    // -------------------------------------------------------------
    // SYMBOLS 2 LAYER
    // -------------------------------------------------------------
    private fun renderSymbols2Layer() {
        val hGap = resources.getDimensionPixelSize(R.dimen.ime_key_gap_h)

        // Row 1: ~ ` | • √ π ÷ × ¶ ∆
        val row1 = createRowLayout()
        val r1 = listOf("~", "`", "|", "•", "√", "π", "÷", "×", "¶", "∆")
        for (i in r1.indices) {
            row1.addView(createSimpleLetterKey(r1[i], 1.0f, if (i < r1.size - 1) hGap else 0))
        }
        layoutKeysContainer.addView(row1)

        // Row 2: £ ¢ € ¥ ^ ° = { }
        val row2 = createRowLayout()
        val r2 = listOf("£", "¢", "€", "¥", "^", "°", "=", "{", "}")
        row2.addView(View(context).apply { layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, 0.5f) })
        for (i in r2.indices) {
            row2.addView(createSimpleLetterKey(r2[i], 1.0f, if (i < r2.size - 1) hGap else 0))
        }
        row2.addView(View(context).apply { layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, 0.5f) })
        layoutKeysContainer.addView(row2)

        // Row 3: ?123 (1.5x), \ / © ® ™ ✓ [ ] (1.0x), Backspace (1.5x)
        val row3 = createRowLayout()
        val sym1Key = createFunctionTextKey("?123", 1.5f, marginEnd = hGap) {
            setLayer(KeyboardLayer.SYMBOLS_1)
        }
        row3.addView(sym1Key)
        val r3 = listOf("\\", "/", "©", "®", "™", "✓", "[", "]")
        for (ch in r3) {
            row3.addView(createSimpleLetterKey(ch, 1.0f, marginEnd = hGap))
        }
        row3.addView(createBackspaceKey(1.5f))
        layoutKeysContainer.addView(row3)

        // Row 4: ABC (1.5x), <, Space, >, Enter
        val row4 = createRowLayout(isLastRow = true)
        val abcKey = createFunctionTextKey("ABC", 1.5f, marginEnd = hGap) {
            setLayer(KeyboardLayer.LETTERS)
        }
        row4.addView(abcKey)
        row4.addView(createSimpleLetterKey("<", 1.0f, marginEnd = hGap))
        row4.addView(createSpaceKey(4.5f, marginEnd = hGap))
        row4.addView(createSimpleLetterKey(">", 1.0f, marginEnd = hGap))
        row4.addView(createEnterKey(1.5f))
        layoutKeysContainer.addView(row4)
    }

    // -------------------------------------------------------------
    // KEY CREATION HELPERS
    // -------------------------------------------------------------
    private fun createSimpleLetterKey(char: String, weight: Float, marginEnd: Int): View {
        val tv = TextView(context).apply {
            layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, weight).apply {
                this.marginEnd = marginEnd
            }
            gravity = Gravity.CENTER
            text = char
            setTextColor(Color.WHITE)
            applyInterFont(this, 20f, weight = 400, opsz = 20f)
            setBackgroundResource(R.drawable.bg_ime_key_letter)
            contentDescription = char
            isClickable = true
            isFocusable = false
        }
        letterKeyViews.add(tv)
        attachKeyTouchListener(tv, char)
        return tv
    }

    private fun createLetterKeyWithHint(char: String, hint: String, weight: Float, marginEnd: Int): View {
        val frame = FrameLayout(context).apply {
            layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, weight).apply {
                this.marginEnd = marginEnd
            }
            setBackgroundResource(R.drawable.bg_ime_key_letter)
            contentDescription = "$char, long press for $hint"
            isClickable = true
            isFocusable = false
        }

        // Digit hint top-right
        val tvHint = TextView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP or Gravity.END
            ).apply {
                topMargin = 2
                rightMargin = 4
                setMarginEnd(4)
            }
            text = hint
            setTextColor(ContextCompat.getColor(context, R.color.ime_key_hint))
            applyInterFont(this, 9f, weight = 500, opsz = 14f)
        }
        frame.addView(tvHint)

        // Main char center
        val tvMain = TextView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            gravity = Gravity.CENTER
            text = char
            setTextColor(Color.WHITE)
            applyInterFont(this, 20f, weight = 400, opsz = 20f)
        }
        frame.addView(tvMain)
        letterKeyViews.add(tvMain)

        attachKeyTouchListener(frame, char, longPressText = hint)
        return frame
    }

    private fun createFunctionTextKey(
        text: String,
        weight: Float,
        marginEnd: Int,
        onClick: () -> Unit
    ): View {
        return TextView(context).apply {
            layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, weight).apply {
                this.marginEnd = marginEnd
            }
            gravity = Gravity.CENTER
            this.text = text
            setTextColor(Color.WHITE)
            applyInterFont(this, 15f, weight = 500, opsz = 15f)
            setBackgroundResource(R.drawable.bg_ime_key_function)
            contentDescription = text
            isClickable = true
            isFocusable = false
            setOnClickListener {
                performHaptic()
                onClick()
            }
        }
    }

    private fun createShiftKey(weight: Float, marginEnd: Int): View {
        val iv = ImageView(context).apply {
            layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, weight).apply {
                this.marginEnd = marginEnd
            }
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setImageResource(R.drawable.ic_ime_shift)
            setBackgroundResource(R.drawable.bg_ime_key_function)
            contentDescription = "Shift"
            isClickable = true
            isFocusable = false
            setOnClickListener {
                performHaptic()
                val newState = stateMachine.onShiftTap()
                updateShiftUI(newState)
                actionListener?.onShiftChanged(newState)
            }
        }
        ivShiftKey = iv
        return iv
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createBackspaceKey(weight: Float): View {
        val iv = ImageView(context).apply {
            layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, weight)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setImageResource(R.drawable.ic_ime_backspace)
            setBackgroundResource(R.drawable.bg_ime_key_function)
            contentDescription = "Backspace"
            isClickable = true
            isFocusable = false

            setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        v.isPressed = true
                        performHaptic()
                        actionListener?.onBackspace()
                        backspaceStartTime = System.currentTimeMillis()
                        repeatHandler.postDelayed(backspaceRepeatRunnable, 400L)
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        v.isPressed = false
                        repeatHandler.removeCallbacks(backspaceRepeatRunnable)
                        true
                    }
                    else -> false
                }
            }
        }
        return iv
    }

    private fun createSpaceKey(weight: Float, marginEnd: Int): View {
        return TextView(context).apply {
            layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, weight).apply {
                this.marginEnd = marginEnd
            }
            gravity = Gravity.CENTER
            text = "Synqvia"
            setTextColor(ContextCompat.getColor(context, R.color.ime_key_text_secondary))
            applyInterFont(this, 13f, weight = 400, opsz = 14f)
            setBackgroundResource(R.drawable.bg_ime_key_letter)
            contentDescription = "Space"
            isClickable = true
            isFocusable = false

            setOnClickListener {
                performHaptic()
                // Handled via actionListener
                val (strToInsert, deletePrev) = stateMachine.onSpaceTap(null)
                if (deletePrev) {
                    actionListener?.onBackspace()
                }
                actionListener?.onText(strToInsert)
            }

            setOnLongClickListener {
                performHaptic()
                actionListener?.onOpenKeyboardPicker()
                true
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createEnterKey(weight: Float): View {
        val iv = ImageView(context).apply {
            layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, weight)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setImageResource(enterAction.iconRes)
            setBackgroundResource(R.drawable.bg_ime_key_enter)
            contentDescription = "Enter"
            isClickable = true
            isFocusable = false

            setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        v.isPressed = true
                        performHaptic()
                        v.animate().scaleX(0.92f).scaleY(0.92f).setDuration(70L).start()
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        v.isPressed = false
                        v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120L).start()
                        actionListener?.onEnter()
                        true
                    }
                    MotionEvent.ACTION_CANCEL -> {
                        v.isPressed = false
                        v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120L).start()
                        true
                    }
                    else -> false
                }
            }
        }
        ivEnterKey = iv
        return iv
    }

    private fun updateEnterKeyIcon() {
        ivEnterKey?.setImageResource(enterAction.iconRes)
    }

    private fun updateShiftUI(state: ShiftState) {
        val isUpper = state != ShiftState.OFF
        for (tv in letterKeyViews) {
            val cur = tv.text.toString()
            if (cur.length == 1 && cur[0].isLetter()) {
                tv.text = if (isUpper) cur.uppercase() else cur.lowercase()
            }
        }

        when (state) {
            ShiftState.CAPS_LOCKED -> {
                ivShiftKey?.setImageResource(R.drawable.ic_ime_shift_locked)
            }
            ShiftState.ON -> {
                ivShiftKey?.setImageResource(R.drawable.ic_ime_shift)
                ivShiftKey?.setColorFilter(ContextCompat.getColor(context, R.color.ime_toolbar_btn_active_icon))
            }
            ShiftState.OFF -> {
                ivShiftKey?.setImageResource(R.drawable.ic_ime_shift)
                ivShiftKey?.clearColorFilter()
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun attachKeyTouchListener(
        view: View,
        initialChar: String,
        longPressText: String? = null
    ) {
        val longPressHandler = Handler(Looper.getMainLooper())
        var didLongPress = false

        val longPressRunnable = Runnable {
            if (longPressText != null) {
                didLongPress = true
                performHaptic()
                dismissKeyPreview()
                actionListener?.onText(longPressText)
            }
        }

        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.isPressed = true
                    didLongPress = false
                    performHaptic()

                    val charToType = if (v is TextView) {
                        v.text.toString()
                    } else {
                        (v.findViewById<TextView>(android.R.id.text1) ?: letterKeyViews.find { it.parent == v })?.text?.toString() ?: initialChar
                    }

                    if (!isPasswordMode) {
                        showKeyPreview(v, charToType)
                    }

                    if (longPressText != null) {
                        longPressHandler.postDelayed(longPressRunnable, 450L)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    v.isPressed = false
                    longPressHandler.removeCallbacks(longPressRunnable)
                    dismissKeyPreview()

                    if (!didLongPress) {
                        val charToType = if (v is TextView) {
                            v.text.toString()
                        } else {
                            letterKeyViews.find { it.parent == v }?.text?.toString() ?: initialChar
                        }
                        actionListener?.onText(charToType)

                        // Update shift if in single-tap mode
                        if (stateMachine.shiftState == ShiftState.ON) {
                            val newState = stateMachine.onCharacterTyped()
                            updateShiftUI(newState)
                            actionListener?.onShiftChanged(newState)
                        }
                    }
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    v.isPressed = false
                    longPressHandler.removeCallbacks(longPressRunnable)
                    dismissKeyPreview()
                    true
                }
                else -> false
            }
        }
    }

    private fun showKeyPreview(anchor: View, text: String) {
        if (!anchor.isAttachedToWindow) return
        tvPreviewChar.text = text
        val location = IntArray(2)
        anchor.getLocationInWindow(location)

        val popupW = resources.getDimensionPixelSize(R.dimen.ime_preview_bubble_width)
        val popupH = resources.getDimensionPixelSize(R.dimen.ime_preview_bubble_height)

        val x = location[0] + (anchor.width - popupW) / 2
        val y = location[1] - popupH - 8

        previewPopup.contentView?.let { cv ->
            cv.scaleX = 0.85f
            cv.scaleY = 0.85f
            cv.animate().scaleX(1.0f).scaleY(1.0f).setDuration(60L).start()
        }

        try {
            if (previewPopup.isShowing) {
                previewPopup.update(x, y, popupW, popupH)
            } else {
                previewPopup.showAtLocation(anchor, Gravity.NO_GRAVITY, x, y)
            }
        } catch (_: Exception) {}
    }

    private fun dismissKeyPreview() {
        try {
            if (previewPopup.isShowing) {
                previewPopup.dismiss()
            }
        } catch (_: Exception) {}
    }

    override fun onDetachedFromWindow() {
        repeatHandler.removeCallbacks(backspaceRepeatRunnable)
        dismissKeyPreview()
        super.onDetachedFromWindow()
    }
}
