package gg.modl.replay.backend.replay.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

@Document(collection = "labels")
@Data
@NoArgsConstructor
public class LabelDocument {
    @Id
    private String id;

    @Indexed
    private String replayId;

    private Date submittedAt;
    private List<PlayerLabelEntry> players;

    @Data
    @NoArgsConstructor
    public static class PlayerLabelEntry {
        private String uuid;
        private String playerName;
        private String verdict;
        private int confidence;
        private List<CheatEntry> cheats;
        private String notes;
    }

    @Data
    @NoArgsConstructor
    public static class CheatEntry {
        private String type;
        private List<TimeRangeEntry> timeRanges;
    }

    @Data
    @NoArgsConstructor
    public static class TimeRangeEntry {
        private long startMs;
        private long endMs;
    }
}
