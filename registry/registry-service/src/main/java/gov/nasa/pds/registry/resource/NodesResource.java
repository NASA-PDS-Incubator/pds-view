//	Copyright 2009-2010, by the California Institute of Technology.
//	ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
//	Any commercial use must be negotiated with the Office of Technology 
//	Transfer at the California Institute of Technology.
//	
//	This software is subject to U. S. export control laws and regulations 
//	(22 C.F.R. 120-130 and 15 C.F.R. 730-774). To the extent that the software 
//	is subject to U.S. export control laws and regulations, the recipient has 
//	the responsibility to obtain export licenses or other export authority as 
//	may be required before exporting such information to foreign countries or 
//	providing access to foreign nationals.
//	
//	$Id$
//

package gov.nasa.pds.registry.resource;

import java.net.URI;

import gov.nasa.pds.registry.exception.RegistryServiceException;
import gov.nasa.pds.registry.model.ClassificationNode;
import gov.nasa.pds.registry.model.ClassificationScheme;
import gov.nasa.pds.registry.model.RegistryResponse;
import gov.nasa.pds.registry.service.RegistryService;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * This resource is responsible Classification Nodes for a given Classification
 * Scheme.
 * 
 * @author pramirez
 * 
 */
public class NodesResource {

  @Context
  UriInfo uriInfo;

  @Context
  Request request;

  @Context
  ClassificationScheme scheme;

  @Context
  RegistryService registryService;

  public NodesResource(UriInfo uriInfo, Request request,
      RegistryService registryService, ClassificationScheme scheme) {
    this.uriInfo = uriInfo;
    this.request = request;
    this.registryService = registryService;
    this.scheme = scheme;
  }

  /**
   * Publishes a classification node to the registry.
   * 
   * @request.representation.qname 
   *                               {http://registry.pds.nasa.gov}classificationNode
   * @request.representation.mediaType application/xml
   * @request.representation.example {@link gov.nasa.pds.registry.util.Examples#REQUEST_NODE}
   * @response.param {@name Location} {@style header} {@type
   *                 {http://www.w3.org/2001/XMLSchema}anyURI} {@doc The URI
   *                 where the created item is accessible.}
   * @param node
   *          to publish
   * @return a HTTP response that indicates an error or the location of the
   *         created association and its guid
   */
  @POST
  @Consumes( { MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
  public Response publishNode(ClassificationNode node) {
    if (node.getParent() == null) {
      node.setParent(scheme.getGuid());
    }
    try {
      String guid = registryService.publishObject("Unknown", node);
      return Response.created(
          NodesResource.getNodeUri(scheme.getGuid(),
              (ClassificationNode) registryService.getObject(guid, node
                  .getClass()), uriInfo)).entity(guid).build();
    } catch (RegistryServiceException ex) {
      throw new WebApplicationException(Response.status(
          ex.getExceptionType().getStatus()).entity(ex.getMessage()).build());
    }
  }

  /**
   * Retrieves the classification nodes for the scheme
   * 
   * @response.representation.200.qname {http://registry.pds.nasa.gov}response
   * @response.representation.200.mediaType application/xml
   * @response.representation.200.example {@link gov.nasa.pds.registry.util.Examples#RESPONSE_NODES}
   * 
   * @return List of nodes
   */
  @GET
  @Produces( { MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
  public RegistryResponse<ClassificationNode> getClassificationNodes() {
    return new RegistryResponse<ClassificationNode>(registryService
        .getClassificationNodes(scheme.getGuid()));
  }

  /**
   * Retrieves the classification node with the given identifier
   * 
   * @response.representation.200.qname 
   *                                    {http://registry.pds.nasa.gov}classificationNode
   * @response.representation.200.mediaType application/xml
   * @response.representation.200.example {@link gov.nasa.pds.registry.util.Examples#RESPONSE_NODE}
   * 
   * @param nodeGuid
   *          globally unique identifier of classification node
   * @return classification node
   */
  @GET
  @Path("{nodeGuid}")
  @Produces( { MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
  public ClassificationNode getClassificationNode(
      @PathParam("nodeGuid") String nodeGuid) {
    return (ClassificationNode) registryService.getObject(nodeGuid,
        ClassificationNode.class);
  }

  /**
   * Deletes the classification node from the registry
   * 
   * @param nodeGuid
   *          globally unique identifier of node
   * @return Response indicating whether the operation succeeded or had an error
   */
  @DELETE
  @Path("{nodeGuid}")
  public Response deleteClassificationNode(
      @PathParam("nodeGuid") String nodeGuid) {
    registryService.deleteObject("Unknown", nodeGuid,
        ClassificationNode.class);
    return Response.ok().build();
  }

  protected static URI getNodeUri(String schemeGuid, ClassificationNode node,
      UriInfo uriInfo) {
    return uriInfo.getBaseUriBuilder().clone().path(RegistryResource.class)
        .path(RegistryResource.class, "getSchemesResource").path(schemeGuid)
        .path("nodes").path(node.getGuid()).build();
  }
}
