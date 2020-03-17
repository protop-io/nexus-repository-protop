package org.sonatype.nexus.repository.protop.internal;

import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.search.IndexSettingsContributorSupport;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * @since 3.7
 */
@Named
@Singleton
public class ProtopIndexSettingsContributor extends IndexSettingsContributorSupport {
    @Inject
    public ProtopIndexSettingsContributor(@Named(ProtopFormat.NAME) final Format format) {
        super(format);
    }
}
