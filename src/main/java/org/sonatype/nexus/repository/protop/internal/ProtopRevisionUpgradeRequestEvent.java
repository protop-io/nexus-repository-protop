package org.sonatype.nexus.repository.protop.internal;

import org.sonatype.nexus.repository.storage.Asset;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Event for requesting an upgrade on the revision field of an protop package root.
 *
 * @since 3.17
 */
public class ProtopRevisionUpgradeRequestEvent {
    private Asset packageRootAsset;

    private String revision;

    public ProtopRevisionUpgradeRequestEvent(final Asset packageRootAsset, final String revision) {
        this.packageRootAsset = checkNotNull(packageRootAsset);
        this.revision = checkNotNull(revision);
    }

    public Asset getPackageRootAsset() {
        return packageRootAsset;
    }

    public String getRevision() {
        return revision;
    }
}
