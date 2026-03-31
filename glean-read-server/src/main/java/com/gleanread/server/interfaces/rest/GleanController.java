package com.gleanread.server.interfaces.rest;

import com.gleanread.server.application.dto.CaptureRequest;
import com.gleanread.server.application.service.GleanAppService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/glean")
@RequiredArgsConstructor
public class GleanController {

    private final GleanAppService gleanService;

    /**
     * 对接移动端：上报新产生的碎片
     */
    @PostMapping("/capture")
    public ResponseEntity<String> capture(@RequestBody CaptureRequest request) {
        gleanService.captureFragment(request);
        return ResponseEntity.ok("Fragment knowledge captured and stashed to inbox!");
    }
}
