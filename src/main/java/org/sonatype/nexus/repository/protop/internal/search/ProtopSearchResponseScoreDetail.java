package org.sonatype.nexus.repository.protop.internal.search;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.annotation.Nullable;

/**
 * Data carrier (mapped to JSON) that contains score detail information for a particular protop search response entry.
 *
 * @since 3.7
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProtopSearchResponseScoreDetail {
    @Nullable
    private Double quality;

    @Nullable
    private Double popularity;

    @Nullable
    private Double maintenance;

    @Nullable
    public Double getQuality() {
        return quality;
    }

    public void setQuality(@Nullable final Double quality) {
        this.quality = quality;
    }

    @Nullable
    public Double getPopularity() {
        return popularity;
    }

    public void setPopularity(@Nullable final Double popularity) {
        this.popularity = popularity;
    }

    @Nullable
    public Double getMaintenance() {
        return maintenance;
    }

    public void setMaintenance(@Nullable final Double maintenance) {
        this.maintenance = maintenance;
    }
}
