package org.sonatype.nexus.repository.protop.rest;

import io.swagger.annotations.*;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.sonatype.nexus.repository.rest.api.AbstractRepositoriesApiResource;
import org.sonatype.nexus.repository.rest.api.AbstractRepositoryApiRequestToConfigurationConverter;
import org.sonatype.nexus.repository.rest.api.AuthorizingRepositoryManager;
import org.sonatype.nexus.repository.rest.api.RepositoriesApiResource;
import org.sonatype.nexus.validation.Validate;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import static org.sonatype.nexus.rest.ApiDocConstants.*;

/**
 * @since 3.next
 */
@Api(value = API_REPOSITORY_MANAGEMENT)
@Named
@Singleton
@Path(RepositoriesApiResource.RESOURCE_URI + "/protop/proxy")
public class ProtopProxyRepositoriesApiResource
        extends AbstractRepositoriesApiResource<ProtopProxyRepositoryApiRequest> {
    @Inject
    public ProtopProxyRepositoriesApiResource(
            final AuthorizingRepositoryManager authorizingRepositoryManager,
            final AbstractRepositoryApiRequestToConfigurationConverter<ProtopProxyRepositoryApiRequest> configurationAdapter) {
        super(authorizingRepositoryManager, configurationAdapter);
    }

    @ApiOperation("Create Maven proxy repository")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = REPOSITORY_CREATED),
            @ApiResponse(code = 401, message = AUTHENTICATION_REQUIRED),
            @ApiResponse(code = 403, message = INSUFFICIENT_PERMISSIONS)
    })
    @POST
    @RequiresAuthentication
    @Validate
    @Override
    public Response createRepository(final ProtopProxyRepositoryApiRequest request) {
        return super.createRepository(request);
    }

    @ApiOperation("Update Maven proxy repository")
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = REPOSITORY_UPDATED),
            @ApiResponse(code = 401, message = AUTHENTICATION_REQUIRED),
            @ApiResponse(code = 403, message = INSUFFICIENT_PERMISSIONS)
    })
    @PUT
    @Path("/{repositoryName}")
    @RequiresAuthentication
    @Validate
    @Override
    public Response updateRepository(
            final ProtopProxyRepositoryApiRequest request,
            @ApiParam(value = "Name of the repository to update") @PathParam("repositoryName") final String repositoryName) {
        return super.updateRepository(request, repositoryName);
    }
}
