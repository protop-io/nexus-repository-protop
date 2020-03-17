package org.sonatype.nexus.repository.protop.internal;

import org.sonatype.nexus.repository.http.HttpResponses;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Handler;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.payloads.StringPayload;

import javax.annotation.Nonnull;
import javax.inject.Named;
import javax.inject.Singleton;

import static org.sonatype.nexus.repository.view.ContentTypes.APPLICATION_JSON;

/**
 * @since 3.21
 */
@Named
@Singleton
public class ProtopPingHandler
        implements Handler {

    @Nonnull
    @Override
    public Response handle(@Nonnull final Context context) throws Exception {
        StringPayload payload = new StringPayload("{}", APPLICATION_JSON);
        return HttpResponses.ok(payload);
    }
}
