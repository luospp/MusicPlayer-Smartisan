package com.yibao.music.activity.view

import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import com.yibao.music.base.listener.OnStylusChangeListener
import com.yibao.music.base.listener.StylusState
import com.yibao.music.databinding.CustTonearmViewBinding
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class CustTonearmView @JvmOverloads constructor(
    context: Context?, attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {


    var listener: OnStylusChangeListener? = null
        private set
    private var returnAnimator: ObjectAnimator? = null
    private val returnDefaultPositionDegree = 102f
    var isUserTouching = false
        private set
    private val startDegree = 103f
    private val endDegree = 120f
    private var lastAnimator: ObjectAnimator? = null

    // 旋转中心点（相对于当前组合控件的绝对坐标，用于手势判定）
    private var centerX = 0f
    private var centerY = 0f

    private val minDegree = 86f
    private val maxDegree = 120f


    private val mBinding = CustTonearmViewBinding.inflate(LayoutInflater.from(context), this, true)

    private var pivotsInitialized = false

    init {
        clipChildren = false
        clipToPadding = false
    }


    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        if (width > 0) {
            post { initPivots() }
        }
    }

    /**
     * 读取视觉锚点，高精度绑定轴心
     */
    private fun initPivots() {
        val container = mBinding.rotationContainer
        val anchor = mBinding.viewPivotAnchor


        if (anchor.width == 0) return

        // 1. 直接读取你在 XML 里纯手工视觉对齐好的红点作为唯一旋转轴心
        val pivotX = anchor.left + anchor.width / 2f
        val pivotY = anchor.top + anchor.height / 2f

        // 2. 注入旋转容器
        container.pivotX = pivotX
        container.pivotY = pivotY

        // 3. 换算得到绝对手势判定中心
        centerX = container.left + pivotX
        centerY = container.top + pivotY

        // 4. 同步配置外部阴影的旋转中心
        mBinding.ivStylusShadow.let {
            val parentCenterX = this.left + centerX
            val parentCenterY = this.top + centerY
            it.pivotX = parentCenterX - it.left
            it.pivotY = parentCenterY - it.top
            mBinding.ivStylusShadow.rotation = -3f
        }


        pivotsInitialized = true
    }


    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (!pivotsInitialized) initPivots()
        if (ev.action == MotionEvent.ACTION_DOWN) {
            if (!isPointInStylusArea(ev.x, ev.y)) return false
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun isPointInStylusArea(x: Float, y: Float): Boolean {
        if (!pivotsInitialized) return false
        val dx = x - centerX
        val dy = y - centerY
        val distance = sqrt((dx * dx + dy * dy).toDouble())

        val stylusFullLength = mBinding.ivStylus.height.toDouble()
        val isDistanceInHead = distance > 0 && distance < (stylusFullLength * 1.2)

        val radians = atan2(dy.toDouble(), dx.toDouble())
        var touchDegrees = Math.toDegrees(radians).toFloat()
        if (touchDegrees < 0) touchDegrees += 360f

        val currentStylusAbsDegree = mBinding.rotationContainer.rotation + 90f

        var angleDiff = abs(touchDegrees - currentStylusAbsDegree)
        if (angleDiff > 180) angleDiff = 360 - angleDiff

        return isDistanceInHead && angleDiff < 45f
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        mBinding.ivStylusShadow.visibility = VISIBLE
        if (!pivotsInitialized) return super.onTouchEvent(event)

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // 1. 获取唱针当前的实际宽高
                val totalWidth = mBinding.ivStylus.width.toFloat()
                val totalHeight = mBinding.ivStylus.height.toFloat()
                val pivotY = totalWidth * 0.5f // 73.5px 对应的比例

                // 2. 计算轴心到唱头（最底端）的物理距离 R
                val r = totalHeight - pivotY

                // 3. 计算当前唱头相对于轴心的绝对角度（Android 中垂直向下为 90度）
                val currentRotation = mBinding.rotationContainer.rotation
                val armAngleRad = Math.toRadians((currentRotation + 90f).toDouble())

                // 4. 根据三角函数，算出唱头当前在 View 中的绝对坐标 (X, Y)
                val headshellX = centerX + r * cos(armAngleRad).toFloat()
                val headshellY = centerY + r * sin(armAngleRad).toFloat()

                // 5. 计算用户手指按下的点，距离“真实唱头”的绝对距离
                val dx = event.x - headshellX
                val dy = event.y - headshellY
                val distance = sqrt((dx * dx + dy * dy).toDouble()).toFloat()

                // 6. 设定一个合理的触摸感应半径（50dp 转换为像素，确保手指好抓取）
                val density = context.resources.displayMetrics.density
                val touchRadius = 50f * density

                // 🎯 【核心拦截】如果用户按下的地方离唱头太远，直接拒绝响应响应
                if (distance > touchRadius) {
                    return false // 返回 false 后，后续的 ACTION_MOVE 和 UP 都不会再触发
                }

                // 只有点中了唱头，才允许继续往下走
                isUserTouching = true
                lastAnimator?.cancel()
                returnAnimator?.cancel()
            }

            MotionEvent.ACTION_MOVE -> {
                // 之前的触摸旋转逻辑保持完全不变
                val radians = atan2((event.y - centerY).toDouble(), (event.x - centerX).toDouble())
                var degrees = Math.toDegrees(radians).toFloat()

                if (degrees < 0) {
                    degrees += 360f
                }

                val constrainedDegrees = degrees.coerceIn(minDegree, maxDegree)
                val targetRotation = constrainedDegrees - 90f
                mBinding.rotationContainer.rotation = targetRotation

                if (constrainedDegrees in startDegree..endDegree) {
                    val progress = (constrainedDegrees - startDegree) / (endDegree - startDegree)
                    listener?.onStateChanged(StylusState.Adjusting(progress.coerceIn(0f, 1f)))
                } else if (constrainedDegrees < startDegree) {
                    listener?.onStateChanged(StylusState.Reset)
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                // 之前的松手归位逻辑保持完全不变
                isUserTouching = false
                val currentRotation = mBinding.rotationContainer.rotation
                val offDegree = returnDefaultPositionDegree - 90f

                if (currentRotation < offDegree) {
                    resetStylus()
                    listener?.onStateChanged(StylusState.Reset)
                }
            }
        }
        return true
    }

    fun updateProgress(progress: Float, animate: Boolean = false) {
        if (isUserTouching) return
        val safeProgress = progress.coerceIn(0f, 1f)
        val targetDegree = startDegree + (endDegree - startDegree) * safeProgress
        val targetRotation = targetDegree - 90f

        // 1. 判断当前唱针是否处于归位/暂停状态（旋转角度接近 0f）
        val isAtResetPosition = abs(mBinding.rotationContainer.rotation) < 1f

        // 2. 如果外部强制要求动画，或者当前处于归位状态准备“落针”，则触发动画
        val shouldAnimate = animate || isAtResetPosition

        if (shouldAnimate) {
            // 如果切入唱片的动画正在运行，则不再重复创建，防止动画频繁重置引发抖动
            if (lastAnimator?.isRunning == true) return

            // 恢复播放时，立刻取消可能还没播完的【归位动画】
            returnAnimator?.cancel()
            lastAnimator?.cancel()

            lastAnimator = ObjectAnimator.ofFloat(
                mBinding.rotationContainer,
                "rotation",
                mBinding.rotationContainer.rotation,
                targetRotation
            ).apply {
                duration = 800 // 唱针落到唱片上的平滑过渡时间（毫秒）
                interpolator = DecelerateInterpolator()
                start()
            }
        } else {
            // 3. 【核心保护逻辑】只有当切入动画“没有在运行”时，才允许日常的进度微调
            // 这样可以完美防止 Activity 的高频定时器打断正在过渡的落针动画
            if (lastAnimator?.isRunning != true) {
                mBinding.rotationContainer.rotation = targetRotation

            }
        }
    }

    fun resetStylus() {
        isUserTouching = false
        lastAnimator?.cancel()
        returnAnimator?.cancel()
        returnAnimator = ObjectAnimator.ofFloat(
            mBinding.rotationContainer, "rotation",
            mBinding.rotationContainer.rotation, 0f
        ).apply {
            duration = 1000
            interpolator = DecelerateInterpolator()
            start()
        }
        // 在两秒内渐渐消失
        // 2. 阴影淡出动画（新增）
        mBinding.ivStylusShadow.apply {
            // 确保可见且 alpha=1.0（如果之前可能被修改过）
            visibility = VISIBLE
            alpha = 1.0f

            animate()  // ViewPropertyAnimator
                .alpha(0f)
                .setDuration(3000)
                .setInterpolator(DecelerateInterpolator())
                .withEndAction {
                    // 动画结束后隐藏视图（避免仍占用点击事件等）
//                    visibility = GONE
                }
                .start()
        }

    }

    fun setListener(l: OnStylusChangeListener) {
        listener = l

    }
}