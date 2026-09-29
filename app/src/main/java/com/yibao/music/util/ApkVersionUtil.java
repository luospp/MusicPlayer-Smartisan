package com.yibao.music.util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;

import androidx.core.content.FileProvider;

import java.io.File;

/**
 * @author luoshipeng
 * createDate：2019/6/5 0005 15:22
 * className   ApkVersionUtil
 * Des：TODO
 */
public class ApkVersionUtil {
    private static final String TAG = " ==== " + ApkVersionUtil.class.getSimpleName() + "  ";

    /**
     * @param mContext c
     * @return 获取当前本地apk的版本
     */
    public static int getVersionCode(Context mContext) {
        int versionCode = 0;
        try {
            //获取软件版本号，对应AndroidManifest.xml下android:versionCode
            versionCode = mContext.getPackageManager().
                    getPackageInfo(mContext.getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {

            LogUtil.d(TAG, e.getLocalizedMessage());
        }
        return versionCode;
    }

    /**
     * @param context 上下文
     * @return 获取版本号名称
     */
    public static String getVersionName(Context context) {
        String verName = "";
        try {
            verName = context.getPackageManager().
                    getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            LogUtil.d(TAG, e.getLocalizedMessage());
        }
        return verName;
    }

    /**
     * 提示安装
     *
     * @param context 上下文
     * @param files   apk安装文件
     */
    public static void installApk(Context context, File files) {
        LogUtil.d(TAG, "==  开始安装     ");
        Intent intent = new Intent(Intent.ACTION_VIEW);
        //版本在7.0以上是不能直接通过uri访问的
        // 由于没有在Activity环境下启动Activity,设置下面的标签
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        //参数1:上下文, 参数2:Provider主机地址 和配置文件中保持一致,参数3:共享的文件
        Uri apkUri = FileProvider.getUriForFile(context, FileUtil.getFileProviderAuthority(context), files);
        LogUtil.d(TAG, "=  7.0 Url   " + apkUri);
        //添加这一句表示对目标应用临时授权该Uri所代表的文件
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
        context.startActivity(intent);
    }

}
