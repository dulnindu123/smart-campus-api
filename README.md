# Smart Campus RESTful Web Service

Welcome to the **Smart Campus API**, a lightweight, highly-performant RESTful web service built completely on the JAX-RS (Jakarta RESTful Web Services) specification using Jersey and Grizzly. 

This infrastructure provides centralized routing and data processing for managing campus `Rooms`, hardware environmental `Sensors`, and high-frequency `SensorReadings`.

---

## 🛠️ Build and Run Instructions

### Prerequisites
- **Java Development Kit (JDK 11+)**
- **Maven** (or an IDE with Maven support like IntelliJ IDEA, Eclipse, or NetBeans)

### Running via Command Line
1. Open your terminal or PowerShell and navigate to the project directory:
   ```bash
   cd e:/CSA/smart-campus
   ```
2. Build and execute the application utilizing the Maven exec plugin:
   ```bash
   mvn clean compile exec:java
   ```
3. The server will launch and listen for incoming HTTP traffic at:
   `http://localhost:8080/api/v1/`

### Running via IDE (e.g., NetBeans)
1. Select **File -> Open Project** and choose the `smart-campus` directory.
2. Allow a moment for the IDE to resolve and download the standard `jersey-container-grizzly2-http` dependencies from the `pom.xml`.
3. Locate `Main.java` within the `com.smartcampus` package.
4. **Right-click** `Main.java` -> **Run File**. Watch the console to verify the Grizzly engine has started!

---

## 🚀 Terminal Integration (5 Sample cURL Commands)

Here are five commands covering different functionality to demonstrate the API locally:

**1. Discover API Entry Points (HATEOAS)**
```bash
curl -X GET http://localhost:8080/api/v1/
```

**2. Provision a new Room Location**
```bash
curl -X POST http://localhost:8080/api/v1/rooms \
     -H "Content-Type: application/json" \
     -d '{"name": "Main Server Room", "capacity": 5}'
```

**3. Register a Sensor to a Room**
*(Ensure you replace `<ROOM_ID>` with the ID generated in the previous step)*
```bash
curl -X POST http://localhost:8080/api/v1/sensors \
     -H "Content-Type: application/json" \
     -d '{"type": "Temperature", "status": "ACTIVE", "roomId": "<ROOM_ID>"}'
```

**4. Publish a Sub-Resource Data Reading**
*(Deploy real-time telemetry into the sub-resource locator context)*
```bash
curl -X POST http://localhost:8080/api/v1/sensors/<SENSOR_ID>/readings \
     -H "Content-Type: application/json" \
     -d '{"value": 72.5}'
```

**5. Test Referential Integrity / Prevention Logic**
*(Attempt to delete a Room that is actively hosting the hardware above. Expect an HTTP 409 Conflict)*
```bash
curl -v -X DELETE http://localhost:8080/api/v1/rooms/<ROOM_ID>
```

---

## 📝 Conceptual Report 
*(Addressing coursework theoretical questions)*

#### 1. JAX-RS Lifecycle & Concurrency Implications
The JAX-RS runtime has a default lifecycle of a Request-Scoped. This implies the creation of a new object of the Resource class (e.g., SensorResource) only to support one new inbound HTTP request and then being destroyed afterward. Since the instance variables disappear instantly, it is impossible to locally represent state. Therefore, our backend basically delegates long-term state to an accessibly-statical context (DataStore.java). But since hundreds of request scoped endpoints would be trying to mutate this read-only state concurrently when heavily loaded, simple lists/maps would die with a ConcurrentModificationException. To avoid this loss of data it is safe to use ConcurrentHashMap which will ensure non-blocking synchronization which is thread safe. 

#### 2. The Benefits of HATEOAS & Hypermedia
Hypermedia As The Engine Of Application State (HATEOAS) dynamically adds contextual links navigation to the responses of HTTP. HATEOAS can be used to have client applications programmatically understand what transitions between states are valid at a given point in time, unlike documenting APIs where any documentation can become quickly out-of-practical with the underlying backend code. It separates the codebase of creating hardcoded URL paths off the client-side and effectively limits tight coupling, causing the API to be truly self-documenting.

#### 3. Data Transfer Objects: Hydration vs ID Collections
When returning collections (e.g., /api/v1/rooms) it is disastrous to a network well-to-return a very hydrated object (their arrays of sensors and historic readings) which tips the scale toward network utilization, and overloads memory. The decreasing the amount of overhead in the payload to an array of ID strings, on the other hand, maximizes the throughput at the cost of making the client send further GET /room/{id} requests afterwards, should they need the metadata (the N+1 problem). Our middle ground implementation would only render back the shallow metadata (Room name, capacity) but deliberately only go deeper with full relationships being limited to arrays of ID references to penalize bandwidth rights. 

#### 4. The Idempotency of DELETE
Indeed, our DELETE is extremely idempotent. Idempotency ensures that it is possible to execute a given request a thousand times in the same state deliberately to get the server into the exact same state as it would have with the same request executed once. When the client sends in a DELETE request DELETE /rooms/123 the target is destroyed (the HTTP response is 204 No Content). In case the network lag makes the client resent the DELETE /rooms/123 twice, one second later and the server code recognizes that it already has deleted the target from the ConcurrentHashMap, it merely results in an HTTP 404 Not Found. The underlying state of the database has not changed: the HTTP status code has changed; the resource has been deleted gracefully.

#### 5. Restrictive Media Types (@Consumes)
Applying @Consumes(MediaType.APPLICATION_JSON) requires very strict content negotiation mechanisms. When a client tries to bypass this by pushing data but using application/xml or text/plain, the routing tree used internally by the JAX-RS engine automatically blocks the payload and does not even invoke our custom Java underloads. It automatically responds to the client’s HTTP 415 Unsupported Media Type. It is great in terms of security and performance since we do not have our own Jackson JSON un-marshallers canying at the CPU with the futile use of trying to deserialize fundamentally corrupted non-JSON buffers.

#### 6. URL Filtering Strategies: Query Params vs. Path Params
We fetch modules that have been typed with ?type=CO2 as opposed to having to force it into the URL hierarchy (/sensors/type/CO2). This is an important REST boundary in terms of semantics. A path parameter is essentially an identity (finding a particular noun/resource in the hierarchy) and query parameters a configuration (filtering, slicing or sorting that collection). With the help of @QueryParam, we get it right and indicate that we are not requesting a totally alternative underlying resource but instead, simply adjusting the visualization lens to the underlying collection of /sensors.

#### 7. Architectural Benefits of Sub-Resource Locators
Directed forwarding of high-frequency streaming events throughput into and out of a particular sensor by passing through a path of @Path("/{sensorId}/readings) and returning an entirely independent SensorReadingResource is an admirable organizational choice. To do all the nested entities at once within SensorResource would be in an appalling explosion of God Class code that dealt with administrative provisioning (hardware control) and dynamically streamed logic (logs management). The sub-resource locator is a dynamic delegator of routing with all the dynamism washing out monolithic files and neurotically devolving them into single-resource domain objects functioning as single responsibilities.

#### 8. Semantic Accuracy of HTTP 422 vs 404
Registering a new sensor Via JSON Payload, when the client provides an invalid foreign key: roomId, an HTTP 404 Not Found is most misleading since a 404 strongly suggests the endpoint of the associated URI (/api/v1/ sensors) does not exist in the server. Since the routing is correct, and the content is a structurally correct JSON, HTTP 422 Unprocessable Entity correctly informs the client: "The syntax of your request was impeccable, and the endpoint is real, yet the logical instructions you are asking it to follow cannot be executed due to semantic conflicts within it.

#### 9. Cyber-Securing Information Exposure via the 500 Catch-All
Verbose error logs often are the target of modern cyber-reconnaissance. Unless our API allowed NullPointerExceptions to be bubbled out in a native fashion or IndexOutOfboundExceptions, Apache/Grizzly servers serve by default as HTML page servers interspersed with raw Java stack traces. Such stack traces can give away our internal package structure (com.smartcampus.resource), will reveal specifically the filesystem pathing names and will also show which third party dependencies are on which version is running on the server. These dependency versions are used by attackers to query CVE databases to find use of known zero-day vulnerabilities. Our GlobalExceptionMapper captures any crashes forever but wraps them with a generic facade of a JSON HTTP 500, deliberately foaming at the mouth of attacker intelligence collection.

#### 10. Aspect-Oriented Logging via JAX-RS Filters
The use of Core business logic and cross-cutting concerns is split apart in essence using the ContainerRequestFilter. The explicit LOGGER.info() statements in dozens of resource methods create a massive SPLC replication of code and intensely overblow the functional components so that the underlying intent of adding the logging line is that some developer merely forgets to copy paste it into another resource in a newly created endpoint. Centralized filters are security gateways that inspect all web server data worldwide without any exception and ensure ideal audit trails irrespective of the attention of a developer.
