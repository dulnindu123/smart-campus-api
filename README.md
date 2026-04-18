# Smart Campus RESTful Web Service

This project is a JAX-RS backend built in Java for a "Smart Campus" managing Rooms and Sensors.

## Project Characteristics
- **JAX-RS (Jersey)** with an **Embedded Grizzly Server**.
- **No Spring Framework** or Boot.
- **In-Memory Thread-Safe Data Store** (using `ConcurrentHashMap`).
- Deep REST routing via Sub-Resource Locators (`/{sensorId}/readings`).

## How to Run

1. Open this project in your preferred IDE (e.g., IntelliJ IDEA, Eclipse).
2. Ensure you have Java 11 or higher installed.
3. Use Maven to compile and start the server:
   ```bash
   mvn clean compile exec:java
   ```
4. The API will start on: `http://localhost:8080/api/v1/`
