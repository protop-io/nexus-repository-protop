package org.sonatype.nexus.repository.protop.internal;

import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Response;

/**
 * protop token management facet.
 */
@Facet.Exposed
public interface ProtopTokenFacet extends Facet {
    /**
     * Performs a login for user authenticated in the request (creates token and returns login specific response).
     */
    Response login(Context context);

    /**
     * Performs a log-out for currently authenticated user (deletes the token if found and returns logout specific
     * response).
     */
    Response logout(Context context);
}
