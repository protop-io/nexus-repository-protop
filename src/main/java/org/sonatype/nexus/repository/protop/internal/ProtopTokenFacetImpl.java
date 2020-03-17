package org.sonatype.nexus.repository.protop.internal;

import com.google.common.collect.Maps;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.http.HttpResponses;
import org.sonatype.nexus.repository.protop.internal.security.ProtopTokenManager;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.TempBlob;
import org.sonatype.nexus.repository.view.ContentTypes;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.payloads.BytesPayload;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Implementation of {@link ProtopTokenFacet}.
 */
@Named
public class ProtopTokenFacetImpl extends FacetSupport implements ProtopTokenFacet {

    private final ProtopTokenManager protopTokenManager;

    @Inject
    public ProtopTokenFacetImpl(final ProtopTokenManager protopTokenManager) {
        this.protopTokenManager = checkNotNull(protopTokenManager);
    }

    @Override
    public Response login(final Context context) {
        final Payload payload = context.getRequest().getPayload();

        if (payload == null) {
            return ProtopResponses.badRequest("Missing body");
        }

        StorageFacet storageFacet = facet(StorageFacet.class);
        try (TempBlob tempBlob = storageFacet.createTempBlob(payload, ProtopFacetUtils.HASH_ALGORITHMS)) {
            NestedAttributesMap request = ProtopJsonUtils.parse(tempBlob);
            String token = protopTokenManager.login(request.get("username", String.class), request.get("password", String.class));
            if (null != token) {
                NestedAttributesMap response = new NestedAttributesMap("response", Maps.newHashMap());
                response.set("token", token);
                return HttpResponses.created(new BytesPayload(ProtopJsonUtils.bytes(response), ContentTypes.APPLICATION_JSON));
            } else {
                return ProtopResponses.badCredentials("Bad username or password");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * TODO change this to no response body.
     */
    @Override
    public Response logout(final Context context) {
        if (protopTokenManager.logout()) {
            NestedAttributesMap response = new NestedAttributesMap("response", Maps.newHashMap());
            response.set("ok", Boolean.TRUE.toString());
            return ProtopResponses.ok(new BytesPayload(ProtopJsonUtils.bytes(response), ContentTypes.APPLICATION_JSON));
        } else {
            return ProtopResponses.notFound("Token not found");
        }
    }
}
