package com.gleanread.server.service.llm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Mock LLM 适配器（本地开发 / 测试环境默认）
 * 不消耗任何 API Token，返回预设占位大纲文本
 * 激活条件：gleanread.ai.provider=mock（或未配置，matchIfMissing=true）
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "gleanread.ai.provider", havingValue = "mock", matchIfMissing = true)
public class MockLlmAdapter implements LlmPort {

    @Override
    public String complete(String prompt) {
        log.info("[MockLlm] 使用 Mock 适配器，跳过真实 LLM 调用（Prompt 长度: {} 字符）", prompt.length());
        return "## AI 提炼合成（Mock）\n\n" +
               "> ⚠️ 当前使用本地 Mock 模式，未调用真实大语言模型。\n" +
               "> 设置 `gleanread.ai.provider=deepseek` 并配置 `DEEPSEEK_API_KEY` 环境变量以启用真实 AI 合成。\n\n" +
               "通过这套服务体系，碎片从孤岛走向了大陆，完成了其技术栈生命周期的跃迁。" +
               "由于融入了用户随手记下的灵感 `(引自 碎片#2)` ，这棵知识树将不仅仅是一个收藏架，" +
               "更是专属于开发者自己的第二大脑 `(引自 碎片#1)` ...";
    }
}
