package org.sonatype.nexus.repository.protop.internal;

import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher;

import javax.annotation.Nonnull;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Map;
import java.util.Objects;

/**
 * Merge dist tag results from all member repositories.
 *
 * @since 3.19
 */
@Named
@Singleton
public class ProtopGroupDistTagsHandler extends ProtopGroupHandler {

    @Override
    protected Response doGet(@Nonnull final Context context,
                             @Nonnull final DispatchedRepositories dispatched) throws Exception {
        log.debug("[getDistTags] group repository: {} tokens: {}",
                context.getRepository().getName(),
                context.getAttributes().require(TokenMatcher.State.class).getTokens());

        ProtopGroupFacet groupFacet = getGroupFacet(context);

        // Dispatch requests to members to trigger update events and group cache invalidation when a member has changed
        Map responses = getResponses(context, dispatched, groupFacet);

        if (Objects.isNull(responses) || responses.isEmpty()) {
            return ProtopResponses.notFound("Not found");
        }


        return ProtopFacetUtils.mergeDistTagResponse(responses);
    }
}
