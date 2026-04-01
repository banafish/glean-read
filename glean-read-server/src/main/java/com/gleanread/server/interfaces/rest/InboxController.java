package com.gleanread.server.interfaces.rest;

import com.gleanread.server.domain.model.excerpt.Excerpt;
import com.gleanread.server.domain.model.tag.Tag;
import com.gleanread.server.application.service.InboxQueryAppService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inbox")
@RequiredArgsConstructor
public class InboxController {

    private final InboxQueryAppService inboxQueryAppService;

    /**
     * 实现任务 3.3: 供前台调用，分页展示当前所有还未被归入某一个大纲树节点的、处于野生状态的摘录。
     */
    @GetMapping("/excerpts")
    public ResponseEntity<Page<Excerpt>> getInboxExcerpts(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size) {

        Page<Excerpt> page = inboxQueryAppService.getInboxExcerpts(current, size);
        return ResponseEntity.ok(page);
    }

    /**
     * 实现任务 3.4: 获取所有标签，并按引用热度排倒序。供前端构建热力标签云。
     */
    @GetMapping("/tag-cloud")
    public ResponseEntity<List<Tag>> getTagCloud() {
        return ResponseEntity.ok(inboxQueryAppService.getTagCloud());
    }

    /**
     * 按标签筛选 Inbox 摘录，用于 AI 合成前的选料流程
     * GET /api/v1/inbox/excerpts/by-tag/{tagId}
     */
    @GetMapping("/excerpts/by-tag/{tagId}")
    public ResponseEntity<Page<Excerpt>> getExcerptsByTag(
            @PathVariable Long tagId,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size) {

        Page<Excerpt> page = inboxQueryAppService.getExcerptsByTag(tagId, current, size);
        return ResponseEntity.ok(page);
    }
}
