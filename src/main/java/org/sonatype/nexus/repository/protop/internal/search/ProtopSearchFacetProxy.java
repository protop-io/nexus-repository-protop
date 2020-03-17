package org.sonatype.nexus.repository.protop.internal.search;

import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.protop.internal.ProtopFacetUtils;
import org.sonatype.nexus.repository.protop.internal.ProtopProxyFacetImpl.ProxyTarget;
import org.sonatype.nexus.repository.proxy.ProxyFacet;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Parameters;
import org.sonatype.nexus.repository.view.Request;

import javax.inject.Named;
import java.io.IOException;

import static org.sonatype.nexus.repository.http.HttpMethods.GET;

/**
 * Implementation of {@code ProtopSearchFacet} for proxy repositories.
 *
 * @since 3.7
 */
@Named
public class ProtopSearchFacetProxy extends FacetSupport implements ProtopSearchFacet {

    @Override
    public Content search(final Parameters parameters) throws IOException {
        try {
            final Request getRequest = new Request.Builder()
                    .action(GET)
                    .path("/" + ProtopFacetUtils.REPOSITORY_SEARCH_ASSET)
                    .parameters(parameters)
                    .build();

            Context context = new Context(getRepository(), getRequest);
            context.getAttributes().set(ProxyTarget.class, ProxyTarget.SEARCH_RESULTS);
            Content searchResults = getRepository().facet(ProxyFacet.class).get(context);

            if (searchResults == null) {
                throw new IOException("Could not retrieve registry search");
            }

            return searchResults;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }
}
