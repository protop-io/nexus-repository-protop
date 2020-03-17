package org.sonatype.nexus.repository.protop.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.sonatype.nexus.repository.http.HttpResponses;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Handler;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.payloads.StringPayload;
import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.security.user.User;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.view.ContentTypes.APPLICATION_JSON;

/**
 * @since 3.20
 */
@Named
@Singleton
public class ProtopWhoamiHandler
        implements Handler {
    private final SecuritySystem securitySystem;

    private final ObjectMapper objectMapper;

    @Inject
    public ProtopWhoamiHandler(final SecuritySystem securitySystem, final ObjectMapper objectMapper) {
        this.securitySystem = checkNotNull(securitySystem);
        this.objectMapper = checkNotNull(objectMapper);
    }

    @Nonnull
    @Override
    public Response handle(@Nonnull final Context context) throws Exception {
        User user = securitySystem.currentUser();

        Username username = new Username(user == null ? "anonymous" : user.getUserId());

        StringPayload payload = new StringPayload(objectMapper.writeValueAsString(username), APPLICATION_JSON);

        return HttpResponses.ok(payload);
    }

    private static final class Username {
        private final String username;

        public Username(final String username) {
            this.username = username;
        }

        public String getUsername() {
            return username;
        }
    }
}
