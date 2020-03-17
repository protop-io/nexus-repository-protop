package org.sonatype.nexus.repository.protop.internal;

import org.sonatype.nexus.repository.security.ContentPermissionChecker;
import org.sonatype.nexus.repository.security.SecurityFacetSupport;
import org.sonatype.nexus.repository.security.VariableResolverAdapter;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * protop format security facet.
 *
 * @since 3.0
 */
@Named
public class ProtopSecurityFacet
        extends SecurityFacetSupport {
    @Inject
    public ProtopSecurityFacet(final ProtopFormatSecurityContributor securityContributor,
                               @Named("simple") final VariableResolverAdapter variableResolverAdapter,
                               final ContentPermissionChecker contentPermissionChecker) {
        super(securityContributor, variableResolverAdapter, contentPermissionChecker);
    }
}
