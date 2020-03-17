package org.sonatype.nexus.repository.protop.internal.search;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.annotation.Nullable;

/**
 * Data carrier for a single search response object for protop V1 search.
 *
 * @since 3.7
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProtopSearchResponseObject {
    @Nullable
    private ProtopSearchResponsePackage packageEntry;

    @Nullable
    private ProtopSearchResponseScore score;

    @Nullable
    private Double searchScore;

    @Nullable
    @JsonProperty("package")
    public ProtopSearchResponsePackage getPackageEntry() {
        return packageEntry;
    }

    public void setPackageEntry(@Nullable final ProtopSearchResponsePackage packageEntry) {
        this.packageEntry = packageEntry;
    }

    @Nullable
    public ProtopSearchResponseScore getScore() {
        return score;
    }

    public void setScore(@Nullable final ProtopSearchResponseScore score) {
        this.score = score;
    }

    @Nullable
    public Double getSearchScore() {
        return searchScore;
    }

    public void setSearchScore(@Nullable final Double searchScore) {
        this.searchScore = searchScore;
    }
}
