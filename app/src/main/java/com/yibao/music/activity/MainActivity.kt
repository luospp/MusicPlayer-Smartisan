package com.yibao.music.activity

import android.Manifest
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.bumptech.glide.Glide
import com.google.android.material.navigation.NavigationBarView
import com.yibao.music.R
import com.yibao.music.adapter.MainViewPagerAdapter
import com.yibao.music.base.BaseActivity
import com.yibao.music.base.listener.OnGlideLoadListener
import com.yibao.music.base.listener.OnMusicItemClickListener
import com.yibao.music.base.listener.OnUpdateTitleListener
import com.yibao.music.databinding.ActivityMainBinding
import com.yibao.music.model.MoreMenuStatus
import com.yibao.music.model.MusicBean
import com.yibao.music.model.greendao.MusicBeanDao
import com.yibao.music.service.MusicPlayService
import com.yibao.music.service.MusicPlayService.AudioBinder
import com.yibao.music.util.*
import com.yibao.music.util.HandleBackUtil.handleBackPress
import com.yibao.music.util.SpUtils.ContentValue
import com.yibao.music.view.music.QqControlBar
import com.yibao.music.view.music.QqControlBar.OnPagerSelectListener
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * @author lsp
 * Des：${主Activity}
 * Time:2017/5/30 13:27
 */
class MainActivity : BaseActivity(), OnMusicItemClickListener, OnUpdateTitleListener,
    OnGlideLoadListener {
    private var mConnection: AudioServiceConnection? = null
    private var mCurrentMusicBean: MusicBean? = null
    private var mCurrentPosition = 0
    private var mMusicConfig = false
    private var isShowQqBar = false
    private var mPlayState = 0
    private var lyricsPlayPosition = 0
    private var mQqBarBean: MusicBean? = null
    private lateinit var mBinding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        initPermission()
        initNotifyPermission()
        initData()
        initMusicConfig()
        initListener()
    }

    private val mHanler = Handler(Looper.getMainLooper())
    private fun initData() {
        mCurrentPosition = mSps.getInt(Constant.MUSIC_POSITION)

        // 初始化 MusicPagerAdapter 主页面
        val pagerAdapter = MainViewPagerAdapter(this)
        mBinding.musicViewpager2.adapter = pagerAdapter
        mBinding.musicViewpager2.setCurrentItem(Constant.NUMBER_TWO, false)
        mBinding.musicViewpager2.offscreenPageLimit = 5
        mBinding.musicViewpager2.isUserInputEnabled = false
        // 初次设置喇叭显示,只在 position>0 时设置。
        mHanler.postDelayed({
            val cPosition = mSps.getInt(Constant.MUSIC_POSITION)
            if (cPosition > 0) {
                mBus.post(Constant.MUSIC_SPEAKER, cPosition.toString())
            }
        }, 1000)

    }

    private fun initMusicConfig() {
        mMusicConfig = mSps.getBoolean(Constant.MUSIC_INIT_FLAG, false)
//        LogUtil.d(TAG, "播放记录标识    $mMusicConfig")

        if (mMusicConfig) {
            mPlayState = mSps.getInt(Constant.MUSIC_PLAY_STATE)
//            LogUtil.d(TAG, "自动播放    $mPlayState")
            if (mPlayState == Constant.NUMBER_ONE) {
                // 读取用户的播放记录，设置UI显示，做好播放的准备。(暂停和播放两种状态)
                if (mCurrentMusicBean != null) {
                    setMusicInfo(mCurrentMusicBean!!)
                }
            } else if (mPlayState == Constant.NUMBER_TWO) {
                startServiceAndAnimation()
            }
        } else {
            // 没有播放记录
            LogUtil.d(TAG, "用户 ++++  nothing ")
        }
    }

    private fun startServiceAndAnimation() {
        val pageType = mSps.getInt(Constant.PAGE_TYPE)
        startMusicService(mCurrentPosition, pageType)
        mBinding.smartisanControlBar.setPlayButtonState(R.drawable.btn_playing_pause_selector)
        mBinding.qqControlBar.setPlayButtonState(R.drawable.btn_playing_pause_selector)
        mPlayState = Constant.NUMBER_THREE
    }

    override fun refreshBtnAndNotify(playStatus: Int) {
        when (playStatus) {
            Constant.NUMBER_ZERO -> {
                audioBinder?.let { mBinding.smartisanControlBar.animatorOnResume(it.isPlaying) }
                updatePlayBtnStatus()
            }

            Constant.NUMBER_ONE -> checkCurrentSongIsFavorite(
                mCurrentMusicBean, mBinding.qqControlBar, mBinding.smartisanControlBar
            )

            Constant.NUMBER_TWO -> {
                updatePlayBtnStatus()
                mBinding.smartisanControlBar.animatorOnPause()
            }

            else -> {}
        }
    }

    private fun initListener() {
        mBinding.bnvMusic.selectedItemId = R.id.navigation_song
        mBinding.bnvMusic.setOnItemSelectedListener(mNbvListener)
        mBinding.smartisanControlBar.setOnClickListener { readyMusic() }
        mBinding.musicNavigationBar.setOnNavigationBarListener { position: Int ->
            setCurrentPosition(position)
        }
        mBinding.smartisanControlBar.setClickListener { clickFlag: Int ->
            if (mMusicConfig) {
                if (clickFlag == Constant.NUMBER_THREE) {
                    switchPlayState()
                } else {
                    if (audioBinder != null) {
                        when (clickFlag) {
                            Constant.NUMBER_ONE -> {
                                audioBinder?.updateFavorite()
                                checkCurrentSongIsFavorite(
                                    mCurrentMusicBean,
                                    mBinding.qqControlBar,
                                    mBinding.smartisanControlBar
                                )
                            }

                            Constant.NUMBER_TWO -> {
                                clearDisposableProgress()
                                audioBinder?.playPre()
                            }

                            Constant.NUMBER_FOUR -> {
                                clearDisposableProgress()
                                audioBinder?.playNext()
                            }

                            else -> {}
                        }
                    } else {
                        SnakbarUtil.firstPlayMusic(mBinding.smartisanControlBar)
                    }
                }
            } else {
                ToastUtil.showNoMusic(this@MainActivity)
            }
        }


        mBinding.qqControlBar.setOnButtonClickListener(object : QqControlBar.OnButtonClickListener {
            override fun click(clickFlag: Int) {
                if (mMusicConfig) {
                    when (clickFlag) {
                        Constant.NUMBER_ONE -> switchPlayState()
                        Constant.NUMBER_TWO -> if (audioBinder != null) {
                            audioBinder?.updateFavorite()
                            checkCurrentSongIsFavorite(
                                mCurrentMusicBean,
                                mBinding.qqControlBar,
                                mBinding.smartisanControlBar
                            )
                        } else {
                            SnakbarUtil.firstPlayMusic(mBinding.smartisanControlBar)
                        }

                        else -> {}
                    }
                } else {
                    ToastUtil.showNoMusic(this@MainActivity)
                }
            }
        })

        // QQ bar 左右滑上一曲、下一曲
        mBinding.qqControlBar.setOnPagerSelectListener(object : OnPagerSelectListener {
            override fun selectPosition(pagerPosition: Int) {
                val position = mSps.getInt(Constant.MUSIC_POSITION)
                if (position > pagerPosition) {
                    clearDisposableProgress()
                    audioBinder?.playPre()
                } else if (position < pagerPosition) {
                    clearDisposableProgress()
                    audioBinder?.playNext()
                }
            }
        })


        mBinding.musicViewpager2.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                mBinding.bnvMusic.menu.getItem(position).isChecked = true
            }
        })
    }


    private val mNbvListener = NavigationBarView.OnItemSelectedListener { item: MenuItem ->
        when (item.itemId) {
            R.id.navigation_play_list -> {
                mBinding.musicViewpager2.setCurrentItem(0, false)

            }

            R.id.navigation_artist -> {
                mBinding.musicViewpager2.setCurrentItem(1, false)

            }

            R.id.navigation_song -> {
                mBinding.musicViewpager2.setCurrentItem(2, false)

            }

            R.id.navigation_album -> {
                mBinding.musicViewpager2.setCurrentItem(3, false)

            }

            R.id.navigation_about -> {
                mBinding.musicViewpager2.setCurrentItem(4, false)

            }

        }
        false
    }

    private fun setCurrentPosition(position: Int) {
        mBinding.musicViewpager2.setCurrentItem(position, false)
    }


    /**
     * 切换当前播放状态
     * mPlayState  将音乐的播放状态记录到本地，方便用户下次打开时进行UI初始化操作。
     *
     *
     * mPlayState = 1 ：表示用户点击暂停后，并退出音乐播放器。下次打开播放器的界面时，
     * 不会自动播放上一次记录的歌曲，需要点击播放按钮，才能播放上一次记录的歌曲。
     *
     *
     * mPlayState = 2 ：表示在播放时退出音乐播放器的界面，只是短暂的离开，但并没有退出程序(程序并没有被后台杀死)，
     * 下次打开播放器的界面时，继续自动播放当前的歌曲。
     */
    private fun switchPlayState() {
        LogUtil.d(TAG, "switchPlayState   =====  $mPlayState")
        when (mPlayState) {
            Constant.NUMBER_ONE -> {
                startServiceAndAnimation()
            }

            Constant.NUMBER_TWO -> {
                mPlayState = Constant.NUMBER_THREE
            }

            else -> {
                if (audioBinder == null) {
                    ToastUtil.showNoMusic(this)
                } else if (audioBinder?.isPlaying == true) {
                    // 当前播放  暂停
                    audioBinder?.pause()
                    clearDisposableProgress()
                } else if (audioBinder?.isPlaying != true) {
                    // 当前暂停  播放
                    audioBinder?.start()
                    upDataPlayProgress()
                }
                if (audioBinder != null) {
                    mBinding.smartisanControlBar.animatorOnResume(audioBinder?.isPlaying ?: false)
                }
                //更新播放状态按钮
                updatePlayBtnStatus()
            }
        }
    }

    /**
     * 在主列表播放音乐
     * 开启服务，播放音乐并且将数据标记传送过去
     *
     * @param position 当前点击的曲目
     * @param pageType 页面标识
     */
    override fun startMusicService(position: Int, pageType: Int) {
        LogUtil.d(TAG, "播放的页面标识   $pageType")
        mCurrentPosition = position
        val musicIntent = Intent(applicationContext, MusicPlayService::class.java)
        musicIntent.putExtra(Constant.PAGE_TYPE, pageType)
        //        musicIntent.putExtra(Constant.CONDITION, condition);
        musicIntent.putExtra(Constant.POSITION, mCurrentPosition)
        mConnection = AudioServiceConnection()
        bindService(musicIntent, mConnection!!, BIND_AUTO_CREATE)
        startServiceIntent(musicIntent)
    }

    /**
     * 在详情页面播放音乐回调
     *
     * @param position  当前点击的曲目
     * @param pageType  页面标识
     * @param condition 关键字
     */
    override fun startMusicServiceFlag(position: Int, pageType: Int, condition: String) {
        LogUtil.d(TAG, "详情界面播放歌曲 ====   $pageType")
        mCurrentPosition = position
        val intent = Intent(this, MusicPlayService::class.java)
        intent.putExtra(Constant.PAGE_TYPE, pageType)
        intent.putExtra(Constant.CONDITION, condition)
        intent.putExtra(Constant.POSITION, position)
        mConnection = AudioServiceConnection()
        bindService(intent, mConnection!!, BIND_AUTO_CREATE)
        startServiceIntent(intent)
    }

    private fun startServiceIntent(intent: Intent) {
        startService(intent)
    }

    /**
     * PagerAdapter回调
     */
    override fun onOpenMusicPlayDialogFag() {
        readyMusic()
    }


    private fun readyMusic() {
        if (audioBinder != null) {
            if (mMusicConfig) {
                startPlayActivity()
            } else {
                ToastUtil.showNoMusic(this@MainActivity)
            }
        } else {
            SnakbarUtil.favoriteSuccessView(mBinding.smartisanControlBar, "请先播放音乐!")
        }
    }

    /**
     * @param musicItem 当前播放的歌曲信息，用于更新进度和动画状态,需要用的界面复写这个方法
     */
    override fun updateCurrentPlayInfo(musicItem: MusicBean) {
        // 将MusicConfig设置为ture
        mSps.putValues(ContentValue(Constant.MUSIC_INIT_FLAG, true))
        mMusicConfig = true
        // 更新歌曲的信息
        setMusicInfo(musicItem)
        // 设置歌曲最大进度
        setDuration()
        // 更新播放按钮状态
        updatePlayBtnStatus()
        // 初始化动画
        mBinding.smartisanControlBar.initAnimation()
        //更新歌曲的进度
        upDataPlayProgress()
        if (isShowQqBar) {
            mBinding.qqControlBar.updatePosition()
            setQqPagerLyric()
        }
    }

    /**
     * 切换音乐控制面板的样式
     */
    private fun switchMusicControlBar() {
        if (audioBinder != null && audioBinder?.isPlaying ?: false) {
            if (isShowQqBar) {
                mBinding.qqControlBar.visibility = View.INVISIBLE
                mBinding.smartisanControlBar.visibility = View.VISIBLE
                disposableQqLyric()
            } else {
                if (audioBinder != null) {
                    mBinding.qqControlBar.setPagerData()
                }
                mBinding.qqControlBar.visibility = View.VISIBLE
                mBinding.smartisanControlBar.visibility = View.INVISIBLE

                //TODO 这里做更新歌词的操作
//                setQqPagerLyric()
            }
            isShowQqBar = !isShowQqBar
        } else {
            SnakbarUtil.firstPlayMusic(mBinding.smartisanControlBar)
        }
    }

    /**
     * QQbar时时更新歌词
     */
    private fun setQqPagerLyric() {
        val lyricList = LyricsUtil.getLyricList(mCurrentMusicBean)
        disposableQqLyric()
        if (mQqLyricsDisposable == null) {
            if (lyricList.size > 1 && lyricsPlayPosition < lyricList.size) {
                mQqLyricsDisposable = Observable.interval(
                    1600, TimeUnit.MICROSECONDS
                ) //                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread()).subscribe {
                        //通过集合，播放过的歌词就从集合中删除
                        val lyrBean =
                            lyricList[if (lyricsPlayPosition == lyricList.size || lyricsPlayPosition > lyricList.size) lyricList.size - 1 else lyricsPlayPosition]
                        val lyrics = lyrBean.content
                        val progress = audioBinder?.progress ?: 0
                        val startTime = lyrBean.startTime
                        val musicList = audioBinder?.musicList
                        if (musicList != null && progress > startTime) {
                            LogUtil.d(TAG, "歌词List的长度    ==  " + lyricList.size)
                            if (mCurrentPosition < musicList.size) {
                                mQqBarBean = musicList[mCurrentPosition]
                                mQqBarBean!!.currentLyrics = lyrics
                                musicList[mCurrentPosition] = mQqBarBean
                            }
                            LogUtil.d(TAG, "当前的位置 ===  $mCurrentPosition  进度 ===  $progress")
                            LogUtil.d(TAG, "当前的时间和歌词 ===  $startTime ==  $lyrics")
                            mBinding.qqControlBar.setPagerData()
                            lyricsPlayPosition++
                        }
                    }
            } else {
                LogUtil.d(TAG, "========MusicActivity =====没有发现歌词 ")
            }
        } else {
            LogUtil.d(TAG, "=============没有时间和歌词 ")
            mBinding.qqControlBar.setPagerData()
        }
    }

    private fun setDuration() {
        val duration = audioBinder?.duration ?: 0
        mBinding.smartisanControlBar.setMaxProgress(duration)
        mBinding.qqControlBar.setMaxProgress(duration)
    }

    /**
     * 设置歌曲名和歌手名
     *
     * @param musicItem g
     */
    private fun setMusicInfo(musicItem: MusicBean) {
        mCurrentMusicBean = musicItem
        checkCurrentSongIsFavorite(
            mCurrentMusicBean, mBinding.qqControlBar, mBinding.smartisanControlBar
        )
        // 更新音乐标题
        TitleArtistUtil.getMusicBean(musicItem)
        mBinding.smartisanControlBar.setSongName(musicItem.title)
        // 更新歌手名称
        mBinding.smartisanControlBar.setSingerName(mCurrentMusicBean!!.artist)


        // 设置专辑
        mBinding.smartisanControlBar.setAlbumUrl(this, mCurrentMusicBean)
    }


    private fun updateQqBar() {
        if (isShowQqBar) {
            mBinding.qqControlBar.setPagerData()
            setQqPagerLyric()
        }
    }

    override fun updateCurrentPlayProgress() {
        if (audioBinder != null) {
            if (audioBinder?.isPlaying ?: false) {
                if (isShowQqBar) {
                    mBinding.qqControlBar.setProgress(audioBinder?.progress ?: 0)
                } else {
                    mBinding.smartisanControlBar.setSongProgress(audioBinder?.progress ?: 0)
                }

            }
        }
    }

    private fun updatePlayBtnStatus() {
        //根据当前播放状态设置图片
        if (audioBinder != null) {
            mBinding.smartisanControlBar.updatePlayBtnStatus(audioBinder?.isPlaying ?: false)
            mBinding.qqControlBar.updatePlayButtonState(audioBinder?.isPlaying ?: false)
        } else {
            SnakbarUtil.firstPlayMusic(mBinding.smartisanControlBar)
        }
    }


    private class AudioServiceConnection : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            audioBinder = service as AudioBinder
            isPlayFlag = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            audioBinder = null
            isPlayFlag = false
        }
    }

    override fun onPause() {
        super.onPause()
        mBinding.smartisanControlBar.animatorOnPause()
    }

    override fun onResume() {
        super.onResume()
        if (isPlayFlag) {
            val binder = audioBinder ?: run {
                isPlayFlag = false
                return
            }
            val musicBean = binder.musicBean
            if (musicBean != null) {
                setMusicInfo(musicBean)
            }
            mBinding.smartisanControlBar.animatorOnResume(binder.isPlaying)
            checkCurrentSongIsFavorite(
                mCurrentMusicBean, mBinding.qqControlBar, mBinding.smartisanControlBar
            )
            updatePlayBtnStatus()
            updateCurrentPlayProgress()
            setDuration()
            updateQqBar()
        }

    }

    override fun moreMenu(moreMenuStatus: MoreMenuStatus) {
        super.moreMenu(moreMenuStatus)
        val musicBean = moreMenuStatus.musicBean
        when (moreMenuStatus.position) {
            Constant.NUMBER_ZERO -> startPlayListActivity(musicBean.title)
            Constant.NUMBER_ONE -> SnakbarUtil.keepGoing(mBinding.smartisanControlBar)
            Constant.NUMBER_TWO -> if (audioBinder != null) {
                if (audioBinder?.position ?: 0 == moreMenuStatus.musicPosition) {
                    audioBinder?.updateFavorite()
                    checkCurrentSongIsFavorite(
                        musicBean, mBinding.qqControlBar, mBinding.smartisanControlBar
                    )
                } else {
                    audioBinder!!.updateFavorite(moreMenuStatus.musicBean)
                }
            } else {
                SnakbarUtil.firstPlayMusic(mBinding.smartisanControlBar)
            }

            Constant.NUMBER_THREE -> SnakbarUtil.keepGoing(mBinding.smartisanControlBar)
            Constant.NUMBER_FOUR -> deleteSong(moreMenuStatus)
            else -> {}
        }
    }

    /**
     * 删除歌曲，如果删除当前正在播放的歌曲，先进行 《下一曲》 操作, 删除后通知
     * SongCategoryFragment刷新列表。
     *
     * @param moreMenuStatus m
     */
    private fun deleteSong(moreMenuStatus: MoreMenuStatus) {
        val musicBean = moreMenuStatus.musicBean
        if (audioBinder != null) {
            if (mCurrentMusicBean!!.title == musicBean.title) {
                audioBinder?.playNext()
            }
            val songUrl = musicBean.songUrl
            // 先从本地数据库删除歌曲，再彻底删除歌曲文件。
            mMusicDao.delete(musicBean)
            FileUtil.deleteFile(File(songUrl))
            mBus.post(Constant.DELETE_SONG, moreMenuStatus.position)
        } else {
            SnakbarUtil.favoriteSuccessView(mBinding.smartisanControlBar, "请先播放音乐!")
        }
    }

    /**
     * AboutFragment 界面恢复收藏歌曲后调用
     */
    override fun checkCurrentFavorite() {
        val musicBean = mMusicDao.queryBuilder().where(
            MusicBeanDao.Properties.Id.eq(
                mCurrentMusicBean!!.id
            )
        ).build().unique()
        checkCurrentSongIsFavorite(
            musicBean, mBinding.qqControlBar, mBinding.smartisanControlBar
        )
    }

    override fun switchControlBar() {
        //TODO
        switchMusicControlBar()
    }


    override fun onBackPressed() {
        if (!handleBackPress(this)) {
            super.onBackPressed()
        } else {
            LogUtil.d(TAG, "自己处理")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handleAftermath()
        unbindAudioService()
    }

    private fun unbindAudioService() {
        if (mConnection != null) {
            unbindService(mConnection!!)
            mConnection = null
        }
        //        stopService(new Intent(this, AudioPlayService.class));
    }

    private fun handleAftermath() {
        mBinding.smartisanControlBar.animatorStop()
        if (audioBinder != null) {
            mPlayState = if (audioBinder?.isPlaying == true) Constant.NUMBER_TWO else Constant.NUMBER_ONE

            mSps.putValues(ContentValue(Constant.MUSIC_PLAY_STATE, mPlayState))
        }
    }

    override fun resumeRequests() {
        if (!isDestroyed) {
            Glide.with(this).resumeRequests()
        }
    }

    override fun pauseRequests() {
        if (!isDestroyed) {
            Glide.with(this).pauseRequests()
        }
    }

    private fun initNotifyPermission() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (!notificationManager.areNotificationsEnabled()) {
            if (VersionUtil.checkAndroidVersionS()) {
                requestNotifyPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private val requestNotifyPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        LogUtil.d(TAG, "通知权限获取结果   $granted")
        if (!granted) {
            openNotifyCation()
        }
    }

    /**
     * 如果通知未打开 跳转到通知设定界面
     * @param mContext
     */

    private fun openNotifyCation() {
        val intent = Intent()
        try {
            intent.action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            startActivity(intent)
        } catch (e: Exception) {
            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            val uri = Uri.fromParts("package", packageName, null)
            intent.data = uri
            startActivity(intent)
        }
    }



    private fun initPermission() {
        if (VersionUtil.checkAndroidVersionS()) {
            requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        LogUtil.d(TAG, "相册权限获取结果   $granted")
    }


    companion object {
        @JvmStatic
        var audioBinder: AudioBinder? = null
            private set
        @JvmStatic
        var isPlayFlag = false
            private set
    }
}