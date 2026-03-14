package gg.modl.replay.backend.replay.controller;

import gg.modl.replay.backend.replay.model.LabelDocument;
import gg.modl.replay.backend.replay.service.ReplayService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/v1/public/replay")
@RequiredArgsConstructor
public class PublicReplayController {

    private final ReplayService replayService;

    @GetMapping("/{replayId}")
    public ResponseEntity<?> getReplay(@PathVariable String replayId) {
        Optional<ReplayService.PublicReplayInfo> info = replayService.getPublicReplay(replayId);

        if (info.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ReplayService.PublicReplayInfo replay = info.get();
        return ResponseEntity.ok(Map.of(
            "replayId", replay.replayId(),
            "mcVersion", replay.mcVersion(),
            "fileSize", replay.fileSize(),
            "timestamp", replay.timestamp(),
            "replayUrl", replay.replayUrl() != null ? replay.replayUrl() : "",
            "status", replay.status()
        ));
    }

    @PostMapping("/{replayId}/label")
    public ResponseEntity<?> submitLabel(@PathVariable String replayId,
                                          @RequestBody LabelDocument label) {
        try {
            String labelId = replayService.submitLabel(replayId, label);
            return ResponseEntity.ok(Map.of("labelId", labelId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{replayId}/labels")
    public ResponseEntity<List<LabelDocument>> getLabels(@PathVariable String replayId) {
        List<LabelDocument> labels = replayService.getLabels(replayId);
        return ResponseEntity.ok(labels);
    }
}
