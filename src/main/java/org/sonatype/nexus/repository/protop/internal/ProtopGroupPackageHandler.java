package org.sonatype.nexus.repository.protop.internal;

import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.sonatype.nexus.repository.http.HttpStatus;
import org.sonatype.nexus.repository.storage.MissingAssetBlobException;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.Status;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher;

import javax.annotation.Nonnull;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Objects;

/**
 * Merge metadata results from all member repositories.
 */
@Named
@Singleton
public class ProtopGroupPackageHandler extends ProtopGroupHandler {

    @Override
    protected Response doGet(@Nonnull final Context context,
                             @Nonnull final DispatchedRepositories dispatched) throws Exception {
        log.debug("[getPackage] group repository: {} tokens: {}",
                context.getRepository().getName(),
                context.getAttributes().require(TokenMatcher.State.class).getTokens());

        return buildMergedPackageRoot(context, dispatched);
    }

    private Response buildMergedPackageRoot(final Context context,
                                            final DispatchedRepositories dispatched) throws Exception {
        final ProtopGroupFacet groupFacet = getGroupFacet(context);

        // Dispatch requests to members to trigger update events and group cache invalidation when a member has changed
        final Map responses = getResponses(context, dispatched, groupFacet);

        ProtopContent content = groupFacet.getFromCache(context);

        // first check cached content against itself only
        if (Objects.isNull(content)) {
            if (Objects.isNull(responses) || responses.isEmpty()) {
                return ProtopResponses.notFound("Not found");
            }
            return ProtopResponses.ok(groupFacet.buildPackageRoot(responses, context));
        }

        // only add missing blob handler if we actually had content, no need otherwise
        content.missingBlobInputStreamSupplier(e ->
                handleMissingBlob(context, responses, groupFacet, e));

        return new Response.Builder().status(Status.success(HttpStatus.OK))
                .payload(content)
                .attributes(content.getAttributes())
                .build();
    }

    private StorageFacet getStorageFacet(final Context context) {
        return DefaultGroovyMethods.asType(context.getRepository()
                .facet(StorageFacet.class), StorageFacet.class);
    }

    private InputStream handleMissingBlob(final Context context,
                                          final Map responses,
                                          final ProtopGroupFacet groupFacet,
                                          final MissingAssetBlobException e) throws IOException {
        // why check the response? It might occur that the members don't have cache on their own and that their remote
        // doesn't have any responses (404) for the request. For this to occur allot must be wrong on its own already.
        if (responses.isEmpty()) {
            // We can't change the status of a response, as we are streaming out therefor be kind and return an error stream
            return ProtopFacetUtils.errorInputStream("Members had no metadata to merge for repository " + context.getRepository().getName());
        }

        return groupFacet.buildMergedPackageRootOnMissingBlob(responses, context, e);
    }
}
