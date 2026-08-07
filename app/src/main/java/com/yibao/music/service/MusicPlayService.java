package com.yibao.music.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.PlaybackParams;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.provider.MediaStore;

import com.yibao.music.MusicApplication;
import com.yibao.music.R;
import com.yibao.music.manager.MediaSessionManager;
import com.yibao.music.manager.MusicNotifyManager;
import com.yibao.music.model.MusicBean;
import com.yibao.music.model.greendao.MusicBeanDao;
import com.yibao.music.network.QqMusicRemote;
import com.yibao.music.util.Constant;
import com.yibao.music.util.LogUtil;
import com.yibao.music.util.LyricsUtil;
import com.yibao.music.util.NetworkUtil;
import com.yibao.music.util.QueryMusicFlagListUtil;
import com.yibao.music.util.ReadFavoriteFileUtil;
import com.yibao.music.util.RxBus;
import com.yibao.music.util.SpUtils;
import com.yibao.music.util.StringUtil;
import com.yibao.music.util.ThreadPoolProxyFactory;
import com.yibao.music.util.VersionUtil;

import java.util.List;
import java.util.Random;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;


/**
 * @author Stran
 * Des：${控制音乐的Service}
 * Time:2017/5/30 13:27
 */
public class MusicPlayService extends Service {
    private static final String TAG = "====" + MusicPlayService.class.getSimpleName() + "    ";
    private MediaPlayer mediaPlayer;
    private AudioBinder mAudioBinder;
    private SpUtils mSp;

    // 播放位置
    private int playPosition = 0;
    // 播放模式
    private int playMode = 0;

    /**
     * 三种播放模式
     */
    public static final int PLAY_MODE_ALL = 0;
    public static final int PLAY_MODE_SINGLE = 1;
    public static final int PLAY_MODE_RANDOM = 2;
    private List<MusicBean> mMusicDataList;
    private MusicBroadcastReceiver mMusicReceiver;
    private MusicBeanDao mMusicDao;
    private RxBus mBus;
    private Disposable mDisposable;
    private AudioManager mAudioManager;
    private MediaSessionManager mSessionManager;


    @Override
    public IBinder onBind(Intent intent) {
        return mAudioBinder;
    }

    @Override
    public void unbindService(ServiceConnection conn) {
        super.unbindService(conn);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        init();
        initNotifyBroadcast();
        registerHeadsetReceiver();
    }

    private void init() {
        mAudioBinder = new AudioBinder();
        mBus = RxBus.getInstance();
        mSp = new SpUtils(getApplication(), Constant.MUSIC_CONFIG);
        mMusicDao = MusicApplication.getInstance().getMusicDao();
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        //初始化播放模式
        playMode = mSp.getInt(Constant.PLAY_MODE);
        mSessionManager = new MediaSessionManager(this, mAudioBinder);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        playPosition = intent.getIntExtra(Constant.POSITION, 0);
        int pageType = intent.getIntExtra(Constant.PAGE_TYPE, 0);
        String condition = intent.getStringExtra(Constant.CONDITION);
        // 当前的页面标识，如果和传递的pageType一样，就不用重新查询音乐列表数据，否则重新查询音乐列表数据。
        int cPageType = mSp.getInt(Constant.PAGE_TYPE);
        LogUtil.d(TAG, "当前标识：   ===cPageType===   " + cPageType);
        LogUtil.d(TAG, "播放位置 ：" + playPosition + "  页面标识 ： " + pageType + "  条件Key ：" + condition);
        if (cPageType != pageType || pageType == 0 || mMusicDataList == null || mMusicDataList.isEmpty()) {
            // 播放列表数据
            mMusicDataList = QueryMusicFlagListUtil.getMusicDataList(mMusicDao.queryBuilder(), pageType, condition);
            // 保存页面标识
            mSp.putValues(new SpUtils.ContentValue(Constant.PAGE_TYPE, pageType));
            LogUtil.d(TAG, "重新加载音乐列表数据： " + mMusicDataList.size());
        } else {
            LogUtil.d(TAG, "不需要重新加载音乐列表数据： " + mMusicDataList.size());
        }

        LogUtil.d(TAG, "当前列表长度： " + mMusicDataList.size());
        // 保存关键字
        if (condition != null) {
            mSp.putValues(new SpUtils.ContentValue(Constant.CONDITION, condition));
        }

        LogUtil.d(TAG, " 播放位置== " + playPosition);
        //执行播放
        mAudioBinder.play();
        //通知播放界面更新
        sendCurrentMusicInfo();
        if (mMusicDataList != null && !mMusicDataList.isEmpty()) {
            MusicBean musicBean = mMusicDataList.get(playPosition);
            LogUtil.d(TAG, " 当前播放信息  ==  " + musicBean.getTitle());
            musicBean.setPlayFrequency(musicBean.getPlayFrequency() + 1);
            mMusicDao.update(musicBean);
        }
        return START_NOT_STICKY;
    }


    /**
     * 通知播放界面更新
     */
    private void sendCurrentMusicInfo() {
        if (mMusicDataList != null && playPosition < mMusicDataList.size()) {
            MusicBean musicBean = mMusicDataList.get(playPosition);
            musicBean.setCureetPosition(playPosition);
            mBus.post(Constant.SERVICE_MUSIC, musicBean);
        }
    }


    public class AudioBinder extends Binder implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener {
        private MusicBean mMusicInfo;
        private MusicNotifyManager mNotifyManager;

        private void play() {

            if (mediaPlayer != null) {
                mediaPlayer.reset();
                mediaPlayer.release();
                mediaPlayer = null;
            }
            // “>=” 确保模糊搜索时播放不出现索引越界
            if (mMusicDataList != null && !mMusicDataList.isEmpty()) {
                playPosition = playPosition >= mMusicDataList.size() ? 0 : playPosition;
                mMusicInfo = mMusicDataList.get(playPosition);
                mediaPlayer = MediaPlayer.create(MusicPlayService.this, getSongFileUri());
                mediaPlayer.setOnPreparedListener(this);
                mediaPlayer.setOnCompletionListener(this);
                String songName = StringUtil.getSongName(mMusicInfo.getTitle());
                String artist = StringUtil.getArtist(mMusicInfo.getArtist());
                boolean lyricIsExists = LyricsUtil.checkLyricFile(songName, artist);
                if (!lyricIsExists && NetworkUtil.isNetworkConnected()) {
                    // 自动下载歌词
                    QqMusicRemote.getSongLyrics(songName, artist);
                }
                mSp.putValues(new SpUtils.ContentValue(Constant.MUSIC_POSITION, playPosition));

                mSp.putValues(new SpUtils.ContentValue(Constant.SONG_NAME, songName));
                showNotification(true);
                mSessionManager.updatePlaybackState(true);
                mSessionManager.updateLocMsg();
                postSpeakerState(playPosition, true);
            }

        }

        // 更新小喇叭
        private void postSpeakerState(int position, boolean isPlay) {
            mSp.putValues(new SpUtils.ContentValue(Constant.MUSIC_PLAY_STATUS, isPlay));
            mBus.post(Constant.MUSIC_SPEAKER, String.valueOf(position));
        }

        private void showNotification(boolean b) {
            mNotifyManager = new MusicNotifyManager(getApplication(), mMusicInfo, b);
            mNotifyManager.show();
        }

        public void updateFavorite() {
            if (playPosition < mMusicDataList.size()) {
                MusicBean musicBean = mMusicDataList.get(playPosition);
                boolean favorite = mMusicDao.load(musicBean.getId()).getIsFavorite();
                mNotifyManager.updateFavoriteBtn(favorite);
                ThreadPoolProxyFactory.newInstance().execute(() -> {
                    refreshFavorite(musicBean, favorite);
                    // 更新本地收藏文件
                    updateFavoriteFile(musicBean, favorite);
                });
            }
        }

        private void hintNotification() {
            if (mNotifyManager != null) {
                mNotifyManager.hide();
            }
        }

        public MusicBean getMusicBean() {
            return mMusicInfo;
        }
        // 准备完成回调

        @Override
        public void onPrepared(MediaPlayer mediaPlayer) {
            // 开启播放
            mediaPlayer.start();
            // 通知播放界面更新
            sendCurrentMusicInfo();
        }

        // 获取当前播放进度
        public int getProgress() {
            return mediaPlayer.getCurrentPosition();
        }

        // 获取音乐总时长
        public int getDuration() {
            return mediaPlayer.getDuration();
        }

        // 音乐播放完成监听
        @Override
        public void onCompletion(MediaPlayer mediaPlayer) {
            // 自动播放下一首歌曲
            autoPlayNext();
        }

        // 自动播放下一曲
        private void autoPlayNext() {
            // 单曲循环不改变playPosition ,不用判断，只判断 随机播放 和 循环所有 两种模式。
            switch (playMode) {
                case PLAY_MODE_ALL:
                    playPosition = playPosition == mMusicDataList.size() - 1 ? 0 : playPosition + 1;
                    break;
                case PLAY_MODE_RANDOM:
                    playPosition = new Random().nextInt(Math.abs(mMusicDataList.size()));

                    break;
                default:
                    break;
            }
            play();
        }

        // 获取当前的播放模式

        public int getPlayMode() {
            return playMode;
        }

        //设置播放模式

        public void setPlayMode(int mode) {
            playMode = mode;
            //保存播放模式
            mSp.putValues(new SpUtils.ContentValue(Constant.PLAY_MODE, playMode));

        }

        //手动播放上一曲

        public void playPre() {
            if (playMode == PLAY_MODE_RANDOM) {
                playPosition = new Random().nextInt(Math.abs(mMusicDataList.size()));
            } else {
                playPosition = playPosition == 0 ? mMusicDataList.size() - 1 : playPosition - 1;

            }
            play();
        }

        // 手动播放下一曲

        public void playNext() {
            if (playMode == PLAY_MODE_RANDOM) {
                playPosition = new Random().nextInt(mMusicDataList.size());
            } else {
                playPosition = playPosition == mMusicDataList.size() - 1 ? 0 : playPosition + 1;
            }
            play();
        }

        //true 当前正在播放

        public boolean isPlaying() {
            return mediaPlayer.isPlaying();

        }

        public void start() {
            mediaPlayer.start();
            mSessionManager.updatePlaybackState(true);
            showNotification(true);
            initAudioFocus();
            postSpeakerState(playPosition, true);
        }

        // 改变播放倍速实现搓碟效果
        public void setPlaybackParams(PlaybackParams params) {

            if (mediaPlayer.isPlaying()) {
                mediaPlayer.setPlaybackParams(params);
            }
        }


        // 暂停播放
        public void pause() {
            mediaPlayer.pause();
            mSessionManager.updatePlaybackState(false);
            showNotification(false);
            postSpeakerState(playPosition, false);
        }

        // 跳转到指定位置进行播放
        public void seekTo(int progress) {
            mediaPlayer.seekTo(progress);
        }

        public List<MusicBean> getMusicList() {
            return mMusicDataList;
        }

        public int getPosition() {
            return playPosition;
        }

        public void updateFavorite(MusicBean bean) {
            bean.setIsFavorite(!bean.isFavorite());
            bean.setTime(StringUtil.getTime());
            mMusicDao.update(bean);
        }

        private Uri getSongFileUri() {
            int songId = mMusicInfo.getId().intValue();
            return VersionUtil.checkAndroidVersionQ() ? ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songId) : Uri.parse(mMusicInfo.getSongUrl());
        }

    }


    private void refreshFavorite(MusicBean currentMusicBean, boolean mCurrentIsFavorite) {
        // 数据更新
        currentMusicBean.setIsFavorite(!mCurrentIsFavorite);
        if (!mCurrentIsFavorite) {
            currentMusicBean.setTime(StringUtil.getTime());
        }
        mMusicDao.update(currentMusicBean);
    }

    private void updateFavoriteFile(MusicBean musicBean, boolean currentIsFavorite) {
        if (currentIsFavorite) {
            mDisposable = ReadFavoriteFileUtil.deleteFavorite(musicBean.getTitle()).observeOn(AndroidSchedulers.mainThread()).subscribe(aBoolean -> {
                if (!aBoolean) {
//                    ToastUtil.show(this, getResources().getString(R.string.song_not_favorite));
                    LogUtil.d(TAG, getResources().getString(R.string.song_not_favorite));
                }
            });
        } else {
            //更新收藏文件  将歌名和收藏时间拼接储存，恢复的时候，歌名和时间以“T”为标记进行截取
            String songInfo = musicBean.getTitle() + "T" + musicBean.getTime();
            ReadFavoriteFileUtil.writeFile(songInfo);

        }

    }

    /**
     * 控制通知栏的广播
     */
    private void initNotifyBroadcast() {
        mMusicReceiver = new MusicBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constant.ACTION_MUSIC);
        registerReceiver(mMusicReceiver, filter);

    }

    private class MusicBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                if (action.equals(Constant.ACTION_MUSIC)) {
                    int id = intent.getIntExtra(Constant.NOTIFY_BUTTON_ID, 0);
                    switch (id) {
                        case Constant.FAVORITE:
                            mAudioBinder.updateFavorite();
                            mBus.post(Constant.PLAY_STATUS, Constant.NUMBER_ONE);
                            break;
                        case Constant.CLOSE:
                        case Constant.COUNTDOWN_FINISH:
                            pauseMusic();
                            break;
                        case Constant.PREV:
                            mAudioBinder.playPre();
                            break;
                        case Constant.PLAY:
                            if (mediaPlayer != null) {
                                if (mAudioBinder.isPlaying()) {
                                    mAudioBinder.pause();
                                } else {
                                    mAudioBinder.start();
                                }
                                mBus.post(Constant.PLAY_STATUS, Constant.NUMBER_ZERO);
                            }
                            break;
                        case Constant.NEXT:
                            mAudioBinder.playNext();
                            break;
                        default:
                            break;
                    }
                }
            }
        }

        private void pauseMusic() {
            if (mAudioBinder != null) {
                mAudioBinder.pause();
                mAudioBinder.hintNotification();
                mBus.post(Constant.PLAY_STATUS, Constant.NUMBER_TWO);
                mAudioBinder.postSpeakerState(mAudioBinder.getPosition(), false);
                stopSelf();
            }
        }
    }

    /**
     * 耳机插入和拔出监听广播
     */
    private void registerHeadsetReceiver() {
        IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(headsetReceiver, intentFilter);
    }

    BroadcastReceiver headsetReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mAudioBinder != null && mAudioBinder.isPlaying()) {
                mAudioBinder.pause();
                mBus.post(Constant.PLAY_STATUS, Constant.NUMBER_ZERO);
            }
        }
    };

    /**
     * 音频焦点
     */
    private void initAudioFocus() {
        // 申请焦点
        if (mAudioManager != null) {
            mAudioManager.requestAudioFocus(mAudioFocusChange, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        }
    }

    private final AudioManager.OnAudioFocusChangeListener mAudioFocusChange = focusChange -> {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_LOSS:
                // 长时间丢失焦点,会触发此回调事件，(QQ音乐，网易云音乐)，需要暂停音乐播放，避免和其他音乐同时输出声音
                lossAudioFocus(true);
                // 若焦点释放掉之后，将不会再自动获得
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                // 短暂性丢失焦点，例如播放微博短视频，拨打电话等，暂停音乐播放。
                lossAudioFocus(true);
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // 短暂性丢失焦点并作降音处理
                LogUtil.d(TAG, "====== 短暂性丢失焦点并作降音处理 ");
                break;
            case AudioManager.AUDIOFOCUS_GAIN:
                // 当其他应用申请焦点之后又释放焦点会触发此回调,可重新播放音乐
                lossAudioFocus(false);
                break;
            default:
                break;
        }
    };

    /**
     * 是否失去焦点
     *
     * @param isLossFocus b
     */
    private void lossAudioFocus(boolean isLossFocus) {
        if (isLossFocus) {
            mAudioBinder.pause();
            mSp.putValues(new SpUtils.ContentValue(Constant.MUSIC_FOCUS, true));

        } else {
            mAudioBinder.start();
        }
        mBus.post(Constant.PLAY_STATUS, Constant.NUMBER_ZERO);
    }

    public void abandonAudioFocus() {
        if (mAudioManager != null) {
            mAudioManager.abandonAudioFocus(mAudioFocusChange);

        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mAudioBinder != null) {
            mAudioBinder.hintNotification();
        }
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (mMusicReceiver != null) {
            unregisterReceiver(mMusicReceiver);
        }
        if (headsetReceiver != null) {
            unregisterReceiver(headsetReceiver);
        }
        if (mDisposable != null) {
            mDisposable.dispose();
            mDisposable = null;
        }
        abandonAudioFocus();
        mSessionManager.release();
    }
}
