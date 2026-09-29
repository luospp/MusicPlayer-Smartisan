package com.yibao.music.activity

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import com.yibao.music.R
import com.yibao.music.adapter.SplashPagerAdapter
import com.yibao.music.base.BaseActivity
import com.yibao.music.base.listener.OnScanConfigListener
import com.yibao.music.databinding.ActivityMusicBinding
import com.yibao.music.fragment.dialogfrag.PermissionsDialog
import com.yibao.music.fragment.dialogfrag.ScannerConfigDialog
import com.yibao.music.model.MusicCountBean
import com.yibao.music.service.LoadMusicDataService
import com.yibao.music.util.ColorUtil
import com.yibao.music.util.Constant
import com.yibao.music.util.ServiceUtil
import com.yibao.music.util.SpUtils.ContentValue
import com.yibao.music.util.SystemUiVisibilityUtil
import com.yibao.music.util.VersionUtil
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

/**
 * @author lsp
 * Des：${TODO}
 * Time:2017/4/22 02:00
 */

class MusicActivity : BaseActivity(), OnScanConfigListener {
    private var mIsHandleScanner = false
    private var mIsFirstScanner = false
    private lateinit var mBinding: ActivityMusicBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityMusicBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        initPermission()
        initView()
        mIsHandleScanner = intent.getBooleanExtra(Constant.SCANNER_MEDIA, false)

    }

    override fun onResume() {
        super.onResume()
        if (mIsHandleScanner) {
            handleLoad()
        }
    }

    private var checkFlag = 1
    private fun initPermission() {
        if (VersionUtil.checkAndroidVersionS()) {
            requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            if (!mIsHandleScanner) {
                loadMusicData()
            }
        } else {
            if (shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)
                || shouldShowRequestPermissionRationale(Manifest.permission.READ_MEDIA_AUDIO)) {
                againPermission(R.string.storage_permission)
            } else if (checkFlag == 1) {
                checkFlag = 2
                initPermission()
            } else {
                againPermission(R.string.storage_permission)
            }
        }
    }


    private fun initView() {
        SystemUiVisibilityUtil.hideStatusBar(window, true)
        val urlFlag = mSps.getBoolean(Constant.PIC_URL_FLAG, false)
        val splashPagerAdapter = SplashPagerAdapter(urlFlag)
        mBinding.vpSplash.adapter = splashPagerAdapter
    }


    private fun againPermission(resId: Int) {
        PermissionsDialog.newInstance(getString(resId)).show(supportFragmentManager, "permissions")
    }

    private fun loadMusicData() {
        if (mSps.getInt(Constant.MUSIC_LOAD) == Constant.NUMBER_EIGHT) {
            startMusicActivity()
        } else {
            ScannerConfigDialog.newInstance(true, this).show(supportFragmentManager, "auto_config")
        }
    }

    private fun updateLoadProgress() {
        mBinding.tvMusicCount.visibility = View.VISIBLE
        mBinding.musicCountPb.visibility = View.VISIBLE
        mCompositeDisposable.add(mBus.toObserverable(MusicCountBean::class.java)
            .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
            .subscribe { musicCountBean: MusicCountBean ->
                // 初次启动时size为所有音乐的数量，当手动扫描时为新增歌曲的数量。
                val size = musicCountBean.size
                var str: String
                val currentCount = musicCountBean.currentCount
                if (size > 0) {
                    mBinding.musicCountPb.setMax(size)
                    str = "已经加载  $currentCount 首本地音乐"
                    mBinding.tvMusicCount.text = str
                    mBinding.musicCountPb.progress = currentCount
                    if (currentCount == size) {
                        mBinding.tvMusicCount.setTextColor(ColorUtil.lyricsSelecte)
                        str = "本地音乐加载完成 -_-  共" + size + "首歌"
                        mBinding.tvMusicCount.text = str
                        // 初次扫描完成后进入MusicActivity
                        if (mIsFirstScanner) {
                            // 初次加载的标记
                            mSps.putValues(
                                ContentValue(
                                    Constant.MUSIC_LOAD, Constant.NUMBER_EIGHT
                                )
                            )
                        } else {
                            // 手动扫描新增歌曲数量
                            str = "新增 $size 首歌曲"
                            mBinding.tvMusicCount.text = str
                        }
                        countDownOperation(mIsFirstScanner)
                    }
                } else {
                    mBinding.tvMusicCount.setTextColor(ColorUtil.musicbarTvDown)
                    mBinding.tvMusicCount.text =
                        if (mIsFirstScanner) "本地没有发现音乐,去下载歌曲后再来体验吧!" else "没有新增歌曲!"
                    countDownOperation(mIsFirstScanner)
                }
            })
    }

    /**
     * 倒计时操作
     *
     * @param b true 表示初次安装，自动扫描完成后直接进入MusicActivity 。 false 表示手动扫描，完成后停在SplashActivity页面。
     */
    private fun countDownOperation(b: Boolean) {
        if (b) {
            startMusicActivity()
        } else {
//            mBinding.tvMusicCount.visibility = View.GONE
            mBinding.musicCountPb.visibility = View.GONE
        }
    }

    private fun startMusicActivity() {
        this@MusicActivity.startActivity(Intent(this@MusicActivity, MainActivity::class.java))
        finish()
    }

    override fun scanMusic(isAutoScan: Boolean) {
        if (isAutoScan) {
            mIsFirstScanner = true
            // 是否是首次安装，本地数据库是否创建，等于 8 表示不是首次安装，数据库已经创建，直接进入MusicActivity。
            if (mSps.getInt(Constant.MUSIC_LOAD) == Constant.NUMBER_EIGHT) {
                countDownOperation(true)
            } else {
                if (!ServiceUtil.isServiceRunning(applicationContext, Constant.LOAD_SERVICE_NAME)) {
                    startService(Intent(applicationContext, LoadMusicDataService::class.java))
                }
            }
            updateLoadProgress()
        } else {
            handleLoad()
        }
    }

    private fun handleLoad() {
        // 手动扫描歌曲
        mIsFirstScanner = false
        val intent = Intent(this, LoadMusicDataService::class.java)
        intent.putExtra(Constant.AUTO_LOAD, true)
        startService(intent)
        updateLoadProgress()
    }
}