package com.yibao.music.activity

import android.content.Intent
import android.graphics.drawable.AnimationDrawable
import android.view.View
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.yibao.music.R
import com.yibao.music.adapter.SearchLyricsPagerAdapter
import com.yibao.music.base.bindings.BaseBindingActivity
import com.yibao.music.databinding.ActivitySearchLyricsBinding
import com.yibao.music.model.qq.SearchLyricsBean
import com.yibao.music.util.Constant
import com.yibao.music.util.LogUtil
import com.yibao.music.viewmodel.SearchViewModel

/**
 * @author lsp
 * createDate：2019/12/26 0026 14:54
 * className   SearchLyricsActivity
 * Des：TODO
 */
class SearchLyricsActivity : BaseBindingActivity<ActivitySearchLyricsBinding>() {


    private val searchViewModel = SearchViewModel()

    private val mLyricsBeanList: MutableList<SearchLyricsBean> = ArrayList()
    private var mSongMid: String? = null

    override fun initView() {
    }

    override fun initData() {
        val songName = intent.getStringExtra(Constant.SONG_NAME)
        val singer = intent.getStringExtra(Constant.SONG_ARTIST)
        LogUtil.d(TAG, "$songName == $singer")
        if (songName != null && singer != null) {
            mBinding.editSearchLyricsName.setText(songName)
            mBinding.editSearchLyricsArtist.setText(singer)
            searchViewModel.searchLyrics(songName, singer, false)
        }
    }


    override fun initListener() {
        mBinding.ivSearchDown.setOnClickListener { v: View? -> finish() }
        mBinding.ivSearchLyrics.setOnClickListener { v: View? ->
            showProgress()
            val isNeed =
                mBinding.editSearchLyricsName.text.toString().trim { it <= ' ' }.isEmpty()
            val songName = mBinding.editSearchLyricsName.text.toString().trim { it <= ' ' }
            val singer = mBinding.editSearchLyricsArtist.text.toString().trim { it <= ' ' }
            searchViewModel.searchLyrics(songName, singer, isNeed)
        }
        mBinding.tvSearchLyricsComplete.setOnClickListener { v: View? -> searchComplete() }
        mBinding.vp2SearchLyrics.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val searchLyricsBean = if (position >= 0 && position < mLyricsBeanList.size) mLyricsBeanList[position] else null
                mSongMid = searchLyricsBean?.songMid ?: ""
                LogUtil.d(TAG, mSongMid)
                val lyricsIndex = position + 1
                mBinding.tvLyricsPageIndex.text = lyricsIndex.toString()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        searchViewModel.lrcViewModel.observe(this) { lyricsList: List<SearchLyricsBean>? ->
            val animation = mBinding.ivSearchLyricsLoading.background as? AnimationDrawable
            animation?.stop()
            if (!lyricsList.isNullOrEmpty()) {
                mLyricsBeanList.clear()
                mLyricsBeanList.addAll(lyricsList)
                mBinding.ivSearchLyricsLoading.visibility = View.GONE
                setTvIndex(lyricsList.size)
                val pagerAdapter2 = SearchLyricsPagerAdapter(this@SearchLyricsActivity, lyricsList)
                mBinding.vp2SearchLyrics.adapter = pagerAdapter2
            } else {
                mBinding.tvSearchLyricsCount.text = "没搜索到歌词"
            }
        }
    }

    private fun showProgress() {
        mBinding.ivSearchLyricsLoading.visibility = View.VISIBLE
        val animation = mBinding.ivSearchLyricsLoading.background as? AnimationDrawable ?: return
        animation.start()
    }

    private fun setTvIndex(size: Int) {
        val lyricsCount =
            if (size > 1) "搜索到" + size + "个结果 (左右滑动查看多个歌词)" else "搜索到" + size + "个结果"
        mBinding.tvSearchLyricsCount.text = lyricsCount
        if (size != 0) {
            mBinding.tvLyricsPageIndex.text = "1"
        }
    }

    private fun searchComplete() {
        LogUtil.d(TAG, "搜索歌词完成")
        // 传递歌曲ID ，用于搜索歌词。
        val intent = Intent()
        intent.putExtra(Constant.SONGMID, mSongMid)
        setResult(Constant.SELECT_LYRICS, intent)
        finish()
    }


    override fun finish() {
        super.finish()
        overridePendingTransition(0, R.anim.dialog_push_out)
    }
}
