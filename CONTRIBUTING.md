# 参与开发

欢迎 issue 和 PR。下面几条能让改动更好合并。

## 提 issue

带上这些信息：

- 手机型号、Android 版本（设置 → 关于 → 系统）
- 录音文件的**文件名**（号码可以打码，格式不要打码）
- 现象 + 截图/录屏
- 如果是 AI 相关：用的是哪个接口、哪个模型名（**不要贴 API Key**）

## 提 PR

- 一个 PR 做一件事，说明动机和结果
- 代码风格跟随现有文件：中文注释、Compose 里不自己造 Material 组件（能用官方的就用官方的）
- 新增图标：`res/drawable/ic_ms_<name>.xml`（24dp、viewport 960、`<group android:translateY="960">`）+ `IconSet.kt` 里加一行 `<name> to <含义>`
- 不要提交 `local.properties`、`app/libs/*.aar`、`build/`
- 一次提交前请确认 `./gradlew :app:assembleDebug` 能过

## 可能的坑

- `compileSdk 37` 是预览版，需要较新的 AGP 与 Android Studio
- 只编 `arm64-v8a`，模拟器（x86_64）跑不了转写
- 首帧播放页、列表滚动这类问题最好附一段录屏，光看代码不好判断

## 许可

提交的代码默认以 GPL-3.0 授权。
