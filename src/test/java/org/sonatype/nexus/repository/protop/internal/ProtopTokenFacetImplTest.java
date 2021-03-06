
package org.sonatype.nexus.repository.protop.internal;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.protop.internal.security.ProtopTokenManager;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.TempBlob;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.Request;
import org.sonatype.nexus.repository.view.Response;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for {@link ProtopTokenFacetImpl}
 */
public class ProtopTokenFacetImplTest
    extends TestSupport
{

  @Mock
  private ProtopTokenManager protopTokenManager;

  @Mock
  private Repository repository;

  @Mock
  private StorageFacet storageFacet;

  private ProtopTokenFacet underTest;

  @Before
  public void setUp() throws Exception {
    when(repository.facet(StorageFacet.class)).thenReturn(storageFacet);
    underTest = new ProtopTokenFacetImpl(protopTokenManager);
    underTest.attach(repository);
  }

  @Test
  public void failedLoginTest() throws Exception {
    // mocks
    final Context contextMocked = mock(Context.class);
    final Request requestMocked = mock(Request.class);
    final Payload payloadMocked = mock(Payload.class);
    final TempBlob tempBlob = mock(TempBlob.class);

    // data
    final String someJson = "{\"username\":\"username\",\"password\":\"password\"}";

    // behaviors
    when(contextMocked.getRequest()).thenReturn(requestMocked);
    when(requestMocked.getPayload()).thenReturn(payloadMocked);
    when(storageFacet.createTempBlob(eq(payloadMocked), any())).thenReturn(tempBlob);
    when(tempBlob.get()).thenReturn(new ByteArrayInputStream(someJson.getBytes(StandardCharsets.UTF_8)));
    when(protopTokenManager.login("username", "password")).thenReturn(null);

    // test
    Response response = underTest.login(contextMocked);

    // checks
    assertThat("login call should have returned a value", response, notNullValue() );
    verify(protopTokenManager).login("username", "password");
    assertThat("expecting a 401 for the status", response.getStatus().getCode(), is(401));

  }

}
