package gg.modl.replay.backend.replay.repository;

import gg.modl.replay.backend.replay.model.ReplayDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReplayRepository extends MongoRepository<ReplayDocument, String> {

    Optional<ReplayDocument> findByReplayId(String replayId);
}
