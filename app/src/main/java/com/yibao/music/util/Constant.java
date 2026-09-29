package com.yibao.music.util;

import android.os.Environment;

import com.yibao.music.MusicApplication;

/**
 * 作者：Stran on 2017/3/23 15:26
 * 描述：${常量类}
 * 邮箱：strangermy@outlook.com
 *
 * @author Stran
 */
public class Constant {
    /**
     * 广播匹配
     */
    public final static String NOTIFY_BUTTON_ID = "notify_btn_id";
    public final static String ACTION_MUSIC = "com.yibao.music.action.MUSIC_CONTROL";

    /**
     * 音乐通知栏
     */
    public static final int FAVORITE = 0;
    public static final int PREV = 1;
    public static final int PLAY = 2;
    public static final int NEXT = 3;
    public static final int CLOSE = 4;


    /**
     * 倒计时结束，广播通知暂停播放音乐
     */
    public static final int COUNTDOWN_FINISH = 5;

    public static final int NUMBER_ZERO = 0;
    public static final int NUMBER_ONE = 1;
    public static final int NUMBER_TWO = 2;
    public static final int NUMBER_THREE = 3;
    public static final int NUMBER_FOUR = 4;
    public static final int NUMBER_FIVE = 5;
    public static final int NUMBER_SIX = 6;
    public static final int NUMBER_SEVEN = 7;
    public static final int NUMBER_EIGHT = 8;
    public static final int NUMBER_NINE = 9;
    public static final int NUMBER_TEN = 10;

    public static final int SEARCH_SONG_FOR_NAME = 11;
    public static final int SEARCH_SONG_FOR_ALBUM = 12;
    public static final int SEARCH_SONG_FOR_ARTIST = 13;
    public static final int SEARCH_SONG_FOR_ALL = 14;
    // RxBus post  flag


    public static final String SONG_FAG_EDIT = "song_edit";
    public static final String FAVORITE_POSITION = "favorite_position";
    public static final String POSITION = "position";
    /**
     * 0 表示是通知栏控制播放和暂停
     * 1 表示在通知栏收藏音乐
     * 2 在通知栏关闭通知栏
     */
    public static final String PLAY_STATUS = "play_status";
    public static final String HEADER_PIC_URI = "header_uri";

    public static final String CONDITION = "condition";


    static final char LETTER_A = 'A';
    static final char LETTER_Z = 'Z';
    public static final char LETTER_HASH = '#';
    public static final String SONG_NAME = "song_name";
    public static final String SONG_ARTIST = "song_artist";
    public static final String TIME_SERVICE_NAME = "com.yibao.music.service.CountdownService";
    public static final String LOAD_SERVICE_NAME = "com.yibao.music.service.LoadMusicDataService";
    public static final String NULL_STRING = "";
    public static final String SERVICE_MUSIC = "service_music";
    /**
     * 歌词下载结果通知
     */
    public static final String MUSIC_LYRIC_OK = "lyric_ok";
    public static final String MUSIC_LYRIC_FAIL = "lyric_fail";
    public static final String PURE_MUSIC = "此歌曲为没有填词的纯音乐，请您欣赏";
    public static final String NO_LYRICS = "暂无歌词";
    public static final String NO_NETWORK = "网络异常,稍后重试!";


    public static final String PLAY_LIST_BACK_FLAG = "LSP_98";


    public static final String MUSIC_SETTING = "artist_music_setting";

    public static final String MUSIC_LOAD = "music_load";


    /**
     * 用于存储和获取音乐的播放模式
     * PLAY_MODE
     * 0 全部  1 单曲   2 随机
     */
    public static final String PLAY_MODE = "play_mode";

    /**
     * 音乐的播放状态
     */
    public static final String MUSIC_PLAY_STATUS = "music_play-status";

    /**
     * 用于标记详情页面是否打开
     * <
     * 0 在PlayListFragment弹出新建AddListDialog和删除DeleteDialog 时赋值为 0 。
     * 1  点击MoreMenuDialog-->添加到播放列表-->PlayListActivity弹出新建AddListDialog  时赋值为 1 。
     */
    public static final String ADD_TO_PLAY_LIST_FLAG = "detail_flag";

    /**
     * 用于存储程序退出或关闭音乐界面时，音乐列表的分类标记，。
     * 1 歌曲名   2  评分   3  播放次数    4  添加时间    8 收藏    10:  专辑 歌手等精确条件
     */
    public static final String MUSIC_DATA_FLAG = "music_data_flag";

    public static final String PIC_URL_FLAG = "pic_url_flag";


    /**
     * 用于存储程序退出或关闭音乐界面时，音乐播放的位置。
     */
    public static final String MUSIC_POSITION = "music_position";

    /**
     * 用于存储退出程序或关闭音乐界面时，音乐的播放状态 。 1：表示暂停时关闭 ， 2：表示播放时关闭
     */
    public static final String MUSIC_PLAY_STATE = "music_play_state";

    /**
     * 音频焦点管理
     */
    public static final String MUSIC_FOCUS = "music_focus";

    // 显示播放小喇叭
    public static final String MUSIC_SPEAKER = "music_speaker";


    /**
     * 用于存储和获取用户是否有播放记录
     */
    public static final String MUSIC_INIT_FLAG = "music_init";


    public static final String MUSIC_DURATION_FLAG = "music_duration_flag";
    public static final String MUSIC_FILE_SIZE_FLAG = "music_file_size_flag";

    public static final String MUSIC_CONFIG = "music_config";


    public static final String SCANNER_MEDIA = "scanner";
    public static final String ADD_TO_LIST = "add2List";

    public static final String COUNTDOWN_TIME = "countdown_time";
    public static final String FINISH_TIME = "00:01";
    public static String MUSIC_LYRICS_DIR = "lyrics";
    public static String CRASH_DIR = "crash";
    public static String LOAD_FLAG = "load_flag";
    public static String AUTO_LOAD = "auto_load";
    /**
     * 页面标识:  1 歌曲名列表、2 评分、3 播放次数、4 添加时间、5 自定义播放列表详情 、6 艺术家列表、7 专辑列表、
     *           8 收藏列表、    【  搜索类别：  11歌曲 、 12专辑 、 13 艺术家 、14 全部 】
     */
    public static String PAGE_TYPE = "page_type";

    public static String MUSIC_LYRICS_ROOT = Environment.getExternalStorageDirectory().getAbsolutePath() + "/smartisan/music/lyrics/";


    /**
     * 延迟获取专辑缓存目录，避免类加载时依赖 Application 实例（第三方 SDK 的 ContentProvider
     * 可能在 Application.onCreate() 之前触发类加载，导致 NPE → ExceptionInInitializerError）。
     */
    private static String sSongAlbumRoot;
    public static String getMusicSongAlbumRoot() {
        if (sSongAlbumRoot == null) {
            java.io.File dir = MusicApplication.getInstance().getExternalFilesDir("songAlbum");
            sSongAlbumRoot = (dir != null ? dir.getAbsolutePath() : ".") + "/";
        }
        return sSongAlbumRoot;
    }
    public static String MUSIC_ALBUM_ROOT = Environment.getExternalStorageDirectory().getAbsolutePath() + "/smartisan/music/album/";
    public static String MUSIC_ARTIST_IMG_ROOT = Environment.getExternalStorageDirectory().getAbsolutePath() + "/smartisan/music/artistImage/";
    static final String FAVORITE_FILE = Environment.getExternalStorageDirectory().getAbsolutePath() + "/smartisan/music/favorite.txt/";
    // 崩溃日志本地保存地址
    public static final String CRASH_LOG_PATH = Environment.getExternalStorageDirectory().getPath() + "/CrashLog/log/";
    public static final String DATA_TYPE_TXT = "text/plain";
    public static String UNSPLASH_URL = "https://picsum.photos/1080/1920/?image=";
    // 头像文件(应用私有目录下的子目录)
    public static final String PHOTO_DIR = "photo";
    public static final String IMAGE_FILE_NAME = "temp_head_image.jpg";
    public static final String CROP_IMAGE_FILE_NAME = "header.jpg";
    public static final String MQMS2 = "[mqms2]";
    /**
     * 选择歌词完成
     */
    public static final int SELECT_LYRICS = 22;
    public static final String SONGMID = "song_mid";

    public static final String DELETE_SONG = "delete_song";
    public static final String SONG_LYRICS = "lyrics";
    public static final String MUSIC_BEAN = "musicBean";


    public static final String PERMISSION_HINT = "permission_hint";

    public static final String LANGUAGE = "language";
}
