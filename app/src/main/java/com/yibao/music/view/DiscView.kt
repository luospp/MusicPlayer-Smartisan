package com.yibao.music.view


import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.animation.LinearInterpolator
import android.widget.RelativeLayout
import androidx.core.animation.doOnEnd
import com.yibao.music.base.listener.OnDiscTouchListener
import kotlin.math.atan2

class DiscView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : RelativeLayout(context, attrs) {

    private var currentRotation = 0f
    private var isUserTouching = false
    private var lastAngle = 0f

    var discListener: OnDiscTouchListener? = null
    var autoAnimator: ValueAnimator? = null
    var flingAnimator: ValueAnimator? = null

    // 💡 优化 1：手势检测器只保留 onFling，必须重写 onDown 并返回 true，否则无法触发 Fling
    private val gestureDetector =
        GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean {
                return true
            }

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                startFling(velocityX, velocityY)
                return true
            }
        })

    // 💡 优化 2：全权负责角度计算，每帧只触发一次
    private fun handleScroll(x: Float, y: Float) {
        val centerX = width / 2f
        val centerY = height / 2f

        // 计算当前手指相对于中心点的绝对角度
        val angle = Math.toDegrees(atan2((y - centerY).toDouble(), (x - centerX).toDouble())).toFloat()

        // 💡 修复：去掉了 if (lastAngle != 0f) 的错误判定，改用 ACTION_DOWN 必然初始化保证
        var diff = angle - lastAngle

        // 处理 180/-180 度边界跳变
        if (diff > 180) diff -= 360
        if (diff < -180) diff += 360

        updateRotation(currentRotation + diff)

        // 🎯 此时回调给 Activity 的 diff 极其平滑稳定，音频变调再也不会抖动
        discListener?.onActionMove(currentRotation, diff)

        lastAngle = angle
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // 仅让手势检测器在后台收集数据（用于 UP 时触发 onFling）
        gestureDetector.onTouchEvent(event)

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isUserTouching = true
                autoAnimator?.pause()
                flingAnimator?.cancel()

                val centerX = width / 2f
                val centerY = height / 2f
                // 完美初始化起始角度
                lastAngle = Math.toDegrees(
                    atan2((event.y - centerY).toDouble(), (event.x - centerX).toDouble())
                ).toFloat()

                discListener?.onActionDown()
            }

            MotionEvent.ACTION_MOVE -> {
                // 💡 核心改动：统一、唯一在这里处理滑动，远离双重调用
                handleScroll(event.x, event.y)
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isUserTouching = false
                // 如果没有触发惯性动画，恢复自动旋转
                if (flingAnimator == null || !flingAnimator!!.isRunning) {
                    autoAnimator?.resume()
                }
                discListener?.onActionUp()
            }
        }
        return true
    }

    private fun updateRotation(rot: Float) {
        currentRotation = rot % 360
        rotation = currentRotation
    }

    private fun startFling(vx: Float, vy: Float) {
        val angularVelocity = (vx + vy) / 50f
        flingAnimator = ValueAnimator.ofFloat(angularVelocity, 0f).apply {
            duration = 1500
            interpolator = android.view.animation.DecelerateInterpolator()
            addUpdateListener {
                if (!isUserTouching) {
                    val v = it.animatedValue as Float
                    updateRotation(currentRotation + v)
                }
            }
            doOnEnd { if (!isUserTouching) autoAnimator?.resume() }
            start()
        }
    }

    fun initAutoRotation() {
        autoAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
            duration = 15000
            interpolator = LinearInterpolator()
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener {
                if (!isUserTouching && (flingAnimator == null || !flingAnimator!!.isRunning)) {
                    updateRotation(currentRotation + 360f / (15000 / 16f))
                }
            }
            start()
        }
    }
}
