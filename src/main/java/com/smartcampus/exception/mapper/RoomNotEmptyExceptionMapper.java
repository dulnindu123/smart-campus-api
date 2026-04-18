package com.smartcampus.exception.mapper;

import com.smartcampus.exception.ErrorResponse;
import com.smartcampus.exception.RoomNotEmptyException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class RoomNotEmptyExceptionMapper implements ExceptionMapper<RoomNotEmptyException> {
    @Override
    public Response toResponse(RoomNotEmptyException exception) {
        ErrorResponse errorResponse = new ErrorResponse("Conflict", exception.getMessage(), 409);
        return Response.status(Response.Status.CONFLICT)
                .entity(errorResponse)
                .type("application/json")
                .build();
    }
}
