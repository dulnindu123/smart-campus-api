package com.smartcampus.resource;

import com.smartcampus.db.DataStore;
import com.smartcampus.model.SensorReading;
import com.smartcampus.model.Sensor;
import com.smartcampus.exception.SensorUnavailableException;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// Note: No @Path at class level because it's a Sub-Resource Locator
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private String sensorId;

    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    @GET
    public Response getReadings() {
        List<SensorReading> readings = DataStore.READINGS.getOrDefault(sensorId, new ArrayList<>());
        return Response.ok(readings).build();
    }

    @POST
    public Response addReading(SensorReading reading) {
        Sensor parentSensor = DataStore.SENSORS.get(sensorId);
        if (parentSensor == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("Parent sensor not found.").build();
        }

        // CRITICAL LOGIC: Check sensor status
        if ("MAINTENANCE".equalsIgnoreCase(parentSensor.getStatus())) {
            throw new SensorUnavailableException("Sensor " + sensorId + " is currently in maintenance. Cannot accept new readings.");
        }

        if (reading.getId() == null || reading.getId().trim().isEmpty()) {
            reading.setId(UUID.randomUUID().toString());
        }
        if (reading.getTimestamp() == 0) {
            reading.setTimestamp(System.currentTimeMillis());
        }

        // Add reading to history
        DataStore.READINGS.computeIfAbsent(sensorId, k -> new ArrayList<>()).add(reading);

        // Map parent sensor's current value
        parentSensor.setCurrentValue(reading.getValue());

        return Response.status(Response.Status.CREATED).entity(reading).build();
    }
}
