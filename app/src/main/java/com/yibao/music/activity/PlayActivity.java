package com.yibao.music.activity;

import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.media.AudioManager;
import android.media.PlaybackParams;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.SeekBar;

import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.yibao.music.R;
import com.yibao.music.base.BasePlayActivity;
import com.yibao.music.base.listener.OnDiscTouchListener;
import com.yibao.music.base.listener.StylusState;
import com.yibao.music.databinding.PlayActivityBinding;
import com.yibao.music.fragment.dialogfrag.CountdownBottomSheetDialog;
import com.yibao.music.fragment.dialogfrag.FavoriteBottomSheetDialog;
import com.yibao.music.fragment.dialogfrag.MoreMenuBottomDialog;
import com.yibao.music.fragment.dialogfrag.PreviewBigPicDialogFragment;
import com.yibao.music.model.MoreMenuStatus;
import com.yibao.music.model.MusicBean;
import com.yibao.music.model.MusicLyricBean;
import com.yibao.music.network.QqMusicRemote;
import com.yibao.music.util.Constant;
import com.yibao.music.util.FileUtil;
import com.yibao.music.util.ImageUitl;
import com.yibao.music.util.LogUtil;
import com.yibao.music.util.LyricsUtil;
import com.yibao.music.util.SnakbarUtil;
import com.yibao.music.util.StringUtil;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;


/**
 * @项目名： ArtisanMusic
 * @包名： com.yibao.music.activity
 * @文件名: PlayActivity
 * @author: lsp
 * @创建时间: 2018/2/17 20:39
 * @描述： {TODO}
 */

public class PlayActivity extends BasePlayActivity implements View.OnClickListener {
    private PlayActivityBinding mBinding;
    private int mDuration;
    private MusicBean mCurrentMusicInfo;
    boolean isShowLyrics = false;

    private Disposable mCloseLyrDisposable;
    private List<MusicLyricBean> mLyricList;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = PlayActivityBinding.inflate(getLayoutInflater());
        View rootView = mBinding.getRoot();
        setContentView(rootView);
        init();
        initSongInfo();
        initListener();

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mCurrentMusicInfo != null && audioBinder != null) {
            checkCurrentIsFavorite(mMusicDao.load(mCurrentMusicInfo.getId()).isFavorite());
            updateCurrentPlayInfo(audioBinder.getMusicBean());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isShowLyrics) {
            showLyrics();
        }
        disposableLyricsView();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBinding.rotateRl.clearAnimation();
        mHandler.removeCallbacksAndMessages(null);
    }


    private void initSongInfo() {
        mCurrentMusicInfo = audioBinder != null ? audioBinder.getMusicBean() : getIntent().getParcelableExtra("currentBean");
        if (mCurrentMusicInfo != null) {
            setTitleAndArtist(mCurrentMusicInfo);
            setAlbum(FileUtil.getAlbumUrl(mCurrentMusicInfo, 1));
        }
    }

    public void checkCurrentIsFavorite(boolean cureentMusicIsFavorite) {
//        mBinding.ivFavoriteMusic.setImageResource(cureentMusicIsFavorite ? R.drawable.favorite_yes : R.drawable.music_qqbar_favorite_normal_selector);
        mBinding.ivFavoriteMusic.setImageResource(cureentMusicIsFavorite ? R.drawable.btn_favorite_red_selector : R.drawable.btn_favorite_gray_selector);
    }

    private void init() {
        // 初始化旋转动画
        mBinding.rotateRl.initAutoRotation();
        if (audioBinder != null) {
            setSongDuration();
            if (audioBinder.isPlaying()) {
                updatePlayBtnStatus();
            }

        }

        //设置播放模式图片
        int mode = mSps.getInt(Constant.PLAY_MODE);
        updatePlayModeImage(mode, mBinding.musicPlayerMode);
        //音量设置
        mBinding.sbVolume.setMax(mMaxVolume);
        updateMusicVolume(mVolume);
    }

    private void updateMusicVolume(int volume) {
        mBinding.sbVolume.setProgress(volume);
        // 更新音量值  flag 0 默认不显示系统控制栏  1 显示系统音量控制
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
    }

    @Override
    protected void moreMenu(MoreMenuStatus moreMenuStatus) {
        super.moreMenu(moreMenuStatus);
        switch (moreMenuStatus.getPosition()) {
            case Constant.NUMBER_ZERO:
                startPlayListActivity(mCurrentMusicInfo.getTitle());
                break;
            case Constant.NUMBER_ONE:
                SnakbarUtil.keepGoing(mBinding.musicPlayerNext);
                break;
            case Constant.NUMBER_TWO:
                if (audioBinder != null) {
                    if (audioBinder.getPosition() == moreMenuStatus.getMusicPosition()) {
                        audioBinder.updateFavorite();
                        checkCurrentIsFavorite(getFavoriteState(mCurrentMusicInfo));
                    }
                } else {
                    SnakbarUtil.firstPlayMusic(mBinding.playingSongAlbum);
                }

                break;
            case Constant.NUMBER_THREE:
                showLyrics();
                break;
            case Constant.NUMBER_FOUR:
                CountdownBottomSheetDialog.newInstance().getBottomDialog(this);
                break;
            case Constant.NUMBER_FIVE:
                audioBinder.playNext();
                String songUrl = mCurrentMusicInfo.getSongUrl();
                // 先从本地数据库删除歌曲，再彻底删除歌曲文件。
                mMusicDao.delete(mCurrentMusicInfo);
                FileUtil.deleteFile(new File(songUrl));
                break;
            default:
                break;
        }
    }

    @Override
    protected void updateCurrentPlayInfo(MusicBean musicBean) {
        mCurrentMusicInfo = musicBean;
        checkCurrentIsFavorite(mCurrentMusicInfo.isFavorite());
        initAnimation();
        setTitleAndArtist(musicBean);
        setAlbum(FileUtil.getAlbumUrl(musicBean, 1));
        setSongDuration();
        updatePlayBtnStatus();
        // 设置当前歌词
        mLyricList = LyricsUtil.getLyricList(musicBean);
        String msg = mLyricList.size() > 1 ? Constant.MUSIC_LYRIC_OK : Constant.PURE_MUSIC;
        mBinding.lyricsView.setLrcFile(mLyricList, msg);
        if (isShowLyrics) {
            startRollPlayLyrics(mBinding.lyricsView);
            closeLyricsView();
            mBinding.groupBrightDelete.setVisibility(mLyricList.size() > 2 ? View.VISIBLE : View.INVISIBLE);

        }
    }

    private void setTitleAndArtist(MusicBean bean) {
        mBinding.tvSongName.setText(StringUtil.getSongName(bean.getTitle()));
        mBinding.tvArtistName.setText(StringUtil.getArtist(bean.getArtist()));
    }

    /**
     * MusicPlayService发送的，RxBus接收歌曲时时的进度 和 时间，并更新UI
     */
    @Override
    protected void updateCurrentPlayProgress() {
        if (audioBinder != null && audioBinder.isPlaying()) {
            updateMusicProgress(audioBinder.getProgress());
            // 实时更新唱针
            updateStylus();
        }
    }


    private void updateMusicProgress(int progress) {
        // 时间进度
        mBinding.startTime.setText(StringUtil.parseDuration(progress));
        // 时时播放进度
        mBinding.sbProgress.setProgress(progress);
        // 歌曲总时长递减
        mBinding.endTime.setText(StringUtil.parseDuration(mDuration - progress));
    }

    //  根据播放进度实时更新唱针角度
    private void updateStylus() {
        if (!mBinding.stylusContainer.isUserTouching() && audioBinder.isPlaying()) {
            float current = audioBinder.getProgress();
            float duration = audioBinder.getDuration();
            // 确保结果在 0.0 ~ 1.0 之间
            float progress = duration > 0f ? current / duration : 0f;
//            LogUtil.d(TAG, "实时角度  " + audioBinder.isPlaying() + "---" + mBinding.stylusContainer.isUserTouching() + "  -------- " + current + "/ " + duration + " == " + progress);
            mBinding.stylusContainer.updateProgress(progress, false);
        }
    }

    private void setSongDuration() {
        // 获取并记录总时长
        mDuration = audioBinder.getDuration();
        // 设置进度条的总进度
        mBinding.sbProgress.setMax(mDuration);
        // 设置歌曲总时长
        mBinding.endTime.setText(StringUtil.parseDuration(mDuration));
    }


    private void disposableLyricsView() {
        if (mCloseLyrDisposable != null) {
            mCloseLyrDisposable.dispose();
            mCloseLyrDisposable = null;
        }
//        clearDisposableLyric();
    }

    private void setAlbum(String url) {
        try {
            ImageUitl.loadPic(this, url, mBinding.playingSongAlbum, R.drawable.playing_cover_lp, isSuccess -> {
                if (isSuccess) {
                    showAlbum(true);
                } else {
                    QqMusicRemote.getSongImg(PlayActivity.this, mCurrentMusicInfo.getTitle(), url1 -> {
                        if (url1 == null) {
                            showAlbum(false);
                        } else {
                            Glide.with(PlayActivity.this).load(url1).placeholder(R.drawable.playing_cover_lp).error(R.drawable.playing_cover_lp).into(mBinding.playingSongAlbum);
                            showAlbum(true);
                        }
                    });


                }
            });
        } catch (Exception e) {
            LogUtil.d(TAG, e.getMessage());
        }


    }

    private void showAlbum(boolean b) {
        mBinding.playingSongAlbum.setVisibility(b ? View.VISIBLE : View.INVISIBLE);
        mBinding.albumCover.setVisibility(b ? View.GONE : View.VISIBLE);
    }

    private void switchPlayState(boolean isPlaying) {
        // 更新播放状态按钮
        updatePlayBtnStatus();
        playBtnState(isPlaying);
    }

    private void playBtnState(boolean isPlaying) {
        if (isPlaying) {
            // 当前播放  暂停
            audioBinder.pause();
            pauseAlbumAnimator();
            if (isShowLyrics && mDisposableLyrics != null) {
                clearDisposableLyric();
            }
            mBinding.stylusContainer.resetStylus();
        } else {
            // 当前暂停  播放
            audioBinder.start();
            initAnimation();
            if (isShowLyrics) {
                startRollPlayLyrics(mBinding.lyricsView);
            }
        }
    }

    private void resumeAlbumAnimator() {
        Objects.requireNonNull(mBinding.rotateRl.getAutoAnimator()).resume();

    }

    private void pauseAlbumAnimator() {
        Objects.requireNonNull(mBinding.rotateRl.getAutoAnimator()).pause();
    }


    //根据当前播放状态设置图片

    private void updatePlayBtnStatus() {
        mBinding.musicPlay.setImageResource(audioBinder.isPlaying() ? R.drawable.btn_playing_pause_selector : R.drawable.btn_playing_play_selector);
    }


    private void initAnimation() {

        if (audioBinder != null && audioBinder.isPlaying()) {
            resumeAlbumAnimator();
        } else {
            pauseAlbumAnimator();
        }
    }

    @Override
    protected void refreshBtnAndNotify(int playStatus) {
        switch (playStatus) {
            case 0:
                switchPlayState(!audioBinder.isPlaying());
                break;
            case 1:
                checkCurrentIsFavorite(audioBinder.getMusicBean().isFavorite());
                break;
            case 2:
                pauseAlbumAnimator();
                updatePlayBtnStatus();
                break;
            default:
                break;
        }
    }

    private long lastParamUpdateTime = 0; // 引入时间戳防抖

    private void initListener() {
        mBinding.stylusContainer.setListener(state -> {
            if (state instanceof StylusState.Reset) {
                // 暂停音乐，更新 UI 状态
                audioBinder.pause();
                pauseAlbumAnimator();
                updatePlayBtnStatus();
            } else if (state instanceof StylusState.Adjusting) {
                // 根据进度调整音乐播放位置
                StylusState.Adjusting adjustingState = (StylusState.Adjusting) state;
                // 指针旋转角度对应的播放进度条比例0～1,通过唱针旋转的角度，最大只能将播放进度调节到总时长的97%,剩余5、6秒钟，避免旋转到最大角度，自动下一曲
                float progress = adjustingState.getProgress();
                float targetProgress = progress > 0.97 ? 0.97f : progress;
                long targetMs = (long) (targetProgress * audioBinder.getDuration());
                audioBinder.seekTo((int) targetMs);
                audioBinder.start();
                resumeAlbumAnimator();
                updatePlayBtnStatus();
            }


        });


        mBinding.rotateRl.setDiscListener(new OnDiscTouchListener() {
            @Override
            public void onActionMove(float rotation, float speed) {
                // 1. 状态安全检查：如果当前 binder 没准备好或者根本没歌，直接拦截
                if (audioBinder == null) return;

                // 2. 性能优化：限制每 30ms 才能调用一次底层底层 setPlaybackParams
                // 避免一秒钟调用成百上千次导致底层 Native 缓冲区溢出
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastParamUpdateTime < 30) {
                    return;
                }
                lastParamUpdateTime = currentTime;

                // 3. 调整播放进度
                if (Math.abs(speed) > 1.0f) {
                    long currentPos = audioBinder.getProgress();
                    long offset = (long) (speed * 100);
                    // audioBinder.seekTo(Math.toIntExact(currentPos + offset));
                }

                // 4. 模拟“搓碟”变调效果
                float pitch = 1.0f + (speed / 50.0f);
                float safePitch = Math.max(0.5f, Math.min(2.0f, pitch));

                PlaybackParams params = new PlaybackParams();
                params.setSpeed(safePitch);
                params.setPitch(safePitch);
                // 顺畅安全调用
                audioBinder.setPlaybackParams(params);

            }

            @Override
            public void onActionDown() {
                if (audioBinder != null) {
                    audioBinder.pause();
                }
            }

            @Override
            public void onActionUp() {
                if (audioBinder != null) {
                    // 恢复正常播放倍速
                    PlaybackParams normalParams = new PlaybackParams();
                    normalParams.setSpeed(1.0f);
                    normalParams.setPitch(1.0f);
                    audioBinder.setPlaybackParams(normalParams);
                    // 稍作延时或者直接启动
                    audioBinder.start();
                    updatePlayBtnStatus();
                }
            }
        });

        mBinding.sbProgress.setOnSeekBarChangeListener(new SeekBarListener());
        mBinding.sbVolume.setOnSeekBarChangeListener(new SeekBarListener());
        mBinding.playingSongAlbum.setOnLongClickListener(view -> {
            PreviewBigPicDialogFragment.newInstance(FileUtil.getAlbumUrl(mCurrentMusicInfo, 1))
                    .show(getSupportFragmentManager(), "album");
            return true;
        });
        mBinding.ivPlayDown.setOnClickListener(this);
        mBinding.groupTitleArtist.setOnClickListener(this);
        mBinding.playingSongAlbum.setOnClickListener(this);
        mBinding.ivLyricsSwitch.setOnClickListener(this);
        mBinding.ivDeleteLyric.setOnClickListener(this);
        mBinding.ivSearchPlayLyric.setOnClickListener(this);
        mBinding.ivAlwaysOn.setOnClickListener(this);
        mBinding.musicPlayerMode.setOnClickListener(this);
        mBinding.musicPlayerPre.setOnClickListener(this);
        mBinding.musicPlayerNext.setOnClickListener(this);
        mBinding.musicPlay.setOnClickListener(this);
        mBinding.ivFavoriteMusic.setOnClickListener(this);
        mBinding.tvSongName.setOnClickListener(this);
        mBinding.tvArtistName.setOnClickListener(this);
        mBinding.ivFavoriteList.setOnClickListener(this);
        mBinding.lyricsView.setOnClickListener(this);
        mBinding.lyricsView.setOnLongClickListener(v -> {
            if (mLyricList.size() == 1) {
                startSearchLyricsActivity();
            }
            return false;
        });

    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.iv_play_down) {
            finish();
        } else if (id == R.id.tv_song_name || id == R.id.tv_artist_name) {
            startSearchActivity(mCurrentMusicInfo);
        } else if (id == R.id.playing_song_album || id == R.id.lyrics_view) {
            showLyrics();
        } else if (id == R.id.iv_lyrics_switch) {
            MoreMenuBottomDialog.newInstance(mCurrentMusicInfo, audioBinder.getPosition(), true, true).getBottomDialog(this);
        } else if (id == R.id.iv_delete_lyric) {
            // 删除歌词关先关闭隐藏歌词View
            showLyrics();
            LyricsUtil.deleteCurrentLyric(mCurrentMusicInfo.getTitle(), mCurrentMusicInfo.getArtist());
        } else if (id == R.id.iv_search_play_lyric) {
            startSearchLyricsActivity();
        } else if (id == R.id.iv_always_on) {
            screenAlwaysOnSwitch(mBinding.ivAlwaysOn);
        } else if (id == R.id.music_player_mode) {
            switchPlayMode(mBinding.musicPlayerMode);
        } else if (id == R.id.music_player_pre) {
            pauseAlbumAnimator();
            audioBinder.playPre();
        } else if (id == R.id.music_play) {
            playBtnState(audioBinder.isPlaying());
            updatePlayBtnStatus();
        } else if (id == R.id.music_player_next) {
            pauseAlbumAnimator();
            audioBinder.playNext();
        } else if (id == R.id.iv_favorite_music) {
            boolean favoriteState = getFavoriteState(mCurrentMusicInfo);
            audioBinder.updateFavorite();
            checkCurrentIsFavorite(!favoriteState);
        } else if (id == R.id.iv_favorite_list) {
            FavoriteBottomSheetDialog.newInstance(mCurrentMusicInfo.getTitle())
                    .getBottomDialog(this);
        }
    }

    private void startSearchLyricsActivity() {
        Intent intent = new Intent(this, SearchLyricsActivity.class);
        intent.putExtra(Constant.SONG_NAME, StringUtil.getSongName(mCurrentMusicInfo.getTitle()));
        intent.putExtra(Constant.SONG_ARTIST, StringUtil.getArtist(mCurrentMusicInfo.getArtist()));
        startActivityForResult(intent, Constant.SELECT_LYRICS);
        overridePendingTransition(R.anim.dialog_push_in, 0);
    }


    @Override
    protected void updateLyricsView(boolean lyricsOk, String downMsg) {
        if (lyricsOk) {
            mLyricList = LyricsUtil.getLyricList(mCurrentMusicInfo);
        }
        mBinding.lyricsView.setLrcFile(lyricsOk ? mLyricList : null, downMsg);
        closeLyricsView();

    }

    /**
     * 显示歌词 和 屏幕常亮图标显示
     */
    private void showLyrics() {
        if (isShowLyrics) {
            clearDisposableLyric();
            disposableLyricsView();
            resumeAlbumAnimator();
        } else {
            pauseAlbumAnimator();
            boolean lyricIsExists = LyricsUtil.checkLyricFile(StringUtil.getSongName(mCurrentMusicInfo.getTitle()), StringUtil.getArtist(mCurrentMusicInfo.getArtist()));
            if (lyricIsExists) {
                mLyricList = LyricsUtil.getLyricList(mCurrentMusicInfo);
                String msg = mLyricList.size() > 1 ? Constant.MUSIC_LYRIC_OK : Constant.PURE_MUSIC;

                mBinding.lyricsView.setLrcFile(mLyricList, msg);
                // 开始滚动歌词
                if (audioBinder.isPlaying()) {
                    startRollPlayLyrics(mBinding.lyricsView);
                }
                closeLyricsView();
            } else {
                mBinding.lyricsView.setLrcFile(null, Constant.NO_LYRICS);
            }
        }
        mBinding.groupLyrics.setVisibility(isShowLyrics ? View.GONE : View.VISIBLE);
        mBinding.groupStylus.setVisibility(isShowLyrics ? View.VISIBLE : View.GONE);
        mBinding.groupBrightDelete.setVisibility(isShowLyrics ? View.INVISIBLE : mLyricList.size() > 2 ? View.VISIBLE : View.INVISIBLE);
        mBinding.ivLyricsSwitch.setBackgroundResource(isShowLyrics ? R.drawable.music_lrc_close : R.drawable.music_lrc_open);
        AnimationDrawable animation = (AnimationDrawable) mBinding.ivLyricsSwitch.getBackground();
        animation.start();
        isShowLyrics = !isShowLyrics;
    }

    @Override
    protected void updateMusicBarAndVolumeBar(SeekBar seekBar, int progress, boolean b) {
        int id = seekBar.getId();
        if (id == R.id.sb_progress) {
            if (!b) {
                return;
            }
            //拖动音乐进度条播放
            audioBinder.seekTo(progress);
            //更新音乐进度数值
            updateMusicProgress(progress);
            //更新音量  SeekBar
        } else if (id == R.id.sb_volume) {
            updateMusicVolume(progress);
        }
    }

    /**
     * 广播监听系统音量，同时更新VolumeSeekBar
     *
     * @param currVolume c
     */
    @Override
    public void updateVolumeProgress(int currVolume) {
        mBinding.sbVolume.setProgress(currVolume);
    }

    /**
     * 清空收藏列表中所有音乐后的回调，
     */
    @Override
    public void updateFavoriteStatus() {
        checkCurrentIsFavorite(getFavoriteState(mCurrentMusicInfo));
    }

    /**
     * size 小于2表示没有歌词，5秒后自动关闭歌词画面。
     */
    public void closeLyricsView() {
        disposableLyricsView();
        if (mLyricList.size() < Constant.NUMBER_TWO) {
            if (mCloseLyrDisposable == null) {
                mCloseLyrDisposable = Observable.timer(5, TimeUnit.SECONDS)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(aLong -> PlayActivity.this.showLyrics());
            }
        }
    }

    private final Handler mHandler = new Handler();

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constant.SELECT_LYRICS) {
            if (data != null) {
                // 先删除当前歌词
                LyricsUtil.deleteCurrentLyric(mCurrentMusicInfo.getTitle(), mCurrentMusicInfo.getArtist());
                // 获取歌曲ID，下载歌词。
                mHandler.postDelayed(() -> {
                    String songMid = data.getStringExtra(Constant.SONGMID);
                    if (songMid != null) {
                        QqMusicRemote.getOnlineLyrics(songMid, mCurrentMusicInfo.getTitle(), mCurrentMusicInfo.getArtist());
                        showLyrics();
                    }
                }, 600);

            }
        }
    }

}
