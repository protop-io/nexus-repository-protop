package org.sonatype.nexus.repository.protop.internal.search;

import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Parameters;

import java.io.IOException;

/**
 * Facet for protop search.
 */
@Facet.Exposed
public interface ProtopSearchFacet extends Facet {

    /**
     * Fetches the search results.
     */
    Content search(final Parameters parameters) throws IOException;
}
