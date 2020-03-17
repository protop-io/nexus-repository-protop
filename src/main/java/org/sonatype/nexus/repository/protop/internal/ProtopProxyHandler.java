package org.sonatype.nexus.repository.protop.internal;

import org.sonatype.nexus.repository.proxy.ProxyHandler;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher.State;

/**
 * Protop specific handling for proxy repositories.
 *
 * @since 3.0
 */
public class ProtopProxyHandler
        extends ProxyHandler {
    @Override
    protected Response buildNotFoundResponse(final Context context) {
        State state = context.getAttributes().require(TokenMatcher.State.class);
        ProtopProjectId packageId = ProtopHandlers.projectId(state);
        return ProtopResponses.packageNotFound(packageId);
    }
}
