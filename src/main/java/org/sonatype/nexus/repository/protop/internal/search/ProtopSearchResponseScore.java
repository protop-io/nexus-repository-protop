package org.sonatype.nexus.repository.protop.internal.search;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.annotation.Nullable;

/**
 * Data carrier (mapping to JSON) that contains the search result score information for a particular package for protop
 * search V1.
 *
 * @since 3.7
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProtopSearchResponseScore {
    @Nullable
    private Double finalScore;

    @Nullable
    private ProtopSearchResponseScoreDetail detail;

    @Nullable
    @JsonProperty("final")
    public Double getFinalScore() {
        return finalScore;
    }

    public void setFinalScore(@Nullable final Double finalScore) {
        this.finalScore = finalScore;
    }

    @Nullable
    public ProtopSearchResponseScoreDetail getDetail() {
        return detail;
    }

    public void setDetail(@Nullable final ProtopSearchResponseScoreDetail detail) {
        this.detail = detail;
    }
}
