package com.gleanread.server.interfaces.rest;

import com.gleanread.server.domain.model.fragment.Fragment;
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
     * 实现任务 3.3: 供前台调用，分页展示当前所有还未被归入某一个大纲树节点的、处于野生状态的碎片。
     */
    @GetMapping("/fragments")
    public ResponseEntity<Page<Fragment>> getInboxFragments(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size) {

        Page<Fragment> page = inboxQueryAppService.getInboxFragments(current, size);
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
     * 新增任务 2.4: 按标签筛选 Inbox 碎片，用于 AI 合成前的选料流程
     * GET /api/v1/inbox/fragments/by-tag/{tagId}
     */
    @GetMapping("/fragments/by-tag/{tagId}")
    public ResponseEntity<Page<Fragment>> getFragmentsByTag(
            @PathVariable Long tagId,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size) {

        Page<Fragment> page = inboxQueryAppService.getFragmentsByTag(tagId, current, size);
        return ResponseEntity.ok(page);
    }
}
