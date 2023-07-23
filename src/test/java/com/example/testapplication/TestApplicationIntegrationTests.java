package com.example.testapplication;

import com.example.testapplication.item.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;

import java.util.*;



@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class TestApplicationIntegrationTests {

	@Autowired
	private TestRestTemplate restTemplate;

	private List<String> listOfIdsCreated;

	@BeforeEach
	public void init(){
		listOfIdsCreated = new ArrayList<>();
	}

	//fired after each test (even if they fail), used to cleanup entries in db
	@AfterEach
	public void cleanup() {

		//delete our entries we created
		for (String uid: listOfIdsCreated) {
			try{
				this.deleteItemRequest(uid);
			}catch(Exception e){
				System.err.println("Failed to delete " + uid);
				e.printStackTrace();
			}
		}
	}

	@Test
	public void testCreateGetUpdateItem() {

		//params to test with
		int value = 22;
		List<String> tagList = Arrays.asList("tag1", "tag2");

		//create
		String uniqueId = this.createItemRequest(value, tagList);

		//check it
		Assertions.assertNotNull(uniqueId);

		//parsing this will test if it's an actual uuid
		try {
			UUID.fromString(uniqueId);
		} catch (IllegalArgumentException e) {
			Assertions.fail("uniqueId failed :: " + uniqueId);
		}

		//get and check it
		Item gottenItem = this.getItemRequest(uniqueId);

		//run tests
		Assertions.assertNotNull(gottenItem);
		Assertions.assertEquals(uniqueId, gottenItem.getId());
		Assertions.assertEquals(value, gottenItem.getValue());
		Assertions.assertTrue(tagList.containsAll(gottenItem.getTags()));

		//now update item
		Item updatedItem = this.updateItemRequest(uniqueId, value+1);

		//tests
		Assertions.assertNotNull(updatedItem);
		Assertions.assertEquals(uniqueId, updatedItem.getId());
		Assertions.assertEquals(value+1, updatedItem.getValue());
		Assertions.assertTrue(tagList.containsAll(updatedItem.getTags()));

		//delete item and clean up
		this.deleteItemRequest(uniqueId);

		//verify it's gone
		Assertions.assertNull(this.getItemRequest(uniqueId));
	}

	/**
	 * Search tags
	 * we will test this by initially having just one tag, searching it, searching for a nonexistant tag, adding that tag
	 * then searching again and verifying. we also will search multiple tags
	 */
//	@Test
	public void testSearchTags(){
		//params to test with
		int value1 = 1;
		String tag1 = "tag1";
		String tag2 = "tag2";
		List<String> tagList1 = List.of(tag1);
		List<String> tagList12 = List.of(tag1, tag2);

		//create our item
		String tag1Id = this.createItemRequest(value1, tagList1);

		List<Item> itemList = this.searchTagRequest(tagList1, 0, FilterValueType.NONE.toString(), OrderByType.VALUE.toString(), OrderType.ASCENDING.toString(), 10, 0);

		Assertions.assertNotNull(itemList);
		Assertions.assertFalse(itemList.isEmpty());

		boolean foundUid1 = false;

		for (Item item:itemList) {
			if(!item.getTags().contains(tag1))
				Assertions.fail(tag1 + " is not in item: " + item.getId() + " " + item.getTags().toString());

			if(tag1Id.equals(item.getId())){

				foundUid1 = true;
				break;
			}
		}

		if(!foundUid1)
			Assertions.fail(tag1Id + " was not found in search for tag: " + tag1);


		//now let's check if we use 2 tags and query only 1, we'll still get our item
		String tag12Id = this.createItemRequest(value1, tagList12);
		List<Item> itemList2tags = this.searchTagRequest(tagList1, 0, FilterValueType.NONE.toString(), OrderByType.VALUE.toString(), OrderType.ASCENDING.toString(), 10, 0);

		boolean foundUid2 = false;

		for (Item item:itemList2tags) {
			if(!item.getTags().contains(tag1))
				Assertions.fail(tag1 + " is not in item: " + item.getId() + " " + item.getTags().toString());

			if(tag12Id.equals(item.getId())){

				foundUid2 = true;
				break;
			}
		}

		if(!foundUid2)
			Assertions.fail(tag12Id + " was not found in search for tag: " + tag1);

	}

	/**
	 * This test is only for initial creation of database. after there are items there, it cannot be used.
	 */
	@Test
	public void testFiltersTags(){
		//params to test with
		int itemsTotal = 30;
		String tag1 = "tag1";
		String tag2 = "tag2";
		List<String> tagList1 = List.of(tag1);
		List<String> tagList2 = List.of(tag2);


		//create 30 new entries 1-30 for values
		for (int i = 0; i < 30; i++) {
			this.createItemRequest(i+1, tagList1);
		}

		//create 30 more entries, same but with different tag (these should be ignored)
		for (int i = 0; i < itemsTotal; i++) {
			this.createItemRequest(i+1, tagList2);
		}

		List<Item> lessThanItemList = this.searchTagRequest(tagList1, 11, FilterValueType.LESS_THAN.toString(), OrderByType.VALUE.toString(), OrderType.ASCENDING.toString(), itemsTotal, 0);
		Assertions.assertNotNull(lessThanItemList);
		Assertions.assertEquals(10, lessThanItemList.size());

		List<Item> greaterThanItemList = this.searchTagRequest(tagList1, 11, FilterValueType.GREATER_THAN.toString(), OrderByType.VALUE.toString(), OrderType.ASCENDING.toString(), itemsTotal, 0);
		Assertions.assertNotNull(greaterThanItemList);
		Assertions.assertEquals(19, greaterThanItemList.size());

		List<Item> equalsItemList = this.searchTagRequest(tagList1, 11, FilterValueType.EQUALS.toString(), OrderByType.VALUE.toString(), OrderType.ASCENDING.toString(), itemsTotal, 0);
		Assertions.assertNotNull(equalsItemList);
		Assertions.assertEquals(1, equalsItemList.size());
	}

	private String createItemRequest(int value, List<String> tagList){

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		JSONObject requestBody = new JSONObject();
		try{
			requestBody.put("value",value);
			requestBody.put("tags",new JSONArray(tagList));
		}catch(JSONException exception){
			Assertions.fail("request body creation failed");
		}

		HttpEntity<String> requestEntity = new HttpEntity<>(requestBody.toString(), headers);
		ResponseEntity<String> response = restTemplate.postForEntity("/api/item", requestEntity, String.class);

		Assertions.assertEquals(response.getStatusCode(),HttpStatus.OK);

		String id = response.getBody();
		listOfIdsCreated.add(id);

		return id;
	}

	private Item getItemRequest(String id){
		ResponseEntity<Item> response = restTemplate.getForEntity("/api/item/getitem/{id}", Item.class, id);
		Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());

		return response.getBody();
	}

	private Item updateItemRequest(String id, int value){

		//create
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		JSONObject requestBody = new JSONObject();
		try{
			requestBody.put("id",id);
			requestBody.put("value",value);
		}catch(JSONException exception){
			Assertions.fail("request body creation failed");
		}

		HttpEntity<String> requestEntity = new HttpEntity<>(requestBody.toString(), headers);
		ResponseEntity<Item> response = restTemplate.postForEntity("/api/item/update", requestEntity, Item.class);

		Assertions.assertEquals(response.getStatusCode(), HttpStatus.OK);

		return response.getBody();
	}

	private void  deleteItemRequest(String id){
		ResponseEntity<String> response = restTemplate.exchange(
				"/api/item/{id}",
				HttpMethod.DELETE,
				null,
				String.class,
				id);

		Assertions.assertEquals(response.getStatusCode(), HttpStatus.NO_CONTENT);

	}

	private List<Item> searchTagRequest(List<String> tags, int filterValue, String filterType, String orderByType, String orderType, int limit, int offset){

		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(List.of(MediaType.APPLICATION_JSON));
		HttpEntity<String> entity = new HttpEntity<String>(headers);

		String url = "/api/item/search?";

		//add tags
		if(tags != null){
			String joinedTags = String.join(",", tags);
			url+= "tags=" + joinedTags;
		}

		url += "&filterValue=" + filterValue + "&filterType=" + filterType;	
		url += "&orderByType=" + orderByType + "&orderType=" + orderType;
		url += "&limit=" + limit + "&offset=" + offset;
		
		ResponseEntity<List<Item>> response = restTemplate.exchange(
				url,
				HttpMethod.GET,
				entity,
				new ParameterizedTypeReference<List<Item>>() {}
		);

		return response.getBody();

	}
}
