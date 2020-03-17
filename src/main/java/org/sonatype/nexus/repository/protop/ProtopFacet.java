package org.sonatype.nexus.repository.protop;

import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetBlob;

import javax.annotation.Nullable;
import java.io.IOException;

/**
 * Protop facet, present on all Protop repositories.
 */
@Facet.Exposed
public interface ProtopFacet extends Facet {

    @Nullable
    Asset findRepositoryRootAsset();

    @Nullable
    Asset putRepositoryRoot(AssetBlob assetBlob, @Nullable AttributesMap contentAttributes) throws IOException;


    @Nullable
    Asset findPackageRootAsset(String packageId);

    @Nullable
    Asset putPackageRoot(String packageId, AssetBlob assetBlob, @Nullable AttributesMap contentAttributes)
            throws IOException;

    @Nullable
    Asset findTarballAsset(String packageId,
                           String tarballName);

    @Nullable
    Asset putTarball(String packageId,
                     String tarballName,
                     AssetBlob assetBlob,
                     @Nullable AttributesMap contentAttributes) throws IOException;
}
