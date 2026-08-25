package com.yibao.music.view.music;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.yibao.music.MusicApplication;
import com.yibao.music.R;
import com.yibao.music.adapter.SongAdapter;
import com.yibao.music.base.listener.OnGlideLoadListener;
import com.yibao.music.model.MusicBean;
import com.yibao.music.util.Constant;
import com.yibao.music.util.SpUtils;

import java.util.List;
import java.util.Objects;

/**
 * Des：${将音乐列表(RecyclerView) ,StickyView, SlideBar导航栏 封装到一个View里面，方便多个页面使用}
 * Time:2017/9/10 00:43
 *
 * @author Stran
 */

public class MusicView
        extends RelativeLayout {

    private RecyclerView mRecyclerView;
    private MusicSlidBar mSlideBar;
    private ImageView ivPosition;

    private boolean isShowFloat = false;

    public MusicView(Context context) {
        super(context);
        initView();
        initListener(context);
    }

    public MusicView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.MusicView);
        isShowFloat = typedArray.getBoolean(0, false);
        typedArray.recycle();
        initView();
        initListener(context);
    }

    private void initView() {
        LayoutInflater.from(getContext())
                .inflate(R.layout.music_view, this, true);
        mRecyclerView = findViewById(R.id.rv);
        mSlideBar = findViewById(R.id.music_slide_bar);
        ivPosition = findViewById(R.id.iv_play_position);
        int visible = isShowFloat ? View.VISIBLE : View.GONE;
        ivPosition.setVisibility(visible);
    }

    private void initListener(Context context) {
        ivPosition.setOnClickListener(view -> scrollPlayPosition());
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (context instanceof OnGlideLoadListener) {
                    switch (newState) {
                        case RecyclerView.SCROLL_STATE_IDLE:
                            ((OnGlideLoadListener) context).resumeRequests();
                            if (isShowFloat) {
                                ivPosition.setVisibility(View.VISIBLE);
                            }
                            break;
                        // 加载图片
                        case RecyclerView.SCROLL_STATE_DRAGGING:
                        case RecyclerView.SCROLL_STATE_SETTLING:
                            ((OnGlideLoadListener) context).pauseRequests();
                            if (isShowFloat) {
                                ivPosition.setVisibility(View.GONE);
                            }
                            break;
                        default:
                            break;
                    }
                }
            }
        });
    }

    /**
     * 获取列表控件，用于列表/平铺切换动画等场景
     */
    public RecyclerView getRecyclerView() {
        return mRecyclerView;
    }

    /**
     * //设置列表的适配器
     *
     * @param context        c
     * @param adapterType    1 : SongListAdapter  、 2  ：ArtistAdapter  、
     *                       3  ： AlbumAdapter  专辑 普通视图  、 4  ： AlbumAdapter 专辑 平铺视图 GridView 3列
     * @param isShowSlideBar 只有按歌曲名排列时，SlidBar才显示 。
     */
    public void setAdapter(Context context, int adapterType, boolean isShowSlideBar, RecyclerView.Adapter<RecyclerView.ViewHolder> adapter) {
        mSlideBar.setAdapterType(adapterType);
        if (adapterType == Constant.NUMBER_FOUR) {
            GridLayoutManager manager = new GridLayoutManager(context, Constant.NUMBER_THREE);
            manager.setOrientation(GridLayoutManager.VERTICAL);
            mRecyclerView.setLayoutManager(manager);
        } else {
            LinearLayoutManager manager = new LinearLayoutManager(context);
            manager.setOrientation(LinearLayoutManager.VERTICAL);
            mRecyclerView.setLayoutManager(manager);
            DividerItemDecoration divider = new DividerItemDecoration(context, DividerItemDecoration.VERTICAL);
            divider.setDrawable(Objects.requireNonNull(ContextCompat.getDrawable(context, R.drawable.shape_item_decoration)));
            mRecyclerView.addItemDecoration(divider);
        }

        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setAdapter(adapter);
        mSlideBar.setBarVisibility(isShowSlideBar ? VISIBLE : GONE);
    }

    /**
     * 更新Item喇叭
     */
    public void updateSpeakerState(int position) {
        SongAdapter adapter = (SongAdapter) mRecyclerView.getAdapter();
        if (adapter != null) {
            adapter.setSelectedPosition(position);
        }
    }

    /**
     * 更新歌曲列表选中状态
     */
    public void updateCbState() {
        SongAdapter adapter = (SongAdapter) mRecyclerView.getAdapter();
        if (adapter != null) {
            adapter.updateCbState();
        }
    }


    /**
     * 滚动到当前播放位置
     */
    public void scrollPlayPosition() {

        SpUtils sp = new SpUtils(MusicApplication.getInstance(), Constant.MUSIC_CONFIG);
        String songName = sp.getString(Constant.SONG_NAME);
        SongAdapter adapter = (SongAdapter) mRecyclerView.getAdapter();
        if (adapter != null) {
            List<MusicBean> data = adapter.getData();
            if (data != null) {
                for (int i = 0; i < data.size(); i++) {
                    if (songName != null && !songName.isEmpty() && songName.equals(data.get(i).getTitle())) {
                        mRecyclerView.scrollToPosition(i);
                        return;
                    }
                }
            }
        }
    }
}






