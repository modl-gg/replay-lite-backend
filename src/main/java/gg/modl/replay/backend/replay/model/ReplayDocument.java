package gg.modl.replay.backend.replay.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Document(collection = "replays")
@Data
@NoArgsConstructor
public class ReplayDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    private String replayId;

    private String serverId;
    private String serverIp;
    private String mcVersion;
    private long fileSize;
    private String status; // PENDING, COMPLETE, FAILED
    private String b2Key;
    private Date createdAt;

    public enum Status {
        PENDING, COMPLETE, FAILED
    }
}
