package com.example.testapplication;

import com.example.testapplication.item.Item;
import com.example.testapplication.item.ItemServiceDao;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
public class TestApplicationConcurrencyTests {

    @Autowired
    private ItemServiceDao itemServiceDao;

    private List<String> listOfIdsCreated;

    @BeforeEach
    public void init(){
        listOfIdsCreated = new ArrayList<>();
    }

    @AfterEach
    public void cleanup() {

        //delete our entries we created
        for (String uid: listOfIdsCreated) {
            try{
                itemServiceDao.deleteItem(uid);
            }catch(Exception e){
                System.err.println("Failed to delete " + uid);
                e.printStackTrace();
            }
        }
    }

    /**
     * Here we will spam threads on update, incrementing value and check that value after it's all done
     */
    @Test
    public void testConcurrency() throws InterruptedException {
        //params to test with
        int numThreads = 2;
        int incrementCount = 5;
        int value = 0;
        List<String> tagList = Arrays.asList("tag1", "tag2");

        //create item
        String uniqueId = itemServiceDao.createItem(value, tagList);
        assertNotNull(uniqueId);

        listOfIdsCreated.add(uniqueId);

        //create threads, they are based on increment thread which takes value, increments and sends update
        IncrementThread[] threadArr = new IncrementThread[numThreads];

        for (int i = 0; i < numThreads; i++) {
            threadArr[i] = new IncrementThread(uniqueId, incrementCount);
            threadArr[i].start();
        }

        //wait until all threads are finished
        for (IncrementThread thread:threadArr) {
            thread.join();
        }

        //time to check it
        Item item = itemServiceDao.getItem(uniqueId);
        assertEquals(numThreads * incrementCount, item.getValue());
    }

    private class IncrementThread extends Thread{
        private final int incrementCount;
        private final String uid;

        private IncrementThread(String uid, int incrementCount) {
            this.uid = uid;
            this.incrementCount = incrementCount;
        }

        @Override
        public void run() {
            for (int i = 0; i < incrementCount; ) {
                try {
                    updateWithRetry(uid);
                    i++; // Increment i only if the update succeeds
                } catch (RuntimeException ex) {
                    // If the retry logic also fails, handle or throw the exception here.
                    throw ex;
                }
            }
        }
        private void updateWithRetry(String uid) {
            final int maxRetries = 10;
            for (int attempt = 0; attempt <= maxRetries; attempt++) {
                try {
                    System.out.println("update failed, trying again");
                    Item item = itemServiceDao.getItem(uid);
                    itemServiceDao.updateItem(uid, item.getValue() + 1);
                    // If update is successful, return from the method
                    return;
                } catch (OptimisticLockingFailureException ex) {

                    //fail out if we hit max retries
                    if (attempt == maxRetries) {
                        throw new RuntimeException("Too many update failures for item " + uid, ex);
                    }
                }
            }
        }
    }
}
