# 更新日志

## 0.1.0

第一个公开版本。

**列表**
- SAF 目录扫描、文件名解析、通讯录匹配
- 分组卡片列表、拖动柄时间气泡、下拉刷新
- 左右滑归档 / 删除、长按多选与浮动工具条
- 官方 SearchBar 搜索，命中高亮
- 归档页、收藏夹、设置（外观 / 录音 / AI / 同步 / 关于）

**播放**
- Gramophone 风格播放页：跟手拖动、波浪进度、AVD 花瓣播放键、倍速、收藏、分享
- MediaSessionService 前台播放，通知栏与锁屏控制

**转写与 AI**
- sherpa-onnx + SenseVoice-Small int8 端侧转写，模型首次使用时下载
- OpenAI 兼容接口：总结、对话（以全部转写为语料）
- 200 个 Material Symbols 图标库，转写完成后由模型自动匹配图标
- 批量匹配 / 删除所有图标

**同步**
- 转写、AI 总结、归档与收藏、聊天记录同步到任意 SAF 目录，双向合并

**界面**
- Material 3 Expressive：`SegmentedListItem`、按钮形状动画、`LoadingIndicator`
- 手机底部导航栏 / 平板左侧导航栏 + 播放页侧栏
