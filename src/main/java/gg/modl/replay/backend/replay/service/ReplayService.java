package gg.modl.replay.backend.replay.service;

import gg.modl.replay.backend.replay.model.LabelDocument;
import gg.modl.replay.backend.replay.model.ReplayDocument;
import gg.modl.replay.backend.replay.model.ServerDocument;
import gg.modl.replay.backend.replay.repository.LabelRepository;
import gg.modl.replay.backend.replay.repository.ReplayRepository;
import gg.modl.replay.backend.util.HmacUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReplayService {

    private final ReplayRepository replayRepository;
    private final LabelRepository labelRepository;
    private final StorageService storageService;

    @Value("${replay.viewer.base-url:https://replays.modl.gg}")
    private String viewerBaseUrl;

    @Value("${replay.auth.hmac-secret:}")
    private String hmacSecret;

    /**
     * Initialize a replay upload: validate, create metadata, generate presigned URL.
     */
    public UploadInitResult initUpload(ServerDocument server, String serverIp,
                                        String mcVersion, long fileSize, String signature) {
        // Validate HMAC signature if secret is configured
        if (hmacSecret != null && !hmacSecret.isBlank()) {
            // Signature validation could be implemented here
            // For now, we trust the API key auth
        }

        String replayId = UUID.randomUUID().toString();
        String b2Key = "replays/" + replayId + ".modlreplay";

        // Create replay document in MongoDB
        ReplayDocument doc = new ReplayDocument();
        doc.setReplayId(replayId);
        doc.setServerId(server.getId());
        doc.setServerIp(serverIp);
        doc.setMcVersion(mcVersion);
        doc.setFileSize(fileSize);
        doc.setStatus(ReplayDocument.Status.PENDING.name());
        doc.setB2Key(b2Key);
        doc.setCreatedAt(new Date());
        replayRepository.save(doc);

        // Generate presigned upload URL
        String uploadUrl = storageService.createPresignedUploadUrl(
            b2Key, "application/octet-stream", fileSize
        );

        String viewerUrl = viewerBaseUrl + "/?id=" + replayId;

        log.info("Initialized replay upload: replayId={}, server={}, mcVersion={}, fileSize={}",
            replayId, server.getName(), mcVersion, fileSize);

        return new UploadInitResult(replayId, uploadUrl, viewerUrl);
    }

    /**
     * Confirm that a replay upload is complete.
     */
    public boolean confirmUpload(String replayId, ServerDocument server) {
        Optional<ReplayDocument> opt = replayRepository.findByReplayId(replayId);
        if (opt.isEmpty()) {
            log.warn("Confirm upload for unknown replay: {}", replayId);
            return false;
        }

        ReplayDocument doc = opt.get();

        // Verify ownership
        if (!doc.getServerId().equals(server.getId())) {
            log.warn("Server {} attempted to confirm replay {} owned by {}",
                server.getId(), replayId, doc.getServerId());
            return false;
        }

        // Optionally verify file exists in B2
        if (storageService.isConfigured()) {
            boolean exists = storageService.verifyUploadExists(doc.getB2Key());
            if (!exists) {
                log.warn("Replay file not found in storage: {}", doc.getB2Key());
                doc.setStatus(ReplayDocument.Status.FAILED.name());
                replayRepository.save(doc);
                return false;
            }
        }

        doc.setStatus(ReplayDocument.Status.COMPLETE.name());
        replayRepository.save(doc);

        log.info("Replay upload confirmed: replayId={}", replayId);
        return true;
    }

    /**
     * Get public replay metadata by UUID.
     */
    public Optional<PublicReplayInfo> getPublicReplay(String replayId) {
        return replayRepository.findByReplayId(replayId).map(doc -> {
            String replayUrl = storageService.getCdnUrl(doc.getB2Key());
            return new PublicReplayInfo(
                doc.getReplayId(),
                doc.getMcVersion(),
                doc.getFileSize(),
                doc.getCreatedAt() != null ? doc.getCreatedAt().getTime() : 0,
                replayUrl,
                doc.getStatus()
            );
        });
    }

    /**
     * Submit a label for a replay.
     */
    public String submitLabel(String replayId, LabelDocument label) {
        // Verify replay exists
        Optional<ReplayDocument> opt = replayRepository.findByReplayId(replayId);
        if (opt.isEmpty()) {
            throw new IllegalArgumentException("Replay not found: " + replayId);
        }

        label.setReplayId(replayId);
        label.setSubmittedAt(new Date());
        LabelDocument saved = labelRepository.save(label);

        log.info("Label submitted for replay {}: labelId={}", replayId, saved.getId());
        return saved.getId();
    }

    /**
     * Get all labels for a replay.
     */
    public List<LabelDocument> getLabels(String replayId) {
        return labelRepository.findByReplayId(replayId);
    }

    public record UploadInitResult(String replayId, String uploadUrl, String viewerUrl) {}
    public record PublicReplayInfo(String replayId, String mcVersion, long fileSize,
                                   long timestamp, String replayUrl, String status) {}
}
