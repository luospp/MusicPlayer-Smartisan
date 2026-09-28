package com.yibao.music.fragment.dialogfrag

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import com.yibao.music.R
import com.yibao.music.activity.MusicActivity
import com.yibao.music.base.bindings.BaseBindingDialog
import com.yibao.music.base.listener.OnScanConfigListener
import com.yibao.music.databinding.ScannerConfigDialogBinding
import com.yibao.music.util.Constant
import com.yibao.music.util.SpUtils

/**
 * Author：Sid
 * Des：${删除列表}
 * Time:2017/5/31 18:37
 *
 * @author Stran
 */
class ScannerConfigDialog : BaseBindingDialog<ScannerConfigDialogBinding>(), View.OnClickListener {
    override fun initData() {
        initView()
    }

    override fun onResume() {
        super.onResume()
        val window = dialog!!.window
        isCancelable = false
        dialog!!.setCanceledOnTouchOutside(false)
        if (window != null) {
            window.setGravity(Gravity.CENTER)
            window.setWindowAnimations(R.style.Theme_AppCompat_Dialog_Alert)
        }
    }

    override fun initListener() {
        mBinding.tvScannerCancel.setOnClickListener(this)
        mBinding.tvScannerContinue.setOnClickListener(this)
        mBinding.cbSize.setOnClickListener(this)
        mBinding.cbDuration.setOnClickListener(this)
    }

    private fun initView() {

        val aBooleanFileSize = mSp.getBoolean(Constant.MUSIC_FILE_SIZE_FLAG, false)
        val aBooleanDuration = mSp.getBoolean(Constant.MUSIC_DURATION_FLAG, false)
        mBinding.cbSize.isChecked = aBooleanFileSize
        mBinding.cbDuration.isChecked = aBooleanDuration
    }

    override fun onClick(v: View) {
        val isAutoFlag = requireArguments().getBoolean(Constant.LOAD_FLAG)
        val id = v.id
        if (id == R.id.tv_scanner_cancel) {
            // 自动扫描，点击取消扫描音乐
            if (isAutoFlag) {
                getScanListener()?.scanMusic(true)
            }
            dismiss()
        } else if (id == R.id.tv_scanner_continue) {
            mSp.putValues(
                SpUtils.ContentValue(Constant.MUSIC_FILE_SIZE_FLAG, mBinding.cbSize.isChecked),
                SpUtils.ContentValue(Constant.MUSIC_DURATION_FLAG, mBinding.cbDuration.isChecked)
            )
            if (isAutoFlag) {
                getScanListener()?.scanMusic(true)
            } else {
                val intent = Intent(activity, MusicActivity::class.java)
                intent.putExtra(Constant.SCANNER_MEDIA, true)
                startActivity(intent)
            }
            dismiss()
        }
    }

    companion object {
        private val TAG = " ==== " + ScannerConfigDialog::class.java.simpleName + "  "

        /**
         * @param loadFlag true 自动扫描 、 false 手动扫描
         * @return ScannerConfigDialog
         */
        @JvmStatic
        fun newInstance(loadFlag: Boolean, listener: OnScanConfigListener): ScannerConfigDialog {
            val dialog = ScannerConfigDialog()
            val bundle = Bundle()
            bundle.putBoolean(Constant.LOAD_FLAG, loadFlag)
            dialog.arguments = bundle
            return dialog
        }
    }

    /**
     * 通过 parentFragment 或 Activity 查找回调接口，避免静态字段在进程重建后丢失。
     */
    private fun getScanListener(): OnScanConfigListener? {
        val parent = parentFragment
        if (parent is OnScanConfigListener) return parent
        val act = activity
        if (act is OnScanConfigListener) return act
        return null
    }
}