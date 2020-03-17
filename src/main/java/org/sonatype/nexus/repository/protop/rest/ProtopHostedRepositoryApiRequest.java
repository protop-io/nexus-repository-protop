package org.sonatype.nexus.repository.protop.rest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.sonatype.nexus.repository.protop.internal.ProtopFormat;
import org.sonatype.nexus.repository.rest.api.model.CleanupPolicyAttributes;
import org.sonatype.nexus.repository.rest.api.model.HostedRepositoryApiRequest;
import org.sonatype.nexus.repository.rest.api.model.HostedStorageAttributes;

/**
 * @since 3.next
 */
@JsonIgnoreProperties({"format", "type"})
public class ProtopHostedRepositoryApiRequest
        extends HostedRepositoryApiRequest {
    @JsonCreator
    public ProtopHostedRepositoryApiRequest(
            @JsonProperty("name") final String name,
            @JsonProperty("online") final Boolean online,
            @JsonProperty("storage") final HostedStorageAttributes storage,
            @JsonProperty("cleanup") final CleanupPolicyAttributes cleanup
    ) {
        super(name, ProtopFormat.NAME, online, storage, cleanup);
    }
}
