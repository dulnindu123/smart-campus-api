package com.smartcampus.resource;

import com.smartcampus.db.DataStore;
import com.smartcampus.exception.LinkedResourceNotFoundException;
import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    @GET
    public Response getSensors(@QueryParam("type") String type) {
        List<Sensor> sensors = new ArrayList<>(DataStore.SENSORS.values());
        
        if (type != null && !type.trim().isEmpty()) {
            sensors = sensors.stream()
                    .filter(s -> type.equalsIgnoreCase(s.getType()))
                    .collect(Collectors.toList());
        }
        
        return Response.ok(sensors).build();
    }

    @POST
    public Response createSensor(Sensor sensor) {
        // CRITICAL LOGIC: Verify roomId exists
        String targetRoomId = sensor.getRoomId();
        if (targetRoomId == null || !DataStore.ROOMS.containsKey(targetRoomId)) {
            throw new LinkedResourceNotFoundException("Cannot create sensor. Room ID " + targetRoomId + " does not exist.");
        }

        if (sensor.getId() == null || sensor.getId().trim().isEmpty()) {
            sensor.setId(UUID.randomUUID().toString());
        }

        // Save sensor
        DataStore.SENSORS.put(sensor.getId(), sensor);

        // Update the Room's sensorIds list
        Room targetRoom = DataStore.ROOMS.get(targetRoomId);
        targetRoom.getSensorIds().add(sensor.getId());

        return Response.status(Response.Status.CREATED).entity(sensor).build();
    }

    // Sub-Resource Locator Pattern
    @Path("/{sensorId}/readings")
    public SensorReadingResource getSensorReadingResource(@PathParam("sensorId") String sensorId) {
        return new SensorReadingResource(sensorId);
    }
}
