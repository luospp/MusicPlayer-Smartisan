package com.yibao.music.view.music;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.yibao.music.R;
import com.yibao.music.activity.PlayListActivity;
import com.yibao.music.adapter.DetailsViewAdapter;
import com.yibao.music.base.listener.OnMusicItemClickListener;
import com.yibao.music.fragment.dialogfrag.AlbumDetailDialogFragment;
import com.yibao.music.fragment.dialogfrag.PreviewBigPicDialogFragment;
import com.yibao.music.fragment.dialogfrag.RelaxDialogFragment;
import com.yibao.music.model.AlbumInfo;
import com.yibao.music.model.ArtistInfo;
import com.yibao.music.model.MusicBean;
import com.yibao.music.network.QqMusicRemote;
import com.yibao.music.util.Constant;
import com.yibao.music.util.ImageUitl;
import com.yibao.music.util.LogUtil;
import com.yibao.music.util.RandomUtil;
import com.yibao.music.util.SnakbarUtil;
import com.yibao.music.util.SpUtils;
import com.yibao.music.util.StringUtil;
import com.yibao.music.view.MusicScrollView;
import com.yibao.music.view.SwipeItemLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Des：${将详情页面封装到一个View里面，方便多个页面使用}
 * Time:2017/9/10 00:43
 *
 * @author Stran
 */

public class DetailsView
        extends RelativeLayout implements View.OnClickListener {
    private static final String TAG = "====" + DetailsView.class.getSimpleName() + "    ";
    private RecyclerView mRecyclerView;
    private ImageView mIvArtistAlbumDetails;
    private TextView mTvArtistAlbumDetailsTitle;
    private TextView mTvArtistAlbumDetailsArtist;
    private TextView mTvArtistAlbumDetailsDate;
    private ImageView mIvDetailsAddToList;
    private ImageView mIvDetailsAddToPlayList;
    private LinearLayout mLlAlbumDetailsPlayaLl;
    private LinearLayout mLlAlbumDetailsRandomPlay;
    private int mPageType;
    private int mListSize;
    private String mCondition;
    private FragmentManager mFragmentManager;
    private Long mAlbumId;
    private List<MusicBean> mMusicList;
    private MusicScrollView mMusicScrollView;
    private LinearLayout mSuspensionLl;
    private String mArtist;
    private int mPicType;
    private String imageUrl;

    /**
     * @param fragmentManager f
     * @param listSize        列表长度 随机播放的取数范围
     * @param condition       关键字  歌手名 、 专辑名
     * @param pageType        页面标识
     */
    public void setDataFlag(FragmentManager fragmentManager, int listSize, String condition, int pageType) {
        this.mFragmentManager = fragmentManager;
        this.mPageType = pageType;
        this.mCondition = condition;
        this.mListSize = listSize;
    }

    public DetailsView(Context context) {
        super(context);
        initView();
        initListener();
    }


    public DetailsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
        initListener();
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
//        LogUtil.d("==========++== 详情显示  " + visibility);

    }

    /**
     * //设置列表的适配器
     *
     * @param dataType 将数据 强转成指定的类型  1 ： ArtistInfo   2 ： AlbumInfo
     * @param bean     d
     * @param adapter  d
     */
    public void setAdapter(int dataType, Object bean, DetailsViewAdapter adapter) {
        initData(dataType, bean);
        mMusicList = adapter.getData();
        LinearLayoutManager manager = new LinearLayoutManager(getContext());
        manager.setOrientation(LinearLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(manager);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setNestedScrollingEnabled(true);
        mRecyclerView.setAdapter(adapter);
        adapter.notifyDataSetChanged();

    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.tv_artist_albumm_details_title) {
            openAlbumDetail(imageUrl, mArtist);
        } else if (id == R.id.iv_artist_albumm_details) {
            PreviewBigPicDialogFragment.newInstance(imageUrl)
                    .show(mFragmentManager, "album");
        } else if (id == R.id.iv_details_add_to_list) {
            startPlayListActivity();
        } else if (id == R.id.iv_details_add_to_play_list) {
            LogUtil.d(TAG, "=================添加到当前播放列表");
            SnakbarUtil.keepGoing(mIvDetailsAddToPlayList);
        } else if (id == R.id.ll_album_details_playall) {
            startMusic(Constant.NUMBER_ZERO);
        } else if (id == R.id.ll_album_details_random_play) {
            startMusic(RandomUtil.getRandomPosition(mListSize));
        }
    }

    private void openAlbumDetail(String albumUrl, String albumName) {
        AlbumDetailDialogFragment.newInstance(albumUrl, albumName).show(mFragmentManager, "album detail");
    }

    private void startPlayListActivity() {
        ArrayList<String> arrayList = new ArrayList<>();
        for (MusicBean musicBean : mMusicList) {
            arrayList.add(musicBean.getTitle());
        }
        Intent intent = new Intent(getContext(), PlayListActivity.class);
        intent.putStringArrayListExtra(Constant.ADD_TO_LIST, arrayList);
        getContext().startActivity(intent);
    }

    private void startMusic(int startPosition) {
        if (getContext() instanceof OnMusicItemClickListener) {
            SpUtils sp = new SpUtils(getContext().getApplicationContext(), Constant.MUSIC_CONFIG);
            sp.putValues(new SpUtils.ContentValue(Constant.MUSIC_DATA_FLAG, Constant.NUMBER_TEN));
            LogUtil.d(TAG, "当前标识  ==  " + mPageType);
            ((OnMusicItemClickListener) getContext()).startMusicServiceFlag(startPosition, mPageType, mCondition);
        }
    }


    /**
     * 根据dataType将bean转换具体的数据类型
     *
     * @param dataType d
     * @param bean     b
     */
    private void initData(int dataType, Object bean) {
        mPicType = dataType;
        if (dataType == Constant.NUMBER_ONE) {
            ArtistInfo info = (ArtistInfo) bean;
            mArtist = info.getArtist();
            mAlbumId = info.getAlbumId();
            setMusicInfo(dataType, info.getAlbumName(), info.getArtist(), mAlbumId, info.getYear());

        } else if (dataType == Constant.NUMBER_TWO) {
            AlbumInfo info = (AlbumInfo) bean;
            mAlbumId = info.getAlbumId();
            mArtist = info.getAlbumName();
            setMusicInfo(dataType, info.getAlbumName(), info.getArtist(), mAlbumId, info.getYear());
        }

    }


    /**
     * @param dataType  1 歌手图片、2 专辑图片
     * @param albumName a
     * @param artist    a
     * @param albumId   a
     * @param issueYear i
     */
    private void setMusicInfo(int dataType, String albumName, String artist, long albumId, int issueYear) {
        mTvArtistAlbumDetailsTitle.setText(albumName);
        mTvArtistAlbumDetailsArtist.setText(artist);
        try {
            ImageUitl.loadPic((Activity) getContext(), StringUtil.getAlbum(dataType, albumId, artist), mIvArtistAlbumDetails, R.drawable.noalbumcover_220, isSuccess -> {
                if (!isSuccess) {
                    // 歌手图片接口异常，暂时都显示专辑图片
//                if (dataType == 1) {
//                    QqMusicRemote.getArtistImg(getContext(), artist, url -> {
//                        LogUtil.d(TAG, "专辑URL：  " + url);
//                        if (!url.isEmpty()) {
////                            Glide.with(getContext()).load(url).placeholder(R.drawable.noalbumcover_220).error(R.drawable.noalbumcover_220).into(mIvArtistAlbumDetails);
//                        }
//                    });
//
//
//                } else if (dataType == 2) {
//                }
                    String searchKey = (dataType == 1) ? artist : albumName;
                    QqMusicRemote.getAlbumImg(getContext(), searchKey, url -> {
                        if (url != null && !url.isEmpty()) {
                            imageUrl = url;
                            Glide.with(DetailsView.this.getContext()).load(url).placeholder(R.drawable.noalbumcover_220).error(R.drawable.noalbumcover_220).into(mIvArtistAlbumDetails);
                        }
                    });
                }
            });
        } catch (Exception e) {
            LogUtil.d(TAG, "专辑图片下载失败：" + e.getMessage());
        }


        if (issueYear != Constant.NUMBER_ZERO) {
            String year = String.valueOf(issueYear);
            mTvArtistAlbumDetailsDate.setText(year);
        }

    }

    private void initListener() {
        mIvDetailsAddToList.setOnClickListener(this);
        mIvDetailsAddToPlayList.setOnClickListener(this);
        mLlAlbumDetailsPlayaLl.setOnClickListener(this);
        mLlAlbumDetailsRandomPlay.setOnClickListener(this);
        mIvArtistAlbumDetails.setOnClickListener(this);
        mTvArtistAlbumDetailsTitle.setOnClickListener(this);
        mIvArtistAlbumDetails.setOnLongClickListener(v -> {
            RelaxDialogFragment.newInstance().show(mFragmentManager, "girlsDialog");
            return true;
        });
    }


    private void initView() {
        LayoutInflater.from(getContext())
                .inflate(R.layout.details_fragment, this, true);
        mMusicScrollView = findViewById(R.id.detail_root);
        mSuspensionLl = findViewById(R.id.ll_play);
        mRecyclerView = findViewById(R.id.rv_artist_album_details);
        mIvArtistAlbumDetails = findViewById(R.id.iv_artist_albumm_details);
        mTvArtistAlbumDetailsTitle = findViewById(R.id.tv_artist_albumm_details_title);
        mTvArtistAlbumDetailsArtist = findViewById(R.id.tv_artist_albumm_details_artist);
        mTvArtistAlbumDetailsDate = findViewById(R.id.tv_artist_albumm_details_date);
        mIvDetailsAddToList = findViewById(R.id.iv_details_add_to_list);
        mIvDetailsAddToPlayList = findViewById(R.id.iv_details_add_to_play_list);
        mLlAlbumDetailsPlayaLl = findViewById(R.id.ll_album_details_playall);
        mLlAlbumDetailsRandomPlay = findViewById(R.id.ll_album_details_random_play);
        mRecyclerView.setNestedScrollingEnabled(false);
        DividerItemDecoration divider = new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL);
        divider.setDrawable(Objects.requireNonNull(ContextCompat.getDrawable(getContext(), R.drawable.shape_item_decoration)));
        mRecyclerView.addOnItemTouchListener(new SwipeItemLayout.OnSwipeItemTouchListener(getContext()));
        mRecyclerView.addItemDecoration(divider);
    }

    public void setSuspension() {
        mMusicScrollView.resetHeight(mSuspensionLl, mRecyclerView);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return false;
    }
}







