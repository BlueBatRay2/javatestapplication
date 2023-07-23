package com.example.testapplication.item;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/item")
public class ItemController {

    private final ItemServiceDao itemServiceDao;

    public ItemController(ItemServiceDao itemServiceDao) {
        this.itemServiceDao = itemServiceDao;
    }

    @PostMapping
    public ResponseEntity<String> createItem(@RequestBody Item item) {
        return ResponseEntity.ok(itemServiceDao.createItem(item.getValue(), item.getTags()));
    }

    @GetMapping("/getitem/{id}")
    public ResponseEntity<Item> getItem(@PathVariable("id") String id) {
        return ResponseEntity.ok(itemServiceDao.getItem(id));
    }

    @GetMapping("/search")
    public ResponseEntity<List<Item>> searchitems(@RequestParam List<String> tags, @RequestParam int filterValue, @RequestParam FilterValueType filterType,
                                                  @RequestParam OrderByType orderByType, @RequestParam OrderType orderType, //order by
                                                  @RequestParam int limit, @RequestParam int offset) { //pagination
        try {
            return ResponseEntity.ok(itemServiceDao.searchItems(tags, filterValue, filterType, orderByType, orderType, limit, offset));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PostMapping("/update")
    public ResponseEntity<Item> updateItem(@RequestBody Item item) {
        try {
            Item updatedItem = itemServiceDao.updateItem(item.getId(), item.getValue());
            return ResponseEntity.ok(updatedItem);
        } catch (OptimisticLockingFailureException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable String id) {
        itemServiceDao.deleteItem(id);
        return ResponseEntity.noContent().build();
    }
}
