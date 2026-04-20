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
By default, the JAX-RS runtime implements a **Request-Scoped lifecycle**. This means a brand-new instance of the Resource class (e.g., `SensorResource`) is instantiated just to handle a single incoming HTTP request, and it is subsequently destroyed. Because instance variables vanish instantaneously, state cannot be maintained locally. Consequently, our backend explicitly delegates long-term state to a statically accessible context (`DataStore.java`). However, because hundreds of request-scoped endpoints may attempt to access this static state simultaneously during heavy loads, simple lists/maps would crash with a `ConcurrentModificationException`. We circumvent this data loss constraint safely by utilizing `ConcurrentHashMap`, guaranteeing thread-safe, non-blocking synchronization.

#### 2. The Benefits of HATEOAS & Hypermedia
Hypermedia As The Engine Of Application State (HATEOAS) dynamically embeds contextual navigation links within HTTP responses. Unlike static API documentation—which can become easily desynchronized from the actual backend logic as the system updates—HATEOAS allows client applications to programmatically interpret what state transitions are actually valid *right now*. It shifts the burden of building hardcoded URL paths away from the client codebase, severely reducing tight coupling and making the API genuinely self-documenting.

#### 3. Data Transfer Objects: Hydration vs ID Collections
When returning collections (e.g., `/api/v1/rooms`), returning a heavily hydrated object (including the arrays of sensors and historical readings) severely bottlenecks network bandwidth and burdens memory. Conversely, returning just an array containing ID strings minimizes payload overhead, drastically improving throughput but inherently forcing the client to make subsequent `GET /room/{id}` calls if they actually require the metadata (the N+1 problem). Our middle-ground implementation returns shallow metadata (Room name, capacity) but intentionally limits deeper relationships strictly to ID reference arrays to balance bandwidth constraints.

#### 4. The Idempotency of DELETE
Yes, our `DELETE` operation is highly idempotent. Idempotency guarantees that executing a specific request thousands of times intentionally leaves the server in the exact identical state as conducting it exactly once. If a client transmits `DELETE /rooms/123`, the target is destroyed (resulting in `HTTP 204 No Content`). If network lag causes the client to erroneously transmit `DELETE /rooms/123` again a second later, the server logic identifies the target is already absent from the `ConcurrentHashMap` and simply triggers an `HTTP 404 Not Found`. Despite the HTTP status code changing, the fundamental database state hasn't shifted; the resource gracefully remains deleted.

#### 5. Restrictive Media Types (@Consumes)
Applying `@Consumes(MediaType.APPLICATION_JSON)` mandates strict content negotiation mechanisms. If a client attempts to bypass this by pushing data using `application/xml` or `text/plain`, the JAX-RS engine’s internal routing tree immediately rejects the payload without ever invoking our custom Java methods. It automatically returns an `HTTP 415 Unsupported Media Type` to the client. This is excellent for security and performance because it prevents our internal Jackson JSON un-marshallers from wasting CPU cycles attempting to deserialize fundamentally corrupted non-JSON buffers.

#### 6. URL Filtering Strategies: Query Params vs. Path Params
We retrieve typed modules using `?type=CO2` rather than forcing it directly into the URI structure (`/sensors/type/CO2`). This is a crucial semantic REST boundary. A path parameter fundamentally denotes identity (locating a specific noun/resource in the hierarchy), while query parameters dictate configuration (filtering, slicing, or sorting that collection). By using `@QueryParam`, we correctly signal that we are not asking for a completely different underlying resource, we are merely tweaking the visualization lens applied to the primary `/sensors` collection.

#### 7. Architectural Benefits of Sub-Resource Locators
Routing high-frequency streaming events explicitly via `@Path("/{sensorId}/readings")` returning a completely isolated `SensorReadingResource` is a masterful organizational strategy. If we handled every nested entity immediately inside `SensorResource`, it would violently degrade into an unmaintainable "God Class" handling both administrative provisioning (managing hardware) and dynamic streaming logic (managing logs). The sub-resource locator delegates routing dynamically, heavily breaking down monolithic files into cohesive, domain-specific single-responsibility objects.

#### 8. Semantic Accuracy of HTTP 422 vs 404
When registering a new sensor via JSON payload, if the client supplies an invalid `roomId` foreign key, an HTTP 404 Not Found is highly misleading, as 404 strongly implies the URI endpoint itself (`/api/v1/sensors`) does not exist on the server. Because the routing is correct, and the JSON format is structurally valid, HTTP 422 Unprocessable Entity accurately tells the client: "The syntax of your request was pristine, and the endpoint exists, but the logical instructions you requested cannot be processed because of internal semantic conflicts."

#### 9. Cyber-Securing Information Exposure via the 500 Catch-All
Modern cyber-reconnaissance frequently targets verbose error logs. If our API permitted `NullPointerExceptions` or `IndexOutOfBoundsExceptions` to bubble up natively, Apache/Grizzly servers default to returning an HTML page laced with raw Java stack traces. These stack traces leak our internal package hierarchy (`com.smartcampus.resource`), expose exact filesystem pathing names, and reveal explicitly which third-party dependency versions are running on the server. Attackers leverage these dependency versions to search CVE databases for known zero-day vulnerabilities. Our `GlobalExceptionMapper` intercepts crashes permanently, wrapping them behind a generic JSON HTTP 500 facade, intentionally frustrating attacker intelligence gathering.

#### 10. Aspect-Oriented Logging via JAX-RS Filters
Utilizing `ContainerRequestFilter` fundamentally separates core business logic from cross-cutting concerns. Implementing explicit `LOGGER.info()` statements inside dozens of resource methods generates massive code duplication, heavily bloats the functional components, and drastically heightens the risk that a developer simply forgets to copy-paste the logging line into a newly created endpoint. Centralized filters act as security checkpoints intercepting all web server traffic globally, unconditionally ensuring perfect audit trails regardless of developer oversight.
