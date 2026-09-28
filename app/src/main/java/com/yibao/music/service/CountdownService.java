package com.yibao.music.service;

import android.app.Service;
import android.content.Intent;
import android.os.CountDownTimer;
import android.os.IBinder;
import androidx.annotation.Nullable;

import com.yibao.music.util.Constant;
import com.yibao.music.util.RxBus;
import com.yibao.music.util.StringUtil;

/**
 * @ Name:   CountdownService
 * @ Email:  strangermy98@gmail.com
 * @ Time:   2019/1/2/ 22:49
 * @ Des:    倒计时服务
 * @author Luoshipeng
 */
public class CountdownService extends Service {

    private MusicTimer mMusicTimer;


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return START_NOT_STICKY;
        }
        long countdownTime = intent.getLongExtra(Constant.COUNTDOWN_TIME, 0);
        mMusicTimer = new MusicTimer(countdownTime, 1000);
        mMusicTimer.start();
        return START_NOT_STICKY;
    }


    private class MusicTimer extends CountDownTimer {
        private MusicTimer(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onTick(long millisUntilFinished) {
            String countdownTime = StringUtil.parseDuration((int) millisUntilFinished);
            RxBus.getInstance().post(Constant.COUNTDOWN_TIME,countdownTime);
        }

        @Override
        public void onFinish() {
            cancel();
            sendCloseMusicBroadcast();
            stopSelf();
        }

        private void sendCloseMusicBroadcast() {
            Intent intent = new Intent(Constant.ACTION_MUSIC);
            intent.setPackage(getPackageName());
            intent.putExtra(Constant.NOTIFY_BUTTON_ID, Constant.COUNTDOWN_FINISH);
            sendBroadcast(intent);
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mMusicTimer != null) {
            mMusicTimer.cancel();
            mMusicTimer = null;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


}
