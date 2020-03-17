package org.sonatype.nexus.repository.protop.rest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.sonatype.nexus.repository.protop.internal.ProtopFormat;
import org.sonatype.nexus.repository.rest.api.model.*;

/**
 * @since 3.next
 */
@JsonIgnoreProperties({"format", "type"})
public class ProtopProxyRepositoryApiRequest extends ProxyRepositoryApiRequest {

    @JsonCreator
    @SuppressWarnings("squid:S00107") // suppress constructor parameter count
    public ProtopProxyRepositoryApiRequest(
            @JsonProperty("name") final String name,
            @JsonProperty("online") final Boolean online,
            @JsonProperty("storage") final StorageAttributes storage,
            @JsonProperty("cleanup") final CleanupPolicyAttributes cleanup,
            @JsonProperty("proxy") final ProxyAttributes proxy,
            @JsonProperty("negativeCache") final NegativeCacheAttributes negativeCache,
            @JsonProperty("httpClient") final HttpClientAttributes httpClient,
            @JsonProperty("routingRule") final String routingRule) {
        super(name, ProtopFormat.NAME, online, storage, cleanup, proxy, negativeCache, httpClient, routingRule);
    }
}
