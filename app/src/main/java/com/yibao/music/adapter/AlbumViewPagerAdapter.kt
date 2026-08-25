package com.yibao.music.adapter

import android.util.SparseArray
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.yibao.music.fragment.AlbumCategoryFragment
import com.yibao.music.viewmodel.AlbumViewModel

/**
 * 作者：Stran on 2017/3/23 03:31
 * 描述：${TODO}
 * 邮箱：strangermy@outlook.com
 *
 * @author Stran
 */
class AlbumViewPagerAdapter(
    fragment: Fragment, private val albumViewModel: AlbumViewModel
) :
    FragmentStateAdapter(fragment) {
    private val mFragments = SparseArray<AlbumCategoryFragment>()

    override fun createFragment(position: Int): Fragment {
        val categoryFragment = AlbumCategoryFragment.newInstance(position, albumViewModel)
        mFragments.put(position, categoryFragment)
        return categoryFragment
    }

    /**
     * 获取指定模式(列表/平铺)的 AlbumCategoryFragment，供切换动画使用
     */
    fun getAlbumCategoryFragment(position: Int): AlbumCategoryFragment? = mFragments.get(position)

    override fun getItemCount(): Int {
        return 2
    }
}
