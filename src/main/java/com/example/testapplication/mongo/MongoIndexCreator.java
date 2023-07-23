/**
 * This class is created to make the expire time (ttl) dynamic and setable in application.properties file
 */
package com.example.testapplication.mongo;

import com.example.testapplication.item.Item;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class MongoIndexCreator {

    @Value("${db.item.expiration}")
    private int expireAfterSeconds;

    private final MongoTemplate mongoTemplate;

    public MongoIndexCreator(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @PostConstruct
    public void initIndicesAfterStartup() {
        IndexOperations indexOps = mongoTemplate.indexOps(Item.class);
        Index index = new Index().on("ttl", Direction.DESC).expire(expireAfterSeconds);
        indexOps.ensureIndex(index);
    }
}
