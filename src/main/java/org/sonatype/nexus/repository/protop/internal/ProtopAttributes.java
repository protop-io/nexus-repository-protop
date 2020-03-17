package org.sonatype.nexus.repository.protop.internal;

import org.sonatype.nexus.repository.view.ContentTypes;

import java.util.Date;

/**
 * protop format specific CMA attributes.
 */
public final class ProtopAttributes {

    public static final String P_ORG = "org";

    public static final String P_NAME = "name";

    public static final String P_VERSION = "version";

    public static final String P_AUTHOR = "author";

    /**
     * A mapping of bin commands to set up for this version
     */
    public static final String P_BIN = "bin";

    /**
     * An array of dependencies bundled with this version
     */
    public static final String P_BUNDLE_DEPENDENCIES = "bundleDependencies";

    public static final String P_DESCRIPTION = "description";

    /**
     * An array of directories included by this version
     */
    public static final String P_DIRECTORIES = "directories";

    public static final String P_CONTRIBUTORS = "contributors";

    public static final String P_LICENSE = "license";

    public static final String P_KEYWORDS = "keywords";

    /**
     * The package's entry point
     */
    public static final String P_MAIN = "main";

    /**
     * Array of human objects for people with permission to publish this package
     */
    public static final String P_MAINTAINERS = "maintainers";

    /**
     * An object mapping package names to the required semver ranges of optional dependencies
     */
    public static final String P_OPTIONAL_DEPENDENCIES = "optionalDependencies";

    /**
     * A mapping of package names to the required semver ranges of peer dependencies
     */
    public static final String P_PEER_DEPENDENCIES = "peerDependencies";

    /**
     * On package root metadata this is a URL, on protop.json this is an object with <code>url</code> and
     * <code>email</code> fields.
     */
    public static final String P_BUGS = "bugs";

    public static final String P_BUGS_URL = "bugs_url";

    public static final String P_BUGS_EMAIL = "bugs_email";

    /**
     * The first 64K of the README data for the most-recently published version of the package
     */
    public static final String P_README = "readme";

    /**
     * The name of the file from which the readme data was taken.
     */
    public static final String P_README_FILENAME = "readmeFilename";

    /**
     * An object with type and url fields.
     */
    public static final String P_REPOSITORY = "repository";

    public static final String P_REPOSITORY_TYPE = "repository_type";

    public static final String P_REPOSITORY_URL = "repository_url";

    public static final String P_HOMEPAGE = "homepage";

    /**
     * The SHA-1 sum of the tarball
     */
    public static final String P_SHASUM = "shasum";

    /**
     * <code>true</code> if this version is known to have a shrinkwrap that must be used to install it; <code>false</code>
     * if this version is known not to have a shrinkwrap. Unset otherwise
     */
    public static final String P_HAS_SHRINK_WRAP = "_hasShrinkwrap";

    public static final String P_OS = "os";

    public static final String P_CPU = "cpu";

    public static final String P_ENGINES = "engines";

    /**
     * Special format attribute used for supporting search on "is" and "not" (currently for "unstable" packages only).
     */
    public static final String P_TAGGED_IS = "tagged_is";

    /**
     * Special format attribute used for supporting search on "is" and "not" (currently for "unstable" packages only).
     */
    public static final String P_TAGGED_NOT = "tagged_not";

    public static final String P_URL = "url";

    /**
     * An object whose keys are the protop user names of people who have starred this package
     */
    public static final String P_USERS = "users";

    /**
     * Special format attribute used for ordering by version in a lexicographic manner within ES queries.
     */
    public static final String P_SEARCH_NORMALIZED_VERSION = "search_normalized_version";

    /**
     * Format attribute on component for protop tarball that shows that given version (to which this tarball belongs to) is
     * deprecated. Attribute's value is deprecation message extracted from protop package metadata. Like in protop metadata,
     * solely by the presence of this attribute one can tell is tarball deprecated or not, while the value can provide
     * more insight why it was deprecated.
     */
    public static final String P_DEPRECATED = "deprecated";

    /**
     * Format attribute on package root asset to designate protop "modified" timestamp as {@link Date}, extracted from protop
     * package metadata "time/modified".
     */
    public static final String P_protop_LAST_MODIFIED = "last_modified";

    /**
     * Marker for asset kinds.
     */
    public enum AssetKind {

        REPOSITORY_ROOT(ContentTypes.APPLICATION_JSON, false),
        PACKAGE_ROOT(ContentTypes.APPLICATION_JSON, true),
        TARBALL(ContentTypes.APPLICATION_GZIP, false);

        private final String contentType;

        private final boolean skipContentVerification;

        AssetKind(final String contentType, final boolean skipContentVerification) {
            this.skipContentVerification = skipContentVerification;
            this.contentType = contentType;
        }

        public String getContentType() {
            return contentType;
        }

        public boolean isSkipContentVerification() {
            return skipContentVerification;
        }
    }

    private ProtopAttributes() {
        // empty
    }
}
