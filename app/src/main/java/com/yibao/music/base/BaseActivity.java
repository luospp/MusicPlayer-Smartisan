package com.yibao.music.base;

import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.DisplayMetrics;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.baidu.mobstat.StatService;
import com.yibao.music.MusicApplication;
import com.yibao.music.R;
import com.yibao.music.activity.PlayActivity;
import com.yibao.music.activity.PlayListActivity;
import com.yibao.music.activity.SearchActivity;
import com.yibao.music.model.LyricDownBean;
import com.yibao.music.model.MoreMenuStatus;
import com.yibao.music.model.MusicBean;
import com.yibao.music.model.greendao.MusicBeanDao;
import com.yibao.music.model.greendao.PlayListBeanDao;
import com.yibao.music.model.greendao.SearchHistoryBeanDao;
import com.yibao.music.util.Constant;
import com.yibao.music.util.QueryMusicFlagListUtil;
import com.yibao.music.util.RxBus;
import com.yibao.music.util.SpUtils;
import com.yibao.music.util.ToastUtil;
import com.yibao.music.view.music.QqControlBar;
import com.yibao.music.view.music.SmartisanControlBar;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;


/**
 * @项目名： ArtisanMusic
 * @包名： com.yibao.music.base
 * @文件名: BaseActivity
 * @author: Stran
 * @创建时间: 2018/2/20 13:07
 * @描述： {TODO}
 */

public abstract class BaseActivity extends AppCompatActivity {

    protected RxBus mBus;
    protected MusicBeanDao mMusicDao;
    protected SearchHistoryBeanDao mSearchDao;
    protected CompositeDisposable mCompositeDisposable;
    protected Disposable mDisposableProgress;
    protected Disposable mRxViewDisposable;
    protected PlayListBeanDao mPlayListDao;
    protected SpUtils mSps;
    protected final String TAG = "====" + this.getClass().getSimpleName() + "    ";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StatService.start(getApplicationContext());
        mBus = RxBus.getInstance();
        mSps = new SpUtils(MusicApplication.getInstance(), Constant.MUSIC_CONFIG);
        switchLanguage();
        mMusicDao = MusicApplication.getInstance().getMusicDao();
        mSearchDao = MusicApplication.getInstance().getSearchDao();
        mPlayListDao = MusicApplication.getInstance().getPlayListDao();

    }


    private void switchLanguage() {
        String language = mSps.getString(Constant.LANGUAGE);
        if (language != null) {
            Locale locale = new Locale(language);
            Resources resources = getResources();
            DisplayMetrics dm = resources.getDisplayMetrics();
            Configuration cf = resources.getConfiguration();
            cf.setLocale(locale);
            resources.updateConfiguration(cf, dm);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        subscribe();
    }

    private void subscribe() {
        if (mCompositeDisposable == null) {
            mCompositeDisposable = new CompositeDisposable();
        }
        mCompositeDisposable.add(mBus.toObservableType(Constant.SERVICE_MUSIC, MusicBean.class)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::updateCurrentPlayInfo));
        // 接收歌词下载状态
        mCompositeDisposable.add(mBus.toObservableType(Constant.MUSIC_LYRIC_OK, LyricDownBean.class)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(bean -> {
                    updateLyricsView(bean.isDoneOK(), bean.getDownMsg());
                    if (!bean.isDoneOK()) {
                        ToastUtil.show(this, "暂无歌词");
                    }
                }));
        mCompositeDisposable.add(mBus.toObservableType(Constant.PLAY_STATUS, Object.class)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(o -> refreshBtnAndNotify((Integer) o)));
        mCompositeDisposable.add(mBus.toObserverable(MoreMenuStatus.class)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::moreMenu));

        upDataPlayProgress();
    }

    /**
     * @param lyricsOK 歌词下载是否成功 ，成功就重新设置歌词View
     * @param downMsg  歌词下载的信息。
     */
    protected void updateLyricsView(boolean lyricsOK, String downMsg) {
    }


    protected void moreMenu(MoreMenuStatus moreMenuStatus) {

    }

    protected void refreshBtnAndNotify(int playStatus) {
    }

    protected void upDataPlayProgress() {
        if (mDisposableProgress != null && !mDisposableProgress.isDisposed()) {
            return;
        }

        mDisposableProgress = Observable.interval(0, 250, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(aLong -> BaseActivity.this.updateCurrentPlayProgress());
        mCompositeDisposable.add(mDisposableProgress);


    }

    protected void updateCurrentPlayProgress() {
    }

    /**
     * @param musicItem 当前播放的歌曲信息，用于更新进度和动画状态,需要用的界面复写这个方法
     */
    protected void updateCurrentPlayInfo(MusicBean musicItem) {
        upDataPlayProgress();
    }

    protected void startPlayActivity() {
        startActivity(new Intent(this, PlayActivity.class));
        overridePendingTransition(R.anim.dialog_push_in, 0);
    }

    protected void startPlayListActivity(String songName) {
        ArrayList<String> arr = new ArrayList<>();
        Intent intent = new Intent(this, PlayListActivity.class);
        intent.putStringArrayListExtra("aar", arr);
        intent.putExtra(Constant.SONG_NAME, songName);
        startActivity(intent);
        overridePendingTransition(R.anim.dialog_push_in, 0);
    }

    /**
     * @param currentMusicBean bean
     */
    protected void startSearchActivity(MusicBean currentMusicBean) {
        Intent intent = new Intent(this, SearchActivity.class);
        intent.putExtra(Constant.PAGE_TYPE, Constant.NUMBER_TEN);
        intent.putExtra(Constant.MUSIC_BEAN, currentMusicBean);
        startActivity(intent);
        overridePendingTransition(R.anim.dialog_push_in, 0);
    }

    protected void checkCurrentSongIsFavorite(MusicBean currentMusicBean, QqControlBar qqControlBar, SmartisanControlBar smartisanControlBar) {
        if (currentMusicBean != null) {
            MusicBean loadBean = mMusicDao.load(currentMusicBean.getId());
            if (loadBean != null) {
                boolean favorite = mMusicDao.load(currentMusicBean.getId()).getIsFavorite();
                smartisanControlBar.setFavoriteButtonState(favorite);
                if (qqControlBar != null) {
                    qqControlBar.setFavoriteButtonState(favorite);
                }

            }
        }
    }

    protected List<MusicBean> getCurrentList() {
        int pageType = getIntent().getIntExtra(Constant.PAGE_TYPE, 0);
        String condition = getIntent().getStringExtra(Constant.CONDITION);
        return QueryMusicFlagListUtil.getMusicDataList(mMusicDao.queryBuilder(), pageType, condition);
    }

    protected boolean getFavoriteState(MusicBean musicBean) {
        return mMusicDao.load(musicBean.getId()).isFavorite();
    }

    protected void showMsg(String msg) {
        ToastUtil.show(this, msg);
    }

    @Override
    protected void onPause() {
        super.onPause();
        clearDisposableProgress();
        if (mCompositeDisposable != null) {
            mCompositeDisposable.dispose();
            mCompositeDisposable.clear();
            mCompositeDisposable = null;
        }
    }

    protected void clearDisposableProgress() {
        if (mDisposableProgress != null) {
            mDisposableProgress.dispose();
            mDisposableProgress = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mRxViewDisposable != null) {
            mRxViewDisposable.dispose();
            mRxViewDisposable = null;
        }
    }
}
