
package org.sonatype.nexus.repository.protop.internal;

import java.util.Collections;
import java.util.Optional;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.manager.RepositoryUpdatedEvent;

import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ProtopProxyCacheInvalidatorFacetImplTest
    extends TestSupport
{
  @Mock
  private ProtopProxyFacetImpl protopProxyFacet;

  private ProtopProxyCacheInvalidatorFacetImpl underTest = new ProtopProxyCacheInvalidatorFacetImpl();

  @Test
  public void testOnUrlChange() {
    underTest
        .on(new RepositoryUpdatedEvent(mockRepository("http://example.org"), mockConfiguration("http://example.com")));

    verify(protopProxyFacet).invalidateProxyCaches();
  }

  @Test
  public void testOnNoUrlChange() {
    underTest
        .on(new RepositoryUpdatedEvent(mockRepository("http://example.org"), mockConfiguration("http://example.org")));

    verify(protopProxyFacet, never()).invalidateProxyCaches();
  }

  private Repository mockRepository(final String remoteUrl) {
    Repository repository = mock(Repository.class);
    Configuration configuration = mockConfiguration(remoteUrl);

    when(repository.getConfiguration()).thenReturn(configuration);

    Optional<ProtopProxyFacetImpl> facet = Optional.of(protopProxyFacet);
    when(repository.optionalFacet(ProtopProxyFacetImpl.class)).thenReturn(facet);

    return repository;
  }

  private Configuration mockConfiguration(final String remoteUrl) {
    Configuration configuration = mock(Configuration.class);
    when(configuration.getAttributes())
        .thenReturn(Collections.singletonMap("proxy", Collections.singletonMap("remoteUrl", remoteUrl)));
    return configuration;
  }
}
