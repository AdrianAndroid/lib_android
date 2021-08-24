/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("NOTHING_TO_INLINE")

package androidx.core.text

import android.annotation.SuppressLint
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned.SPAN_INCLUSIVE_EXCLUSIVE

/**
 * Returns a new [Spannable] from [CharSequence],
 * or the source itself if it is already an instance of [SpannableString].
 */
public inline fun CharSequence.toSpannable(): Spannable = SpannableString.valueOf(this)

/** Clear all spans from this text. */
@SuppressLint("SyntheticAccessor") // TODO remove https://issuetracker.google.com/issues/110243369
public inline fun Spannable.clearSpans(): Unit = getSpans<Any>().forEach { removeSpan(it) }

/**
 * Add [span] to the range [start]&hellip;[end] of the text.
 *
 * ```
 * val s = "Hello, World!".toSpannable()
 * s[0, 5] = UnderlineSpan()
 * ```
 *
 * Note: The [end] value is exclusive.
 *
 * @see Spannable.setSpan
 */
public inline operator fun Spannable.set(start: Int, end: Int, span: Any) {
    setSpan(span, start, end, SPAN_INCLUSIVE_EXCLUSIVE)
}

/**
 * Add [span] to the [range] of the text.
 *
 * ```
 * val s = "Hello, World!".toSpannable()
 * s[0..5] = UnderlineSpan()
 * ```
 *
 * Note: The range end value is exclusive.
 *
 * @see Spannable.setSpan
 */
public inline operator fun Spannable.set(range: IntRange, span: Any) {
    // This looks weird, but endInclusive is just the exact upper value.
    setSpan(span, range.start, range.endInclusive, SPAN_INCLUSIVE_EXCLUSIVE)
}
