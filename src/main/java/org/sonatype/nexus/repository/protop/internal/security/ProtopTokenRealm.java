package org.sonatype.nexus.repository.protop.internal.security;

import org.apache.shiro.realm.AuthenticatingRealm;
import org.apache.shiro.subject.Subject;
import org.eclipse.sisu.Description;
import org.sonatype.nexus.repository.protop.security.ProtopToken;
import org.sonatype.nexus.security.UserPrincipalsHelper;
import org.sonatype.nexus.security.authc.apikey.ApiKeyStore;
import org.sonatype.nexus.security.token.BearerTokenRealm;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * {@link AuthenticatingRealm} that maps protop tokens to valid {@link Subject}s.
 *
 * @since 3.0
 */
@Named(ProtopToken.NAME)
@Singleton
@Description("protop Bearer Token Realm")
public final class ProtopTokenRealm
        extends BearerTokenRealm {
    @Inject
    public ProtopTokenRealm(final ApiKeyStore keyStore, final UserPrincipalsHelper principalsHelper) {
        super(keyStore, principalsHelper, ProtopToken.NAME);
    }
}
