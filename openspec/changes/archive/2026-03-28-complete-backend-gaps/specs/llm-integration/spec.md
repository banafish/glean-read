## 新增需求

### 需求：可切换供应商的 LLM 适配层

系统必须通过策略接口隔离大语言模型调用，使得生产环境可接入真实 LLM（DeepSeek），本地/测试环境可退回 Mock，且两者切换不需要修改业务代码。

#### 场景：生产环境下 AI 合成调用真实大模型

- **当** 配置 `gleanread.ai.provider=deepseek` 且填写有效 API Key 时，用户触发 `POST /api/v1/knowledge/synthesis`
- **那么** 系统将碎片拼装的 Prompt 通过 DeepSeek API（OpenAI 兼容协议）发送给 `deepseek-chat` 模型，接收并返回真实的结构化 Markdown 大纲，并完成树节点创建和碎片归档

#### 场景：本地开发环境无 API Key 时退回 Mock

- **当** 配置 `gleanread.ai.provider=mock`（或未配置 API Key）时，用户触发合成
- **那么** 系统使用 `MockLlmAdapter` 返回预设的占位大纲文本，整个合成流程正常走完（节点入库、碎片关联），仅最终大纲内容为 Mock 数据
