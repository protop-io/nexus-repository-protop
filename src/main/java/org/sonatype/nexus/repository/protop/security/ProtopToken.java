package org.sonatype.nexus.repository.protop.security;

import org.sonatype.nexus.security.token.BearerToken;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.List;

/**
 * protop API-Key; used by protop CLI to authenticate.
 */
@Named(ProtopToken.NAME)
@Singleton
public final class ProtopToken extends BearerToken {

    public static final String NAME = "ProtopToken";

    public ProtopToken() {
        super(NAME);
    }

    @Override
    protected boolean matchesFormat(final List<String> parts) {
        return super.matchesFormat(parts) || !parts.get(1).contains(".");
    }
}
