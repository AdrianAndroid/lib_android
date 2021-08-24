package com.zj.core.view.custom

import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import java.util.*

/**
 * 版权：Zhujiang 个人版权
 *
 * @author zhujiang
 * 版本：1.5
 * 创建日期：2020-01-18
 * 描述：pwqgc
 */
class FragmentAdapter(private val mFragmentManager: FragmentManager?) : FragmentStatePagerAdapter(
    mFragmentManager!!, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
) {
    private val mFragment: MutableList<Fragment> = ArrayList()
    private var isUpdateFlag = false
    private var curFragment: Fragment? = null
    private lateinit var mTitles: Array<String>
    override fun getItemPosition(any: Any): Int {
        return POSITION_NONE
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        if (!isUpdateFlag) {
            return super.instantiateItem(container, position)
        }
        var f = super.instantiateItem(container, position) as Fragment
        val fragmentTag = f.tag
        if (f === getItem(position)) return f
        //如果是新建的fragment，f 就和getItem(position)是同一个fragment，否则进入下面
        val ft = mFragmentManager?.beginTransaction()
        ft?.apply {
            //移除旧的fragment
            remove(f)
            //换成新的fragment
            f = getItem(position)
            //添加新fragment时必须用前面获得的tag
            add(container.id, f, fragmentTag)
            attach(f)
            setReorderingAllowed(true)
            commitAllowingStateLoss()
        }
        return f
    }

    fun reset(fragments: List<Fragment>?) {
        mFragment.clear()
        mFragment.addAll(fragments!!)
    }

    override fun setPrimaryItem(container: ViewGroup, position: Int, any: Any) {
        super.setPrimaryItem(container, position, any)
        if (any is Fragment) {
            curFragment = any
        }
    }

    fun reset(titles: Array<String>) {
        mTitles = titles
    }

    override fun getItem(position: Int): Fragment {
        return mFragment[position]
    }

    override fun getCount(): Int {
        return mFragment.size
    }

    override fun getPageTitle(position: Int): CharSequence {
        return mTitles[position]
    }
}