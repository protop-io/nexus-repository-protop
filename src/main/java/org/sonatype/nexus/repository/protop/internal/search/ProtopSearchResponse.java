package org.sonatype.nexus.repository.protop.internal.search;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Response for an protop V1 search request.
 *
 * @since 3.7
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProtopSearchResponse {
    @Nullable
    private List<ProtopSearchResponseObject> objects;

    @Nullable
    private Integer total;

    @Nullable
    private String time;

    @Nullable
    public List<ProtopSearchResponseObject> getObjects() {
        return objects;
    }

    public void setObjects(@Nullable final List<ProtopSearchResponseObject> objects) {
        this.objects = objects;
    }

    @Nullable
    public Integer getTotal() {
        return total;
    }

    public void setTotal(@Nullable final Integer total) {
        this.total = total;
    }

    @Nullable
    public String getTime() {
        return time;
    }

    public void setTime(@Nullable final String time) {
        this.time = time;
    }
}
