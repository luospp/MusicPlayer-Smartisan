package com.yibao.music.fragment

import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.yibao.music.R
import com.yibao.music.adapter.AlbumViewPagerAdapter
import com.yibao.music.adapter.DetailsViewAdapter
import com.yibao.music.base.bindings.BaseBindingAdapter
import com.yibao.music.base.bindings.BaseMusicFragmentDev
import com.yibao.music.databinding.AlbumFragmentBinding
import com.yibao.music.fragment.dialogfrag.MoreMenuBottomDialog
import com.yibao.music.model.AlbumInfo
import com.yibao.music.model.MusicBean
import com.yibao.music.model.greendao.MusicBeanDao
import com.yibao.music.util.ColorUtil
import com.yibao.music.util.Constant
import com.yibao.music.util.MusicListUtil
import com.yibao.music.view.music.MusicToolBar.OnToolbarClickListener
import com.yibao.music.viewmodel.AlbumViewModel

/**
 * @项目名： ArtisanMusic
 * @包名： com.yibao.music.album
 * @文件名: AlbumFragment
 * @author: Stran
 * @Email: www.strangermy@outlook.com / www.stranger98@gmail.com
 * @创建时间: 2018/2/8 20:01
 * @描述： {TODO}
 */
class AlbumFragment : BaseMusicFragmentDev<AlbumFragmentBinding>(), View.OnClickListener,
    SwipeRefreshLayout.OnRefreshListener {
    private val mViewModel: AlbumViewModel by lazy { gets(AlbumViewModel::class.java) }
    private var mDetailsAdapter: DetailsViewAdapter? = null
    private var isShowDetailsView = false
    private var mDetailList = ArrayList<MusicBean>()
    private lateinit var mPagerAdapter: AlbumViewPagerAdapter
    private var mSwitchOverlay: FrameLayout? = null
    private var mSwitchOverlayCleanup: Runnable? = null
    override fun initView() {
        mBinding.musicBar.setToolbarTitle(getString(R.string.music_album))
        mBinding.musicBar.isShowAlbumWall(visibility = true)
    }


    override fun initData() {
        mPagerAdapter = AlbumViewPagerAdapter(this, mViewModel)
        mBinding.viewPager2Album.adapter = mPagerAdapter
        // 预创建平铺页，保证首次切换时 Fragment 与封面都已就绪，不会闪现空壳页面
        mBinding.viewPager2Album.offscreenPageLimit = 1
        mBinding.viewPager2Album.isUserInputEnabled = false
        mBinding.viewPager2Album.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                switchCategory(position)
            }
        })

        initListener()
    }

    override fun onResume() {
        super.onResume()
        //    接收AlbumAdapter发过来的当前点击Item的Position
        mViewModel.albumViewModel.observe(this) { bean ->
            showDetailsView(bean)
        }

    }


    private fun initListener() {

        mBinding.albumCategory.ivAlbumCategoryRandomPlay.setOnClickListener(this)
        mBinding.albumCategory.albumCategoryTileLl.setOnClickListener(this)
        mBinding.albumCategory.albumCategoryListLl.setOnClickListener(this)
        mBinding.albumCategory.ivAlbumCategoryPlay.setOnClickListener(this)

        mBinding.musicBar.setClickListener(object : OnToolbarClickListener {
            override fun clickEdit() {
                if (isShowDetailsView) {
                    showDetailsView(null)
                }
            }

            override fun switchMusicControlBar() {
                switchControlBar()
            }

            override fun clickDelete() {}
        })
    }


    override fun onClick(v: View) {
        when (v.id) {
            R.id.iv_album_category_random_play, R.id.iv_album_category_play -> randomPlayMusic(6)
            R.id.album_category_list_ll -> switchCategory(Constant.NUMBER_ZERO)
            R.id.album_category_tile_ll -> switchCategory(Constant.NUMBER_ONE)
            else -> {}
        }
    }

    private fun showDetailsView(albumInfo: AlbumInfo?) {
        if (isShowDetailsView) {
            mBinding.detailsView.visibility = View.GONE
            mBinding.musicBar.setToolbarTitle(getString(R.string.music_album))
            mBinding.musicBar.setTvEditVisibility(false)
        } else {
            if (albumInfo != null) {
                mBinding.detailsView.visibility = View.VISIBLE
                mBinding.musicBar.setTvEditVisibility(true)
                mBinding.musicBar.setToolbarTitle(albumInfo.albumName)
                mBinding.musicBar.setTvEditText(R.string.music_album)

                mDetailList = MusicListUtil.sortByAbc(
                    mMusicBeanDao.queryBuilder()
                        .where(MusicBeanDao.Properties.Album.eq(albumInfo.albumName)).build()
                        .list()
                ) as ArrayList<MusicBean>
                // DetailsView播放音乐需要的参数
                mBinding.detailsView.setDataFlag(
                    childFragmentManager,
                    mDetailList.size,
                    albumInfo.albumName,
                    Constant.NUMBER_SEVEN
                )
                mDetailsAdapter = DetailsViewAdapter(
                    mContext,
                    mDetailList,
                    Constant.NUMBER_SEVEN,
                    albumInfo.albumName
                )
                mBinding.detailsView.setAdapter(Constant.NUMBER_TWO, albumInfo, mDetailsAdapter)
                mDetailsAdapter!!.setOnItemMenuListener(object :
                    BaseBindingAdapter.OnOpenItemMoreMenuListener {
                    override fun openClickMoreMenu(position: Int, musicBean: MusicBean) {
                        MoreMenuBottomDialog.newInstance(
                            musicBean,
                            position,
                            isNeedScore = false,
                            isNeedSetTime = false
                        ).getBottomDialog(mActivity)
                    }
                })
            }
        }
        isShowDetailsView = !isShowDetailsView

    }

    override fun deleteItem(musicPosition: Int) {
        super.deleteItem(musicPosition)
        if (mDetailsAdapter != null) {
            mDetailList.removeAt(musicPosition)
            mDetailsAdapter!!.setData(mDetailList)
        }
    }


    private fun switchCategory(showType: Int) {
        updateCategoryUi(showType)
        if (mBinding.viewPager2Album.currentItem == showType) {
            return
        }
        if (showType == Constant.NUMBER_ONE) {
            morphListToTile()
        } else {
            morphTileToList()
        }
    }

    private fun updateCategoryUi(showType: Int) {
        if (showType == Constant.NUMBER_ZERO) {
            mBinding.albumCategory.albumCategoryListLl.setBackgroundResource(R.drawable.btn_category_start_down_selector)
            mBinding.albumCategory.ivAlbumCategoryList.setImageResource(R.drawable.album_category_list_down_selector)
            mBinding.albumCategory.tvAlbumCategoryList.setTextColor(ColorUtil.wihtle)
            mBinding.albumCategory.albumCategoryTileLl.setBackgroundResource(R.drawable.btn_category_end_selector)
            mBinding.albumCategory.ivAlbumCategoryTile.setImageResource(R.drawable.album_category_tile_selector)
            mBinding.albumCategory.tvAlbumCategoryTile.setTextColor(ColorUtil.textName)
        } else if (showType == Constant.NUMBER_ONE) {
            mBinding.albumCategory.albumCategoryTileLl.setBackgroundResource(R.drawable.btn_category_end_down_selector)
            mBinding.albumCategory.ivAlbumCategoryTile.setImageResource(R.drawable.album_category_tile_down_selector)
            mBinding.albumCategory.tvAlbumCategoryTile.setTextColor(ColorUtil.wihtle)
            mBinding.albumCategory.albumCategoryListLl.setBackgroundResource(R.drawable.btn_category_start_selector)
            mBinding.albumCategory.ivAlbumCategoryList.setImageResource(R.drawable.album_category_list_selector)
            mBinding.albumCategory.tvAlbumCategoryList.setTextColor(ColorUtil.textName)
        }
    }

    /**
     * 列表模式切换到平铺模式：以列表第一个专辑图片为起点原地放大，
     * 后续专辑图片依次放大并向右上移动，最终形成三列平铺。
     */
    private fun morphListToTile() {
        val source = mPagerAdapter.getAlbumCategoryFragment(Constant.NUMBER_ZERO)
        val target = mPagerAdapter.getAlbumCategoryFragment(Constant.NUMBER_ONE)
        val sourceRv = source?.getModeRecyclerView()
        val targetRv = target?.getModeRecyclerView()
        if (sourceRv == null || targetRv == null) {
            mBinding.viewPager2Album.setCurrentItem(Constant.NUMBER_ONE, false)
            return
        }
        // 列表滚动过的话先回到顶部，保证变形从第一个专辑开始、且与平铺页的位置对齐
        if (sourceRv.computeVerticalScrollOffset() > 0) {
            sourceRv.scrollToPosition(0)
        }
        if (targetRv.computeVerticalScrollOffset() > 0) {
            targetRv.scrollToPosition(0)
        }
        sourceRv.post {
            if (!isAdded) return@post
            val sourceCovers = collectVisibleCovers(sourceRv, isTile = false)
            if (sourceCovers.isEmpty()) {
                mBinding.viewPager2Album.setCurrentItem(Constant.NUMBER_ONE, false)
                return@post
            }
            // 等目标页布局稳定后再隐藏（必须在切换前完成，否则会闪出完整目标页一帧）
            targetRv.post {
                if (!isAdded || targetRv.childCount == 0) {
                    mBinding.viewPager2Album.setCurrentItem(Constant.NUMBER_ONE, false)
                    return@post
                }
                target.hideModeViews()
                mBinding.viewPager2Album.setCurrentItem(Constant.NUMBER_ONE, false)
                mBinding.viewPager2Album.post {
                    if (isAdded) {
                        morphCovers(sourceCovers, target, targetRv, isToTile = true)
                    }
                }
            }
        }
    }

    /**
     * 平铺模式切换到列表模式：与列表切换到平铺相反，封面依次缩小并向左下方移动。
     */
    private fun morphTileToList() {
        val source = mPagerAdapter.getAlbumCategoryFragment(Constant.NUMBER_ONE)
        val target = mPagerAdapter.getAlbumCategoryFragment(Constant.NUMBER_ZERO)
        val sourceRv = source?.getModeRecyclerView()
        val targetRv = target?.getModeRecyclerView()
        if (sourceRv == null || targetRv == null) {
            mBinding.viewPager2Album.setCurrentItem(Constant.NUMBER_ZERO, false)
            return
        }
        // 平铺页若滚动过也先回到顶部；列表页同样回到顶部，保证两边条目位置对齐
        if (sourceRv.computeVerticalScrollOffset() > 0) {
            sourceRv.scrollToPosition(0)
        }
        if (targetRv.computeVerticalScrollOffset() > 0) {
            targetRv.scrollToPosition(0)
        }
        sourceRv.post {
            if (!isAdded) return@post
            val sourceCovers = collectVisibleCovers(sourceRv, isTile = true)
            if (sourceCovers.isEmpty()) {
                mBinding.viewPager2Album.setCurrentItem(Constant.NUMBER_ZERO, false)
                return@post
            }
            // 等目标页布局稳定后再隐藏（必须在切换前完成，否则会闪出完整目标页一帧）
            targetRv.post {
                if (!isAdded || targetRv.childCount == 0) {
                    mBinding.viewPager2Album.setCurrentItem(Constant.NUMBER_ZERO, false)
                    return@post
                }
                target.hideModeViews()
                mBinding.viewPager2Album.setCurrentItem(Constant.NUMBER_ZERO, false)
                mBinding.viewPager2Album.post {
                    if (isAdded) {
                        morphCovers(sourceCovers, target, targetRv, isToTile = false)
                    }
                }
            }
        }
    }

    private data class CoverInfo(
        val position: Int,
        val drawable: Drawable?,
        val rect: Rect
    )

    /**
     * 收集当前可见的专辑封面：适配器位置、图片、屏幕位置
     */
    private fun collectVisibleCovers(recyclerView: RecyclerView, isTile: Boolean): List<CoverInfo> {
        val covers = mutableListOf<CoverInfo>()
        for (i in 0 until recyclerView.childCount) {
            val child = recyclerView.getChildAt(i) ?: continue
            val position = recyclerView.getChildAdapterPosition(child)
            if (position < 0) continue
            val coverView = if (isTile) {
                child.findViewById<ImageView>(R.id.iv_album_tile_album)
            } else {
                child.findViewById<ImageView>(R.id.iv_item_album_list)
            } ?: continue
            val rect = viewRectInWindow(coverView)
            if (rect.width() <= 0 || rect.height() <= 0) continue
            val drawable = coverView.drawable?.constantState?.newDrawable() ?: coverView.drawable
            covers.add(CoverInfo(position, drawable, rect))
        }
        return covers
    }

    /**
     * 用覆盖层将源页封面搬运、缩放到目标页位置，落地后淡入目标页封面与文字
     */
    private fun morphCovers(
        sourceCovers: List<CoverInfo>,
        target: AlbumCategoryFragment,
        targetRv: RecyclerView,
        isToTile: Boolean
    ) {
        val overlay = createSwitchOverlay()
        val pagerLoc = IntArray(2)
        mBinding.viewPager2Album.getLocationInWindow(pagerLoc)

        // 计算每个封面在目标页的落地位置
        val targetRects = mutableMapOf<Int, Rect>()
        val coveredPositions = mutableSetOf<Int>()
        for (info in sourceCovers) {
            val targetCover = findCoverView(targetRv, info.position, isToTile) ?: continue
            targetRects[info.position] = viewRectInWindow(targetCover)
            coveredPositions.add(info.position)
        }
        if (targetRects.isEmpty()) {
            mBinding.root.removeView(overlay)
            mSwitchOverlay = null
            // 兜底：恢复目标页可见条目，避免切换后出现空白
            for (i in 0 until targetRv.childCount) {
                val position = targetRv.getChildAdapterPosition(targetRv.getChildAt(i))
                if (position >= 0) {
                    target.revealModeItem(position)
                }
            }
            return
        }

        var index = 0
        for (info in sourceCovers) {
            val targetRect = targetRects[info.position] ?: continue
            val localSource = Rect(info.rect).apply { offset(-pagerLoc[0], -pagerLoc[1]) }
            val localTarget = Rect(targetRect).apply { offset(-pagerLoc[0], -pagerLoc[1]) }
            val overlayView = ImageView(requireContext()).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                setImageDrawable(info.drawable)
                pivotX = 0f
                pivotY = 0f
            }
            overlay.addView(
                overlayView,
                FrameLayout.LayoutParams(info.rect.width(), info.rect.height())
            )
            val layoutParams = overlayView.layoutParams as FrameLayout.LayoutParams
            layoutParams.leftMargin = localSource.left
            layoutParams.topMargin = localSource.top
            overlayView.layoutParams = layoutParams

            val delay = index * SWITCH_ITEM_STAGGER
            overlayView.post {
                if (!isAdded) return@post
                val scaleX = targetRect.width().toFloat() / info.rect.width()
                val scaleY = targetRect.height().toFloat() / info.rect.height()
                overlayView.animate()
                    .scaleX(scaleX)
                    .scaleY(scaleY)
                    .translationX((localTarget.left - localSource.left).toFloat())
                    .translationY((localTarget.top - localSource.top).toFloat())
                    .setDuration(SWITCH_FLY_DURATION)
                    .setStartDelay(delay)
                    .setInterpolator(DecelerateInterpolator())
                    .withEndAction {
                        overlayView.animate().alpha(0f).setDuration(SWITCH_OVERLAY_FADE)
                            .withEndAction { overlay.removeView(overlayView) }
                            .start()
                        target.revealModeItem(
                            info.position,
                            imageDuration = SWITCH_REVEAL_IMAGE,
                            textDuration = SWITCH_REVEAL_TEXT
                        )
                    }
                    .start()
            }
            index++
        }

        // 未被封面覆盖到的目标条目，等主体动画结束后再显现
        targetRv.post {
            for (i in 0 until targetRv.childCount) {
                val child = targetRv.getChildAt(i) ?: continue
                val position = targetRv.getChildAdapterPosition(child)
                if (position >= 0 && position !in coveredPositions) {
                    target.revealModeItem(
                        position,
                        delay = SWITCH_EXTRA_REVEAL_DELAY,
                        imageDuration = SWITCH_REVEAL_IMAGE,
                        textDuration = SWITCH_REVEAL_TEXT
                    )
                }
            }
        }

        // 动画结束后移除覆盖层，并强制恢复目标页可见条目，兜底避免残留空白行
        val cleanup = Runnable {
            if (mSwitchOverlay === overlay) {
                mBinding.root.removeView(overlay)
                mSwitchOverlay = null
                target.restoreAllModeViews()
            }
        }
        mSwitchOverlayCleanup?.let { mBinding.root.removeCallbacks(it) }
        mSwitchOverlayCleanup = cleanup
        mBinding.root.postDelayed(
            cleanup,
            index * SWITCH_ITEM_STAGGER + SWITCH_FLY_DURATION + SWITCH_OVERLAY_FADE + SWITCH_CLEANUP_MARGIN
        )
    }

    private fun findCoverView(recyclerView: RecyclerView, position: Int, isTile: Boolean): View? {
        val holder = recyclerView.findViewHolderForAdapterPosition(position) ?: return null
        return if (isTile) {
            holder.itemView.findViewById(R.id.iv_album_tile_album)
        } else {
            holder.itemView.findViewById(R.id.iv_item_album_list)
        }
    }

    private fun viewRectInWindow(view: View): Rect {
        val loc = IntArray(2)
        view.getLocationInWindow(loc)
        return Rect(loc[0], loc[1], loc[0] + view.width, loc[1] + view.height)
    }

    /**
     * 创建覆盖在列表区域之上的动画层，负责搬运专辑封面
     */
    private fun createSwitchOverlay(): FrameLayout {
        mSwitchOverlay?.let {
            mBinding.root.removeView(it)
            mSwitchOverlay = null
        }
        val overlay = FrameLayout(requireContext())
        // 0dp + 四条约束，覆盖层与 ViewPager 区域完全重合；
        // 不能使用 MATCH_PARENT，否则 ConstraintLayout 会忽略约束把覆盖层放到整个 Fragment 左上角
        val layoutParams = ConstraintLayout.LayoutParams(0, 0)
        layoutParams.topToTop = R.id.view_pager2_album
        layoutParams.bottomToBottom = R.id.view_pager2_album
        layoutParams.startToStart = R.id.view_pager2_album
        layoutParams.endToEnd = R.id.view_pager2_album
        mBinding.root.addView(overlay, layoutParams)
        mSwitchOverlay = overlay
        return overlay
    }

    override fun onDestroyView() {
        mSwitchOverlayCleanup?.let { mBinding.root.removeCallbacks(it) }
        mSwitchOverlay?.let { mBinding.root.removeView(it) }
        mSwitchOverlay = null
        super.onDestroyView()
    }

    companion object {
        fun newInstance(): AlbumFragment {
            return AlbumFragment()
        }

        // ===== 列表/平铺切换动画节奏（单位：毫秒），调整这里即可整体变速 =====
        const val SWITCH_FLY_DURATION = 360L        // 封面飞行时长
        const val SWITCH_ITEM_STAGGER = 30L         // 每个条目错峰间隔
        const val SWITCH_OVERLAY_FADE = 90L         // 落地时覆盖层淡出时长
        const val SWITCH_REVEAL_IMAGE = 150L        // 落地后封面淡入时长
        const val SWITCH_REVEAL_TEXT = 200L         // 落地后专辑名称等文字淡入时长
        const val SWITCH_EXTRA_REVEAL_DELAY = 550L  // 未被封面覆盖到的条目延迟显现
        const val SWITCH_CLEANUP_MARGIN = 600L      // 动画结束后清理覆盖层的余量
    }


    override fun onBackPressed(): Boolean {
        return if (isShowDetailsView && isVisible && isResumed) {
            showDetailsView(null)
            true
        } else {
            false
        }
    }

    override fun onRefresh() {
        showDetailsView(null)

    }
}
