package com.yibao.music.activity

import android.content.Intent
import android.text.Editable
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.yibao.music.R
import com.yibao.music.adapter.DetailsViewAdapter
import com.yibao.music.base.bindings.BaseBindingActivity
import com.yibao.music.base.bindings.BaseBindingAdapter
import com.yibao.music.base.listener.OnFlowLayoutClickListener
import com.yibao.music.base.listener.OnMusicItemClickListener
import com.yibao.music.base.listener.TextChangedListener
import com.yibao.music.databinding.ActivitySearchBinding
import com.yibao.music.fragment.dialogfrag.MoreMenuBottomDialog
import com.yibao.music.model.MusicBean
import com.yibao.music.service.MusicPlayService
import com.yibao.music.service.MusicPlayService.AudioBinder
import com.yibao.music.util.ColorUtil
import com.yibao.music.util.Constant
import com.yibao.music.util.LogUtil
import com.yibao.music.util.LyricsUtil
import com.yibao.music.util.SoftKeybordUtil
import com.yibao.music.util.TitleArtistUtil
import com.yibao.music.viewmodel.SearchViewModel
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

/**
 * @author lsp
 */
class SearchActivity : BaseBindingActivity<ActivitySearchBinding>(), OnMusicItemClickListener,
    OnFlowLayoutClickListener, View.OnClickListener {
    private val mViewModel: SearchViewModel by lazy { gets(SearchViewModel::class.java) }
    private var mMusicBean: MusicBean? = null
    private var audioBinder: AudioBinder? = null
    private var lyricsFlag = 0
    private var mInputMethodManager: InputMethodManager? = null
    private var mAdapter: DetailsViewAdapter? = null

    // 默认为 11 ，按歌曲名搜索。
    private var mPosition = 11
    override fun initView() {
        initRecyclerView(mBinding.recyclerSearch)
    }

    override fun initData() {
        // pageType 0 toolbar上的搜索，1 播放界面点击歌曲名，直接以歌手搜索。
        val pageType = intent.getIntExtra(Constant.PAGE_TYPE, 0)
        LogUtil.d(TAG, "PageType   $pageType")
        audioBinder = MainActivity.audioBinder
        mBinding.smartisanControlBar.setPbColorAndPreBtnGone()
        // 从PlayActivity过来的,按歌手搜索。
        if (pageType > Constant.NUMBER_ZERO) {
            mMusicBean = intent.getParcelableExtra(Constant.MUSIC_BEAN)

            mBinding.editSearch.setText(mMusicBean!!.artist)
            switchListCategory(Constant.SEARCH_SONG_FOR_ARTIST)

        } else {
            // 加载历史记录
            mViewModel.getHistory()

            // 从Toolbar上过来的，主动弹出键盘
//            mInputMethodManager =
//                getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//            Timer().schedule(timerTask {
//                SoftKeybordUtil.showAndHintSoftInput(
//                    mInputMethodManager,
//                    2,
//                    InputMethodManager.SHOW_FORCED
//                )
//            }, 100)

        }
    }

    override fun onResume() {
        super.onResume()
        // 搜索结果
        mViewModel.searchViewModel.observe(this) { musicList ->
            setData(musicList)
        }
        mViewModel.historyViewModel.observe(this) { historyList ->
            mBinding.flowlayout.setData(historyList)
            mBinding.flowlayout.setItemClickListener { _, bean ->
//                mPosition = 2
                mBinding.editSearch.setText(bean.searchContent)
                mBinding.editSearch.setSelection(bean.searchContent.length)
            }
        }
        if (audioBinder != null) {
            mMusicBean = audioBinder?.musicBean
            setMusicInfo(mMusicBean)
            checkCurrentSongIsFavorite(mMusicBean, null, mBinding.smartisanControlBar)
            mBinding.smartisanControlBar.updatePlayBtnStatus(audioBinder?.isPlaying ?: false)
            mBinding.smartisanControlBar.animatorOnResume(audioBinder?.isPlaying ?: false)
            updateLyric()
            setDuration()
        }

    }

    private fun setData(musicList: List<MusicBean>) {
        LogUtil.d(TAG, "当前页面标识   $mPosition")
        if (musicList.isEmpty()) {
            mBinding.tvNoSearchResult.visibility = View.VISIBLE
            mBinding.recyclerSearch.visibility = View.GONE
        } else {
            mBinding.tvNoSearchResult.visibility = View.GONE
            mBinding.recyclerSearch.visibility = View.VISIBLE
            val editCondition = mBinding.editSearch.text.toString().trim()
            // 列表数据
            mAdapter = DetailsViewAdapter(this, musicList, mPosition, editCondition)
            mBinding.recyclerSearch.adapter = mAdapter
            mAdapter!!.setOnItemMenuListener(object :
                BaseBindingAdapter.OnOpenItemMoreMenuListener {
                override fun openClickMoreMenu(position: Int, musicBean: MusicBean) {
                    // 关闭键盘
                    SoftKeybordUtil.showAndHintSoftInput(
                        mInputMethodManager, 1, InputMethodManager.SHOW_FORCED
                    )
                    MoreMenuBottomDialog.newInstance(
                        musicBean, position, isNeedScore = false, isNeedSetTime = false
                    ).getBottomDialog(this@SearchActivity)

                }
            })

        }

    }

    override fun initListener() {
        mBinding.tvSearchCancel.setOnClickListener(this)
        mBinding.ivEditClear.setOnClickListener(this)
        mBinding.searchCategoryRoot.tvSearchAll.setOnClickListener(this)
        mBinding.searchCategoryRoot.tvSearchSong.setOnClickListener(this)
        mBinding.searchCategoryRoot.tvSearchArtist.setOnClickListener(this)
        mBinding.searchCategoryRoot.tvSearchAlbum.setOnClickListener(this)
        mBinding.ivClearHistory.setOnClickListener(this)
        mBinding.editSearch.addTextChangedListener(object : TextChangedListener() {
            override fun afterTextChanged(s: Editable) {
                searchMusic(s.toString().trim(), mPosition)
            }
        })
        mBinding.smartisanControlBar.setOnClickListener { startPlayActivity() }
        // 底部控制Bar按钮监听
        mBinding.smartisanControlBar.setClickListener { clickFlag: Int ->
            LogUtil.d(TAG, clickFlag.toString())
            when (clickFlag) {
                Constant.NUMBER_ONE -> {
                    audioBinder?.updateFavorite()
                    checkCurrentSongIsFavorite(mMusicBean, null, mBinding.smartisanControlBar)
                }

                Constant.NUMBER_TWO -> audioBinder?.playPre()
                Constant.NUMBER_THREE -> switchPlayState()
                Constant.NUMBER_FOUR -> audioBinder?.playNext()
                else -> {}
            }
        }
    }


    override fun onClick(v: View) {
        when (v.id) {
            R.id.iv_edit_clear -> {
                mBinding.editSearch.setText("")
                mAdapter?.clear()
            }

            R.id.iv_clear_history -> {
                LogUtil.d(TAG, "历史点击")
                // 清空历史记录
            }

            R.id.tv_search_all -> {
                switchListCategory(Constant.SEARCH_SONG_FOR_ALL)
            }

            R.id.tv_search_song -> {
                switchListCategory(Constant.SEARCH_SONG_FOR_NAME)
            }

            R.id.tv_search_album -> {
                switchListCategory(Constant.SEARCH_SONG_FOR_ALL)
            }

            R.id.tv_search_artist -> {
                switchListCategory(Constant.SEARCH_SONG_FOR_ARTIST)
            }

            R.id.tv_search_cancel -> {
                SoftKeybordUtil.showAndHintSoftInput(
                    mInputMethodManager, 1, InputMethodManager.RESULT_UNCHANGED_SHOWN
                )
                finish()
            }
        }
    }

    /**
     * @param searchKey 搜索关键字key 、
     * @param position 搜索类别：  11歌曲 、 12专辑 、 13 艺术家 、14 全部
     */
    private fun searchMusic(searchKey: String, position: Int) {
        mBinding.ivEditClear.visibility = if (searchKey.isEmpty()) View.GONE else View.VISIBLE
        mBinding.tvNoSearchResult.visibility = if (searchKey.isEmpty()) View.GONE else View.VISIBLE
        mBinding.flowlayout.visibility = if (searchKey.isEmpty()) View.VISIBLE else View.GONE

        mBinding.searchCategoryRoot.root.visibility =
            if (searchKey.isEmpty()) View.GONE else View.VISIBLE
        mBinding.groupSearch.visibility = if (searchKey.isEmpty()) View.VISIBLE else View.GONE
        if (searchKey.isNotEmpty()) {
            mViewModel.searchMusic(searchKey, position)
        } else {
            // 获取历史记录
            mViewModel.getHistory()
        }
    }

    override fun updateLyricsView(lyricsOK: Boolean, downMsg: String) {
        if (lyricsOK) {
            updateLyric()
        }
    }


    override fun updateCurrentPlayInfo(musicItem: MusicBean) {
        mMusicBean = musicItem
        setMusicInfo(musicItem)
        mBinding.smartisanControlBar.initAnimation()
        if (audioBinder != null) {
            setDuration()
            checkCurrentSongIsFavorite(mMusicBean, null, mBinding.smartisanControlBar)
            mBinding.smartisanControlBar.updatePlayBtnStatus(audioBinder?.isPlaying ?: false)
            updateLyric()
        }
    }

    override fun updateCurrentPlayProgress() {
        if (audioBinder != null) {
            mBinding.smartisanControlBar.setSongProgress(audioBinder?.progress ?: 0)
        }
    }

    private fun setDuration() {
        val duration = audioBinder?.duration ?: 0
        mBinding.smartisanControlBar.setMaxProgress(duration)
    }

    private fun setMusicInfo(bean: MusicBean?) {
        if (bean != null) {
            mBinding.smartisanControlBar.visibility = View.VISIBLE
            val musicItem = TitleArtistUtil.getMusicBean(bean)
            mBinding.smartisanControlBar.setSongName(musicItem.title)
            mBinding.smartisanControlBar.setSingerName(musicItem.artist)
            mBinding.smartisanControlBar.setAlbumUrl(this, musicItem)
        }
        if (audioBinder != null) {
            mBinding.smartisanControlBar.updatePlayBtnStatus(audioBinder?.isPlaying ?: false)
            mBinding.smartisanControlBar.initAnimation()
        } else {
            mBinding.smartisanControlBar.visibility = View.GONE
        }
    }

    private fun switchListCategory(position: Int) {
        mPosition = position
        val searchKey = mBinding.editSearch.text.trim().toString()
        mViewModel.searchMusic(searchKey, position)
        when (position) {
            Constant.SEARCH_SONG_FOR_NAME -> {
                setAllCategoryNotNormal()
                mBinding.searchCategoryRoot.tvSearchSong.setTextColor(ColorUtil.wihtle)
                mBinding.searchCategoryRoot.tvSearchSong.setBackgroundResource(R.drawable.btn_category_start_down_selector)
            }

            Constant.SEARCH_SONG_FOR_ALBUM -> {
                setAllCategoryNotNormal()
                mBinding.searchCategoryRoot.tvSearchAlbum.setTextColor(ColorUtil.wihtle)
                mBinding.searchCategoryRoot.tvSearchAlbum.setBackgroundResource(R.drawable.btn_category_middle_down_selector)
            }

            Constant.SEARCH_SONG_FOR_ARTIST -> {
                setAllCategoryNotNormal()
                mBinding.searchCategoryRoot.tvSearchArtist.setBackgroundResource(R.drawable.btn_category_middle_down_selector)
                mBinding.searchCategoryRoot.tvSearchArtist.setTextColor(ColorUtil.wihtle)
            }

            Constant.SEARCH_SONG_FOR_ALL -> {
                setAllCategoryNotNormal()
                mBinding.searchCategoryRoot.tvSearchAll.setTextColor(ColorUtil.wihtle)
                mBinding.searchCategoryRoot.tvSearchAll.setBackgroundResource(R.drawable.btn_category_end_down_selector)
            }
        }
    }

    private fun setAllCategoryNotNormal() {
        mBinding.searchCategoryRoot.tvSearchSong.setTextColor(ColorUtil.textName)
        mBinding.searchCategoryRoot.tvSearchSong.setBackgroundResource(R.drawable.btn_category_start_selector)
        mBinding.searchCategoryRoot.tvSearchAlbum.setTextColor(ColorUtil.textName)
        mBinding.searchCategoryRoot.tvSearchAlbum.setBackgroundResource(R.drawable.btn_category_middle_selector)
        mBinding.searchCategoryRoot.tvSearchArtist.setTextColor(ColorUtil.textName)
        mBinding.searchCategoryRoot.tvSearchArtist.setBackgroundResource(R.drawable.btn_category_middle_selector)
        mBinding.searchCategoryRoot.tvSearchAll.setTextColor(ColorUtil.textName)
        mBinding.searchCategoryRoot.tvSearchAll.setBackgroundResource(R.drawable.btn_category_end_selector)
    }

    override fun refreshBtnAndNotify(playStatus: Int) {
        when (playStatus) {
            0 -> {
                mBinding.smartisanControlBar.animatorOnResume(audioBinder?.isPlaying ?: false)
                mBinding.smartisanControlBar.updatePlayBtnStatus(audioBinder?.isPlaying ?: false)
            }

            1 -> {
                checkCurrentSongIsFavorite(mMusicBean, null, mBinding.smartisanControlBar)
            }

            2 -> {
                mBinding.smartisanControlBar.updatePlayBtnStatus(audioBinder?.isPlaying ?: false)
                mBinding.smartisanControlBar.animatorOnPause()
            }

        }
    }


    private fun switchPlayState() {
        if (audioBinder?.isPlaying ?: false) {
            audioBinder?.pause()
        } else {
            audioBinder?.start()
        }
        mBinding.smartisanControlBar.animatorOnResume(audioBinder?.isPlaying ?: false)
        mBinding.smartisanControlBar.updatePlayBtnStatus(audioBinder?.isPlaying ?: false)
    }


    override fun startMusicServiceFlag(
        position: Int, pageType: Int, conditon: String
    ) {
        val condition = mBinding.editSearch.text.toString().trim()
        val intent = Intent(this, MusicPlayService::class.java)
        intent.putExtra(Constant.PAGE_TYPE, pageType)
        intent.putExtra(Constant.CONDITION, condition)
        intent.putExtra(Constant.POSITION, position)
        startService(intent)
    }

    override fun startMusicService(position: Int, mPageType: Int) {}
    override fun onOpenMusicPlayDialogFag() {}
    override fun onPause() {
        super.onPause()
        mBinding.smartisanControlBar.animatorOnPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (audioBinder != null) {
            audioBinder = null
        }
    }

    private fun updateLyric() {
        val lyricList = LyricsUtil.getLyricList(mMusicBean)
        disposableQqLyric()
        if (mQqLyricsDisposable == null) {
            mQqLyricsDisposable =
                Observable.interval(0, 2800, TimeUnit.MICROSECONDS).subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread()).subscribe {
                        if (lyricList.size > 1 && lyricsFlag < lyricList.size) {
                            //通过集合，播放过的歌词就从集合中删除
                            val lyrBean = lyricList[lyricsFlag]
                            val content = lyrBean.content
                            val progress = audioBinder?.progress ?: 0
                            val startTime = lyrBean.startTime
                            if (progress > startTime) {
                                mBinding.smartisanControlBar.setSingerName(content)
                                lyricsFlag++
                            }
                        }
                    }
        }
    }

    override fun click(songName: String) {
        mBinding.editSearch.setText(songName)
        mBinding.editSearch.setSelection(songName.length)
    }


    override fun finish() {
        super.finish()
        overridePendingTransition(0, R.anim.dialog_push_out)
    }
}