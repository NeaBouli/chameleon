/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.core

import android.os.Parcel
import android.os.Parcelable

/**
 * Result from CryptoService.processText() via AIDL.
 * Returned to the AccessibilityService with instructions.
 */
data class ProcessTextResult(
    val shouldReplace: Boolean,
    val text: String?
) : Parcelable {

    constructor(parcel: Parcel) : this(
        shouldReplace = parcel.readByte() != 0.toByte(),
        text = parcel.readString()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeByte(if (shouldReplace) 1 else 0)
        parcel.writeString(text)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<ProcessTextResult> {
        override fun createFromParcel(parcel: Parcel): ProcessTextResult = ProcessTextResult(parcel)
        override fun newArray(size: Int): Array<ProcessTextResult?> = arrayOfNulls(size)
    }
}
