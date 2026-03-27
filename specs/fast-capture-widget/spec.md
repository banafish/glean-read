# 规范：移动端极速捕获组件 (fast-capture-widget)

## 概述
作为 GleanRead 的输入前哨站，通过 Android 系统拦截文字分享行为，实现秒级的打标、想法撰写和云端保存。

## 功能要求
1. **系统接管**: 注册 `Intent.ACTION_SEND`（支持纯文本和 URL）来接收系统分享意图。
2. **极速唤起**: 接收到意图后，不打开完整的 App 主界面，而是以半屏弹窗 (Transparent Activity/Dialog) 的形式弹出轻量级表单。
3. **关键输入项**:
   - 自动获取：剪贴板内容/分享的原文、URL。
   - 用户输入：个人想法（可选）、快速标签（多选，支持预判推荐匹配）。
   - 快速提交流程：最快一键点击“确认保存”送入后台 Inbox。

## API 与交互协定
- `POST /api/v1/glean/capture`: 从移动端上报新碎片的接口。
- **Payload示例**: `{ "content": "高亮文本..", "url": "http..", "user_thought": "我的想法..", "tags": ["Android", "性能"] }`。
