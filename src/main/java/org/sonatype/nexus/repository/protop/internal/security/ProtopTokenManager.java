package org.sonatype.nexus.repository.protop.internal.security;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.sonatype.nexus.repository.protop.security.ProtopToken;
import org.sonatype.nexus.security.SecurityHelper;
import org.sonatype.nexus.security.authc.apikey.ApiKeyStore;
import org.sonatype.nexus.security.token.BearerTokenManager;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * protop api key manager.
 */
@Named
@Singleton
public class ProtopTokenManager extends BearerTokenManager {
    @Inject
    public ProtopTokenManager(final ApiKeyStore apiKeyStore, final SecurityHelper securityHelper) {
        super(apiKeyStore, securityHelper, ProtopToken.NAME);
    }

    /**
     * Verifies passed in principal/credentials combo, and creates (if not already exists) a protop token mapped to given
     * principal and returns the newly created token.
     */
    public String login(final String username, final String password) {
        checkNotNull(username);
        checkNotNull(password);

        try {
            AuthenticationInfo authenticationInfo = securityHelper.getSecurityManager().authenticate(
                    new UsernamePasswordToken(username, password));
            return super.createToken(authenticationInfo.getPrincipals());
        } catch (AuthenticationException e) {
            log.debug("Bad credentials provided for protop token creation", e);
            return null;
        }
    }

    /**
     * Removes any protop API Key token for current user, if exists, and returns {@code true}.
     */
    public boolean logout() {
        return super.deleteToken();
    }
}
