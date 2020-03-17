package org.sonatype.nexus.repository.protop.rest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.sonatype.nexus.repository.protop.internal.ProtopFormat;
import org.sonatype.nexus.repository.rest.api.model.GroupAttributes;
import org.sonatype.nexus.repository.rest.api.model.GroupRepositoryApiRequest;
import org.sonatype.nexus.repository.rest.api.model.StorageAttributes;

/**
 * @since 3.next
 */
@JsonIgnoreProperties({"format", "type"})
public class ProtopGroupRepositoryApiRequest
        extends GroupRepositoryApiRequest {
    @JsonCreator
    public ProtopGroupRepositoryApiRequest(
            @JsonProperty("name") final String name,
            @JsonProperty("online") final Boolean online,
            @JsonProperty("storage") final StorageAttributes storage,
            @JsonProperty("group") final GroupAttributes group) {
        super(name, ProtopFormat.NAME, online, storage, group);
    }
}
