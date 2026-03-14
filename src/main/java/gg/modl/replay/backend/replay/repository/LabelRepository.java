package gg.modl.replay.backend.replay.repository;

import gg.modl.replay.backend.replay.model.LabelDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LabelRepository extends MongoRepository<LabelDocument, String> {
    List<LabelDocument> findByReplayId(String replayId);
}
