package com.yibao.music.base.listener

// 搓碟回调
interface OnDiscTouchListener {
    // rotation: 当前角度, speed: 瞬时角速度（正负代表方向）
    fun onActionMove(rotation: Float, speed: Float)
    fun onActionDown()
    fun onActionUp()
}