package com.yibao.music.activity.view


import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import com.yibao.music.R
import com.yibao.music.base.listener.OnStylusChangeListener
import com.yibao.music.base.listener.StylusState
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

class NewTonearmView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    var listener: OnStylusChangeListener? = null

    private var ivStylus: ImageView? = null
    private var ivStylusShadow: ImageView? = null // 阴影对象

    // 旋转中心点（相对于 TonearmView 容器的坐标）
    private var centerX = 0f
    private var centerY = 0f

    // 旋转限制范围
    private val minDegree = 86f
    private val maxDegree = 120f

    // 阴影与唱针的角度差（向右偏 5 度，可根据视觉效果微调）
    private val shadowAngleOffset = 2f

    override fun onFinishInflate() {
        super.onFinishInflate()
        ivStylus = findViewById(R.id.iv_stylus)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // 自动从父布局寻找阴影 View
        ivStylusShadow = (parent as? ViewGroup)?.findViewById(R.id.iv_stylus_shadow)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (changed) {
            adjustPivot()
        }
    }

    private fun adjustPivot() {
        val stylus = ivStylus ?: return

        // 1. 设置唱针自身的旋转轴心
        val pivotYPx = 48f
        stylus.pivotX = stylus.width / 2f
        stylus.pivotY = pivotYPx

        // 同步设置阴影的旋转轴心，确保与唱针拥有完全一致的圆心
        ivStylusShadow?.let { shadow ->
            shadow.pivotX = shadow.width / 2f
            shadow.pivotY = pivotYPx
        }

        // 2. 计算触摸计算的圆心坐标
        centerX = stylus.left + stylus.pivotX
        centerY = stylus.top + stylus.pivotY
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            if (!isPointInStylusArea(ev.x, ev.y)) {
                return false
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun isPointInStylusArea(x: Float, y: Float): Boolean {
        val stylus = ivStylus ?: return false
        val dx = x - centerX
        val dy = y - centerY
        val distance = sqrt((dx * dx + dy * dy).toDouble())
        val stylusFullLength = stylus.height.toDouble()
        val isDistanceInHead =
            distance > (stylusFullLength * 0.75) && distance < (stylusFullLength * 1.1)

        val radians = atan2(dy.toDouble(), dx.toDouble())
        val touchDegrees = Math.toDegrees(radians).toFloat()
        val currentStylusAbsDegree = stylus.rotation + 90f

        var angleDiff = abs(touchDegrees - currentStylusAbsDegree)
        if (angleDiff > 180) angleDiff = 360 - angleDiff

        val isAngleValid = angleDiff < 30f
        return isDistanceInHead && isAngleValid
    }

    private var returnAnimator: ObjectAnimator? = null
    private val returnDefaultPositionDegree = 102f

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // 手指按下一瞬间，彻底斩断所有正在运行的动画
                returnAnimator?.cancel()
                lastAnimator?.cancel()
                isUserTouching = true
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - centerX
                val dy = event.y - centerY
                val radians = atan2(dy.toDouble(), dx.toDouble())
                val degrees = Math.toDegrees(radians).toFloat()
                val constrainedDegrees = degrees.coerceIn(minDegree, maxDegree)

                val targetRotation = constrainedDegrees - 90f
                ivStylus?.rotation = targetRotation
                ivStylusShadow?.rotation = targetRotation - shadowAngleOffset

                if (constrainedDegrees in 103.0..120.0) {
                    val progress = (constrainedDegrees - 103) / (120f - 103f)
                    listener?.onStateChanged(StylusState.Adjusting(progress.coerceIn(0f, 1f)))
                } else if (constrainedDegrees < 103f) {
                    listener?.onStateChanged(StylusState.Reset)
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isUserTouching = false
                val currentRotation = ivStylus?.rotation ?: 0f
                val offDegree = returnDefaultPositionDegree - 90

                if (currentRotation < offDegree) {
                    resetStylus()
                    listener?.onStateChanged(StylusState.Reset)
                }
            }
        }
        return true
    }

    var isUserTouching = false
        private set

    private val startDegree = 103f
    private val endDegree = 120f
    private var lastAnimator: ObjectAnimator? = null

    /**
     * 【自动同步】播放音乐或恢复播放回到唱片上
     */
    fun updateProgress(progress: Float, animate: Boolean = false) {
        if (isUserTouching) return
        val stylus = ivStylus ?: return
        val safeProgress = progress.coerceIn(0f, 1f)
        val targetDegree = startDegree + (endDegree - startDegree) * safeProgress
        val targetRotation = targetDegree - 90f

        // 1. 判断当前唱针是否处于归位/暂停状态（旋转角度接近 0f）
        val isAtResetPosition = abs(stylus.rotation) < 1f

        // 2. 如果外部强制要求动画，或者当前处于归位状态准备“落针”，则触发动画
        val shouldAnimate = animate || isAtResetPosition

        if (shouldAnimate) {
            // 如果切入唱片的动画正在运行，则不再重复创建，防止动画频繁重置引发抖动
            if (lastAnimator?.isRunning == true) return

            // 恢复播放时，立刻取消可能还没播完的【归位动画】
            returnAnimator?.cancel()
            lastAnimator?.cancel()

            lastAnimator = ObjectAnimator.ofFloat(
                stylus,
                "rotation",
                stylus.rotation,
                targetRotation
            ).apply {
                duration = 800 // 唱针落到唱片上的平滑过渡时间（毫秒）
                interpolator = DecelerateInterpolator()
                // 同步更新阴影
                addUpdateListener { animator ->
                    val currentRot = animator.animatedValue as Float
                    ivStylusShadow?.rotation = currentRot - shadowAngleOffset
                }
                start()
            }
        } else {
            // 3. 【核心保护逻辑】只有当切入动画“没有在运行”时，才允许日常的进度微调
            // 这样可以完美防止 Activity 的高频定时器打断正在过渡的落针动画
            if (lastAnimator?.isRunning != true) {
                stylus.rotation = targetRotation
                ivStylusShadow?.rotation = targetRotation
            }
        }
    }


    /**
     * 唱针平滑归位动画
     */
    fun resetStylus() {
        val stylus = ivStylus ?: return
        isUserTouching = false

        // 【安全清理】归位时，同时取消掉进度的动画，防止产生拉扯冲突
        lastAnimator?.cancel()
        returnAnimator?.cancel()

        returnAnimator = ObjectAnimator.ofFloat(stylus, "rotation", stylus.rotation, 0f).apply {
            duration = 1000
            interpolator = DecelerateInterpolator()
            // 归位动画运行时，同步更新阴影归位
            addUpdateListener { animator ->
                val currentRot = animator.animatedValue as Float
                ivStylusShadow?.rotation = currentRot - shadowAngleOffset
            }
            start()
        }
    }
}
