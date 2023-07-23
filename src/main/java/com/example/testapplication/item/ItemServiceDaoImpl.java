package com.example.testapplication.item;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class ItemServiceDaoImpl implements ItemServiceDao {

    private final ItemRepository itemRepository;

    public ItemServiceDaoImpl(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    @Override
    public String createItem(int value, List<String> tags) {

        String uid = UUID.randomUUID().toString();
        long currentTime = System.currentTimeMillis();
        itemRepository.save(new Item(uid, currentTime, currentTime, value, tags));

        return uid;
    }

    @Override
    public Item getItem(String id) {
        return itemRepository.findById(id).orElse(null);
    }

    @Override
    public List<Item> searchitems(List<String> tags, int filterValue, FilterValueType filterType, OrderByType orderByType, OrderType orderType, int limit,  int offset) {

        Sort sort = Sort.by(orderType.toString());
        sort = OrderType.ASCENDING.equals(orderType)? sort.ascending():sort.descending();

        Pageable pageable = PageRequest.of(offset, limit, sort);

        return switch (filterType) {
            case GREATER_THAN -> itemRepository.findItemsByTagsWithValueGreaterThan(tags, filterValue, pageable);
            case LESS_THAN -> itemRepository.findItemsByTagsWithValueLessThan(tags, filterValue, pageable);
            case EQUALS -> itemRepository.findItemsByTagsWithValue(tags, filterValue, pageable);
            case NONE -> itemRepository.findByTagsIn(tags, pageable);
        };
    }

    /*
    The idea here is we will try to update. We will query the item then check it's version vs it's future version.
    If there's another thread that entered in and did it first, the second thread version check will fail, and so we
    retry.
     */
    @Override
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
    public void deleteItem(String id) {
        itemRepository.deleteById(id);
    }
}
