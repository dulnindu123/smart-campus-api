package com.smartcampus.resource;

import com.smartcampus.db.DataStore;
import com.smartcampus.exception.RoomNotEmptyException;
import com.smartcampus.model.Room;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {

    @GET
    public Response getAllRooms() {
        List<Room> rooms = new ArrayList<>(DataStore.ROOMS.values());
        return Response.ok(rooms).build();
    }

    @POST
    public Response createRoom(Room room) {
        if (room.getId() == null || room.getId().trim().isEmpty()) {
            room.setId(UUID.randomUUID().toString());
        }
        
        if (DataStore.ROOMS.containsKey(room.getId())) {
            return Response.status(Response.Status.CONFLICT)
                    .entity("Room with ID " + room.getId() + " already exists.")
                    .build();
        }

        if (room.getSensorIds() == null) {
            room.setSensorIds(new ArrayList<>());
        }

        DataStore.ROOMS.put(room.getId(), room);
        return Response.status(Response.Status.CREATED).entity(room).build();
    }

    @GET
    @Path("/{roomId}")
    public Response getRoom(@PathParam("roomId") String roomId) {
        Room room = DataStore.ROOMS.get(roomId);
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(room).build();
    }

    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = DataStore.ROOMS.get(roomId);
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        // CRITICAL LOGIC: Check for active sensors
        if (room.getSensorIds() != null && !room.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException("Cannot delete room " + roomId + " because it still has active sensors assigned to it.");
        }

        DataStore.ROOMS.remove(roomId);
        return Response.noContent().build();
    }
}
