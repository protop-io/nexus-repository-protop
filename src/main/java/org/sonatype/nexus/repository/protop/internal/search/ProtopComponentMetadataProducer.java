package org.sonatype.nexus.repository.protop.internal.search;

import org.sonatype.nexus.repository.protop.internal.ProtopFormat;
import org.sonatype.nexus.repository.search.ComponentMetadataProducerExtension;
import org.sonatype.nexus.repository.search.DefaultComponentMetadataProducer;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Component;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Set;

/**
 * Protop implementation of {@link DefaultComponentMetadataProducer}
 *
 * @since 3.14
 */
@Singleton
@Named(ProtopFormat.NAME)
public class ProtopComponentMetadataProducer
        extends DefaultComponentMetadataProducer {
    @Inject
    public ProtopComponentMetadataProducer(final Set<ComponentMetadataProducerExtension> extensions) {
        super(extensions);
    }

    @Override
    protected boolean isPrerelease(final Component component, final Iterable<Asset> assets) {
        return component.version().contains("-");
    }
}
