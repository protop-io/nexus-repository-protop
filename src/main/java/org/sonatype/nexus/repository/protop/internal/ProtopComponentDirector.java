package org.sonatype.nexus.repository.protop.internal;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.ComponentDirector;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.transaction.Transactional;
import org.sonatype.nexus.transaction.UnitOfWork;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.protop.internal.ProtopAttributes.P_NAME;
import static org.sonatype.nexus.repository.protop.internal.ProtopPackageRootMetadataUtils.createFullPackageMetadata;
import static org.sonatype.nexus.repository.protop.internal.ProtopVersionComparator.extractNewestVersion;

/**
 * @since 3.11
 */
@Named("protop")
@Singleton
public class ProtopComponentDirector
        extends ComponentSupport
        implements ComponentDirector {
    private ProtopPackageParser protopPackageParser;

    @Inject
    public ProtopComponentDirector(final ProtopPackageParser protopPackageParser) {
        this.protopPackageParser = protopPackageParser;
    }

    @Override
    public boolean allowMoveTo(final Repository destination) {
        return true;
    }

    @Override
    public boolean allowMoveTo(final Component component, final Repository destination) {
        return true;
    }

    @Override
    public boolean allowMoveFrom(final Repository source) {
        return true;
    }

    @Override
    public Component afterMove(final Component component, final Repository destination) {
        destination.optionalFacet(ProtopHostedFacet.class).ifPresent(protopHostedFacet -> {

            UnitOfWork.begin(destination.facet(StorageFacet.class).txSupplier());
            try {
                updatePackageRoot(protopHostedFacet, component, destination);
            } finally {
                UnitOfWork.end();
            }

        });
        return component;
    }

    @Transactional
    protected void updatePackageRoot(final ProtopHostedFacet protopHostedFacet,
                                     final Component component,
                                     final Repository destination) {
        final StorageTx tx = UnitOfWork.currentTx();
        tx.browseAssets(component).forEach(asset -> {
            Blob blob = checkNotNull(tx.getBlob(asset.blobRef()));
            final Map<String, Object> packageJson = protopPackageParser.parseProtopJson(blob::getInputStream);
            final ProtopProjectId packageId = ProtopProjectId.parse((String) packageJson.get(P_NAME));

            try {
                final NestedAttributesMap updatedMetadata = createFullPackageMetadata(
                        new NestedAttributesMap("metadata", packageJson),
                        destination.getName(),
                        blob.getMetrics().getSha1Hash(),
                        destination,
                        extractNewestVersion);
                protopHostedFacet.putProjectRoot(packageId, null, updatedMetadata);
            } catch (IOException e) {
                log.error("Failed to update package root, projectId: {}", packageId, e);
            }
        });
    }
}
