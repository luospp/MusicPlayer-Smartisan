package com.yibao.music.base.listener;

/**
 * @ Name:   OnTakePhotoListener
 * @ Email:  strangermy98@gmail.com
 * @ Time:   2026/9/29
 * @ Des:    拍照、相册选择头像的回调，由 Fragment 通过 Activity Result API 启动
 * @author Luoshipeng
 */
public interface OnTakePhotoListener {
    /**
     * 拍照
     */
    void takePhoto();

    /**
     * 从相册选择
     */
    void choicePhoto();
}
