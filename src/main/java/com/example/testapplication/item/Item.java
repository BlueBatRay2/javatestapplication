package com.example.testapplication.item;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Document(collection = "items")
public class Item implements Serializable {

    @Id
    private String id;
    private long creationTime;
    private long lastUpdatedTime;

    private int value;
    @Indexed
    private List<String> tags;

    private Date ttl;

    @Version
    private Long version; //used by db for concurrency, db updates

    //required for redis
    public Item(){

    }
    public Item(String id, long creationTime, long lastUpdatedTime, int value, List<String> tags) {
        this.id = id;
        this.creationTime = creationTime;
        this.lastUpdatedTime = lastUpdatedTime;
        this.value = value;
        this.tags = tags;
        this.ttl = new Date(lastUpdatedTime);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }


    public long getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(long creationTime) {
        this.creationTime = creationTime;
    }

    public long getLastUpdatedTime() {
        return lastUpdatedTime;
    }

    public void setLastUpdatedTime(long lastUpdatedTime) {
        this.lastUpdatedTime = lastUpdatedTime;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public Date getTtl() {
        return ttl;
    }

    public void setTtl(Date ttl) {
        this.ttl = ttl;
    }

    public Long getVersion() {
        return version;
    }
}
