package com.yibao.music.fragment.dialogfrag

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.RatingBar
import android.widget.RatingBar.OnRatingBarChangeListener
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.yibao.music.MusicApplication
import com.yibao.music.adapter.MoreMenuAdapter
import com.yibao.music.databinding.BottomMoreMenuDialogBinding
import com.yibao.music.model.MoreMenuStatus
import com.yibao.music.model.MusicBean
import com.yibao.music.model.greendao.MusicBeanDao
import com.yibao.music.util.Constant
import com.yibao.music.util.MenuListUtil
import com.yibao.music.util.RxBus
import com.yibao.music.util.SpUtils
import com.yibao.music.util.SpUtils.ContentValue

/**
 * Des：${TODO}
 * Time:2017/8/22 14:11
 *
 * @author Stran
 */
class MoreMenuBottomDialog {


    private var mMenuAdapter: MoreMenuAdapter? = null
    private var mContext: Context? = null

    private lateinit var mBinding: BottomMoreMenuDialogBinding
    private lateinit var mDialog: BottomSheetDialog

    fun getBottomDialog(context: Context) {
        mContext = context
        mBinding = BottomMoreMenuDialogBinding.inflate(LayoutInflater.from(context), null, false)
        mDialog = BottomSheetDialog(context)
        init()
        initData()
        initListener()
        mDialog.show()
    }

    private fun init() {
        mDialog.setContentView(mBinding.root)
        mDialog.setCancelable(true)
        mDialog.setCanceledOnTouchOutside(true)

        mBinding.ratingBar.visibility = if (mIsNeedScore) View.VISIBLE else View.GONE
        mBinding.bottomTitle.visibility = if (mIsNeedScore) View.GONE else View.VISIBLE

    }

    private fun initListener() {
        mBinding.bottomSheetCancel.setOnClickListener { mDialog.dismiss() }
        mMenuAdapter!!.setClickListener { _: Int, position: Int, musicBean: MusicBean? ->
            if (position == Constant.NUMBER_ZERO) {
                val sp = SpUtils(MusicApplication.getInstance(), Constant.MUSIC_CONFIG)
                sp.putValues(ContentValue(Constant.ADD_TO_PLAY_LIST_FLAG, Constant.NUMBER_ONE))
            }
            RxBus.getInstance().post(MoreMenuStatus(mMusicPosition, position, musicBean))
            mDialog.dismiss()
        }
        mBinding.ratingBar.onRatingBarChangeListener =
            OnRatingBarChangeListener { _: RatingBar?, rating: Float, _: Boolean ->
                mMusicBean!!.songScore = rating.toInt()
                musicDao!!.update(mMusicBean)
            }

    }


    private fun initData() {

        mBinding.ratingBar.rating = mMusicBean!!.songScore.toFloat()

        val manager = GridLayoutManager(mContext, 4)
        manager.orientation = GridLayoutManager.VERTICAL
        mBinding.recyclerMoreMenu.layoutManager = manager
        mMenuAdapter = MoreMenuAdapter(
            MenuListUtil.getMenuData(mMusicBean!!.isFavorite(), mIsNeedSetTime),
            mMusicBean,
            mMusicPosition
        )
        mBinding.recyclerMoreMenu.adapter = mMenuAdapter

    }


    // 实例字段取代 companion object 静态字段，避免进程重建后数据丢失
    private var musicDao: MusicBeanDao? = null
    private var mMusicBean: MusicBean? = null
    private var mMusicPosition = 0
    private var mIsNeedScore = false
    private var mIsNeedSetTime = false

    companion object {
        @JvmStatic
        fun newInstance(
            musicBean: MusicBean?,
            musicPosition: Int,
            isNeedScore: Boolean,
            isNeedSetTime: Boolean
        ): MoreMenuBottomDialog {
            val dialog = MoreMenuBottomDialog()
            dialog.mMusicBean = musicBean
            dialog.mMusicPosition = musicPosition
            dialog.mIsNeedScore = isNeedScore
            dialog.mIsNeedSetTime = isNeedSetTime
            dialog.musicDao = MusicApplication.getInstance().musicDao
            return dialog
        }
    }
}