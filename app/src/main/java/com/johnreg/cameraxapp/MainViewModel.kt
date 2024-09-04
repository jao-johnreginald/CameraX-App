package com.johnreg.cameraxapp

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel : ViewModel() {

    private val _bitmaps = MutableStateFlow<List<Bitmap>>(emptyList())
    val bitmaps = _bitmaps.asStateFlow()

    // If we take a photo, we get a new bitmap, which we want to add to this StateFlow
    fun onTakePhoto(bitmap: Bitmap) {
        _bitmaps.value += bitmap
    }

}