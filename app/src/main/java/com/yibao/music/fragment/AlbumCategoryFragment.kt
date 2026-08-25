package com.yibao.music.fragment

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.yibao.music.R
import com.yibao.music.adapter.AlbumAdapter
import com.yibao.music.base.bindings.BaseBindingAdapter
import com.yibao.music.base.bindings.BaseMusicFragmentDev
import com.yibao.music.databinding.AlbumCategoryFragmentBinding
import com.yibao.music.model.AlbumInfo
import com.yibao.music.util.Constant
import com.yibao.music.util.MusicListUtil
import com.yibao.music.viewmodel.AlbumViewModel

/**
 * @author Luoshipeng
 * @ author: Luoshipeng
 * @ Name:   AlbumCategoryFragment
 * @ Email:  strangermy98@gmail.com
 * @ Time:   2018/9/11/ 23:47
 * @ Des:    TODO
 */
class AlbumCategoryFragment : BaseMusicFragmentDev<AlbumCategoryFragmentBinding>() {
    private var mPosition = 0
    private lateinit var mAlbumList: List<AlbumInfo>

    private lateinit var mAlbumAdapter: AlbumAdapter

    override fun initView() {
        val arguments = arguments
        if (arguments != null) {
            mPosition = arguments.getInt(Constant.POSITION)
        }
        val musicBeanList = mMusicBeanDao.queryBuilder().list()
        mAlbumList = MusicListUtil.getAlbumList(musicBeanList)

    }

    override fun initData() {
        mAlbumAdapter = AlbumAdapter(mActivity, mAlbumList, mPosition)

        mBinding.musicView.setAdapter(
            mContext,
            if (mPosition == 0) 3 else 4,
            mPosition == 0,
            mAlbumAdapter
        )

        mAlbumAdapter.setItemListener(object : BaseBindingAdapter.OnItemListener<AlbumInfo> {
            override fun showDetailsView(bean: AlbumInfo, position: Int) {
                // AlbumFragment 接收
                mViewModel.postAlbum(bean)
            }
        })
    }

    /**
     * 获取当前模式(列表/平铺)的 RecyclerView，供 AlbumFragment 切换动画使用
     */
    fun getModeRecyclerView(): RecyclerView? = mBinding.musicView.recyclerView

    /**
     * 隐藏当前模式页面的封面与名称，切换动画开始时调用，避免目标页面直接闪现
     */
    fun hideModeViews() {
        val recyclerView = mBinding.musicView.recyclerView ?: return
        for (i in 0 until recyclerView.childCount) {
            val child = recyclerView.getChildAt(i) ?: continue
            val views = if (mPosition == Constant.NUMBER_ONE) {
                listOf(
                    child.findViewById<View>(R.id.iv_album_tile_album),
                    child.findViewById<View>(R.id.tv_album_tile_name)
                )
            } else {
                listOf(
                    child.findViewById<View>(R.id.iv_item_album_list),
                    child.findViewById<View>(R.id.tv_album_list_song_name),
                    child.findViewById<View>(R.id.tv_album_list_song_artist),
                    child.findViewById<View>(R.id.tv_album_list_song_count)
                )
            }
            views.forEach { view ->
                view?.animate()?.cancel()
                view?.alpha = 0f
            }
        }
    }

    /**
     * 切换动画落地后，恢复指定条目的封面与专辑名称等文字
     */
    fun revealModeItem(
        position: Int,
        delay: Long = 0L,
        imageDuration: Long = 200L,
        textDuration: Long = 260L
    ) {
        val recyclerView = mBinding.musicView.recyclerView ?: return
        val holder = recyclerView.findViewHolderForAdapterPosition(position) ?: return
        val child = holder.itemView
        if (mPosition == Constant.NUMBER_ONE) {
            child.findViewById<View>(R.id.iv_album_tile_album)
                ?.animate()?.alpha(1f)?.setDuration(imageDuration)?.setStartDelay(delay)?.start()
            child.findViewById<View>(R.id.tv_album_tile_name)
                ?.animate()?.alpha(1f)?.setDuration(textDuration)?.setStartDelay(delay)?.start()
        } else {
            child.findViewById<View>(R.id.iv_item_album_list)
                ?.animate()?.alpha(1f)?.setDuration(imageDuration)?.setStartDelay(delay)?.start()
            child.findViewById<View>(R.id.tv_album_list_song_name)
                ?.animate()?.alpha(1f)?.setDuration(textDuration)?.setStartDelay(delay)?.start()
            child.findViewById<View>(R.id.tv_album_list_song_artist)
                ?.animate()?.alpha(1f)?.setDuration(textDuration)?.setStartDelay(delay)?.start()
            child.findViewById<View>(R.id.tv_album_list_song_count)
                ?.animate()?.alpha(1f)?.setDuration(textDuration)?.setStartDelay(delay)?.start()
        }
    }

    /**
     * 强制恢复当前模式页面所有可见条目的封面与文字，用作动画结束后的兜底，
     * 避免条目回收复用导致个别行残留隐藏状态
     */
    fun restoreAllModeViews() {
        val recyclerView = mBinding.musicView.recyclerView ?: return
        for (i in 0 until recyclerView.childCount) {
            val child = recyclerView.getChildAt(i) ?: continue
            val views = if (mPosition == Constant.NUMBER_ONE) {
                listOf(
                    child.findViewById<View>(R.id.iv_album_tile_album),
                    child.findViewById<View>(R.id.tv_album_tile_name)
                )
            } else {
                listOf(
                    child.findViewById<View>(R.id.iv_item_album_list),
                    child.findViewById<View>(R.id.tv_album_list_song_name),
                    child.findViewById<View>(R.id.tv_album_list_song_artist),
                    child.findViewById<View>(R.id.tv_album_list_song_count)
                )
            }
            views.forEach { view ->
                view?.animate()?.cancel()
                view?.alpha = 1f
            }
        }
    }


    companion object {
        lateinit var mViewModel: AlbumViewModel

        @JvmStatic
        fun newInstance(
            position: Int, albumViewModel: AlbumViewModel
        ): AlbumCategoryFragment {
            mViewModel = albumViewModel
            val args = Bundle()
            val fragment = AlbumCategoryFragment()
            args.putInt(Constant.POSITION, position)
            fragment.arguments = args
            return fragment
        }
    }

}
