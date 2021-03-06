package org.sonatype.nexus.repository.protop.internal;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.TempBlob;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * protop hosted facet.
 *
 * @since 3.0
 */
@Facet.Exposed
public interface ProtopHostedFacet extends Facet {
    /**
     * Returns the package metadata or {@code null}.
     */
    @Nullable
    Content getPackage(ProtopProjectId packageId) throws IOException;

    /**
     * Performs a "publish" of a package as sent by protop CLI.
     */
    void putPackage(ProtopProjectId packageId, @Nullable String revision, Payload payload) throws IOException;

    /**
     * Add the package using the protop.json and <code>TempBlob</code>.
     *
     * @since 3.7
     */
    Asset putPackage(Map<String, Object> packageJson, TempBlob tempBlob) throws IOException;

    /**
     * Deletes complete package along with all belonging tarballs too (if any).
     *
     * @return name of deleted asset(s).
     */
    Set<String> deletePackage(ProtopProjectId packageId, @Nullable String revision) throws IOException;

    /**
     * Deletes complete package along with all belonging tarballs too (if any), maybe deletes the blobs.
     *
     * @return name of deleted asset(s).
     * @since 3.9
     */
    Set<String> deletePackage(ProtopProjectId packageId, @Nullable String revision, boolean deleteBlobs) throws IOException;

    /**
     * Returns the tarball content or {@code null}.
     */
    @Nullable
    Content getTarball(ProtopProjectId packageId, String tarballName) throws IOException;

    /**
     * Deletes given tarball, if exists.
     *
     * @return name of deleted asset(s).
     */
    Set<String> deleteTarball(ProtopProjectId packageId, String tarballName) throws IOException;

    /**
     * Deletes given tarball, if exists, and maybe deletes the blob.
     *
     * @return name of deleted asset(s).
     * @since 3.9
     */
    Set<String> deleteTarball(ProtopProjectId packageId, String tarballName, boolean deleteBlob) throws IOException;

    /**
     * Updates the package root.
     *
     * @param packageId
     * @param revision
     * @param newPackageRoot
     * @since 3.10
     */
    void putProjectRoot(final ProtopProjectId packageId, @Nullable final String revision,
                        final NestedAttributesMap newPackageRoot) throws IOException;

    /**
     * Returns the package metadata or {@code null}.
     */
    @Nullable
    Content getDistTags(ProtopProjectId packageId) throws IOException;

    /**
     * Performs a "publish" of a dist-tag.
     */
    void putDistTags(ProtopProjectId packageId, String tag, Payload payload) throws IOException;

    void deleteDistTags(ProtopProjectId packageId, String tag, Payload payload) throws IOException;
}
