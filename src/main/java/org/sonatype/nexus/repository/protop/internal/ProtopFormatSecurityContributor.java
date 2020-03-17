package org.sonatype.nexus.repository.protop.internal;

import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.security.RepositoryFormatSecurityContributor;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * protop format security contributor.
 *
 * @since 3.0
 */
@Named
@Singleton
public class ProtopFormatSecurityContributor
        extends RepositoryFormatSecurityContributor {
    @Inject
    public ProtopFormatSecurityContributor(@Named(ProtopFormat.NAME) final Format format) {
        super(format);
    }
}
