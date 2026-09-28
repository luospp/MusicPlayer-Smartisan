package com.yibao.music.adapter;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.yibao.music.R;
import com.yibao.music.base.bindings.BaseBindingAdapter;
import com.yibao.music.databinding.ItemAlbumWallBinding;
import com.yibao.music.model.AlbumInfo;
import com.yibao.music.network.QqMusicRemote;
import com.yibao.music.util.ImageUitl;
import com.yibao.music.util.LogUtil;
import com.yibao.music.util.StringUtil;

import java.util.List;


public class AlbumWallAdapter extends BaseBindingAdapter<AlbumInfo> {

    private final Activity mContext;


    public AlbumWallAdapter(Activity context, List<AlbumInfo> list) {
        super(list);
        this.mContext = context;

    }


    @Override
    public void bindView(@NonNull RecyclerView.ViewHolder holder, AlbumInfo info) {

        if (holder instanceof PlayViewHolder) {
            PlayViewHolder albumHolder = (PlayViewHolder) holder;

            albumHolder.mBinding.tvAlbumWallArtist.setText(info.getArtist());

            String album = StringUtil.getDownAlbum(info.getAlbumName(), info.getArtist());
            ImageUitl.customLoadPic(mContext, album, R.drawable.noalbumcover_220, ((PlayViewHolder) holder).mBinding.ivAlbumWall);

            ImageUitl.loadPic(mContext, album, albumHolder.mBinding.ivAlbumWall, R.drawable.noalbumcover_220, isSuccess -> {
                if (!isSuccess) {
                    try {
                        QqMusicRemote.getAlbumImg(mContext, info.getAlbumName(), url -> {
                            if (url != null && !url.isEmpty()) {
                                Glide.with(mContext).load(url)
                                        .placeholder(R.drawable.noalbumcover_220)
                                        .error(R.drawable.noalbumcover_220)
                                        .into(((PlayViewHolder) holder).mBinding.ivAlbumWall);
                            }
                        });
                    } catch (Exception exception) {
                        LogUtil.d(getMTAG(), exception.getLocalizedMessage());
                    }
                }
            });

            albumHolder.mBinding.tvAlbumWallName.setText(info.getAlbumName());

            String songCount = info.getSongCount() + "首";
            albumHolder.mBinding.tvAlbumWallSongCount.setText(songCount);


        }
    }


    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == getTypeItem()) {
            ItemAlbumWallBinding binding = ItemAlbumWallBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new PlayViewHolder(binding);
        } else {
            return moreHolder(parent);
        }

    }

    @Nullable
    @Override
    protected String getLastItemDes() {
        return " 张专辑";
    }


    static class PlayViewHolder extends RecyclerView.ViewHolder {

        ItemAlbumWallBinding mBinding;

        PlayViewHolder(ItemAlbumWallBinding binding) {
            super(binding.getRoot());
            mBinding = binding;

        }
    }

}
