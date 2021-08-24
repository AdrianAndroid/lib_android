package com.zj.core.view.base

import android.annotation.SuppressLint
import android.app.Activity
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.annotation.CallSuper
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import com.blankj.utilcode.util.BarUtils
import com.blankj.utilcode.util.ConvertUtils
import com.zj.core.R
import com.zj.core.util.showToast
import com.zj.core.view.base.lce.DefaultLceImpl
import com.zj.core.view.base.lce.ILce
import java.lang.ref.WeakReference


/**
 * 应用程序中所有Activity的基类。
 *
 */
@SuppressLint("Registered")
abstract class BaseActivity : AppCompatActivity(), ILce, BaseActivityInit {

    /**
     * Activity中显示加载等待的控件。
     */
    private var loading: ProgressBar? = null

    /**
     * Activity中由于服务器异常导致加载失败显示的布局。
     */
    private var loadErrorView: View? = null

    /**
     * Activity中由于网络异常导致加载失败显示的布局。
     */
    private var badNetworkView: View? = null

    /**
     * Activity中当界面上没有任何内容时展示的布局。
     */
    private var noContentView: View? = null

    private var defaultLce: ILce? = null

    private var weakRefActivity: WeakReference<Activity>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BarUtils.transparentStatusBar(this)
        setContentView(getLayoutView())
        ActivityCollector.add(WeakReference(this))
        weakRefActivity = WeakReference(this)
        initView()
        initData()
    }

    override fun initData() {}

    override fun setContentView(view: View?) {
        super.setContentView(view)
        setupViews()
    }

    protected open fun setupViews() {
        val view = View.inflate(this, R.layout.layout_lce, null)
        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        params.setMargins(
            0,
            ConvertUtils.dp2px(if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT || isSearchPage()) 70f else 55f),
            0,
            0
        )
        addContentView(view, params)
        loading = view.findViewById(R.id.loading)
        noContentView = view.findViewById(R.id.noContentView)
        badNetworkView = view.findViewById(R.id.badNetworkView)
        loadErrorView = view.findViewById(R.id.loadErrorView)
        if (loading == null) {
            Log.e(TAG, "loading is null")
        }
        if (badNetworkView == null) {
            Log.e(TAG, "badNetworkView is null")
        }
        if (loadErrorView == null) {
            Log.e(TAG, "loadErrorView is null")
        }
        defaultLce = DefaultLceImpl(
            loading,
            loadErrorView,
            badNetworkView,
            noContentView
        )
        loadFinished()
    }

    protected open fun isSearchPage(): Boolean {
        return false
    }

    /**
     * 设置 LiveData 的状态，根据不同状态显示不同页面
     *
     * @param dataLiveData LiveData
     * @param onDataStatus 数据回调进行使用
     */
    fun <T> setDataStatus(dataLiveData: LiveData<Result<T>>, onDataStatus: (T) -> Unit) {
        dataLiveData.observe(this) {
            if (it.isSuccess) {
                val dataList = it.getOrNull()
                if (dataList != null) {
                    loadFinished()
                    onDataStatus(dataList)
                } else {
                    showLoadErrorView()
                }
            } else {
                showToast(getString(R.string.bad_network_view_tip))
                showBadNetworkView { initData() }
            }
        }
    }

    @CallSuper
    override fun startLoading() {
        defaultLce?.startLoading()
    }

    @CallSuper
    override fun loadFinished() {
        defaultLce?.loadFinished()
    }

    override fun showLoadErrorView(tip: String) {
        defaultLce?.showLoadErrorView(tip)
    }

    override fun showBadNetworkView(listener: View.OnClickListener) {
        defaultLce?.showBadNetworkView(listener)
    }

    override fun showNoContentView(tip: String) {
        defaultLce?.showNoContentView(tip)
    }

    override fun onDestroy() {
        super.onDestroy()
        ActivityCollector.remove(weakRefActivity)
    }

    companion object {
        private const val TAG = "BaseActivity"
    }

}
