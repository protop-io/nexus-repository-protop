package org.sonatype.nexus.repository.protop.internal;

import org.joda.time.DateTime;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.transaction.UnitOfWork;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.sonatype.nexus.repository.protop.internal.ProtopAttributes.P_NAME;
import static org.sonatype.nexus.repository.protop.internal.ProtopAttributes.P_ORG;
import static org.sonatype.nexus.repository.protop.internal.ProtopFacetUtils.findPackageRootAsset;
import static org.sonatype.nexus.repository.protop.internal.ProtopFacetUtils.loadPackageRoot;
import static org.sonatype.nexus.repository.protop.internal.ProtopMetadataUtils.*;

/**
 * Helper for protop package root metadata.
 * <p>
 * See https://github.com/protop/registry/blob/master/docs/responses/package-metadata.md
 *
 * @since 3.7
 */
public class ProtopPackageRootMetadataUtils {
    private static final String MODIFIED = "modified";

    private static final String CREATED = "created";

    private static final String LATEST = "latest";

    private static final String DEPENDENCIES = "dependencies";

    private static final String DEV_DEPENDENCIES = "devDependencies";

    private static final String[] FULL_HOISTED_FIELDS = new String[]{ProtopAttributes.P_AUTHOR,
            ProtopAttributes.P_CONTRIBUTORS, ProtopAttributes.P_DESCRIPTION, ProtopAttributes.P_HOMEPAGE, ProtopAttributes.P_KEYWORDS,
            ProtopAttributes.P_LICENSE, ProtopAttributes.P_MAINTAINERS, ProtopAttributes.P_NAME, ProtopAttributes.P_ORG, ProtopAttributes.P_README,
            ProtopAttributes.P_README_FILENAME, ProtopAttributes.P_REPOSITORY};

    private static final String[] FULL_VERSION_MAP_FIELDS = new String[]{ProtopAttributes.P_AUTHOR,
            ProtopAttributes.P_CONTRIBUTORS,
            ProtopAttributes.P_DEPRECATED, DEPENDENCIES, ProtopAttributes.P_DESCRIPTION, ProtopAttributes.P_LICENSE,
            ProtopAttributes.P_MAIN, ProtopAttributes.P_MAINTAINERS, ProtopAttributes.P_NAME, ProtopAttributes.P_VERSION,
            ProtopAttributes.P_ORG, ProtopAttributes.P_OPTIONAL_DEPENDENCIES, DEV_DEPENDENCIES, ProtopAttributes.P_BUNDLE_DEPENDENCIES,
            ProtopAttributes.P_PEER_DEPENDENCIES, ProtopAttributes.P_BIN, ProtopAttributes.P_DIRECTORIES, ProtopAttributes.P_ENGINES,
            ProtopAttributes.P_README, ProtopAttributes.P_README_FILENAME,
            // This isn't currently in protop.json but could be determined by the presence
            // of protop-shrinkwrap.json
            ProtopAttributes.P_HAS_SHRINK_WRAP};

    private ProtopPackageRootMetadataUtils() {
        // sonar
    }

    /**
     * Creates full package data from the metadata of an individual version. May change <code>packageJson</code> if child
     * nodes do not exist.
     *
     * @param packageJson    the metadata for the version
     * @param repositoryName the repository name
     * @param sha1sum        the hash of the version
     * @since 3.7
     */
    public static NestedAttributesMap createFullPackageMetadata(final NestedAttributesMap packageJson,
                                                                final String repositoryName,
                                                                final String sha1sum,
                                                                @Nullable final Repository repository,
                                                                final BiFunction<String, String, String> function) {
        String org = packageJson.get(ProtopAttributes.P_ORG, String.class);
        String name = packageJson.get(ProtopAttributes.P_NAME, String.class);

        ProtopProjectId projectId = new ProtopProjectId(org, name);
        String version = packageJson.get(ProtopAttributes.P_VERSION, String.class);
        String now = PROTOP_TIMESTAMP_FORMAT.print(DateTime.now());

        NestedAttributesMap packageRoot = new NestedAttributesMap("metadata", new HashMap<String, Object>());

        packageRoot.set(META_ID, projectId.id());

        String packageRootLatestVersion = isNull(repository) ? "" : getPackageRootLatestVersion(packageJson, repository);

        packageRoot.child(DIST_TAGS).set(LATEST, function.apply(packageRootLatestVersion, version));

        packageRoot.child(ProtopAttributes.P_USERS);

        NestedAttributesMap time = packageRoot.child(TIME);
        time.set(version, now);
        time.set(MODIFIED, now);
        time.set(CREATED, now);

        // Hoisting fields from version metadata
        setBugsUrl(packageJson, packageRoot);

        for (String field : FULL_HOISTED_FIELDS) {
            copy(packageRoot, packageJson, field);
        }

        // Copy version specific metadata fields
        NestedAttributesMap versionMap = packageRoot.child(VERSIONS).child(version);
        versionMap.set(META_ID, projectId + "@" + version);

        // required fields
        versionMap.child(DIST).set(ProtopAttributes.P_SHASUM, sha1sum);
        versionMap.child(DIST).set(TARBALL,
                String.format("%s/repository/%s",
                        repositoryName,
                        ProtopMetadataUtils.createRepositoryPath(org, name, version)));

        // optional fields
        for (String field : FULL_VERSION_MAP_FIELDS) {
            copy(versionMap, packageJson, field);
        }

        // needs to happen after copying fields
        rewriteTarballUrl(repositoryName, packageRoot);

        return packageRoot;
    }

    private static String getPackageRootLatestVersion(final NestedAttributesMap protopJson,
                                                      final Repository repository) {
        StorageTx tx = UnitOfWork.currentTx();

        String org = (String) protopJson.get(P_ORG);
        String name = (String) protopJson.get(P_NAME);

        ProtopProjectId projectId = new ProtopProjectId(org, name);

        try {
            NestedAttributesMap packageRoot = getPackageRoot(tx, repository, projectId);
            if (nonNull(packageRoot)) {

                String latestVersion = getLatestVersionFromPackageRoot(packageRoot);
                if (nonNull(latestVersion)) {
                    return latestVersion;
                }
            }
        } catch (IOException ignored) { // NOSONAR
        }
        return "";
    }

    /**
     * Fetches the package root as {@link NestedAttributesMap}
     *
     * @param tx
     * @param repository
     * @param packageId
     * @return package root if found otherwise null
     * @throws IOException
     */
    @Nullable
    public static NestedAttributesMap getPackageRoot(final StorageTx tx,
                                                     final Repository repository,
                                                     final ProtopProjectId packageId) throws IOException {
        Bucket bucket = tx.findBucket(repository);

        Asset packageRootAsset = findPackageRootAsset(tx, bucket, packageId);
        if (packageRootAsset != null) {
            return loadPackageRoot(tx, packageRootAsset);
        }
        return null;
    }

    private static void copy(final NestedAttributesMap map, final NestedAttributesMap src, final String field) {
        Object object = src.get(field);
        if (object instanceof Map) {
            NestedAttributesMap destChild = map.child(field);
            NestedAttributesMap srcChild = src.child(field);
            for (String key : srcChild.keys()) {
                if (srcChild.get(field) instanceof Map) {
                    copy(destChild, srcChild, key);
                } else {
                    destChild.set(key, srcChild.get(key));
                }
            }
        } else if (object != null) {
            map.set(field, object);
        }
    }

    private static void setBugsUrl(NestedAttributesMap packageJson, NestedAttributesMap packageRoot) {
        Object bugs = packageJson.get(ProtopAttributes.P_BUGS);
        String bugsUrl = null;

        if (bugs instanceof String) {
            bugsUrl = (String) bugs;
        } else if (bugs != null) {
            bugsUrl = packageJson.child(ProtopAttributes.P_BUGS).get(ProtopAttributes.P_URL, String.class);
        }

        if (bugsUrl != null) {
            packageRoot.set(ProtopAttributes.P_BUGS, bugsUrl);
        }
    }
}
