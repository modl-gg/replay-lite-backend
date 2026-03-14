package gg.modl.replay.backend.replay.repository;

import gg.modl.replay.backend.replay.model.ServerDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServerRepository extends MongoRepository<ServerDocument, String> {

    ServerDocument findByApiKey(String apiKey);
}
