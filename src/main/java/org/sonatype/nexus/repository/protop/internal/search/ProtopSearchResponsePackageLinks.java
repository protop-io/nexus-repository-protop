package org.sonatype.nexus.repository.protop.internal.search;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.annotation.Nullable;

/**
 * Data carrier (mapping to JSON) for the links portion of a package entry in an protop V1 search response.
 *
 * @since 3.7
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProtopSearchResponsePackageLinks {
    @Nullable
    private String protop;

    @Nullable
    private String homepage;

    @Nullable
    private String repository;

    @Nullable
    private String bugs;

    @Nullable
    public String getProtop() {
        return protop;
    }

    public void setProtop(@Nullable final String protop) {
        this.protop = protop;
    }

    @Nullable
    public String getHomepage() {
        return homepage;
    }

    public void setHomepage(@Nullable final String homepage) {
        this.homepage = homepage;
    }

    @Nullable
    public String getRepository() {
        return repository;
    }

    public void setRepository(@Nullable final String repository) {
        this.repository = repository;
    }

    @Nullable
    public String getBugs() {
        return bugs;
    }

    public void setBugs(@Nullable final String bugs) {
        this.bugs = bugs;
    }
}
