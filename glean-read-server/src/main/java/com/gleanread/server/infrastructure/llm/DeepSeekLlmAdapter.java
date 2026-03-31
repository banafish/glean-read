package com.gleanread.server.infrastructure.llm;

import com.gleanread.server.domain.port.LlmPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * DeepSeek LLM 适配器（生产环境）
 * 通过 OpenAI 兼容协议接入 DeepSeek API
 * 激活条件：gleanread.ai.provider=deepseek
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "gleanread.ai.provider", havingValue = "deepseek")
public class DeepSeekLlmAdapter implements LlmPort {

    private final ChatClient.Builder chatClientBuilder;

    @Override
    public String complete(String prompt) {
        log.info("[DeepSeek] 正在请求模型，Prompt 长度: {} 字符", prompt.length());
        ChatClient chatClient = chatClientBuilder.build();
        String result = chatClient.prompt(prompt).call().content();
        log.info("[DeepSeek] 模型响应成功");
        return result;
    }
}
