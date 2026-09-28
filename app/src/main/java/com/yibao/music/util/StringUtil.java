package com.yibao.music.util;

import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import com.yibao.music.MusicApplication;
import com.yibao.music.R;
import com.yibao.music.model.MusicBean;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Des：${歌曲的时间格式处理}
 * Time:2017/8/12 18:39
 *
 * @author Stran
 */
public class StringUtil {
    private static final int HOUR = 60 * 60 * 1000;
    private static final int MIN = 60 * 1000;
    private static final int SEC = 1000;
    private static String TAG = "StringUtil";


    /**
     * 解析歌词时间
     *
     * @param duration
     * @return
     */
    @SuppressLint("DefaultLocale")
    public static String parseDuration(int duration) {
        int hour = duration / HOUR;
        int min = duration % HOUR / MIN;
        int sec = duration % MIN / SEC;
        if (hour == 0) {

            return String.format("%02d:%02d", min, sec);
        } else {
            return String.format("%02d:%02d:%02d", hour, min, sec);
        }
    }

    public static Long stringToLong(String str) {
        if (str != null) {
            return Long.valueOf(str.replaceAll("[^\\d]+", ""));

        }
        return 1L;
    }

    /**
     * 返回本地歌手图片、专辑图片地址，通过glide 存到本地的图片。
     *
     * @param picType 1 歌手图片 、 2 专辑图片
     * @param albumId a
     * @return f
     */
    public static String getAlbum(int picType, Long albumId, String artist) {
        String path = picType == 1 ? Constant.MUSIC_ARTIST_IMG_ROOT + artist + ".jpg" : Constant.MUSIC_ALBUM_ROOT + artist + ".jpg";
        File file = new File(path);

        return file.exists() ? path : ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"),
                Math.abs(albumId)).toString();

    }

    /**
     * 返回下载保存到本地的专辑图片路径
     *
     * @param songName a
     * @return f
     */
    public static String getDownAlbum(String songName, String artist) {

        return Constant.getMusicSongAlbumRoot() + songName + ".jpg";

    }


    /**
     * @param context c
     * @param albumId id
     * @return 根据专辑 id 获得专辑图片保存路径,通知栏使用。
     */
    public static synchronized String getAlbumArtPath(Context context, String albumId) {

        if (!StringUtil.isReal(albumId)) {
            return null;
        }

        String[] projection = {MediaStore.Audio.Albums.ALBUM_ART};
        String replaceAlbum = albumId.replace("-", "");
        String imagePath = null;
        Uri uri = Uri.parse("content://media" + MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI.getPath() + "/" + replaceAlbum);

        Cursor cur = context.getContentResolver().query(uri, projection, null, null, null);
        if (cur == null) {
            return null;
        }

        if (cur.getCount() > 0 && cur.getColumnCount() > 0) {
            cur.moveToNext();
            imagePath = cur.getString(0);
        }
        cur.close();
        return imagePath;
    }

    /**
     * 返回一个标准的时间格式   yyyy-MM-dd HH:mm:ss
     *
     * @return
     */
    public static String getCurrentTime() {
        long time = System.currentTimeMillis();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        Date date = new Date(time);
        return format.format(date);

    }

    public static String getFormatDate(Long l) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        Date date = new Date(l);
        return format.format(date);

    }

    public static String getTitle(MusicBean musicBean) {
        String musicTitle = musicBean.getTitle();
        return musicTitle.contains("[mqms2]")
                ? TitleArtistUtil.getBean(musicTitle).getSongName()
                : musicTitle;
    }

    public static String getArtist(MusicBean musicBean) {
        String musicTitle = musicBean.getTitle();
        String musicArtist = musicBean.getArtist();
        return musicTitle.contains("[mqms2]")
                ? TitleArtistUtil.getBean(musicTitle).getSongArtist()
                : "<unknown>".equals(musicArtist) || "群星".equals(musicArtist)
                ? "Smartisan" : musicArtist;
    }

    public static String getSongName(String songName) {
        return songName.contains("[mqms2]") ? songName.substring(songName.lastIndexOf("-") + 2, songName.lastIndexOf("[mqms2]") - 1) : songName;
    }

    public static String getArtist(String singerName) {
        String unknownName = "<unknown>";
        return unknownName.equals(singerName) || "群星".equals(singerName) ? "Smartisan" : singerName;

    }

    public static String getBottomSheetTitle(int size) {

        return "收藏列表 ( " + size + " )";
    }

    public static boolean isReal(String string) {
        return string != null && string.length() > 0 && !"null".equals(string);
    }

    public static String getTime() {
        return String.valueOf(System.currentTimeMillis());
    }

    public static String idToString(Context context, int resourcesId) {
        return context.getResources().getString(resourcesId);
    }

    public static long getSetCountdown(String countdown) {
        int mCountdownTime = 0;
        if (MusicApplication.getInstance().getString(R.string.counting_down).equals(countdown)) {
            mCountdownTime = -1;
        } else if (MusicApplication.getInstance().getString(R.string.no_set_up).equals(countdown)) {
            LogUtil.d(TAG, "     时间为无");
        } else if (MusicApplication.getInstance().getString(R.string.fifteen_minute).equals(countdown)) {
            mCountdownTime = 15 * 60 * 1000;
        } else if (MusicApplication.getInstance().getString(R.string.thirty_minute).equals(countdown)) {
            mCountdownTime = 30 * 60 * 1000;
        } else if (MusicApplication.getInstance().getString(R.string.an_hour).equals(countdown)) {
            mCountdownTime = 60 * 60 * 1000;
        } else if (MusicApplication.getInstance().getString(R.string.one_and_a_half_hours).equals(countdown)) {
            mCountdownTime = 90 * 60 * 1000;
        } else if (MusicApplication.getInstance().getString(R.string.two_hours).equals(countdown)) {
            mCountdownTime = 120 * 60 * 1000;
        }
        return mCountdownTime;
    }
}
