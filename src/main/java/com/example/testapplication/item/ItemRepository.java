package com.example.testapplication.item;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

public interface ItemRepository extends MongoRepository<Item, String> {
    Optional<Item> findById(String id);
    List<Item> findByTagsIn(List<String> tags, Pageable pageable);

    @Query("{ 'value' : { $gt: ?1 }, 'tags' : { $in: ?0 } }")
    List<Item> findItemsByTagsWithValueGreaterThan(List<String> tags, int value, Pageable pageable);

    @Query("{ 'value' : { $lt: ?1 }, 'tags' : { $in: ?0 } }")
    List<Item> findItemsByTagsWithValueLessThan(List<String> tags, int value, Pageable pageable);

    @Query("{ 'value' : ?1, 'tags' : { $in: ?0 } }")
    List<Item> findItemsByTagsWithValue(List<String> tags, int value, Pageable pageable);
}
