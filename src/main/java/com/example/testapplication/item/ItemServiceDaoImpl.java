package com.example.testapplication.item;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class ItemServiceDaoImpl implements ItemServiceDao {

    private final ItemRepository itemRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public ItemServiceDaoImpl(ItemRepository itemRepository, RedisTemplate<String, Object> redisTemplate) {
        this.itemRepository = itemRepository;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public String createItem(int value, List<String> tags) {

        String uid = UUID.randomUUID().toString();
        long currentTime = System.currentTimeMillis();
        itemRepository.save(new Item(uid, currentTime, currentTime, value, tags));

        return uid;
    }

    @Override
    @Cacheable(value = "items", key = "#id", unless = "#result == null")
    public Item getItem(String id) {
        return itemRepository.findById(id).orElse(null);
    }

    @Override
    public List<Item> searchItems(List<String> tags, int filterValue, FilterValueType filterType, OrderByType orderByType, OrderType orderType, int limit, int offset) {
        String key = tags.toString() + filterValue + filterType.name() + orderByType.name() + orderType.name() + limit + offset;
        List<Item> items = (List<Item>) redisTemplate.opsForValue().get(key);
        if (items != null) {
            return items; // return cached value if present
        }

        Sort sort = Sort.by(orderByType.toString());
        sort = OrderType.ASCENDING.equals(orderType) ? sort.ascending() : sort.descending();

        Pageable pageable = PageRequest.of(offset, limit, sort);

        items = switch (filterType) {
            case GREATER_THAN -> itemRepository.findItemsByTagsWithValueGreaterThan(tags, filterValue, pageable);
            case LESS_THAN -> itemRepository.findItemsByTagsWithValueLessThan(tags, filterValue, pageable);
            case EQUALS -> itemRepository.findItemsByTagsWithValue(tags, filterValue, pageable);
            case NONE -> itemRepository.findByTagsIn(tags, pageable);
        };

        redisTemplate.opsForValue().set(key, items); // cache the result

        return items;
    }

    @Override
    @CacheEvict(value = {"items", "searchItems"}, allEntries = true)
    public Item updateItem(String id, int newValue) {

        final int MAX_RETRIES = 50;

        for(int i = 0; i < MAX_RETRIES; i++) {

            Item item = itemRepository.findById(id).orElse(null);
            if(item == null)
                return null;

            // check for version conflicts
            long originalVersion = item.getVersion();
            item.setValue(newValue);
            item.setLastUpdatedTime(System.currentTimeMillis());
            item.setTtl(new Date(System.currentTimeMillis()));

            //note this can throw an optimisticLockingFailure exception itself which we will error back to client to resent request
            item = itemRepository.save(item);

            if (item.getVersion() != originalVersion + 1) {
                continue;
            }
            return item;
        }

        throw new OptimisticLockingFailureException("Conflict detected when updating item " + id + " after " + MAX_RETRIES + " attempts");
    }


    @Override
    @CacheEvict(value = {"items", "searchItems"}, allEntries = true)
    public void deleteItem(String id) {
        itemRepository.deleteById(id);
    }
}
