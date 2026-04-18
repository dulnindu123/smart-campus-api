package com.smartcampus.resource;

import com.smartcampus.model.DiscoveryResponse;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@Path("/")
public class DiscoveryResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response discoverAPI(@Context UriInfo uriInfo) {
        DiscoveryResponse discoveryResponse = new DiscoveryResponse("1.0", "admin@smartcampus.edu");
        
        String baseUri = uriInfo.getBaseUri().toString();
        discoveryResponse.addLink("rooms", baseUri + "rooms");
        discoveryResponse.addLink("sensors", baseUri + "sensors");

        return Response.ok(discoveryResponse).build();
    }
}
