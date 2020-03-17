package org.sonatype.nexus.repository.protop.internal.search;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Data carrier (mapping to JSON) that contains package information in an protop search response.
 *
 * @since 3.7
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProtopSearchResponsePackage {
    @Nullable
    private String name;

    @Nullable
    private String version;

    @Nullable
    private String description;

    @Nullable
    private List<String> keywords;

    @Nullable
    private String date;

    @Nullable
    private ProtopSearchResponsePackageLinks links;

    @Nullable
    private ProtopSearchResponsePerson publisher;

    @Nullable
    private List<ProtopSearchResponsePerson> maintainers;

    @Nullable
    public String getName() {
        return name;
    }

    public void setName(@Nullable final String name) {
        this.name = name;
    }

    @Nullable
    public String getVersion() {
        return version;
    }

    public void setVersion(@Nullable final String version) {
        this.version = version;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    public void setDescription(@Nullable final String description) {
        this.description = description;
    }

    @Nullable
    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(@Nullable final List<String> keywords) {
        this.keywords = keywords;
    }

    @Nullable
    public String getDate() {
        return date;
    }

    public void setDate(@Nullable final String date) {
        this.date = date;
    }

    @Nullable
    public ProtopSearchResponsePackageLinks getLinks() {
        return links;
    }

    public void setLinks(@Nullable final ProtopSearchResponsePackageLinks links) {
        this.links = links;
    }

    @Nullable
    public ProtopSearchResponsePerson getPublisher() {
        return publisher;
    }

    public void setPublisher(@Nullable final ProtopSearchResponsePerson publisher) {
        this.publisher = publisher;
    }

    @Nullable
    public List<ProtopSearchResponsePerson> getMaintainers() {
        return maintainers;
    }

    public void setMaintainers(@Nullable final List<ProtopSearchResponsePerson> maintainers) {
        this.maintainers = maintainers;
    }
}
