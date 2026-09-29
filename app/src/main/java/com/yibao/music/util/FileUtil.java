package com.yibao.music.util;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.Environment;

import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import com.yibao.music.MusicApplication;
import com.yibao.music.model.MusicBean;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;


/**
 * 描述：${文件操作工具类}
 * 邮箱：strangermy@outlook.com
 *
 * @author Luoshipeng
 */

public class FileUtil {
    private static final String TAG = "====" + FileUtil.class.getSimpleName() + "    ";

    public static boolean getFavoriteFile() {
        return VersionUtil.checkAndroidVersionQ() ? FileUtil.isAndroidQFileExists(Constant.FAVORITE_FILE) : new File(Constant.FAVORITE_FILE).exists();

    }


    public static long getId(String str) {
        //        String str="2017-06-12T10:22:59.890Z";
        return Long.parseLong(str.substring(11, 19)
                .replaceAll(":", ""));
    }

    /**
     * 歌曲是否从qq音乐下载过专辑图片
     *
     * @param imageType 1 歌曲图片 、2 歌手图片 、3 专辑图片 、4 通知栏图片
     * @param songName  s
     * @param artist    a
     * @return b
     */
    private static boolean albumFileExists(int imageType, String songName, String artist) {

        String albumPath = imageType == 1
                ? Constant.getMusicSongAlbumRoot() + songName + ".jpg" : imageType == 2
                ? Constant.MUSIC_ARTIST_IMG_ROOT + artist + ".jpg" : Constant.MUSIC_ALBUM_ROOT + artist + ".jpg";
        File file = new File(albumPath);
        return file.exists();
    }

    public static String getAlbumUrl(MusicBean bean, int imageType) {
        boolean b = FileUtil.albumFileExists(imageType, bean.getTitle(), bean.getArtist());
        return b ? StringUtil.getDownAlbum(bean.getTitle(), bean.getArtist()) : StringUtil.getAlbum(2, bean.getAlbumId(), bean.getArtist());
    }

    public static String getNotifyAlbumUrl(Context context, MusicBean bean) {
        boolean b = FileUtil.albumFileExists(1, bean.getTitle(), bean.getArtist());
        return b ? StringUtil.getDownAlbum(bean.getTitle(), bean.getArtist()) : StringUtil.getAlbumArtPath(context, String.valueOf(bean.getAlbumId()));
    }

    public static boolean hasSdcard() {
        String state = Environment.getExternalStorageState();
        // 有存储的SDCard
        return state.equals(Environment.MEDIA_MOUNTED);
    }

    /**
     * 头像文件，保存在应用私有目录下(Android 10 起公共目录不可直接读写)
     */
    public static File getHeaderFile(Context context) {
        File dir = context.getExternalFilesDir(Constant.PHOTO_DIR);
        if (dir != null && !dir.exists()) {
            dir.mkdirs();
        }
        return new File(dir, Constant.CROP_IMAGE_FILE_NAME);
    }

    /**
     * 头像文件的 Uri，用于把裁剪结果授权给裁剪应用
     *
     * @return 外部存储不可用时返回 null
     */
    @Nullable
    public static Uri getHeaderUri(Context context) {
        File headerFile = getHeaderFile(context);
        if (headerFile.getParentFile() == null) {
            return null;
        }
        return FileProvider.getUriForFile(context, getFileProviderAuthority(context), headerFile);
    }

    /**
     * FileProvider 的 authority，必须与 AndroidManifest 中的 authorities 保持一致
     */
    public static String getFileProviderAuthority(Context context) {
        return context.getPackageName() + ".fileprovider";
    }

    public static File createFile(Context context, String fileName, String dirPath) {
        String apkFilePath = context.getExternalFilesDir(dirPath).getAbsolutePath();
        return new File(apkFilePath + File.separator + fileName);
    }

    /**
     * 拍照输出文件(临时头像)的 Uri，文件保存在应用私有目录下
     *
     * @param context c
     * @return 外部存储不可用时返回 null
     */
    @Nullable
    public static Uri getPicUri(Context context) {
        File dir = context.getExternalFilesDir(Constant.PHOTO_DIR);
        if (dir == null) {
            return null;
        }
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File pictureFile = new File(dir, Constant.IMAGE_FILE_NAME);
        return FileProvider.getUriForFile(context, getFileProviderAuthority(context), pictureFile);
    }

    public static void deleteFileDirectory(File dir) {
        if (dir.exists()) {
            File[] files = dir.listFiles();
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteFile(file);
                } else {
                    file.delete();
                }
            }
            dir.delete();
        }
    }

    public static void deleteFile(File file) {
        if (file.exists()) {
            file.delete();
        }
    }

    /**
     * 歌词文件
     *
     * @param songName 歌名
     * @param artist   歌手
     * @return file
     */
    public static File getLyricsFile(String songName, String artist) {
        String lyricsName = songName + "$$" + artist + ".lrc";
        if (VersionUtil.checkAndroidVersionQ()) {
            String apkFilePath = Objects.requireNonNull(MusicApplication.getInstance().getExternalFilesDir(Constant.MUSIC_LYRICS_DIR)).getAbsolutePath();
            return new File(apkFilePath + File.separator + lyricsName);
        } else {
            File file = new File(Constant.MUSIC_LYRICS_ROOT);
            if (!file.exists()) {
                boolean mkdirs = file.mkdirs();
            }
            return new File(file.getAbsolutePath() + lyricsName);

        }
    }

    public static File getLyricsDir() {
        if (VersionUtil.checkAndroidVersionQ()) {
            String apkFilePath = MusicApplication.getInstance().getExternalFilesDir(Constant.MUSIC_LYRICS_DIR).getAbsolutePath();
            return new File(apkFilePath + File.separator);
        } else {
            return new File(Constant.MUSIC_LYRICS_ROOT);

        }
    }


    /**
     * 崩溃文件
     *
     * @return file
     */
    public static File getCrashFile() {
        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:sss", Locale.getDefault()).format(new Date(System.currentTimeMillis()));
        if (VersionUtil.checkAndroidVersionQ()) {
            String apkFilePath = MusicApplication.getInstance().getExternalFilesDir(Constant.CRASH_DIR).getAbsolutePath();
            return new File(apkFilePath + File.separator + time + ".txt");
        } else {
            File file = new File(Constant.CRASH_LOG_PATH);
            return new File(file.getAbsolutePath() + Constant.CRASH_DIR + time + ".txt");

        }
    }

    public static File getCrashDir() {
        if (VersionUtil.checkAndroidVersionQ()) {
            String apkFilePath = MusicApplication.getInstance().getExternalFilesDir(Constant.CRASH_DIR).getAbsolutePath();
            return new File(apkFilePath + File.separator);
        } else {
            return new File(Constant.CRASH_LOG_PATH);

        }
    }


    public static boolean isAndroidQFileExists(String path) {
        AssetFileDescriptor afd = null;
        ContentResolver cr = MusicApplication.getInstance().getContentResolver();
        try {
            Uri uri = Uri.parse(path);
            afd = cr.openAssetFileDescriptor(uri, "r");
            if (afd == null) {
                return false;
            } else {
                afd.close();
            }
        } catch (FileNotFoundException e) {
            return false;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }


}