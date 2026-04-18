package com.smartcampus.exception.mapper;

import com.smartcampus.exception.ErrorResponse;
import com.smartcampus.exception.LinkedResourceNotFoundException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class LinkedResourceNotFoundExceptionMapper implements ExceptionMapper<LinkedResourceNotFoundException> {
    @Override
    public Response toResponse(LinkedResourceNotFoundException exception) {
        ErrorResponse errorResponse = new ErrorResponse("Unprocessable Entity", exception.getMessage(), 422);
        return Response.status(422)
                .entity(errorResponse)
                .type("application/json")
                .build();
    }
}
