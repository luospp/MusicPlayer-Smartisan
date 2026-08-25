# AGENTS.md

## 项目简介

ArtisanMusic（`com.yibao.music`）是一个仿锤子音乐播放器（Smartisan）的本地音乐播放器，单模块 Android 应用（`:app`）。核心是本地音乐播放，附带在线歌词/专辑图、收藏、歌单、定时关闭、通知栏与锁屏控制等功能。本项目仅用于学习，请勿商用。

## 技术栈与关键版本

- Java + Kotlin 混编（历史代码以 Java 为主，新代码优先 Kotlin）
- compileSdk 34 / targetSdk 33 / minSdk 26，仅构建 arm64-v8a
- Gradle 7.5 + AGP 7.4.2 + Kotlin 2.0.21（**需 JDK 17**，机器默认 JDK 19 会报 `Unsupported class file major version 63`；可用 Corretto 17 或 Android Studio 自带 JBR）
- GreenDAO 3.3（schemaVersion 10），生成代码提交在仓库内
- ViewBinding、AIDL 已开启
- RxJava2 + RxAndroid、Retrofit2 + Gson、OkHttp、Glide、Coroutines、LiveData/ViewModel
- 百度统计（mtj-sdk）、LeakCanary（仅 debug）

## 常用命令

```bash
JAVA_HOME=<JDK17> bash gradlew :app:assembleDebug  # 构建 Debug
JAVA_HOME=<JDK17> bash gradlew :app:assembleRelease  # 构建 Release（默认 debug 签名）
JAVA_HOME=<JDK17> bash gradlew :app:greendao       # 重新生成 GreenDAO 代码
JAVA_HOME=<JDK17> bash gradlew :app:installDebug   # 安装到已连接设备
bash verify.sh                                     # 全量验证（构建 + lint，见“验证”节）
```

## 目录结构（`app/src/main/java/com/yibao/music/`）

- `activity` / `activity/view` — Activity；播放页内的唱针等自定义视图
- `fragment` / `fragment/dialogfrag` — 各页面 Fragment；Dialog 与 BottomSheet
- `adapter` — RecyclerView / ViewPager 适配器
- `base` — 基类（BaseActivity、BaseFragment、BaseViewModel 等）
- `base/bindings` — ViewBinding 基类（BaseBindingFragment / BaseBindingActivity / BaseBindingDialog）
- `base/listener` — 回调接口
- `model` — 实体与业务数据；`model/greendao` — GreenDAO 生成代码（勿手改）；`model/qq` — 网络歌词/歌手图数据
- `network` — Retrofit / ApiService / QqMusicRemote
- `service` — MusicPlayService（播放）、LoadMusicDataService（扫描）、CountdownService（定时）
- `view` / `view/music` — 自定义 View；音乐页专属控件（QqControlBar、SmartisanControlBar、LyricsView、DiscView 等）
- `viewmodel` — Kotlin ViewModel（配合 LiveData / SingleLiveEvent）
- `manager` — 通知栏（MusicNotifyManager）、MediaSession
- `util` — 工具类，含 RxBus、SpUtils、Constant（常量集中在此）

## 代码约定

- 改动保持文件原语言：历史 Java 文件不改写成 Kotlin，新代码优先 Kotlin；不要做批量的语言迁移
- 注释用中文；沿用现有 `@author / Des / Time` 风格
- Java 成员变量沿用项目习惯的 `m` 前缀（如 `mBinding`、`mBus`）；Kotlin 按 Kotlin 惯例
- 事件通信优先复用 `RxBus`；页面状态用 ViewModel + LiveData/SingleLiveEvent
- 常量集中在 `util/Constant.java`，避免散落魔法数字
- 布局一律使用 ViewBinding（`XxxBinding.inflate(layoutInflater)`）
- 数据库访问通过 `MusicApplication` 暴露的 DAO 单例获取，不要直接 new DAO

## GreenDAO 注意事项

- 修改 `@Entity` 后：更新 `app/build.gradle` 中 `greendao.schemaVersion`（升级 +1），运行 `:app:greendao` 重新生成
- `model/greendao/` 下的生成代码会提交到仓库，请一并提交；不要手改任何标记 "THIS CODE IS GENERATED" 的文件
- 需要数据迁移时参考 `DaoUpgradeHelper` 中注释掉的 MigrationHelper 方案
- 构建时有已知告警：`:app:compileDebugKotlin` 隐式依赖 `:app:greendao` 的输出（Gradle 会禁用部分优化，不影响产物）；不要手动删除或改动生成代码来"规避"告警

## 已知环境注意事项

- 必须用 JDK 17 构建（见上）；构建需联网拉取依赖
- `gradlew` 当前没有可执行权限（`-rw-r--r--`），需用 `bash gradlew ...` 调用；如需直接 `./gradlew`，先 `chmod +x gradlew`
- `app/build.gradle` 中 `buildTypes`/`buildFeatures`/`compileOptions` 目前嵌套在 `defaultConfig` 内，Groovy 会把它们提升到 `android` 层解析，功能正常；如重构该文件，建议把它们移回 `android` 层
- Release 未配置正式签名，当前产物为 debug 签名
- `local.properties`、`keystore.properties`、`Apk/`、`.kotlin/` 等不入库

## Git 约定

- 主分支 `master`，开发分支 `dev`；新建分支建议使用 `codex/` 前缀
- commit message 用中文，沿用现有风格：`【分类】说明`，如 `【优化】...`、`【Fixed Bug】...`、`【UI】...`、`【代码优化】...`
- 所有本地提交必须经用户人工确认后才能执行，禁止未经确认直接 `git commit`
- 禁止自动推送远程；需要推送时提醒用户，由用户决定是否执行 `git push`

## 验证

- 全量验证命令（每次改动后必须通过）：`bash verify.sh`（等价于 `JAVA_HOME=<JDK17> bash gradlew :app:assembleDebug :app:lintDebug --console=plain`）
- 仓库暂无单元测试/仪器测试目录；新增逻辑时优先补 JUnit/Robolectric 测试，测试就绪后把 `:app:testDebugUnitTest` 纳入 verify.sh
- 涉及权限、通知、后台播放、锁屏的功能需在真机（Android 13+）验证
- 发版前检查 `versionCode` / `versionName`（当前 3 / 2.0.0_20250715_sz）

## 文档

- `README.md`（中文）与 `README-SC.md`（英文）如需更新保持双语同步
