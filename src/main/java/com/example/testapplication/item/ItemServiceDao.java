package com.example.testapplication.item;

import org.springframework.cache.annotation.Cacheable;

import java.util.List;

public interface ItemServiceDao {
    String createItem(int value, List<String> tags);
    Item getItem(String id);
    List<Item> searchItems(List<String> tags, int filterValue, FilterValueType filterType, OrderByType orderByType, OrderType orderType, int limit, int offset);
    Item updateItem(String id, int newValue);
    void deleteItem(String id);
}

