package com.zj.core.util

import androidx.annotation.StringRes
import com.blankj.utilcode.util.ToastUtils

/**
 * 版权：Zhujiang 个人版权
 * @author zhujiang
 * 版本：1.5
 * 创建日期：2020/5/15
 * 描述：PlayAndroid
 *
 */

fun showToast(msg: String) {
    ToastUtils.showShort(msg)
}

fun showToast(@StringRes msg: Int) {
    ToastUtils.showShort(msg)
}

fun showLongToast(msg: String) {
    ToastUtils.showLong(msg)
}

fun showLongToast(@StringRes msg: Int) {
    ToastUtils.showLong(msg)
}