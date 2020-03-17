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

@Api(value = API_REPOSITORY_MANAGEMENT)
@Named
@Singleton
@Path(RepositoriesApiResource.RESOURCE_URI + "/protop/hosted")
public class ProtopHostedRepositoriesApiResource
        extends AbstractRepositoriesApiResource<ProtopHostedRepositoryApiRequest> {

    @Inject
    public ProtopHostedRepositoriesApiResource(
            final AuthorizingRepositoryManager authorizingRepositoryManager,
            final AbstractRepositoryApiRequestToConfigurationConverter<ProtopHostedRepositoryApiRequest> configurationAdapter) {
        super(authorizingRepositoryManager, configurationAdapter);
    }

    @ApiOperation("Create Protop hosted repository")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = REPOSITORY_CREATED),
            @ApiResponse(code = 401, message = AUTHENTICATION_REQUIRED),
            @ApiResponse(code = 403, message = INSUFFICIENT_PERMISSIONS)
    })
    @POST
    @RequiresAuthentication
    @Validate
    @Override
    public Response createRepository(final ProtopHostedRepositoryApiRequest request) {
        return super.createRepository(request);
    }

    @ApiOperation("Update Protop hosted repository")
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
            final ProtopHostedRepositoryApiRequest request,
            @ApiParam(value = "Name of the repository to update") @PathParam("repositoryName") final String repositoryName) {
        return super.updateRepository(request, repositoryName);
    }
}
