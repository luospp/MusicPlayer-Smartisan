package com.yibao.music.fragment.dialogfrag;

import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.yibao.music.R;
import com.yibao.music.base.listener.BottomSheetCallback;
import com.yibao.music.base.listener.OnTakePhotoListener;

/**
 * Des：${TODO}
 * Time:2017/8/22 14:11
 *
 * @author Stran
 */
public class TakePhotoBottomSheetDialog {
    private OnTakePhotoListener mListener;
    private View mTvCancel;
    private View mTvTakePhoto;
    private View mTvChoicePhoto;

    public static TakePhotoBottomSheetDialog newInstance() {
        return new TakePhotoBottomSheetDialog();
    }

    /**
     * @param context  f
     * @param listener 拍照、相册选择的回调，实际启动由 Fragment 的 Activity Result API 完成
     */
    public void getBottomDialog(FragmentActivity context, OnTakePhotoListener listener) {
        this.mListener = listener;
        BottomSheetDialog dialog = new BottomSheetDialog(context);
        View view = LayoutInflater.from(context).inflate(R.layout.takephoto_dialog_fragment, null);
        initView(dialog, view);
        initListener(dialog);
        dialog.show();
    }


    private void initListener(BottomSheetDialog dialog) {
        mTvCancel.setOnClickListener(v -> dialog.dismiss());
        mTvTakePhoto.setOnClickListener(v -> {
            dialog.dismiss();
            mListener.takePhoto();
        });
        mTvChoicePhoto.setOnClickListener(v -> {
            dialog.dismiss();
            mListener.choicePhoto();
        });
    }

    private void initView(BottomSheetDialog dialog, View view) {
        mTvCancel = view.findViewById(R.id.tv_take_photo_cancel);
        mTvTakePhoto = view.findViewById(R.id.tv_take_photo);
        mTvChoicePhoto = view.findViewById(R.id.tv_choice_photo);
        dialog.setContentView(view);
        dialog.setCancelable(true);
        Window window = dialog.getWindow();
        if (window != null) {
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
        BottomSheetBehavior<View> sheetBehavior = BottomSheetBehavior.from((View) view.getParent());
        dialog.setCanceledOnTouchOutside(true);
        dialog.setOnDismissListener(dialog12 -> {
        });
        sheetBehavior.setBottomSheetCallback(new BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View view, int newState) {
                if (newState == BottomSheetBehavior.STATE_DRAGGING) {
                    sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                }
            }
        });
    }


}

