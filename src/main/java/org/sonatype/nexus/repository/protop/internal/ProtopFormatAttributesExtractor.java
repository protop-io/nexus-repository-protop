package org.sonatype.nexus.repository.protop.internal;

import org.elasticsearch.common.Strings;
import org.sonatype.nexus.common.app.VersionComparator;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.storage.Asset;

import javax.annotation.Nullable;
import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.Character.isDigit;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.*;
import static org.sonatype.nexus.repository.protop.internal.ProtopAttributes.*;

/**
 * Extracts supported format attributes from a map of parsed protop.json data.
 *
 * @since 3.7
 */
public class ProtopFormatAttributesExtractor {
    private static final String FIRST_STABLE_VERSION = "1.0.0";

    private static final VersionComparator comparator = ProtopVersionComparator.versionComparator;

    private static final String ORG = "org";

    private static final String NAME = "name";

    private static final String VERSION = "version";

    private static final String AUTHOR = "author";

    private static final String DESCRIPTION = "description";

    private static final String LICENSE = "license";

    private static final String KEYWORDS = "keywords";

    private static final String EMAIL = "email";

    private static final String URL = "url";

    private static final String TYPE = "type";

    private static final String REPOSITORY = "repository";

    private static final String BUGS = "bugs";

    private static final String HOMEPAGE = "homepage";

    private static final String UNSTABLE = "unstable";

    private static final String CONTRIBUTORS = "contributors";

    private static final String OS = "os";

    private static final String CPU = "cpu";

    private static final String ENGINES = "engines";

    private final Map<String, Object> packageJson;

    /**
     * Creates a new format attributes extractor for the specified package JSON.
     */
    public ProtopFormatAttributesExtractor(final Map<String, Object> packageJson) {
        this.packageJson = checkNotNull(packageJson);
    }

    /**
     * Copies the relevant format attributes into the asset's format attributes.
     */
    public void copyFormatAttributes(final Asset asset) {
        NestedAttributesMap assetFormatAttributes = asset.formatAttributes();
        copyIfExists(assetFormatAttributes, P_ORG, packageJson.get(ORG));
        copyIfExists(assetFormatAttributes, P_NAME, packageJson.get(NAME));
        copyIfExists(assetFormatAttributes, P_VERSION, packageJson.get(VERSION));
        copyIfExists(assetFormatAttributes, P_AUTHOR, extractPersonInfo(packageJson.get(AUTHOR)));
        copyIfExists(assetFormatAttributes, P_CONTRIBUTORS, getContributors());
        copyIfExists(assetFormatAttributes, P_DESCRIPTION, packageJson.get(DESCRIPTION));
        copyIfExists(assetFormatAttributes, P_LICENSE, getLicense());
        copyIfExists(assetFormatAttributes, P_KEYWORDS, extractStringCollection(packageJson.get(KEYWORDS)));
        copyIfExists(assetFormatAttributes, P_OS, extractStringCollection(packageJson.get(OS)));
        copyIfExists(assetFormatAttributes, P_CPU, extractStringCollection(packageJson.get(CPU)));
        copyIfExists(assetFormatAttributes, P_ENGINES, getEngines());

        copyIfExists(assetFormatAttributes, P_REPOSITORY_URL, getRepositoryUrl());
        copyIfExists(assetFormatAttributes, P_REPOSITORY_TYPE, getRepositoryType());
        copyIfExists(assetFormatAttributes, P_BUGS_URL, getBugsUrl());
        copyIfExists(assetFormatAttributes, P_BUGS_EMAIL, getBugsEmail());
        copyIfExists(assetFormatAttributes, P_HOMEPAGE, getHomepage());

        boolean unstable = isUnstable();
        copyIfExists(assetFormatAttributes, P_TAGGED_IS, unstable ? UNSTABLE : "");
        copyIfExists(assetFormatAttributes, P_TAGGED_NOT, unstable ? "" : UNSTABLE);

        copyIfExists(assetFormatAttributes, P_SEARCH_NORMALIZED_VERSION, getSearchNormalizedVersion());
    }

    /**
     * Returns the list of contributors, if present, with each contributor represented as a string.
     */
    @Nullable
    private List<String> getContributors() {
        Object contributors = packageJson.get(CONTRIBUTORS);
        if (contributors instanceof List) {
            return ((List<Object>) contributors).stream()
                    .map(this::extractPersonInfo)
                    .filter(Objects::nonNull)
                    .collect(toList());
        } else if (contributors instanceof Map || contributors instanceof String) {
            return singletonList(extractPersonInfo(contributors));
        } else {
            return null;
        }
    }

    /**
     * Returns the list of supported engines, if present, stored as a map of engine names to supported versions.
     */
    @Nullable
    private Map<String, String> getEngines() {
        Object engines = packageJson.get(ENGINES);
        if (engines instanceof Map) {
            Map<Object, Object> enginesMap = (Map<Object, Object>) engines;
            return enginesMap.entrySet().stream().collect(
                    toMap(entry -> Objects.toString(entry.getKey()), entry -> Objects.toString(entry.getValue())));
        } else {
            return null;
        }
    }

    /**
     * Returns "person" information formatted as a string.
     */
    @Nullable
    private String extractPersonInfo(final Object person) {
        if (person instanceof Map) {
            Map<String, Object> authorMap = (Map<String, Object>) person;
            List<String> pieces = new ArrayList<>();
            if (authorMap.containsKey(NAME)) {
                pieces.add(String.valueOf(authorMap.get(NAME)));
            }
            if (authorMap.containsKey(EMAIL)) {
                pieces.add(String.format("<%s>", authorMap.get(EMAIL)));
            }
            if (authorMap.containsKey(URL)) {
                pieces.add(String.format("(%s)", authorMap.get(URL)));
            }
            return String.join(" ", pieces);
        } else if (person instanceof String) {
            return person.toString();
        } else {
            return null;
        }
    }

    /**
     * Returns a collection of strings (or a single string masquerading as such) formatted into a single joined string.
     */
    @Nullable
    private String extractStringCollection(final Object strings) {
        if (strings instanceof Collection) {
            return String.join(" ", ((Collection<Object>) strings).stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .collect(toList()));
        } else if (strings instanceof String) {
            return strings.toString();
        } else {
            return null;
        }
    }

    /**
     * Returns the license string.
     */
    @Nullable
    private String getLicense() {
        Object license = packageJson.get(LICENSE);
        if (license instanceof Map) {
            Map<String, Object> licenseMap = (Map<String, Object>) license;
            if (licenseMap.containsKey(TYPE)) {
                return licenseMap.get(TYPE).toString();
            } else {
                return null;
            }
        } else if (license instanceof String) {
            return license.toString();
        } else {
            return null;
        }
    }

    /**
     * Returns the homepage for the project.
     */
    @Nullable
    private String getHomepage() {
        if (packageJson.containsKey(HOMEPAGE)) {
            return packageJson.get(HOMEPAGE).toString();
        } else {
            return null;
        }
    }

    @Nullable
    private String getBugsUrl() {
        Object bugs = packageJson.get(BUGS);
        if (bugs instanceof String) {
            return (String) bugs;
        }

        return extractStringFromMap(BUGS, URL);
    }

    @Nullable
    private String getBugsEmail() {
        return extractStringFromMap(BUGS, EMAIL);
    }

    @Nullable
    private String getRepositoryUrl() {
        return extractStringFromMap(REPOSITORY, URL);
    }

    @Nullable
    private String getRepositoryType() {
        return extractStringFromMap(REPOSITORY, TYPE);
    }

    /**
     * Returns a "normalized" version suitable for lexicographical ordering in Elasticsearch. Overall this attempts to
     * generally mimic the behavior of the {@code VersionComparator}, which while likely not suitable for general
     * semver ordering, is nevertheless what we use for protop.
     */
    @Nullable
    private String getSearchNormalizedVersion() {
        String versionString = (String) packageJson.get(VERSION);
        if (versionString == null) {
            return null;
        }

        LinkedList<Integer> codePoints = versionString.codePoints()
                .boxed()
                .collect(toCollection(LinkedList::new));

        StringBuilder normalizedVersion = new StringBuilder();
        while (!codePoints.isEmpty()) {
            // do we have a run of one or more integers?
            if (isDigit(codePoints.peek())) {
                // it's an integer, so pad to a fixed set of zeroes for padding (so we can sort as a string)
                StringBuilder temp = new StringBuilder();
                while (!codePoints.isEmpty() && isDigit(codePoints.peek())) {
                    temp.appendCodePoint(codePoints.pop());
                }
                normalizedVersion.append(Strings.padStart(temp.toString(), 5, '0'));
            } else {
                // if it's not numeric, just pass it on through (similar to AlphanumericComparator)
                normalizedVersion.appendCodePoint(codePoints.pop());
            }
        }

        return normalizedVersion.toString();
    }

    /**
     * Returns whether or not this package is "unstable" (has a version number less than 1.0.0).
     */
    private boolean isUnstable() {
        if (packageJson.containsKey(VERSION)) {
            String version = (String) packageJson.get(VERSION);
            return comparator.compare(version, FIRST_STABLE_VERSION) < 0;
        }
        return true;
    }

    /**
     * Returns the child string for the specified map in the protop.json, if present.
     */
    @Nullable
    private String extractStringFromMap(final String mapKey, final String fieldKey) {
        Object possibleMap = packageJson.get(mapKey);
        if (possibleMap instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) possibleMap;
            if (map.containsKey(fieldKey)) {
                return map.get(fieldKey).toString();
            }
        }
        return null;
    }

    /**
     * Sets a value into the map if the value is non-null.
     */
    private void copyIfExists(final NestedAttributesMap attributesMap, final String key, @Nullable final Object value) {
        if (value != null) {
            attributesMap.set(key, value);
        }
    }

    /**
     * Sets values into a child map.
     */
    private void copyIfExists(final NestedAttributesMap attributesMap,
                              final String key,
                              @Nullable final Map<String, String> values) {
        if (values != null) {
            NestedAttributesMap child = attributesMap.child(key);
            values.forEach(child::set);
        }
    }
}
