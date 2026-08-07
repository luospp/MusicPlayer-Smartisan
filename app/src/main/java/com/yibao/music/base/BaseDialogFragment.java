package com.yibao.music.base;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import java.util.Objects;

/**
 * Des：${TODO}
 * Time:2017/5/31 18:50
 *
 * @author Stran
 */
public abstract class BaseDialogFragment
        extends DialogFragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        Objects.requireNonNull(getDialog()).requestWindowFeature(Window.FEATURE_NO_TITLE);
        Objects.requireNonNull(getDialog().getWindow())
                .setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        return getViews();
    }

    /**
     * 经过数据绑定的View
     *
     * @return c
     */
    public abstract View getViews();

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
