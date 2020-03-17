package org.sonatype.nexus.repository.protop.internal;

import org.sonatype.nexus.common.app.VersionComparator;

import java.util.function.BiFunction;

import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * Utility class for Protop version comparison
 */
public final class ProtopVersionComparator {
    public static final VersionComparator versionComparator = new VersionComparator();

    public static final BiFunction<String, String, String> extractPackageRootVersionUnlessEmpty = (packageRootVersion, packageVersion) ->
            isEmpty(packageRootVersion) ? packageVersion : packageRootVersion;

    public static final BiFunction<String, String, String> extractAlwaysPackageVersion = (packageRootVersion, packageVersion) -> packageVersion;

    public static final BiFunction<String, String, String> extractNewestVersion = (packageRootVersion, packageVersion) ->
            versionComparator.compare(packageVersion, packageRootVersion) > 0
                    ? packageVersion : packageRootVersion;

    private ProtopVersionComparator() {
        // no op
    }
}
