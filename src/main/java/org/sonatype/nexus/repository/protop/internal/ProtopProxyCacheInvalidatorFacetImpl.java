package org.sonatype.nexus.repository.protop.internal;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.manager.RepositoryUpdatedEvent;

import javax.inject.Named;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Handles invalidating protop proxy cache when the URL for the repository changes.
 *
 * @since 3.21
 */
@Named
@Facet.Exposed
public class ProtopProxyCacheInvalidatorFacetImpl
        extends FacetSupport {
    @Subscribe
    @AllowConcurrentEvents
    protected void on(final RepositoryUpdatedEvent event) {
        final Repository repository = event.getRepository();

        repository.optionalFacet(ProtopProxyFacetImpl.class).ifPresent(protop -> {
            if (!Objects.equals(getRemoteUrl(repository.getConfiguration()), getRemoteUrl(event.getOldConfiguration()))) {
                protop.invalidateProxyCaches();
            }
        });
    }

    private static Object getRemoteUrl(final Configuration configuration) {
        return Optional.ofNullable(configuration.getAttributes().get("proxy")).map(proxy -> {
            if (proxy instanceof Map) {
                return proxy.get("remoteUrl");
            }
            return null;
        }).orElse(null);
    }
}
