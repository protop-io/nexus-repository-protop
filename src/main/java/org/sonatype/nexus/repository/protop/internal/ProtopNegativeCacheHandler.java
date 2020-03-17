package org.sonatype.nexus.repository.protop.internal;

import org.apache.http.HttpStatus;
import org.sonatype.nexus.repository.cache.NegativeCacheHandler;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.Status;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher.State;

/**
 * Protop specific handling for negative cache responses.
 *
 * @since 3.0
 */
public class ProtopNegativeCacheHandler
        extends NegativeCacheHandler {
    @Override
    protected Response buildResponse(final Status status, final Context context) {
        if (status.getCode() == HttpStatus.SC_NOT_FOUND) {
            State state = context.getAttributes().require(TokenMatcher.State.class);
            ProtopProjectId packageId = ProtopHandlers.projectId(state);
            return ProtopResponses.packageNotFound(packageId);
        }
        return super.buildResponse(status, context);
    }
}
