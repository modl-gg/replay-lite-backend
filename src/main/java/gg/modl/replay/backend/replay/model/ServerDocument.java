package gg.modl.replay.backend.replay.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

@Document(collection = "servers")
@Data
@NoArgsConstructor
public class ServerDocument {

    @Id
    private String id;

    private String name;

    @Indexed(unique = true)
    private String apiKey;

    private List<String> allowedIps;

    private Date createdAt;
}
