package com.gleanread.server.interfaces.rest;

import com.gleanread.server.application.dto.CreateTagRequest;
import com.gleanread.server.application.service.TagAppService;
import com.gleanread.server.domain.model.tag.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagAppService tagAppService;

    @PostMapping
    public ResponseEntity<Tag> createTag(@RequestBody CreateTagRequest request) {
        return ResponseEntity.ok(tagAppService.createTag(request.getTagName()));
    }
}
