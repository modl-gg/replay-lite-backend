package gg.modl.replay.backend.replay.controller;

import gg.modl.replay.backend.auth.ApiKeyFilter;
import gg.modl.replay.backend.replay.model.ServerDocument;
import gg.modl.replay.backend.replay.service.ReplayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/v1/plugin/replay")
@RequiredArgsConstructor
public class PluginReplayController {

    private final ReplayService replayService;

    @PostMapping("/upload")
    public ResponseEntity<?> initUpload(
        @RequestBody UploadRequest request,
        HttpServletRequest httpRequest
    ) {
        ServerDocument server = (ServerDocument) httpRequest.getAttribute(ApiKeyFilter.ATTR_SERVER);
        if (server == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        String serverIp = resolveClientIp(httpRequest);

        try {
            ReplayService.UploadInitResult result = replayService.initUpload(
                server, serverIp,
                request.mcVersion(),
                request.fileSize(),
                request.signature()
            );

            return ResponseEntity.ok(Map.of(
                "replayId", result.replayId(),
                "uploadUrl", result.uploadUrl(),
                "viewerUrl", result.viewerUrl()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/confirm/{replayId}")
    public ResponseEntity<?> confirmUpload(
        @PathVariable String replayId,
        HttpServletRequest httpRequest
    ) {
        ServerDocument server = (ServerDocument) httpRequest.getAttribute(ApiKeyFilter.ATTR_SERVER);
        if (server == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        boolean confirmed = replayService.confirmUpload(replayId, server);
        if (confirmed) {
            return ResponseEntity.ok(Map.of("status", "COMPLETE"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "Confirmation failed"));
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    public record UploadRequest(String mcVersion, long fileSize, String signature) {}
}
