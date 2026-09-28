package com.yibao.music.util;

import android.app.PendingIntent;
import android.os.Build;

/**
 * @author luoshipeng
 * createDate：2020/2/28 0028 17:18
 * className   BuildVersionUtil
 * Des：TODO
 */
public class VersionUtil {

    public static boolean checkAndroidVersionQ() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q;
    }

    public static boolean checkAndroidVersionS() {
        return Build.VERSION.SDK_INT > Build.VERSION_CODES.S_V2;
    }

    /**
     *  通知标识 适配 android 31
     * @return
     */
    public static int getNotifyFlag() {
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return flags;
    }
}
