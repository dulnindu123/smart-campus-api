package com.smartcampus.exception.mapper;

import com.smartcampus.exception.ErrorResponse;
import com.smartcampus.exception.SensorUnavailableException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class SensorUnavailableExceptionMapper implements ExceptionMapper<SensorUnavailableException> {
    @Override
    public Response toResponse(SensorUnavailableException exception) {
        ErrorResponse errorResponse = new ErrorResponse("Forbidden", exception.getMessage(), 403);
        return Response.status(Response.Status.FORBIDDEN)
                .entity(errorResponse)
                .type("application/json")
                .build();
    }
}
