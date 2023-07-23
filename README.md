<div style="text-align: center;">

<h2 align="center">Test Application</h2>

  <p style="text-align: center;">
    A demonstration of CRUD, database, and cache containers. Uses Spring, Java, MongoDB. 
  </p>
</div>


### Built With

* [![OpenJDKCoretto][OpenJDKCoretto]][OpenJDKCoretto-url]
* [![MongoDb][MongoDB]][MongoDB-url]
* [![Docker][Docker]][Docker-url]
* [![Spring][Spring]][Spring-url]
* [![Gradle][Gradle]][Gradle-url]


<!-- GETTING STARTED -->
## Getting Started

### Prerequisites
* **Docker:** You can download it from Docker's website. Make sure to choose the correct operating system. Docker should come with Docker Compose included if you're using Mac or Windows. If you're using Linux, you might need to install Docker Compose separately, which brings us to the next point.


* **Docker Compose:** If you're on Linux or Docker Desktop didn't come with Docker Compose, you can download it by following instructions on the official documentation.

### Installation (for local)

1. **Clone the repo:** Use the following command to clone this repo to your local machine
   ```sh
   git clone https://github.com/BlueBatRay2/javatestapplication.git
   ```
    **Alternative method:** download zip from https://github.com/BlueBatRay2/javatestapplication and extract.
<br/><br/>
2. **Change directory:** Move to project directory
   ```sh
   cd javatestapplication
   ```
3. **Build and run the Docker Container:** This will start the application. Docker Compose will build the images and start the containers.
   ```js
   docker-compose up
   ```

<!-- USAGE EXAMPLES -->
## Usage

#### Item 
An item consists of the following:

* `String` *id*: a unique identifier for the item.
* `long` *creationTime*: the time when the item was created, represented as the number of milliseconds since the Unix epoch.
* `long` *lastUpdatedTime*: the last time when the item was updated, represented as the number of milliseconds since the Unix epoch.
* `int` *value*: an integer value associated with the item.
* `List<String>` *tags*: a list of tags associated with the item.
* `Date` *ttl*: the time when the item should expire, represented as an ISO 8601 date-time string.
* `long` *version*: used by database to handle concurrency

#### Create Item ```POST /api/item```
- To create an item, make a `POST` request.

    **Parameters:** 
    - `int` *value* (Required) initial value of your new item
    - `List<String>` *tags* (Optional) tags for the item, can be any string.

    **Return Value:** 
    - *id*  a unique id genned (UUID)

Here is an example of a request in cURL:
```http
curl -X POST "http://localhost:8080/api/item" --header "Content-Type: application/json" --data "{\"value\":5, \"tags\":[\"tag1\", \"tag2\"]}"
```
Successful response will look something like this
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000"
}
```

#### Get Item ```GET /api/item/getitem/{id}```
- To create an item, make a `GET` request.

  **Parameters:**
  - `id` The unique id given

  **Return Value:** 
  - `Item` *item*

Here is an example of a cURL request:
```http
curl --location "http://localhost:8080/api/item/getitem/3239a8f8-a5b3-4197-9ef8-d60b60b1e2ec"
```
Successful response will look something like this
```json
{
  "id": "3239a8f8-a5b3-4197-9ef8-d60b60b1e2ec",
  "creationTime": 1629998712000,
  "lastUpdatedTime": 1630085112000,
  "value": 20,
  "tags": ["tag1", "tag2", "tag3"],
  "ttl": "2023-08-30T18:38:32.000Z"
}

```

#### Search Item(s) ```GET /api/item/search?tags={tags}&filterValue={filterValue}&filterType={type}&orderByType={orderByType}&orderType={orderType}&limit={limit}&offset={offset}```
- To search for items by tags, make a `GET` request to the `/api/item/search` endpoint with the following query parameters:

  **Parameters:**
  - `tags` (Required): A comma-separated list of tags.
  - `filterValue` (Required): An integer value to filter the items.
  - `filterType` (Required): A string indicating the type of filter to apply. This must be one of the following: `LESS_THAN`, `EQUALS`, `GREATER_THAN`, or `NONE`.
  - `orderByType` (Required): A string indicating the field by which to order the results. This must be one of the following: `VALUE` or`LAST_UPDATED_TIME`.
  - `orderType` (Required): A string indicating the type of ordering to apply. This must be one of the following: `ASCENDING` or `DESCENDING`.
  - `limit` (Required): An integer specifying the maximum number of items to return.
  - `offset` (Required): An integer specifying the number of items to skip before starting to return items.
  
  **Return Value:** 
  - A list of `Item` objects that match the search criteria.

Here is an example of a cURL request:
```http
curl --location "http://localhost:8080/api/item/search?tags=tag1,tag2&filterValue=2&filterType=GREATER_THAN&orderByType=VALUE&orderType=DESCENDING&limit=10&offset=0
```
Successful response will look something like this
```json
[
  {
    "id":"12559f26-db72-4900-aaff-9fc2a7663a6c",
    "creationTime":1690037986583,
    "lastUpdatedTime":1690037986583,
    "value":5,
    "tags":["tag1","tag2"],
    "ttl":"2023-07-22T14:59:46.583+00:00",
    "version":0
  },
  ...
]
```

#### Update Item ```POST /api/item/update```
- To search for items by tags, make a `Post` request to the `/api/item/update` endpoint with the following query parameters:

  **Parameters:**
  - `id` (Required): The unique identifier of the item you want to update.
  - `value` (Optional): The new value for the item.
  - `tags` (Optional): The new tags for the item, provided as a list of strings.
  
  **Return Value:**
    - The updated `Item` object.

Here is an example of a cURL request:
```http 
curl -X POST "http://localhost:8080/api/item/update" --header "Content-Type: application/json" --data "{\"id\":\"12559f26-db72-4900-aaff-9fc2a7663a6c\", \"value\":3, \"tags\":[\"tag1\", \"tag3\"]}"
```

Successful response will look something like this
```json
{
  "id":"12559f26-db72-4900-aaff-9fc2a7663a6c",
  "creationTime":1690037986583,
  "lastUpdatedTime":1690038565028,
  "value":3,
  "tags":["tag1","tag2"],
  "ttl":"2023-07-22T15:09:25.028+00:00",
  "version":1
}
```
An unsuccessful response can possibly be a `409` error because of concurrency. The app will retry but eventually error out after retries have been exhausted.

## Tests
So at the moment tests are tricky to use as they are integration tests and require mongodb and NOT redis to be running.

- First tests must be enabled, this is possible in the environment settings by adding environmental variable `RUN_TESTS` to `1` 
- Second, caching must be disabled and this is simply because the way the tests are set up at the moment. This can be done in the application.properties file by setting `spring.cache.type=none`

[OpenJDKCoretto]: https://img.shields.io/badge/AWS_Corretto-FFFFFF?style=for-the-badge&logo=openjdk&logoColor=black
[OpenJDKCoretto-url]: https://aws.amazon.com/corretto/
[Docker]: https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white
[Docker-url]: https://docker.com/
[MongoDB]: https://img.shields.io/badge/MongoDB-%234ea94b.svg?style=for-the-badge&logo=mongodb&logoColor=white
[MongoDB-url]: https://mongodb.com/
[Spring]: https://img.shields.io/badge/spring-%236DB33F.svg?style=for-the-badge&logo=spring&logoColor=white
[Spring-url]: https://spring.io/
[Gradle]: https://img.shields.io/badge/Gradle-02303A.svg?style=for-the-badge&logo=Gradle&logoColor=white
[Gradle-url]: https://gradle.org/


