package com.klyx.extension.impl

import android.content.Context
import com.klyx.core.showShortToast
import com.klyx.extension.Android

class AndroidImpl(private val context: Context) : Android {
    override fun showToast(message: String) {
        context.showShortToast(message)
    }

    override val namespace: String
        get() = "Android"
}
