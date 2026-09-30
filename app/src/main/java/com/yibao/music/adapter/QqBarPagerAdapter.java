package com.yibao.music.adapter;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;

import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;


import com.bumptech.glide.Glide;
import com.yibao.music.R;
import com.yibao.music.base.listener.MyAnimatorUpdateListener;
import com.yibao.music.base.listener.OnMusicItemClickListener;
import com.yibao.music.model.MusicBean;
import com.yibao.music.network.QqMusicRemote;
import com.yibao.music.util.FileUtil;
import com.yibao.music.util.ImageUitl;
import com.yibao.music.util.LogUtil;
import com.yibao.music.util.StringUtil;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 作者：Stran on 2017/3/23 03:31
 * 描述：${TODO}
 * 邮箱：strangermy@outlook.com
 *
 * @author Stran
 */
public class QqBarPagerAdapter
        extends PagerAdapter {
    private Context mContext;
    private List<MusicBean> mList;
    private ObjectAnimator mAnimator;
    private MyAnimatorUpdateListener mAnimationListener;
    /**
     * 已显示的页面中，用于显示歌词(无歌词时显示歌手)的 TextView
     */
    private final SparseArray<TextView> mLyricsViews = new SparseArray<>();

    public QqBarPagerAdapter(Context context, List<MusicBean> list) {
        this.mContext = context;
        this.mList = list;

    }

    public void setData(List<MusicBean> list) {
        if (mList != null) {
            mList.clear();
        }
        mList = list;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mList != null ? mList.size() : 0;

    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        mLyricsViews.remove(position);
        container.removeView((View) object);
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_music_pager, container, false);
        MusicBean info = mList.get(position);
        initView(info, view);
        initListener(view);
        mLyricsViews.put(position, view.findViewById(R.id.tv_pager_art_name));
        container.addView(view);
        return view;
    }

    /**
     * 更新指定歌曲页面的歌词显示，不重建 Pager。
     * 页面还没创建时只记录到数据上，等页面创建时再显示。
     *
     * @param musicInfo 正在播放的歌曲
     * @param lyrics    当前歌词
     */
    public void updateLyrics(MusicBean musicInfo, String lyrics) {
        if (mList == null || musicInfo == null || musicInfo.getId() == null) {
            return;
        }
        for (int i = 0; i < mList.size(); i++) {
            MusicBean bean = mList.get(i);
            if (musicInfo.getId().equals(bean.getId())) {
                // 记录到页面自己的数据上，页面重建时可以直接显示
                bean.setCurrentLyrics(lyrics);
                TextView tvLyrics = mLyricsViews.get(i);
                if (tvLyrics != null) {
                    tvLyrics.setText(lyrics);
                }
                return;
            }
        }
    }

    @SuppressLint("CheckResult")
    private void initListener(View view) {
        view.setOnClickListener(v -> {
            if (mContext instanceof OnMusicItemClickListener) {
                ((OnMusicItemClickListener) mContext).onOpenMusicPlayDialogFag();
            }
        });


    }


    private void initView(MusicBean musicInfo, View view) {
        ImageView mAlbum = view.findViewById(R.id.iv_pager_album);
        TextView tvSongName = view.findViewById(R.id.tv_pager_song_name);
        TextView tvArtist = view.findViewById(R.id.tv_pager_art_name);
        String albumUri = FileUtil.getAlbumUrl(musicInfo, 1);

        try {
            ImageUitl.loadPic((Activity) mContext, albumUri, mAlbum, R.drawable.playing_cover_lp, isSuccess -> {
                if (!isSuccess) {
                    QqMusicRemote.getSongImg(mContext, musicInfo.getTitle(), url -> {
                        if (url != null) {
                            Glide.with(mContext).load(url).placeholder(R.drawable.playing_cover_lp).error(R.drawable.playing_cover_lp).into(mAlbum);
                        }
                    });
                }
            });
        } catch (Exception e) {
            LogUtil.d("===", e.getMessage());
        }


        String currentLyrics = musicInfo.getCurrentLyrics();
        tvSongName.setText(StringUtil.getTitle(musicInfo));
        tvArtist.setText(currentLyrics != null ? currentLyrics : StringUtil.getArtist(musicInfo));
//        if (currentLyrics != null) {
//            if (mAnimator == null || mAnimationListener == null) {
//                mAnimator = AnimationUtil.getRotation(mAlbulm);
//                mAnimationListener = new MyAnimatorUpdateListener(mAnimator);
//                mAnimator.start();
//                mAnimationListener.play();
//            } else {
//                mAnimator.resume();
//            }
//        }
    }


}
