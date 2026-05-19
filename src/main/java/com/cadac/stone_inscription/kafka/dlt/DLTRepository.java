package com.cadac.stone_inscription.kafka.dlt;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface DLTRepository extends MongoRepository<DLTEvent, String> {
    List<DLTEvent> findByTopic(String topic);
    List<DLTEvent> findByKey(String key);
}
