package com.smartcampus.exception.mapper;

import com.smartcampus.exception.ErrorResponse;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {
    @Override
    public Response toResponse(Throwable exception) {
        // Global safety net to prevent raw stack traces from reaching the client
        ErrorResponse errorResponse = new ErrorResponse("Internal Server Error", "An unexpected error occurred: " + exception.getMessage(), 500);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(errorResponse)
                .type("application/json")
                .build();
    }
}
