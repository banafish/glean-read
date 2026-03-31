package com.gleanread.server.domain.port;

/**
 * LLM 策略接口 - 隔离大语言模型供应商实现
 * 通过 @ConditionalOnProperty 选择装配 DeepSeekLlmAdapter 或 MockLlmAdapter
 */
public interface LlmPort {

    /**
     * 发送 Prompt 并同步返回模型回复文本
     *
     * @param prompt 构建好的完整指令提示词
     * @return 模型返回的文本内容
     */
    String complete(String prompt);
}
