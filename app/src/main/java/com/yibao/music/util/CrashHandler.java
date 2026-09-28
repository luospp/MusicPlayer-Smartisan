package com.yibao.music.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.Process;

import androidx.annotation.NonNull;

import com.baidu.mobstat.StatService;
import com.yibao.music.MusicApplication;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Author：luoshipeng
 * Des：${异常处理器}
 * Time:2017/9/2 19:58
 */
public class CrashHandler
        implements Thread.UncaughtExceptionHandler {


    private static final String TAG = "CrashHandler";
    private static final String FILE_NAME = "crash";
    private static final String FILE_NAME_SUFFIX = ".txt";
    private Context mContext;
    private Thread.UncaughtExceptionHandler mDefaultCrashHandler;


    private static CrashHandler sInstance;
    
    public static synchronized CrashHandler getInstance() {
        if (sInstance == null) {
            sInstance = new CrashHandler();
        }
        return sInstance;
    }

    public void init() {
        mDefaultCrashHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
        mContext = MusicApplication.getInstance().getApplicationContext();
    }

    @Override
    public void uncaughtException(@NonNull Thread thread, @NonNull Throwable ex) {
        try {

            dumpExceptionToSdCard(ex);  //导出异常信息到SD卡中

            uploadExceptionToServer(ex);  //将异常信息上传到百度服务器

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        if (mDefaultCrashHandler != null) {
            mDefaultCrashHandler.uncaughtException(thread, ex);
        } else {
            Process.killProcess(Process.myPid());
        }
    }

    private void uploadExceptionToServer(Throwable ex) {
        //将异常信息上传到服务器
        StatService.recordException(mContext, ex);
    }

    private void dumpExceptionToSdCard(Throwable ex)
            throws PackageManager.NameNotFoundException {
        SpUtils sp = new SpUtils(MusicApplication.getInstance(), Constant.MUSIC_CONFIG);
        sp.putValues(new SpUtils.ContentValue(Constant.MUSIC_PLAY_STATE, 1));
        if (!Environment.getExternalStorageState()
                .equals(Environment.MEDIA_MOUNTED)) {
            if (MusicApplication.isShowLog) {
                LogUtil.d(TAG, "sdcard unmounted,skip dump exception");
                return;
            }
        }

        long current = System.currentTimeMillis();
        String time = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss_SSS", Locale.getDefault()).format(new Date(current));

        File exceptionFile;
        if (VersionUtil.checkAndroidVersionQ()) {
            String fileName = time + FILE_NAME_SUFFIX;
            exceptionFile = FileUtil.createFile(mContext, fileName, Constant.CRASH_DIR);
        } else {
            File dir = new File(Constant.CRASH_LOG_PATH);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            exceptionFile = new File(Constant.CRASH_LOG_PATH + FILE_NAME + time + FILE_NAME_SUFFIX);
        }
        try {
            PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(exceptionFile)));
            pw.println(time);
            dumpPhoneInfo(pw);
            pw.println();
            ex.printStackTrace(pw);
            pw.close();

        } catch (IOException e) {
            e.printStackTrace();
            LogUtil.d(TAG, "dump crash info failed");
        }
    }

    private void dumpPhoneInfo(PrintWriter pw)
            throws PackageManager.NameNotFoundException {
        PackageManager pm = mContext.getPackageManager();
        PackageInfo pi = pm.getPackageInfo(mContext.getPackageName(),
                PackageManager.GET_ACTIVITIES);
        pw.print("App Version: ");
        pw.print(pi.versionName);
        pw.print('_');
        pw.println(pi.versionCode);
        //安卓版本号
        pw.print("OS Version: ");
        pw.print(Build.VERSION.RELEASE);
        pw.print("_");
        pw.println(Build.VERSION.SDK_INT);
        //手机制造厂
        pw.print("Vendor: ");
        pw.println(Build.MANUFACTURER);
        //手机型号
        pw.print("Model: ");
        pw.println(Build.MODEL);
        //CPU架构
        pw.print("CPU ABI: ");
        pw.print(Build.CPU_ABI);


    }


}
