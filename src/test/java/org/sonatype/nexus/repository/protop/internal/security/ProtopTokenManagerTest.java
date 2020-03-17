
package org.sonatype.nexus.repository.protop.internal.security;

import org.sonatype.nexus.repository.protop.security.ProtopToken;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.security.SecurityHelper;
import org.sonatype.nexus.security.authc.apikey.ApiKeyStore;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ProtopTokenManagerTest
    extends TestSupport
{
  private static final String TOKEN = "token";

  @Mock
  private SecurityHelper securityHelper;

  @Mock
  private ApiKeyStore apiKeyStore;

  @Mock
  private SecurityManager securityManager;

  @Mock
  private AuthenticationInfo authenticationInfo;

  @Mock
  private PrincipalCollection principalCollection;

  @Mock
  private Subject subject;

  ProtopTokenManager underTest;

  @Before
  public void setup() throws Exception {
    when(securityHelper.getSecurityManager()).thenReturn(securityManager);
    when(securityManager.authenticate(any())).thenReturn(authenticationInfo);
    when(authenticationInfo.getPrincipals()).thenReturn(principalCollection);
    when(securityHelper.subject()).thenReturn(subject);
    when(subject.getPrincipals()).thenReturn(principalCollection);
    underTest = new ProtopTokenManager(apiKeyStore, securityHelper);
  }

  @Test(expected = NullPointerException.class)
  public void loginFailFastWhenUsernameIsNull() throws Exception {
    underTest.login(null, "password");
  }

  @Test(expected = NullPointerException.class)
  public void loginFailFastWhenPasswordIsNull() throws Exception {
    underTest.login("username", null);
  }

  @Test
  public void createTokenOnLogin() throws Exception {
    when(apiKeyStore.getApiKey(any(), any())).thenReturn(TOKEN.toCharArray());
    assertThat(underTest.login("username", "password"), is(equalTo(ProtopToken.NAME + "." + TOKEN)));
  }

  @Test
  public void deleteKeyOnLogout() throws Exception {
    when(apiKeyStore.getApiKey(any(), any())).thenReturn(TOKEN.toCharArray());
    assertTrue(underTest.logout());
    verify(apiKeyStore).deleteApiKey(ProtopToken.NAME, principalCollection);
  }

  @Test
  public void nullOnAuthException() throws Exception {
    when(apiKeyStore.getApiKey(any(), any())).thenThrow(new AuthenticationException());
    assertThat(underTest.login("username", "password"), is(nullValue()));
  }
}
