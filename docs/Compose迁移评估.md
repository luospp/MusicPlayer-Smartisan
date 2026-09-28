## ArtisanMusic 全面迁移 Jetpack Compose 评估

评估时间：2026-09-20
评估对象：`future` 分支 HEAD `85d28d0`
结论先行：**技术上可行，但当前不具备开工条件。** 建议先完成三项前置改造（工具链升级、Media3 播放内核迁移、数据层 Flow 化），再决定是否迁移；若迁移，采用「新页面 Compose + 老页面按需替换」的渐进路线，不建议大爆炸式重写。

---

## 一、现状盘点

### 1.1 代码规模

| 维度 | 数量 |
|---|---|
| 源文件 | 185 个（Java 133 / Kotlin 52） |
| 代码总行数 | 27343 行（Java 20477 / Kotlin 6866） |
| UI 层代码（activity / fragment / adapter / view / base） | 14227 行 |
| 非 UI 代码（util / model / network / service / manager / viewmodel） | 13030 行 |
| 自定义 View | 21 个，共 5332 行 |
| 布局 XML | 61 个，共 4701 行 |
| drawable | XML 77 个 + 位图 176 个 |
| Activity | 7 个业务 Activity（Manifest 中声明）+ 1 个未声明的测试用 `VieWTestActivity` |
| Fragment | 8 个业务页 + 14 个对话框 |
| RecyclerView Adapter | 15 个 |
| ViewModel | 4 个 |
| Service | 3 个 |

其中 `util/Api.java` 单文件 1920 行，全是硬编码的图片 URL 数组，属于应当直接删除的死代码（见《代码风险与修复清单》P0-12）。剔除它之后，真正需要迁移的代码约 25400 行。

### 1.2 技术栈现状

| 层 | 当前实现 | Compose 迁移相关度 |
|---|---|---|
| UI | View + XML + ViewBinding（反射式基类） | **全部重写** |
| 导航 | 7 个 Activity + Fragment + ViewPager2 + BottomNavigationView | **全部重写** |
| 列表 | RecyclerView + 自研 `BaseBindingAdapter` | 改 `LazyColumn` + `DiffUtil` 语义 |
| 状态 | LiveData + `SingleLiveEvent` + 自研 `RxBus`（PublishSubject） | **需重构为 StateFlow** |
| 异步 | RxJava2 + RxAndroid + 少量协程 | 需统一到协程 |
| 数据 | GreenDAO 3.3（同步阻塞 API，无 Flow 支持） | 需加 Repository 层 |
| 播放 | `android.media.MediaPlayer` + 手写 Binder + 自定义广播 + RemoteViews 通知 | 需迁 Media3，**通知部分无法 Compose 化** |
| 图片 | Glide 4.12 | 换 Coil（Compose 原生） |
| 屏幕适配 | AndroidAutoSize v1.2.1（`InitProvider` 已生效，设计稿 360x640dp） | **重要阻塞项，见 2.1** |
| 构建 | AGP 7.4.2 / Gradle 7.5 / Kotlin 2.0.21 / compileSdk 34 / targetSdk 33 | **必须先升级，见 2.2** |
| 测试 | **零测试**（无 `src/test`、无 `src/androidTest`） | 最大风险项，见 3.1 |

---

## 二、前置阻塞项（不解决无法开工）

### 2.1 AndroidAutoSize 的尺寸语义陷阱（高风险）

现状确认：合并后的 Manifest 中存在 `me.jessyan.autosize.InitProvider`（authorities `com.yibao.music.autosize-init-provider`），配合应用 Manifest 里的 `design_width_in_dp=360` / `design_height_in_dp=640`，说明 **AutoSize 处于激活状态**。

它的工作方式是在每个 Activity 的生命周期回调里改写 `Resources.getDisplayMetrics().density`，使得「设计稿 360dp 宽」等比拉伸到实际屏宽。后果是：

1. 现有 61 个布局里写的所有 `dp` 值，**语义不是真实 dp，而是「360dp 设计稿单位」**。在一个 411dp 宽的设备上，XML 里的 `16dp` 实际渲染为 `16 x 411/360 = 18.3dp`。
2. Compose 的 `LocalDensity` 由宿主 Context 的 `resources.displayMetrics.density` 派生。如果 AutoSize 继续运行，Compose **会继承被改写后的 density**，因此「XML 数值直接照搬到 Compose」在迁移期视觉上是能对齐的。
3. 但只要将来移除 AutoSize（Compose 项目通常不应该再依赖它），**全部尺寸会整体偏移**，每个页面都要重新核对。
4. AutoSize 对 `Dialog`、`PopupWindow`、`Fragment` 有单独的适配开关，与 `ComposeView` 的交互没有官方保证，混编期容易出现「弹窗内 Compose 尺寸不对」的问题。

必须先做的决策（二选一）：

- **方案 A（推荐）**：迁移前先移除 AutoSize，把全部尺寸按真实 dp 重新标定，跑一轮视觉回归；之后在纯真实 dp 的基础上迁 Compose。代价是迁移前多一次全量视觉返工，好处是之后每一步都是确定的。
- **方案 B**：保留 AutoSize 直到 Compose 迁移全部完成，最后再统一移除。代价是整个迁移期都背着这个隐式缩放，Compose 侧的间距体系（`MaterialTheme` 的尺寸令牌）与 AutoSize 的等比缩放会互相干扰，且最后一次移除时仍要做全量视觉返工。

无论哪种，**这一项都不能跳过**，否则迁移完成后会出现「在 360dp 设备正常、在 411dp / 折叠屏 / 平板上全线错位」的问题。

### 2.2 工具链必须先升级（硬性）

| 项 | 当前 | 迁移要求 | 说明 |
|---|---|---|---|
| AGP | 7.4.2 | **8.5+** | Compose Compiler Gradle 插件（`org.jetbrains.kotlin.plugin.compose`）要求 AGP 8.x；AGP 7.4.2 也不支持 compileSdk 35 |
| Gradle | 7.5 | **8.7+** | 构建日志已在提示「与 Gradle 8.0 不兼容」 |
| compileSdk | 34 | **35** | Compose 1.7+ / Material3 1.3+ 要求 |
| targetSdk | 33 | **35** | Google Play 2025-08 起要求 targetSdk 35，当前包已无法上架更新 |
| Kotlin | 2.0.21 | 2.0.21 可用 | Compose Compiler 已内置于 Kotlin 2.0，配 `kotlin("plugin.compose")` 即可 |
| JDK | 17 | 17 | 无需变更 |

升级 AGP/Gradle 本身不是纯配置工作，会连带触发：

- Manifest 适配：`registerReceiver` 必须显式声明导出标志（当前三处都没有，会直接抛 `SecurityException`，见 P0-08）；前台服务必须声明 `foregroundServiceType`。
- `nonFinalResIds` / `nonTransitiveRClass` 默认值变化，`R.id.xxx` 在 `when` 分支里不能再当常量用——项目里 `PlayActivity.onClick`、`BasePlayActivity.updateMusicBarAndVolumeBar`、`AboutFragment.onClick` 等多处是 `switch (id)` / `when (v.id)` 写法，需要改造。
- lint 的 Kotlin 分析当前因元数据版本不兼容而失效（P3-04），升级后会突然冒出大量此前从未被检查过的 Kotlin 告警，需要一轮集中处理。
- 移除 `android.enableJetifier`、`com.android.support:multidex`、`androidx.legacy:legacy-support-v4`。

### 2.3 播放内核必须先换掉（强烈建议）

Compose 迁移的本质是「把 UI 层改成声明式」，但本项目的 UI 层与播放层是**通过一个静态 Binder 字段 + 一个全局 Rx 事件总线硬耦合**的：

```text
MainActivity.companion.audioBinder  <- 静态字段，被 4 个页面直接读
RxBus (PublishSubject)              <- SERVICE_MUSIC / PLAY_STATUS / MUSIC_SPEAKER /
                                       MUSIC_LYRIC_OK / DELETE_SONG / HEADER_PIC_URI /
                                       COUNTDOWN_TIME / SONG_FAG_EDIT ...
```

`RxBus.getInstance()` 在 13 个文件里出现 16 次，另有 10 个文件通过基类的 `mBus` 字段收发事件。Compose 消费不了 `PublishSubject`：热事件流在 Compose 里需要 `SharedFlow` + 明确的 replay 策略，而「当前播放状态」这类**状态**（而非事件）应该是 `StateFlow<PlaybackState>`，靠状态驱动重组，而不是靠事件推 UI。如果不先把这层理顺，迁移出来的 Compose 代码会变成一堆 `LaunchedEffect` + `collectAsState` 包着 Rx 订阅的胶水，比现在更难维护。

因此建议先做（这一步即使不迁 Compose 也该做，能一次性消掉 P0-01、P0-02、P0-08、P1-01、P1-02、P1-03、P2-14 共 7 个问题）：

1. `MediaPlayer` 换成 `androidx.media3:media3-exoplayer`。
2. `MusicPlayService` 换成 `MediaSessionService`，通知改 `MediaStyle`（由 Media3 自动生成）。
3. UI 侧只持有一个 `MediaController`，包进 `PlaybackViewModel`，对外暴露：
   - `StateFlow<PlaybackState>`（当前曲目、是否播放、进度、时长、播放模式、是否收藏）
   - `SharedFlow<PlaybackEvent>`（一次性提示，如「暂无歌词」）
4. 废弃自定义广播与静态 Binder。

### 2.4 数据层需要 Flow 化

GreenDAO 是同步阻塞 API，没有 Flow/LiveData 支持，当前的「数据变了就手动 `mBus.post(...)` 通知各页面刷新」模式在 Compose 下无法工作（Compose 需要可观察的数据源才能自动重组）。

需要新增一个 Repository 层：

```kotlin
class MusicRepository(private val dao: MusicBeanDao) {
    private val invalidator = MutableStateFlow(0)
    val songs: Flow<List<MusicBean>> = invalidator.flatMapLatest {
        flow { emit(dao.queryBuilder().build().list()) }.flowOn(Dispatchers.IO)
    }
    suspend fun toggleFavorite(id: Long) { /* ... */; invalidator.value++ }
}
```

涉及 4 张表（`MusicBean`、`AlbumInfo`、`SearchHistoryBean`、`PlayListBean`）与全部读写入口。另一条路是直接把 GreenDAO 换成 Room（Room 原生返回 Flow，且能顺带解决 schemaVersion 迁移、主线程查询检测、`@Entity` 手写 Parcelable 等问题），但那是一次独立的数据层重构，工作量另计约 8~12 人日。

---

## 三、迁移工作量估算

估算基准：1 名熟悉 Compose 的 Android 工程师全职投入，含自测但不含产品/设计走查；按每天有效编码 6 小时计。

### 3.1 无测试基线是最大的成本放大器

项目当前**没有任何单元测试与仪器测试**（`app/src/test`、`app/src/androidTest` 目录不存在，`build.gradle` 里只有残留的 espresso 依赖）。这意味着：

- 21 个自定义 View、5332 行手势与绘制代码，迁移后只能靠人眼逐个比对。
- 唱针角度与进度的映射、搓碟变速曲线、歌词滚动定位这三处是「手感」类逻辑，没有量化断言就无法证明迁移后行为一致。
- 每次改动都要人肉回归 7 个 Activity x 5 个 Tab x 14 个对话框。

因此在估算里单列了「补测试基线」一项。**如果不补测试，实际工期建议在下面的估算上再加 30%~50% 的返工余量。**

### 3.2 分阶段估算

#### 阶段 0：前置改造（不产出任何 Compose 代码）

| 工作项 | 人日 | 说明 |
|---|---|---|
| AGP 7.4.2 升 8.x、Gradle 7.5 升 8.x、compileSdk/targetSdk 升 35 | 3 | 含 Manifest 前台服务类型、`RECEIVER_NOT_EXPORTED`、预测性返回、`R.id` 常量化改造 |
| 清理 lint 的 Kotlin 分析恢复后新增的告警 | 2 | 见 P3-04 |
| AutoSize 决策与执行（方案 A） | 3 | 含全量视觉尺寸重标定 |
| 播放内核迁 Media3 + `MediaSessionService` + `MediaStyle` 通知 | 7 | 同时消掉 7 个 P0/P1 |
| `PlaybackViewModel` + `StateFlow`/`SharedFlow` 替代静态 Binder | 4 | 全项目消灭 46 处 `audioBinder!!` 与 Java 侧 34 处裸解引用 |
| `RxBus` 改 `SharedFlow`（13 文件 / 16 处入口） | 3 | 事件与状态分离 |
| Repository 层 + GreenDAO Flow 封装 | 5 | 若同时换 Room 则再加 10 |
| 补测试基线（歌词解析、排序比较器、扫描差集、收藏读写、唱针角度映射） | 4 | 迁移的安全网 |
| 修完 P0 清单（14 项） | 4 | 见《代码风险与修复清单》第六节第一、二批 |
| **小计** | **35** | |

#### 阶段 1：Compose 基础设施与设计系统

| 工作项 | 人日 | 说明 |
|---|---|---|
| 依赖接入（Compose BOM、Material3、Navigation Compose、Coil、compose compiler plugin） | 1 | |
| 主题体系（色板、字体、形状、尺寸令牌） | 3 | 需从 `values/colors.xml`、`dimens`、61 个布局中反推现有视觉规范 |
| 深色模式 | 2 | **当前项目完全没有深色主题**，Compose 迁移是顺带补上的最佳时机 |
| 通用组件库：TopBar、两种底部控制条、歌曲列表项、吸顶字母、空态、加载脚、Snackbar 封装 | 7 | |
| Glide 换 Coil | 2 | 涉及 6 处调用点与自定义的 `RequestListener` 逻辑 |
| **小计** | **15** | |

#### 阶段 2：低风险页面（先拿收益、练手）

| 页面 | 现有规模 | 人日 |
|---|---|---|
| 启动页 `MusicActivity` + 权限流程 + 扫描进度 | 186 行 | 2 |
| 关于页 `AboutFragment` | 264 行 | 2 |
| 专辑墙 `AlbumWallActivity` | 30 行 | 1 |
| 歌单页 `PlayListActivity` + `PlayListFragment` + 新建/重命名/删除对话框 | 240 + 203 + 143 行 | 3 |
| 本地搜索 `SearchActivity` + 历史标签云 | 400 行 | 3 |
| 歌词搜索 `SearchLyricsActivity` + `LyricsFragment` | 190 + 47 行 | 2 |
| 14 个对话框改 `AlertDialog` / `ModalBottomSheet` | 约 1800 行 | 5 |
| **小计** | | **18** |

> 说明：`FlowLayoutView`（256 行）可直接换成 Compose 内置的 `FlowRow`；`CircleImageView`（263 行）换成 `Modifier.clip(CircleShape)`；`SwipeItemLayout`（848 行）换成 Material3 的 `SwipeToDismissBox`。这三个 View 合计 1367 行可以**直接删除**，是迁移中最划算的部分。

#### 阶段 3：主骨架与列表页

| 工作项 | 现有规模 | 人日 |
|---|---|---|
| `MainActivity` 骨架：`Scaffold` + `NavigationBar` + `HorizontalPager`（5 Tab）+ 底部控制条联动 | 767 行 | 3 |
| 导航图：7 个 Activity 的跳转关系 + `onActivityResult` 回调迁移 | — | 3 |
| 歌曲页：4 分类 + 多选编辑模式 + 吸顶字母 + 侧滑删除 | 141 + 253 + 173 行 | 6 |
| 艺术家页 + 详情列表 | 143 + 296 行 | 3 |
| 专辑页：列表/平铺双模式 + 详情 | 498 + 164 行 | 4 |
| 专辑页形变动画改 `SharedTransitionLayout` | 约 200 行手写 morph | 3 |
| 侧滑删除统一封装 | 848 行 View | 2 |
| **小计** | | **24** |

> 说明：`AlbumFragment` 里最近新增的列表与平铺形变动画（提交 `f012b21`）手写了约 200 行「收集可见封面坐标 - 建浮层 - 逐个位移动画」的逻辑。Compose 1.7+ 的 `SharedTransitionLayout` + `sharedElement` 可以用十几行声明式代码实现同样效果且更稳定。这是迁移能带来**明确正向收益**的典型例子，但它也意味着刚刚写完的代码要推倒重来。

#### 阶段 4：播放页（最高难度，占整个 UI 工作量的近一半）

| 工作项 | 现有规模 | 人日 | 难点 |
|---|---|---|---|
| 唱针 `CustTonearmView` | 277 行 | 8 | 极坐标命中测试（只有唱头区域响应）、pivot 精确对齐 XML 里的锚点 View、角度到进度的映射、归位动画、阴影层与主体同步旋转 |
| 唱片 `DiscView` + 搓碟 | 139 行 | 5 | 无限旋转动画与手势争夺、fling 惯性、按真实帧率驱动、`PlaybackParams` 变速变调联动、30ms 节流 |
| 歌词 `LyricsView` | 374 行 | 8 | `StaticLayout` 换 `TextMeasurer`、Canvas 逐行绘制、选中行大小字切换且高度不抖、自动滚动 + 手动拖拽 + 松手回弹、长歌词折行 |
| 播放页整体 `PlayActivity` + `BasePlayActivity` | 636 + 243 行 | 5 | 封面/歌词双视图切换、音量条与系统音量双向同步、播放模式、屏幕常亮、进度条 |
| 两种底部控制条 + 滑动切歌 + 实时歌词 | 215 + 157 行 | 5 | QQ 控制条的 `ViewPager2` 卡片滑动切歌语义 |
| 定时滚轮 `WheelView` | 314 行 | 3 | Compose 无对等组件，需用 `LazyColumn` + `snapFlingBehavior` 自研，或暂时 `AndroidView` 互操作保留 |
| 大图预览 `ZoomImageView` | 585 行 | 2 | 换 `transformable` + `graphicsLayer`，Compose 下反而更简单 |
| 唱机顶部装饰 `playing_stylus_top` / `MusicProgressView` / `ProgressBtn` | 约 300 行 | 2 | |
| **小计** | | **38** | |

#### 阶段 5：收尾

| 工作项 | 人日 |
|---|---|
| 全量视觉回归（360dp / 411dp / 折叠屏 / 平板 x 浅色深色） | 5 |
| 性能基线与调优（Macrobenchmark 启动耗时、列表滚动帧率、重组次数） | 3 |
| 清理旧代码（ViewBinding 反射基类、`BaseBindingAdapter`、遗留 View、`AndroidView` 互操作残留） | 2 |
| 通知栏与锁屏验收（RemoteViews 部分无法 Compose 化，随 Media3 迁移已完成） | 1 |
| 真机专项验证（Android 13/14/15 x 后台播放、锁屏、蓝牙、耳机拔出、定时） | 3 |
| **小计** | **14** |

### 3.3 汇总

| 阶段 | 人日 | 占比 |
|---|---|---|
| 0 前置改造 | 35 | 24% |
| 1 基础设施与设计系统 | 15 | 10% |
| 2 低风险页面 | 18 | 12% |
| 3 主骨架与列表 | 24 | 17% |
| 4 播放页 | 38 | 26% |
| 5 收尾 | 14 | 10% |
| **合计** | **144 人日** | 100% |

换算：

- 单人全职：约 **7 个月**（按每月 21 个工作日）。
- 双人并行：约 **4~4.5 个月**。并行度受限——阶段 0 与设计系统必须先串行完成，阶段 4 的三个手势控件也难以拆分给不同人。
- 加上无测试基线带来的返工余量（+30%~50%）：现实预期 **9~10 人月**。

### 3.4 代码量变化预期

| 项 | 现在 | 迁移后 | 变化 |
|---|---|---|---|
| 布局 XML | 61 个 / 4701 行 | 0 | -4701 行 |
| 自定义 View | 21 个 / 5332 行 | 约 6 个 Composable 文件 / 约 1600 行 | -3700 行 |
| Adapter | 15 个 / 约 1600 行 | 0（`LazyColumn` + item） | -1600 行 |
| ViewBinding 反射基类 | 5 个 / 约 800 行 | 0 | -800 行 |
| `Api.java` 死代码 | 1920 行 | 0 | -1920 行 |
| Activity / Fragment | 7 + 22 | 1~2 个 Activity + 若干 Composable | 结构大幅简化 |
| 屏幕页面代码 | 约 5000 行 | 约 3000 行 | -40% |

总体预计**减少 12000~14000 行代码**（约占现有 27343 行的一半），主要来自 XML 布局、Adapter、自定义 View 样板和基类反射的消失。

---

## 四、迁移特有风险

### 4.1 高风险

**R1 手势类控件的语义迁移（唱针 / 唱片 / 搓碟）**

这三个控件是本应用的核心体验，也是最难迁的部分。View 体系里 `CustTonearmView` 通过重写 `dispatchTouchEvent` 并在 `ACTION_DOWN` 时 `return false` 来实现「只有唱头区域才响应」；Compose 的指针输入模型是 `pointerInput` + `awaitPointerEventScope`，没有「不消费就交给父级」这种直接等价物，需要改用 `Modifier.pointerInput` 配合命中判定，或在父容器上做 `draggable` 的 `startDragImmediately` 控制。唱针的 pivot 对齐当前依赖 XML 里一个纯手工视觉对齐的锚点 View（`view_pivot_anchor`），Compose 下要改成 `graphicsLayer { rotationZ = ...; transformOrigin = TransformOrigin(px, py) }` 并用 `onSizeChanged` 计算归一化原点，稍有偏差就是肉眼可见的错位。

缓解：迁移前先把「角度到进度」的映射函数抽成纯函数并补单元测试；唱针、唱片、搓碟三个控件**最后迁移**，且在迁移期间用 `AndroidView` 保留原实现，做到可以随时回退。

**R2 歌词绘制的性能特征变化**

`LyricsView` 现在是 `AppCompatTextView` 子类，用 `StaticLayout` 缓存每行的选中态与普通态排版，`onDraw` 里只做平移绘制。Compose 下要换成 `Canvas` + `TextMeasurer` + `drawText`。两者的文本测量缓存机制、行高计算、折行策略都不完全一致，长歌词（100+ 行）在 `onSizeChanged` 时的一次性排版开销可能变成重组期的抖动。加上现在歌词滚动是 30ms 一次的 Rx 轮询驱动（P2-04），如果不改成 `withFrameNanos` 或动画驱动，Compose 下会产生每帧多次重组，性能可能**比现在更差**。

缓解：歌词滚动改为 `animateFloatAsState` / `Animatable` 驱动，避免轮询；`remember` 住 `TextMeasurer` 结果；用 Macrobenchmark 对比迁移前后的帧率再决定是否放行。

**R3 无测试基线 + 视觉规范靠反推**

项目没有设计规范文档，主题里 `colorPrimary`/`colorAccent` 全是 `@color/colorWhite`，颜色大量散落在 `util/ColorUtil.java` 的静态字段里（如 `lyricsSelecte`、`musicbarTvDown`、`successColor`、`errorColor`、`noClickText`），尺寸散落在 61 个布局的字面值里。建 Compose 设计系统等于**从零反推一套设计令牌**，反推错了就会在几十个页面同时体现。

缓解：阶段 1 先把 `ColorUtil` 与 `dimens.xml` 全量梳理成一份令牌表并截图存档，作为迁移后的比对基准。

### 4.2 中风险

**R4 事件总线到状态流的范式转换**

`RxBus` 目前混用了两类语义完全不同的东西：一类是**状态**（`SERVICE_MUSIC` 当前曲目、`PLAY_STATUS` 播放状态、`MUSIC_SPEAKER` 高亮位置），一类是**一次性事件**（`MUSIC_LYRIC_OK` 歌词下载结果、`DELETE_SONG` 删除通知、`HEADER_PIC_URI` 头像选择结果、`SONG_FAG_EDIT` 编辑模式切换）。用 `PublishSubject` 承载状态意味着「新订阅者拿不到当前值」，这正是现在需要在 `onResume` 里手动重放一堆 UI 更新的根因（`MainActivity.onResume` 连续调 7 个刷新方法）。迁移时必须逐条判定语义，选 `StateFlow`（有初值、可重放）还是 `SharedFlow(replay=0)` + `Channel`（一次性）。判错会导致「事件重复消费」或「状态丢失」，这类 bug 在 UI 层很难复现和定位。

**R5 导航重构**

7 个 Activity 之间有 `startActivityForResult`（歌词选择返回 `SONGMID`、头像裁剪返回 `Uri`）、`overridePendingTransition` 自定义转场、`singleTask`/`singleTop` 启动模式、以及主题里配的 `activityOpenEnterAnimation` 窗口动画。迁到 Navigation Compose 后：`startActivityForResult` 要改 `rememberLauncherForActivityResult` 或导航返回值；窗口级转场动画要改成 `AnimatedContentScope` 的 slide/fade（视觉效果会有差异）；`PlayActivity` 的 `singleTask` + 从通知栏回主页的栈行为需要重新设计。**转场动画是用户能直接感知的差异**，锤子风格的推入推出效果需要专门调。

**R6 主题与窗口**

当前主题是 `Theme.Design.Light.NoActionBar`（Material Components 1.x 的 Design 主题，不是 Material3），并设置了 `windowIsTranslucent=true` + `windowLightStatusBar`。Compose 需要 `ComponentActivity` + `MaterialTheme`；半透明 Activity 与 Compose 的 `WindowInsets` 处理、edge-to-edge（targetSdk 35 强制）叠加时容易出现状态栏遮挡或 inset 计算错误。需要一并改成 `enableEdgeToEdge()` + `Scaffold` 的 inset 消费。

**R7 混编期的 `AndroidView` 互操作成本**

渐进路线必然出现 Compose 页面里嵌 View（`WheelView`、唱针）、View 页面里嵌 `ComposeView` 的中间态。互操作的代价是：View 内部的 `ValueAnimator` 与 Compose 的重组/生命周期不同步（`DiscView` 的无限动画泄漏问题 P2-05 会在互操作下更难发现）；`AndroidView` 的 `update` 块如果写得不好会造成 View 反复重建；触摸事件在两套体系间传递时容易丢失。混编期越长，这类问题越多。

**R8 迁移期功能冻结与双份维护**

144 人日（现实 9~10 人月）期间，任何新需求都要面对「在旧 View 上做一次、之后在 Compose 上再做一次」的抉择。如果不明确冻结策略，实际工期会显著膨胀。

### 4.3 低风险（可忽略）

- `minSdk 26` 满足 Compose 要求（Compose 最低 21）。
- ViewBinding 反射基类（P3-01 提到的混淆风险）在 Compose 下自然消失。
- `SingleLiveEvent` 可换成 `Channel`/`SharedFlow`，无技术障碍。
- 通知栏 RemoteViews **无法也不需要** Compose 化，随 Media3 迁移改 `MediaStyle` 即可，不影响 UI 迁移范围。

---

## 五、迁移能带来的收益

| 收益 | 说明 |
|---|---|
| 代码量减半 | 预计减少 12000~14000 行（XML 布局、Adapter、View 样板、反射基类全部消失） |
| 消灭一类崩溃 | 全项目 46 处 `audioBinder!!` 与 4 处 `mMusicBean!!` 这类静态强引用崩溃，在状态驱动的 Compose 里从结构上不可能出现 |
| 手势与动画实现更简洁 | 专辑页 200 行手写 morph 动画换成 `SharedTransitionLayout` 十几行；`ZoomImageView` 585 行换成 `transformable` + `graphicsLayer` 约 40 行 |
| 删除三个大型自定义 View | `SwipeItemLayout`(848) / `CircleImageView`(263) / `FlowLayoutView`(256) 共 1367 行有官方对等实现 |
| 状态管理正确性 | 强制把「状态」与「事件」分开，消除现在 `onResume` 里手动重放 7 个 UI 刷新的模式 |
| 顺带补齐深色模式与平板适配 | 当前完全没有深色主题；Compose 的自适应布局让折叠屏/平板适配成本大幅降低 |
| 列表性能 | `LazyColumn` + 稳定的 key 天然取代 11 处 `notifyDataSetChanged`，配合 Coil 的默认缓存解决 P2-10 |
| 可测试性 | Composable 可用 `createComposeRule` 做 UI 测试，比 Espresso + ViewBinding 门槛低得多 |

---

## 六、不迁移会怎样

也需要如实评估维持现状的代价：

1. **无法上架**：targetSdk 33 已不满足 Google Play 2025-08 起的 35 要求，国内商店普遍要求 34+。这一项**无论是否迁 Compose 都必须做**（阶段 0 的前 3 人日）。
2. **升级阻塞**：AGP 7.4.2 / Gradle 7.5 已被新版 Android Studio 拒绝打开，团队换机器或升 IDE 就会卡住。
3. **lint 形同虚设**：Kotlin 代码（6866 行，含 `MainActivity`、`AlbumFragment`、`LyricsView` 等核心）目前完全不在静态检查覆盖范围内。
4. **技术债继续复利**：RxJava2 已停止维护 5 年；Gson 2.8.9 有已知 CVE；GreenDAO 3.3 已多年无更新。

但反过来说：**View + XML 本身不是缺陷来源**。本次盘点出的 14 个 P0 问题里，只有 P0-01（静态 Binder）与 UI 框架弱相关，其余 13 个（权限申请错误、广播安全、MediaPlayer 判空、时间单位写错、文件复制 bug、字符串越界、明文流量、超范围权限等）在 Compose 下**同样会发生**。也就是说，转 Compose 不能替代修 bug。

---

## 七、建议路线

### 7.1 推荐：三步走，先修再迁

**第一步（约 3~4 周）：把地基修好，与 Compose 无关但迁移必需**

1. 修完 P0 全部 14 项 + P1 里的通知栏（P1-01）、MediaSession（P1-02）、PendingIntent（P1-05）。
2. 升级 AGP 8.x / Gradle 8.x / compileSdk 35 / targetSdk 35，修复 lint 的 Kotlin 分析。
3. 播放内核迁 Media3 + `MediaSessionService`，UI 只通过 `PlaybackViewModel` 的 `StateFlow` 交互，废弃静态 Binder 与自定义广播。
4. 补第一批单元测试（歌词解析、排序比较器、扫描差集、收藏读写）。

这一步完成后，即使**决定不迁 Compose**，项目也已经从「无法上架 + 必崩」变成「可发布、可维护」，投入不会浪费。

**第二步（约 3~4 周）：数据层与基础设施**

1. Repository 层 + Flow 化（或直接把 GreenDAO 换成 Room，一并解决数据迁移与主线程查询检测）。
2. `RxBus` 逐条拆解为 `StateFlow` / `SharedFlow`。
3. 移除 AndroidAutoSize，全量尺寸按真实 dp 重标定并做视觉回归存档。
4. 接入 Compose，建设计系统（令牌表 + 通用组件 + 深色模式）。

**第三步（约 8~10 周）：页面迁移，按风险从低到高**

1. 关于页、启动页、专辑墙、歌单页、搜索页、歌词搜索页、14 个对话框（阶段 2，18 人日）——快速见效，验证设计系统与协作流程。
2. 主骨架与导航、歌曲/艺术家/专辑三个列表页（阶段 3，24 人日）。
3. 播放页：先整体页壳，再依次是大图预览、唱片、歌词、唱针（阶段 4，38 人日）。**唱针放最后**，并全程保留 `AndroidView` 回退路径。
4. 收尾回归与性能验收（阶段 5，14 人日）。

### 7.2 不推荐：大爆炸式重写

一次性重写全部 UI 的风险在于：项目没有测试、视觉规范需要反推、三个核心手势控件行为难以量化验证。任何一处手感差异都可能到最后一刻才被发现，而那时已经没有可回退的旧实现。**渐进式 + 每个阶段都有可发布产物**是唯一稳妥的路径。

### 7.3 一个务实的备选方案

如果目标只是「让项目健康、可上架、可持续维护」，而不是「用上 Compose」，那么**只做第一步 + 第二步的第 1、2 项**（约 6~8 周）性价比最高：修完 P0/P1、升级工具链、换掉播放内核、Flow 化数据层。UI 层保持 View + XML，额外收益是零回归风险。

考虑到这是一个学习性质的开源项目，如果**学习 Compose 本身就是目标之一**，那么走完整的三步走是值得的——唱针、搓碟、歌词滚动这三个控件恰好是练习 Compose 手势系统与 Canvas 绘制的极佳素材，且项目规模（25000 行、单人可掌控）也适合做完整迁移实验。这个判断取决于项目定位，需要作者自己拍板。

---

## 八、决策清单

开工前需要明确的几个问题：

| # | 问题 | 影响 |
|---|---|---|
| 1 | 是否要上架应用商店？ | 决定 targetSdk 升级是否为硬性前置（是则必须先做阶段 0） |
| 2 | AutoSize 采用方案 A（先移除）还是方案 B（最后移除）？ | 决定阶段 0 是否需要一次额外的全量视觉返工 |
| 3 | GreenDAO 保留并封装 Flow，还是直接换 Room？ | 后者多 10 人日，但省掉后续大量手工失效通知代码 |
| 4 | 迁移期是否冻结新功能？冻结多久？ | 不冻结则工期膨胀 30% 以上 |
| 5 | 是否需要补齐深色模式与平板适配？ | 若需要，阶段 1 多 4 人日、阶段 5 多 3 人日 |
| 6 | 是否接受转场动画视觉与现在有差异？ | Navigation Compose 无法 1:1 复刻窗口级 `dialog_push_in/out` 动画 |
| 7 | 唱针/搓碟的手感是否允许细微差异？ | 若要求 100% 一致，阶段 4 需多 5 人日做量化对比测试 |
