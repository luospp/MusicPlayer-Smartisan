package com.yibao.music.fragment.dialogfrag

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.yibao.music.MusicApplication
import com.yibao.music.R
import com.yibao.music.adapter.CrashAdapter
import com.yibao.music.databinding.CrashBottomSheetDialogBinding
import com.yibao.music.util.Constant
import com.yibao.music.util.FileUtil
import java.io.File

/**
 * Des：${TODO}
 * Time:2017/8/22 14:11
 *
 * @author Stran
 */
class CrashSheetDialog {
    private var mContext: Context? = null
    private lateinit var mBinding: CrashBottomSheetDialogBinding
    private lateinit var mDialog: BottomSheetDialog

    fun getBottomDialog(context: Context?) {
        mContext = context
        mBinding = CrashBottomSheetDialogBinding.inflate(LayoutInflater.from(context), null, false)
        mDialog = BottomSheetDialog(context!!)
        mDialog.setContentView(mBinding.rootCrash)
        mDialog.setCancelable(true)
        mDialog.setCanceledOnTouchOutside(true)
        init()
        mDialog.show()
    }

    private fun init() {
        initRecyclerView(mBinding.recyclerCrashLog)
        // 读取CrashLog
        val crashDir = FileUtil.getCrashDir()
        if (crashDir.exists()) {
            val array = crashDir.listFiles().apply {
                this?.reverse()
            }
            val crashAdapter = CrashAdapter(array)
            mBinding.recyclerCrashLog.adapter = crashAdapter
            crashAdapter.setItemClickListener { crashFile: File -> openCrashLog(crashFile) }
            mBinding.tvCrashTitle.setOnClickListener { backTop(mBinding.recyclerCrashLog) }
            mBinding.tvCrashTitle.setOnLongClickListener {
                FileUtil.deleteFileDirectory(crashDir)
                mDialog.dismiss()
                true
            }
        }

    }


    private fun openCrashLog(crashFile: File) {
        val packageName = mContext!!.packageName
        val intent = Intent()
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.action = Intent.ACTION_VIEW
        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        val contentUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(mContext!!, FileUtil.getFileProviderAuthority(mContext!!), crashFile)
        } else {
            Uri.fromFile(crashFile)
        }
        // 默认用WPS打开日志
        if (isInstallWps()) {
            val bundle = Bundle()
            bundle.putString(WpsModel.OPEN_MODE, WpsModel.READ_MODE)
            bundle.putBoolean(WpsModel.SEND_CLOSE_BROAD, false)
            bundle.putString(WpsModel.THIRD_PACKAGE, packageName)
            bundle.putBoolean(WpsModel.CLEAR_TRACE, true)
            // bundle.putBoolean(CLEAR_FILE, true); //关闭后删除打开文件
            intent.setClassName(WpsModel.NORMAL_PACKAGE, WpsModel.NORMAL)
            intent.setDataAndType(contentUri, "application/vnd.android.package-archive")
            intent.putExtras(bundle)
        } else {
            intent.setDataAndType(contentUri, Constant.DATA_TYPE_TXT)
        }
        mContext!!.startActivity(intent)
    }

    // 获取所有已安装程序的包信息
    private fun isInstallWps(): Boolean {
        val wpsPackageName = "cn.wps.moffice_eng"
        val packageManager = mContext!!.packageManager
        // 获取所有已安装程序的包信息
        val info = packageManager.getInstalledPackages(0)
        for (i in info.indices) {
            if (info[i].packageName == wpsPackageName) {
                return true
            }
        }
        return false
    }

    private fun backTop(recyclerView: RecyclerView) {
        val adapter = recyclerView.adapter as CrashAdapter?
        val manager = recyclerView.layoutManager as LinearLayoutManager?
        if (adapter != null && manager != null) {
            val positionForSection = adapter.getPositionForSection(0)
            manager.scrollToPositionWithOffset(positionForSection, 0)
        }
    }

    private fun initRecyclerView(recyclerView: RecyclerView) {
        val manager = LinearLayoutManager(MusicApplication.getInstance())
        manager.orientation = LinearLayoutManager.VERTICAL
        recyclerView.isVerticalScrollBarEnabled = true
        recyclerView.layoutManager = manager
        val divider = DividerItemDecoration(mContext, DividerItemDecoration.VERTICAL)
        ContextCompat.getDrawable(mContext!!, R.drawable.shape_item_decoration)
            ?.let { divider.setDrawable(it) }
        recyclerView.addItemDecoration(divider)
    }

    private object WpsModel {
        const val OPEN_MODE = "OpenMode"
        const val SEND_CLOSE_BROAD = "SendCloseBroad"
        const val THIRD_PACKAGE = "ThirdPackage"
        const val CLEAR_TRACE = "ClearTrace"
        const val READ_MODE = "ReadMode"
        const val NORMAL = "cn.wps.moffice.documentmanager.PreStartActivity2"
        const val NORMAL_PACKAGE = "cn.wps.moffice_eng"
    }

    companion object {
        fun newInstance(): CrashSheetDialog {
            return CrashSheetDialog()
        }
    }
}