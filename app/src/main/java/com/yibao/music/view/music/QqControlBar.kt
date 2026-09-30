package com.yibao.music.view.music

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.viewpager.widget.ViewPager
import com.yibao.music.MusicApplication
import com.yibao.music.R
import com.yibao.music.adapter.QqBarPagerAdapter
import com.yibao.music.base.listener.MusicPagerListener
import com.yibao.music.databinding.MuiscTabbarQqBinding
import com.yibao.music.model.MusicBean
import com.yibao.music.util.Constant
import com.yibao.music.util.LogUtil
import com.yibao.music.util.QueryMusicFlagListUtil
import com.yibao.music.util.SpUtils
import com.yibao.music.view.MusicProgressView

/**
 * @author Luoshipeng
 * @ Name:   QqControlBar
 * @ Email:  strangermy98@gmail.com
 * @ Time:   2018/8/11/ 21:50
 * @ Des:    TODO
 */
class QqControlBar(context: Context?, attrs: AttributeSet? = null) :
    LinearLayout(context, attrs), View.OnClickListener {

    private var mPagerAdapter: QqBarPagerAdapter? = null


    private val mBinding = MuiscTabbarQqBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        initData()
        initListener()
    }


    private fun initData() {
        mPagerAdapter = QqBarPagerAdapter(context, null)
        mBinding.qqMusicVp.adapter = mPagerAdapter
    }

    private fun initListener() {
        mBinding.musicFloatingPagerFavorite.setOnClickListener(this)
        mBinding.btnQqPlay.setOnClickListener(this)
        mBinding.qqMusicVp.addOnPageChangeListener(object : MusicPagerListener() {
            override fun onPageSelected(position: Int) {
                mSelectListener?.selectPosition(position)
            }
        })
    }

    //**************按钮状态********************
    fun setPlayButtonState(resourceId: Int) {
        mBinding.btnQqPlay.setIcon(resourceId)
    }

    fun updatePlayButtonState(isPlaying: Boolean) {
        mBinding.btnQqPlay.setIcon(if (isPlaying) R.drawable.btn_playing_pause_selector else R.drawable.btn_playing_play_selector)
    }

    fun setFavoriteButtonState(isFavorite: Boolean) {
        mBinding.musicFloatingPagerFavorite.setImageResource(if (isFavorite) R.drawable.btn_favorite_red_selector else R.drawable.btn_favorite_gray_selector)
    }

    // **************ViewPager数据********************
    /**
     * 设置/更新 ViewPager数据
     */
    fun setPagerData() {
        val sp = SpUtils(MusicApplication.getInstance(), Constant.MUSIC_CONFIG)
        val position = sp.getInt(Constant.MUSIC_POSITION)
        mPagerAdapter = QqBarPagerAdapter(context, currentList())
        mBinding.qqMusicVp.adapter = mPagerAdapter
        mBinding.qqMusicVp.setCurrentItem(position, false)
    }


    fun updatePosition() {
        val sp = SpUtils(MusicApplication.getInstance(), Constant.MUSIC_CONFIG)
        val position = sp.getInt(Constant.MUSIC_POSITION)
        mBinding.qqMusicVp.setCurrentItem(position, false)
    }

    /**
     * 只更新指定歌曲页面的歌词，不重建 Pager
     *
     * @param musicInfo 正在播放的歌曲
     * @param lyrics    当前歌词
     */
    fun updateLyrics(musicInfo: MusicBean, lyrics: String) {
        mPagerAdapter?.updateLyrics(musicInfo, lyrics)
    }

    private fun currentList(): List<MusicBean> {
        val sp = SpUtils(MusicApplication.getInstance(), Constant.MUSIC_CONFIG)
        val pageType = sp.getInt(Constant.PAGE_TYPE)
        val condition = sp.getString(Constant.CONDITION)
        val mMusicDao = MusicApplication.getInstance().musicDao

        //        LogUtil.d("lsp", " QQBar 数据源 pageType  ==   " + pageType + "  condition  =  " + condition );

        return QueryMusicFlagListUtil.getMusicDataList(
            mMusicDao.queryBuilder(),
            pageType,
            condition
        )
    }


    //**************歌曲进度********************
    fun setMaxProgress(maxProgress: Int) {
        mBinding.btnQqPlay.setMax(maxProgress)
    }

    fun setProgress(currentProgress: Int) {
        mBinding.btnQqPlay.setProgress(currentProgress)
    }


    override fun onClick(view: View) {
        val id = view.id
        if (id == R.id.btn_qq_play) {
            controlBarClick(Constant.NUMBER_ONE)
        } else if (id == R.id.music_floating_pager_favorite) {
            controlBarClick(Constant.NUMBER_TWO)
        }
    }


    private fun controlBarClick(clickFlag: Int) {
        if (mButtonClickListener != null) {
            mButtonClickListener!!.click(clickFlag)
        }
    }

    //**************按钮点击监听********************
    private var mButtonClickListener: OnButtonClickListener? = null

    fun setOnButtonClickListener(buttonClickListener: OnButtonClickListener) {
        mButtonClickListener = buttonClickListener
    }

    interface OnButtonClickListener {
        fun click(clickFlag: Int)
    }

    private var mSelectListener: OnPagerSelectListener? = null

    fun setOnPagerSelectListener(selectListener: OnPagerSelectListener) {
        mSelectListener = selectListener
    }

    interface OnPagerSelectListener {
        /**
         * p
         *
         * @param currentPosition d
         */
        fun selectPosition(pagerPosition: Int)
    }
}
