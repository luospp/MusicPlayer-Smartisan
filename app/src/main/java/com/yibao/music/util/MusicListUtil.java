package com.yibao.music.util;

import android.database.Cursor;
import android.provider.MediaStore;

import com.yibao.music.MusicApplication;
import com.yibao.music.model.AlbumInfo;
import com.yibao.music.model.ArtistInfo;
import com.yibao.music.model.MusicBean;
import com.yibao.music.model.greendao.MusicBeanDao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.schedulers.Schedulers;

/**
 * Des：${ 获取手机音乐数据 }
 * Time:2017/9/3 14:38
 *
 * @author Stran
 */
public class MusicListUtil {
    private static final String TAG = "====" + MusicListUtil.class.getSimpleName() + "    ";
    /**
     * 过虑掉 文件大小 小于 1M 的音乐文件
     */
    private static final long CONFIG_MUSIC_FILE_SIZE = 1048576;
    /**
     * 过虑掉 音乐时长小于 10800 一分钟  21600 两分钟 的音乐文件
     */
    private static final long CONFIG_MUSIC_DURATION = 10800;

    /**
     * 从本地获取歌曲的信息，保存在List当中
     *
     * @return d
     */
    public static List<MusicBean> getMusicDataList() {
        SpUtils sp = new SpUtils(MusicApplication.getInstance(), Constant.MUSIC_SETTING);
        boolean aBooleanDuration = sp.getBoolean(Constant.MUSIC_DURATION_FLAG, false);
        boolean aBooleanFileSize = sp.getBoolean(Constant.MUSIC_FILE_SIZE_FLAG, false);
        List<MusicBean> musicInfo = new ArrayList<>();
        Cursor cursor = MusicApplication.getInstance().getApplicationContext().getContentResolver()
                .query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        null,
                        null,
                        null,
                        MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
        if (cursor != null) {
            int musicId = cursor.getColumnIndex(MediaStore.Audio.Media._ID);
            int mTitle = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int mArtist = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            int mAlbum = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
            int mAlbumId = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);
            int mDuration = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
            int mSize = cursor.getColumnIndex(MediaStore.Audio.Media.SIZE);
            int mUrl = cursor.getColumnIndex(MediaStore.Audio.Media.DATA);
            int addDed = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED);
            int qualityType = cursor.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE);
            int issueYear = cursor.getColumnIndex(MediaStore.Audio.Media.YEAR);
            for (int i = 0, p = cursor.getCount(); i < p; i++) {
                cursor.moveToNext();
                MusicBean info = new MusicBean();
                // 音乐id
                long mId = cursor.getLong(musicId);
                // 音乐标题
                String title = cursor.getString(mTitle);
                // 艺术家
                String artist = cursor.getString(mArtist);
                // 专辑
                String album = cursor.getString(mAlbum);
                long albumId = cursor.getInt(mAlbumId);
                // 时长
                long duration = cursor.getInt(mDuration);
                // 添加时间
                int addTime = (int) cursor.getLong(addDed);
                // 文件大小
                long size = cursor.getLong(mSize);
                // 文件路径
                String url = cursor.getString(mUrl);
                if (aBooleanDuration && aBooleanFileSize) {
                    if (size > CONFIG_MUSIC_FILE_SIZE && duration > CONFIG_MUSIC_DURATION) {
                        addMusicData(musicInfo, qualityType, issueYear, info, mId, title, artist, album, albumId, duration, addTime, url);
                    }

                } else if (aBooleanDuration) {
                    if (duration > CONFIG_MUSIC_DURATION) {
                        addMusicData(musicInfo, qualityType, issueYear, info, mId, title, artist, album, albumId, duration, addTime, url);
                    } else {
                        LogUtil.d(TAG, "little time  " + duration);
                    }
                } else if (aBooleanFileSize) {
                    if (size > CONFIG_MUSIC_FILE_SIZE) {
                        addMusicData(musicInfo, qualityType, issueYear, info, mId, title, artist, album, albumId, duration, addTime, url);
                    }
                } else {
                    addMusicData(musicInfo, qualityType, issueYear, info, mId, title, artist, album, albumId, duration, addTime, url);

                }

            }
            cursor.close();
        }
        LogUtil.d(TAG, "歌曲数量 ========== " + musicInfo.size());
        return musicInfo;
    }

    private static void addMusicData(List<MusicBean> musicInfo, int qualityType, int issueYear, MusicBean info, long mId, String title, String artist, String album, long albumId, long duration, int addTime, String url) {
        String firstChar = String.valueOf(HanziToPinyins.stringToPinyinSpecial(title));
        info.setMusicQualityType(qualityType);
        info.setFirstChar(firstChar);
        info.setId(mId);
        info.setTitle(title);
        info.setArtist(artist);
        info.setAlbum(album);
        info.setAlbumId(albumId);
        info.setDuration(duration);
        info.setAddTime(addTime);
        info.setSongUrl(url);
        info.setIssueYear(issueYear);
        musicInfo.add(info);
    }


    /**
     * 按ABCD 首字母排序
     *
     * @param musicList d
     */
    public static List<MusicBean> sortByAbc(List<MusicBean> musicList) {
        musicList.sort(MusicListUtil::sortAbc);
        return musicList;
    }

    /**
     * 排序比较器：# 号分组始终排在末尾，其余按字母序升序。
     * 此前当 m2 为 "#" 时恒返回 -1，违反反对称性，导致排序结果不确定。
     */
    private static int sortAbc(MusicBean m1, MusicBean m2) {
        String c1 = m1.getFirstChar();
        String c2 = m2.getFirstChar();
        boolean is1Hash = "#".equals(c1);
        boolean is2Hash = "#".equals(c2);
        if (is1Hash && is2Hash) return 0;
        if (is1Hash) return 1;
        if (is2Hash) return -1;
        return c1.compareTo(c2);
    }


    //  按艺术家分类

    public static List<ArtistInfo> getArtistList(List<MusicBean> list) {
        Map<String, List<MusicBean>> musicMap = new HashMap<>(16);
        ArrayList<ArtistInfo> singerInfoList = new ArrayList<>();

        for (MusicBean musicBean : list) {
            forArtistList(musicBean, musicMap);
        }
        for (Map.Entry<String, List<MusicBean>> entry : musicMap.entrySet()) {
            forArtistMap(singerInfoList, entry.getKey(), entry.getValue());
        }


        Collections.sort(singerInfoList);
        return singerInfoList;
    }

    private static void forArtistMap(ArrayList<ArtistInfo> singerInfoList, String s, List<MusicBean> musicBeanList) {
        ArtistInfo artistInfo = new ArtistInfo();
        artistInfo.setArtist(s);
        artistInfo.setSongCount(musicBeanList.size());
        artistInfo.setAlbumName(musicBeanList.get(0).getAlbum());
        artistInfo.setYear(musicBeanList.get(0).getIssueYear());
        artistInfo.setAlbumId(musicBeanList.get(0).getAlbumId());
        String firstChar = String.valueOf(HanziToPinyins.stringToPinyinSpecial(s));
        artistInfo.setFirstChar(firstChar);
        singerInfoList.add(artistInfo);
    }

    private static void forArtistList(MusicBean musicInfo, Map<String, List<MusicBean>> musicMap) {
        if (musicMap.containsKey(musicInfo.getArtist())) {
            ArrayList<MusicBean> singerList = (ArrayList<MusicBean>) musicMap.get(musicInfo.getArtist());
            if (singerList != null) {
                singerList.add(musicInfo);
            }
        } else {
            ArrayList<MusicBean> tempList = new ArrayList<>();
            tempList.add(musicInfo);
            musicMap.put(musicInfo.getArtist(), tempList);

        }
    }


    // 按专辑分组

    public static List<AlbumInfo> getAlbumList(List<MusicBean> list) {
        Map<String, List<MusicBean>> musicMap = new HashMap<>(16);
        List<AlbumInfo> albumInfoList = new ArrayList<>();

        for (MusicBean musicInfo : list) {
            forAlbumList(musicMap, musicInfo);
        }
        for (Map.Entry<String, List<MusicBean>> entry : musicMap.entrySet()) {
            forAlbumMap(albumInfoList, entry.getKey(), entry.getValue());
        }


        Collections.sort(albumInfoList);
        return albumInfoList;
    }

    private static void forAlbumMap(List<AlbumInfo> albumInfoList, String s, List<MusicBean> musicBeanList) {
        AlbumInfo albumInfo = new AlbumInfo();
        albumInfo.setAlbumName(s);
        albumInfo.setArtist(musicBeanList.get(0).getArtist());
        albumInfo.setAlbumId(musicBeanList.get(0).getAlbumId());
        albumInfo.setSongName(musicBeanList.get(0).getTitle());
        albumInfo.setYear(musicBeanList.get(0).getIssueYear());
        albumInfo.setAlbumId(musicBeanList.get(0).getAlbumId());
        albumInfo.setSongCount(musicBeanList.size());
        String firstChar = String.valueOf(HanziToPinyins.stringToPinyinSpecial(s));
        albumInfo.setFirstChar(firstChar);
        albumInfoList.add(albumInfo);
    }

    private static void forAlbumList(Map<String, List<MusicBean>> musicMap, MusicBean musicInfo) {
        if (musicMap.containsKey(musicInfo.getAlbum())) {
            ArrayList<MusicBean> albumList = (ArrayList<MusicBean>) musicMap.get(musicInfo.getAlbum());
            if (albumList != null) {
                albumList.add(musicInfo);
            }
        } else {
            ArrayList<MusicBean> tempList = new ArrayList<>();
            tempList.add(musicInfo);
            musicMap.put(musicInfo.getAlbum(), tempList);
        }
    }

    public static Observable<List<MusicBean>> getFavoriteList() {
        return Observable.create((ObservableOnSubscribe<List<MusicBean>>) emitter -> {
            List<MusicBean> musicBeanList = MusicApplication.getInstance()
                    .getMusicDao().queryBuilder()
                    .where(MusicBeanDao.Properties.IsFavorite.eq(true)).build().list();
            Collections.sort(musicBeanList);
            emitter.onNext(musicBeanList);
            emitter.onComplete();
        }).subscribeOn(Schedulers.io());
    }
}
