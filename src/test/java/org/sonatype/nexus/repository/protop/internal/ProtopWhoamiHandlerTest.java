
package org.sonatype.nexus.repository.protop.internal;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.security.user.User;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

public class ProtopWhoamiHandlerTest
    extends TestSupport
{
  @Mock
  private SecuritySystem securitySystem;

  @Mock
  private Context context;

  private ProtopWhoamiHandler underTest;

  @Before
  public void setup() {
    underTest = new ProtopWhoamiHandler(securitySystem, new ObjectMapper());
  }

  @Test
  public void testHandle() throws Exception {
    currentUser("testuser");
    Response response = underTest.handle(context);

    assertThat(response.getStatus().getCode(), is(200));
    assertThat(IOUtils.toString(response.getPayload().openInputStream()), is("{\"username\":\"testuser\"}"));
  }

  @Test
  public void testHandle_noUser() throws Exception {
    currentUser(null);
    Response response = underTest.handle(context);

    assertThat(response.getStatus().getCode(), is(200));
    assertThat(IOUtils.toString(response.getPayload().openInputStream()), is("{\"username\":\"anonymous\"}"));
  }

  private void currentUser(String userId) throws Exception {
    if (userId != null) {
      User user = new User();
      user.setUserId(userId);
      when(securitySystem.currentUser()).thenReturn(user);
    }
    else {
      when(securitySystem.currentUser()).thenReturn(null);
    }
  }
}
