package com.yibao.music.fragment;


import android.graphics.Color;
import android.view.View;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.yibao.music.R;
import com.yibao.music.activity.PlayListActivity;
import com.yibao.music.adapter.DetailsViewAdapter;
import com.yibao.music.adapter.PlayListAdapter;
import com.yibao.music.base.bindings.BaseMusicFragmentDev;
import com.yibao.music.base.listener.OnFinishActivityListener;
import com.yibao.music.databinding.PlayListFragmentBinding;
import com.yibao.music.fragment.dialogfrag.AddListDialog;
import com.yibao.music.fragment.dialogfrag.ConfirmDialog;
import com.yibao.music.fragment.dialogfrag.MoreMenuBottomDialog;
import com.yibao.music.model.Message;
import com.yibao.music.model.MusicBean;
import com.yibao.music.model.PlayListBean;
import com.yibao.music.model.greendao.MusicBeanDao;
import com.yibao.music.util.Constant;
import com.yibao.music.util.LogUtil;
import com.yibao.music.util.ToastUtil;
import com.yibao.music.view.music.MusicToolBar;
import com.yibao.music.viewmodel.PlayListViewModel;

import java.util.ArrayList;
import java.util.List;


/**
 * @项目名： ArtisanMusic
 * @包名： com.yibao.music.playlist
 * @文件名: PlayListFragment
 * @author: Stran
 * @Email: www.strangermy@outlook.com / www.strangermy98@gmail.com
 * @创建时间: 2018/2/9 16:07
 * @描述： {个人播放列表}
 */

public class PlayListFragment extends BaseMusicFragmentDev<PlayListFragmentBinding> implements SwipeRefreshLayout.OnRefreshListener {


    private boolean isShowDetailsView = false;

    private String mSongName;


    private ArrayList<String> mArrayLisOpenDetail;
    private String mPlayListTitle;
    private boolean isFromPlayListActivity;


    private final PlayListViewModel mViewModel = new PlayListViewModel();

    @Override
    public void initView() {
        initRecyclerView(getMBinding().recyclerPlayList);
        getMBinding().swipePlayList.setColorSchemeColors(Color.BLUE, Color.RED, Color.YELLOW);
        getMBinding().swipePlayList.setOnRefreshListener(this);

        getMBinding().musicBar.setToolbarTitle(getString(R.string.play_list));
        getMBinding().musicBar.setVisibility(isFromPlayListActivity && mSp.getInt(Constant.ADD_TO_PLAY_LIST_FLAG) == Constant.NUMBER_ONE ? View.GONE : View.VISIBLE);

        initListener();
    }

    @Override
    public void onResume() {
        super.onResume();
        mViewModel.getPlayList();
        mViewModel.getListModel().observe(this, this::setData);
        mViewModel.getResultModel().observe(this, this::ok);
        mViewModel.getErrorLiveData().observe(this, errorBean -> ToastUtil.songalreadyExist(mContext));
        mViewModel.getEditModel().observe(this, errorBean -> {


            AddListDialog.newInstance(2, errorBean.getErrorMessage(), false, this)
                    .show(getChildFragmentManager(), "addList");
        });

    }

    private void ok(Message message) {
        LogUtil.d(getMTag(),"添加标记： "+message.toString());
        if (message.getCode() == 2) {
            ToastUtil.show(requireActivity(), "已删除");
            onRefresh();
        } else {
            ToastUtil.show(requireActivity(), "已添加");
            ((OnFinishActivityListener) mContext).finishActivity();
        }
    }

    private void setData(List<PlayListBean> playList) {

        PlayListAdapter mAdapter = new PlayListAdapter(playList);
        getMBinding().recyclerPlayList.setAdapter(mAdapter);
        getMBinding().swipePlayList.setRefreshing(false);
        // item 点击
        mAdapter.setItemListener((playListBean, position) -> {
            mPlayListTitle = playListBean.getTitle();
            // 从PlayListActivity过来的,添加歌曲到播放列表
            if (isFromPlayListActivity) {
                getMBinding().musicBar.setVisibility(View.GONE);
                addToList(playListBean);
            } else {
                // 打开播放列表
                PlayListFragment.this.showDetailsView(mPlayListTitle);
            }
        });
        // 长按删除
        mAdapter.setItemLongClickListener((musicInfo, currentPosition) -> {
            if (!isFromPlayListActivity) {
                ConfirmDialog.newInstance(musicInfo, () -> mViewModel.deletePlayList(musicInfo)).show(PlayListFragment.this.getChildFragmentManager(), "deleteList");
            }
        });
        // 编辑按钮
        mAdapter.setItemEditClickListener(currentPosition -> mViewModel.editPlayList(currentPosition));
    }

    @Override
    public void initData() {

    }


    private void initListener() {
        getMBinding().llAddNewPlayList.setOnClickListener(v ->
                AddListDialog.newInstance(1, Constant.NULL_STRING, isFromPlayListActivity, this).show(getChildFragmentManager(), "addList"));
        getMBinding().musicBar.setClickListener(new MusicToolBar.OnToolbarClickListener() {
            @Override
            public void clickEdit() {
                if (isShowDetailsView) {
                    showDetailsView(getString(R.string.play_list));
                }
            }

            @Override
            public void switchMusicControlBar() {
                switchControlBar();
            }

            @Override
            public void clickDelete() {


            }
        });

    }


    private void showDetailsView(String title) {
        getMBinding().musicBar.setToolbarTitle(title);
        getMBinding().llAddNewPlayList.setVisibility(isShowDetailsView ? View.VISIBLE : View.GONE);
        getMBinding().playListDetailView.setVisibility(isShowDetailsView ? View.GONE : View.VISIBLE);

        if (!isShowDetailsView) {
            List<MusicBean> mDetailList = mMusicBeanDao.queryBuilder().where(MusicBeanDao.Properties.PlayListFlag.eq(title)).build().list();
            getMBinding().playListDetailView.setQueryFlag(title, mDetailList.size());
            DetailsViewAdapter detailsAdapter = new DetailsViewAdapter(mContext, mDetailList, Constant.NUMBER_FIVE, title);
            getMBinding().playListDetailView.setAdapter(detailsAdapter);
            detailsAdapter.setOnItemMenuListener((position, musicBean) -> MoreMenuBottomDialog.newInstance(musicBean, position, false, false).getBottomDialog(mContext));

            getMBinding().musicBar.setTvEditText(R.string.play_list);
            getMBinding().musicBar.setTvEditVisibility(true);
            getMBinding().musicBar.setToolbarTitle(title);

        }
        isShowDetailsView = !isShowDetailsView;
        getMBinding().musicBar.setTvEditVisibility(isShowDetailsView);
    }

    /**
     * @param playListBean 通过PlayListActivity将选中的歌曲添加到列表中，有批量添加和单曲添加，
     */
    private void addToList(PlayListBean playListBean) {
        if (mContext instanceof PlayListActivity) {
            // 批量添加
            if (mArrayLisOpenDetail != null && !mArrayLisOpenDetail.isEmpty()) {
                mViewModel.insertSongsToList(playListBean, mArrayLisOpenDetail);
            } else {
                // 单曲添加
                mViewModel.insertSongSingle(playListBean, mSongName);
            }
            ToastUtil.show(requireActivity(), "已添加");
            ((OnFinishActivityListener) mContext).finishActivity();

        }
    }


    @Override
    protected void deleteItem(int musicPosition) {
        super.deleteItem(musicPosition);
    }

    /**
     * @param songName             s
     * @param arrayList            歌名列表
     * @param formPlayListActivity 打开标识，false  表示 MainPager添加到主界面， true 表示从歌曲列表打开，添加歌曲到列表。
     * @return s
     */
    public static PlayListFragment newInstance(String songName, ArrayList<String> arrayList, boolean formPlayListActivity) {
        PlayListFragment fragment = new PlayListFragment();
        fragment.isFromPlayListActivity = formPlayListActivity;
        fragment.mSongName = songName;
        fragment.mArrayLisOpenDetail = arrayList;
        return fragment;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mSp.getInt(Constant.ADD_TO_PLAY_LIST_FLAG) == Constant.NUMBER_ZERO) {
            isFromPlayListActivity = false;
        }
    }


    @Override
    public void onRefresh() {
        mViewModel.getPlayList();
    }

    @Override
    public boolean onBackPressed() {
        if (isShowDetailsView && isVisible() && isResumed()) {

            showDetailsView(getString(R.string.play_list));
            return true;
        } else {
            return false;
        }
    }


}
