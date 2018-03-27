package com.locationpicker

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.View.OnTouchListener
import android.widget.AdapterView
import com.google.android.gms.appindexing.AppIndex
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.ResultCallback
import com.google.android.gms.location.places.Place
import com.google.android.gms.location.places.PlaceBuffer
import com.google.android.gms.location.places.Places

class AutoCompleteLocation(context: Context, attrs: AttributeSet) : android.support.v7.widget.AppCompatAutoCompleteTextView(context, attrs) {

    private lateinit var mCloseIcon: Drawable
    private lateinit var mSearchIcon: Drawable
    private lateinit var mGoogleApiClient: GoogleApiClient
    private var mAutoCompleteAdapter: AutoCompleteAdapter? = null
    private var mAutoCompleteLocationListener: AutoCompleteLocationListener? = null
    private val mAutoCompleteTextWatcher = object : TextWatcher {
        override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}

        override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
            this@AutoCompleteLocation.setCompoundDrawablesWithIntrinsicBounds(mSearchIcon, null,
                    if (this@AutoCompleteLocation.text.toString() == "") null else mCloseIcon, null)
            if (mAutoCompleteLocationListener != null) {
                mAutoCompleteLocationListener!!.onTextClear()
            }
        }

        override fun afterTextChanged(editable: Editable) {}
    }
    private val mOnTouchListener = OnTouchListener { view, motionEvent ->
        if (motionEvent.x > (this@AutoCompleteLocation.width
                        - this@AutoCompleteLocation.paddingRight
                        - mSearchIcon.intrinsicWidth
                        - mCloseIcon.intrinsicWidth)) {
            this@AutoCompleteLocation.setText("")
            this@AutoCompleteLocation.setCompoundDrawables(mSearchIcon, null, null, null)
        }
        false
    }
    private val mUpdatePlaceDetailsCallback = ResultCallback<PlaceBuffer> { places ->
        if (!places.status.isSuccess) {
            places.release()
            return@ResultCallback
        }
        val place = places.get(0)
        if (mAutoCompleteLocationListener != null) {
            mAutoCompleteLocationListener!!.onItemSelected(place)
        }
        places.release()
    }
    private val mAutocompleteClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
        UIUtils.hideKeyboard(this@AutoCompleteLocation.context, this@AutoCompleteLocation)
        val item = mAutoCompleteAdapter!!.getItem(position)
        if (item != null) {
            val placeId = item.placeId
            val placeResult = Places.GeoDataApi.getPlaceById(mGoogleApiClient, placeId)
            placeResult.setResultCallback(mUpdatePlaceDetailsCallback)
        }
    }

    init {
        val resources = context.resources
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.AutoCompleteLocation, 0, 0)
        var background = typedArray.getDrawable(R.styleable.AutoCompleteLocation_background_layout)
        if (background == null) {
            background = resources.getDrawable(R.drawable.rounded_corder_background)
        }
        var hintText = typedArray.getString(R.styleable.AutoCompleteLocation_hint_text)
        if (hintText == null) {
            hintText = resources.getString(R.string.search)
        }
        val hintTextColor = typedArray.getColor(R.styleable.AutoCompleteLocation_hint_text_color,
                resources.getColor(android.R.color.darker_gray))
        val textColor = typedArray.getColor(R.styleable.AutoCompleteLocation_text_color,
                resources.getColor(android.R.color.black))
        val padding = resources.getDimensionPixelSize(R.dimen.margin_large_12dp)
        typedArray.recycle()

        setBackground(background)
        hint = hintText
        setHintTextColor(hintTextColor)
        setTextColor(textColor)
        textSize = 18f
        setPadding(padding, padding, padding, padding);
        maxLines = 1
        setSingleLine(true)

        mCloseIcon = context.resources.getDrawable(R.drawable.ic_close)
        mSearchIcon = context.resources.getDrawable(R.drawable.places_ic_search)
        mGoogleApiClient = GoogleApiClient.Builder(context).addApi(Places.GEO_DATA_API)
                .addApi(AppIndex.API)
                .build()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        mGoogleApiClient.connect()
        this.addTextChangedListener(mAutoCompleteTextWatcher)
        this.setOnTouchListener(mOnTouchListener)
        this.onItemClickListener = mAutocompleteClickListener
        this.setAdapter<AutoCompleteAdapter>(mAutoCompleteAdapter)
        mAutoCompleteAdapter = AutoCompleteAdapter(context, mGoogleApiClient, null, null!!)
        this.setAdapter<AutoCompleteAdapter>(mAutoCompleteAdapter)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (mGoogleApiClient.isConnected) {
            mGoogleApiClient.disconnect()
        }
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        if (!enabled) {
            this.setCompoundDrawablesWithIntrinsicBounds(mSearchIcon, null, null, null)
        } else {
            this.setCompoundDrawablesWithIntrinsicBounds(mSearchIcon, null,
                    if (this@AutoCompleteLocation.text.toString() == "") null else mCloseIcon, null)
        }
    }

    fun setAutoCompleteTextListener(
            autoCompleteLocationListener: AutoCompleteLocationListener) {
        mAutoCompleteLocationListener = autoCompleteLocationListener
    }

    interface AutoCompleteLocationListener {
        fun onTextClear()

        fun onItemSelected(selectedPlace: Place)
    }
}
