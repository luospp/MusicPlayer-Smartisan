package com.yibao.music.fragment

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import com.yibao.music.R
import com.yibao.music.base.bindings.BaseMusicFragmentDev
import com.yibao.music.base.listener.OnScanConfigListener
import com.yibao.music.base.listener.OnTakePhotoListener
import com.yibao.music.base.listener.OnUpdateTitleListener
import com.yibao.music.databinding.AboutFragmentBinding
import com.yibao.music.fragment.dialogfrag.CrashSheetDialog
import com.yibao.music.fragment.dialogfrag.RelaxDialogFragment
import com.yibao.music.fragment.dialogfrag.ScannerConfigDialog
import com.yibao.music.fragment.dialogfrag.TakePhotoBottomSheetDialog
import com.yibao.music.fragment.dialogfrag.VersionDialog
import com.yibao.music.model.MusicBean
import com.yibao.music.model.greendao.MusicBeanDao
import com.yibao.music.util.Constant
import com.yibao.music.util.FileUtil
import com.yibao.music.util.ImageUitl
import com.yibao.music.util.LogUtil
import com.yibao.music.util.LyricsUtil
import com.yibao.music.util.ReadFavoriteFileUtil
import com.yibao.music.util.SpUtils
import com.yibao.music.util.ThreadPoolProxyFactory
import com.yibao.music.util.ToastUtil
import com.yibao.music.view.music.MusicToolBar
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.io.File
import java.io.FileNotFoundException

/**
 * @项目名： ArtisanMusic
 * @包名： com.yibao.music.folder
 * @文件名: AboutFragment
 * @author: Stran
 * @创建时间: 2018/2/9 20:51
 * @描述： {TODO}
 */
class AboutFragment : BaseMusicFragmentDev<AboutFragmentBinding>(), OnScanConfigListener {
    // 相册选择或拍照得到的图片 Uri，裁剪成功后用于设置头像
    private var mContentUri: Uri? = null

    // 拍照输出文件的 Uri
    private var mTakePhotoUri: Uri? = null

    // 裁剪前头像文件的修改时间，用于判断裁剪应用是否写出了新的头像文件
    private var mHeaderModified = 0L

    override fun initView() {
        mBinding.musicBar.setToolbarTitle(getString(R.string.about))
        initData()
        initListener()
    }

    override fun initData() {
        val file = File(Constant.MUSIC_LYRICS_ROOT)
        if (file.exists()) {
            mBinding.tvDeleteErrorLyric.visibility = View.VISIBLE
        }
        val headerFile = FileUtil.getHeaderFile(requireContext())
        if (headerFile.exists()) {
            setHeaderView(Uri.fromFile(headerFile))
        }

    }

    private fun initListener() {
        // 手动扫描
        mBinding.tvScannerMedia.setOnClickListener {
            ScannerConfigDialog.newInstance(false, this)
                .show(childFragmentManager, "config_scanner")
        }
        // 分享
        mBinding.tvShare.setOnClickListener { shareMe() }
        // 头像 、拍照
        mBinding.aboutHeaderIv.setOnClickListener {
            showTakePhotoDialog()
        }
        // 设置头像
        mCompositeDisposable.add(mBus.toObservableType(Constant.HEADER_PIC_URI, Uri::class.java)
            .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
            .subscribe { o: Uri -> setHeaderView(o) })

        mBinding.tvBackupsFavorite.setOnClickListener {
            backupsFavoriteList()
        }

        mBinding.tvRecoverFavorite.setOnClickListener {
            recoverFavoriteList()
        }
        mBinding.tvDeleteErrorLyric.setOnClickListener {
            clearErrorLyric()
        }

        mBinding.tvCrashLog.setOnClickListener {
            CrashSheetDialog.newInstance().getBottomDialog(mActivity)
        }

        mBinding.aboutHeaderIv.setOnLongClickListener {
            RelaxDialogFragment.newInstance().show(childFragmentManager, "girlsDialog")
            true
        }
        mBinding.musicBar.setClickListener(object : MusicToolBar.OnToolbarClickListener {
            override fun clickEdit() {

            }

            override fun switchMusicControlBar() {
                switchControlBar()
            }

            override fun clickDelete() {

            }
        })
        mBinding.tvSwitchLanguage.setOnClickListener {
            LogUtil.d(mTag, "切换语言")
            showLanguage()
        }

        mBinding.tvVersionName.setOnClickListener {
            VersionDialog.newInstance().show(childFragmentManager, "show_version")
//            startActivity(Intent(requireActivity(), TestActivity::class.java))
        }
    }

    private fun showLanguage() {

        val arr = arrayOf(getString(R.string.zh), getString(R.string.us))
        val dialog =
            AlertDialog.Builder(requireActivity()).setItems(arr) { _: DialogInterface?, i: Int ->
                when (i) {
                    0 -> switchLanguage("zh")
                    1 -> switchLanguage("en")
                }
            }.create()
        dialog.show()
    }


    private fun switchLanguage(language: String) {
        mSp.putValues(SpUtils.ContentValue(Constant.LANGUAGE, language))
        requireActivity().recreate()
    }


    /**
     * 设置头像入口：选图使用系统照片选择器，拍照委托系统相机，都不需要运行时权限
     */
    private fun showTakePhotoDialog() {
        TakePhotoBottomSheetDialog.newInstance()
            .getBottomDialog(mActivity, object : OnTakePhotoListener {
                override fun takePhoto() {
                    takePhotoByCamera()
                }

                override fun choicePhoto() {
                    choicePhotoFromGallery()
                }
            })
    }

    /**
     * 从相册选择图片，Android 13 及以上使用系统照片选择器，低版本自动回退
     */
    private fun choicePhotoFromGallery() {
        pickPhotoLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    private val pickPhotoLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        LogUtil.d(mTag, "相册选择结果   $uri")
        uri?.let { cropPhoto(it) }
    }

    /**
     * 拍照，图片输出到应用私有目录下的临时文件
     */
    private fun takePhotoByCamera() {
        if (!FileUtil.hasSdcard()) {
            ToastUtil.show(mActivity, "没发现SD卡!")
            return
        }
        mTakePhotoUri = FileUtil.getPicUri(requireContext())
        val uri = mTakePhotoUri
        if (uri == null) {
            ToastUtil.show(mActivity, "没发现SD卡!")
            return
        }
        takePhotoLauncher.launch(uri)
    }

    private val takePhotoLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        LogUtil.d(mTag, "拍照结果   $success")
        val uri = mTakePhotoUri
        if (success && uri != null) {
            cropPhoto(uri)
        }
    }

    /**
     * 裁剪图片，裁剪完成后通过 RxBus 通知刷新头像
     */
    private fun cropPhoto(uri: Uri) {
        mContentUri = uri
        mHeaderModified = FileUtil.getHeaderFile(requireContext()).lastModified()
        try {
            cropPhotoLauncher.launch(ImageUitl.cropRawPhotoIntent(requireContext(), uri))
        } catch (e: ActivityNotFoundException) {
            // 设备上没有可用的裁剪应用时，直接使用原图
            LogUtil.d(mTag, "没有可用的裁剪应用，直接使用原图")
            mBus.post(Constant.HEADER_PIC_URI, uri)
        }
    }

    private val cropPhotoLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        LogUtil.d(mTag, "裁剪结果   ${result.resultCode}")
        if (result.resultCode == Activity.RESULT_OK) {
            // 裁剪应用写出了新的头像文件就使用裁剪结果，否则退回使用原图
            val headerFile = FileUtil.getHeaderFile(requireContext())
            val uri = if (headerFile.lastModified() > mHeaderModified) {
                FileUtil.getHeaderUri(requireContext())
            } else {
                mContentUri
            }
            uri?.let { mBus.post(Constant.HEADER_PIC_URI, it) }
        }
    }

    private fun shareMe() {
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, mActivity.title)
        shareIntent.putExtra(Intent.EXTRA_TEXT, resources.getString(R.string.app_info))
        shareIntent.type = "text/plain"
        startActivity(shareIntent)
    }

    private var mCurrentPosition = 0
    private fun recoverFavoriteList() {
        val musicList = mMusicBeanDao.queryBuilder().list()
        if (FileUtil.getFavoriteFile()) {
            val songInfoMap = HashMap<String, String>(16)
            val stringSet = ReadFavoriteFileUtil.stringToSet()
            for (s in stringSet) {
                val songName = s.substring(0, s.lastIndexOf("T"))
                val favoriteTime = s.substring(s.lastIndexOf("T") + 1)
                songInfoMap[songName] = favoriteTime
            }
            mCompositeDisposable.add(Observable.fromIterable(musicList)
                .map { musicBean: MusicBean ->
                    //将歌名截取出来进行比较
                    val favoriteTime = songInfoMap[musicBean.title]
                    if (favoriteTime != null) {
                        musicBean.time = favoriteTime
                        musicBean.setIsFavorite(true)
                        mMusicBeanDao.update(musicBean)
                    }
                    mCurrentPosition++
                }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribe { currentPosition: Int ->
                    if (currentPosition == musicList.size - 1) {
                        if (mActivity is OnUpdateTitleListener) {
                            (mActivity as OnUpdateTitleListener).checkCurrentFavorite()
                        }
                    }
                })
        } else {
            ToastUtil.showNotFoundFavoriteFile(mActivity)
        }
    }

    private fun backupsFavoriteList() {
        val list =
            mMusicBeanDao.queryBuilder().where(MusicBeanDao.Properties.IsFavorite.eq(true)).build()
                .list()
        mCompositeDisposable.add(Observable.fromIterable(list).map { musicBean: MusicBean ->
            val songInfo = musicBean.title + "T" + musicBean.addTime
            ReadFavoriteFileUtil.writeFile(songInfo)
            songInfo
        }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
            .subscribe { favoriteName: String ->
                LogUtil.d(
                    mTag, " 更新本地收藏文件==========   $favoriteName"
                )
            })
        ToastUtil.showFavoriteListBackupsDown(mActivity)
    }

    private fun setHeaderView(uri: Uri) {
        var bitmap: Bitmap? = null
        try {
            bitmap = BitmapFactory.decodeStream(mActivity.contentResolver.openInputStream(uri))
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
        mBinding.aboutHeaderIv.setImageBitmap(bitmap)
    }

    private val mHandler = Handler(Looper.getMainLooper())
    private fun clearErrorLyric() {

        ThreadPoolProxyFactory.newInstance().execute {
            LyricsUtil.clearLyricList()
            mHandler.post { ToastUtil.show(mActivity, "错误歌词已删除") }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(): AboutFragment {
            return AboutFragment()
        }
    }

    override fun scanMusic(isAutoScan: Boolean) {
        LogUtil.d(mTag, "关于界面扫描   $isAutoScan")

    }

    override fun onDestroy() {
        super.onDestroy()
        mHandler.removeCallbacksAndMessages(null)
    }
}