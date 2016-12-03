package br.com.edsilfer.android.chipinterface.presenter

import android.content.Context
import android.support.v7.widget.CardView
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import br.com.edsilfer.android.chipinterface.R
import br.com.edsilfer.android.chipinterface.model.Chip
import br.com.edsilfer.android.chipinterface.model.ChipEvents
import br.com.edsilfer.android.chipinterface.model.ChipPalette
import br.com.edsilfer.android.chipinterface.model.intf.ChipControl
import br.com.edsilfer.kotlin_support.extensions.isBetween
import br.com.edsilfer.kotlin_support.extensions.log
import br.com.edsilfer.kotlin_support.extensions.notifySubscribers
import br.com.edsilfer.kotlin_support.service.keyboard.EnhancedTextWatcher
import com.mikhaellopez.circularimageview.CircularImageView
import com.squareup.picasso.Picasso


/**
 * Created by User on 24/11/2016.
 */

class ChipEditText : EditText, ChipControl {

    companion object {
        private var mCallback: CustomCallback? = null
        var mChips = mutableSetOf<Chip>()
        var isAddingChip = false
    }

    private var mPalette: ChipPalette? = null

    // CONSTUCTORs =================================================================================
    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        init()
    }

    fun init() {
        addTextChangedListener(object : EnhancedTextWatcher(this) {
            private var isRemoving = false

            override fun onTextChanged(cursor: Int, isBackspace: Boolean, deletedChar: Char) {
                if (isBackspace && deletedChar != ' ') {
                    isRemoving = true
                    val chip = getErasedChip(cursor)
                    if (chip != null) {
                        mChips.remove(chip)
                        notifySubscribers(ChipEvents.CHIP_REMOVED, chip)
                        updateChipsRange()
                    }
                    isRemoving = false
                }
            }

            private fun getErasedChip(cursor: Int): Chip? {
                mChips
                        .filter { it.range.isBetween(cursor) }
                        .forEach { return it }
                return null
            }
        })
    }

    override fun isSuggestionsEnabled(): Boolean {
        return false
    }

    // PUBLIC INTERFACE ============================================================================
    override fun addChip(chip: Chip, replaceable: String) {
        mPalette ?: throw IllegalArgumentException(context.getString(R.string.str_chip_interface_no_preset_error))
        if (!mChips.contains(chip)) {
            if (!ChipEditText.isAddingChip) {
                assemblyChipLayout(chip, replaceable)
            }
        } else {
            Log.e(ChipEditText::class.simpleName, "No duplicated chip is allowed")
        }
    }

    private fun assemblyChipLayout(chip: Chip, replaceable: String) {
        isAddingChip = true
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.rsc_chip, null)
        val thumbnail = view.findViewById(R.id.thumbnail) as CircularImageView
        (view.findViewById(R.id.container) as RelativeLayout).addView(getCardView(chip))
        mCallback = CustomCallback(view, chip, this, replaceable, mPalette!!)
        Picasso.with(context).load(chip.getThumbnail()).into(thumbnail, mCallback)
    }

    private fun getCardView(chip: Chip): CardView {
        val wrapper = CardView(context)
        val params = RelativeLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE)
        wrapper.layoutParams = params
        wrapper.addView(getCollapsedHeader(chip.getHeader()))
        wrapper.setCardBackgroundColor(context.resources.getColor(mPalette!!.collapsedBackground))
        wrapper.radius = 50f
        return wrapper
    }

    private fun getCollapsedHeader(content: String): LinearLayout {
        val wrapper = LinearLayout(context)
        wrapper.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        wrapper.gravity = Gravity.CENTER_VERTICAL

        val collapsedHeader = TextView(ContextThemeWrapper(context, mPalette!!.collapsedHeader), null, 0)
        val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)

        params.setMargins(
                context.resources.getDimension(R.dimen.dim_chip_item_label_left_margin).toInt(),
                context.resources.getDimension(R.dimen.dim_chip_item_label_top_bottom_margin).toInt(),
                context.resources.getDimension(R.dimen.dim_chip_item_label_right_margin).toInt(),
                context.resources.getDimension(R.dimen.dim_chip_item_label_top_bottom_margin).toInt()
        )

        collapsedHeader.layoutParams = params
        collapsedHeader.text = content

        wrapper.addView(collapsedHeader)
        return wrapper
    }

    override fun removeChip(chip: Chip) {
        for (c in mChips) {
            if (c == chip) {
                text = text.replace(c.range.first, c.range.second, "")
                mChips.remove(c)
                notifySubscribers(ChipEvents.CHIP_REMOVED, chip)
                updateChipsRange()
                break
            }
        }
    }

    private fun updateChipsRange() {
        var diff1 = -1
        for (value in mChips) {
            if (value.range.second <= text.length) {
                diff1 = value.range.second - value.range.first
            } else if (diff1 != -1) {
                value.range = Pair(value.range.first - diff1, value.range.second - diff1)
            }
        }
    }

    override fun setChipStyle(style: ChipPalette) {
        mPalette = style
    }

    override fun getTextWithNoSpans(): String {
        var max = 0
        mChips
                .asSequence()
                .filter { max < it.range.second }
                .forEach { max = it.range.second }
        return if (max < text.length) text.substring(max, text.length).trim() else ""
    }
}

