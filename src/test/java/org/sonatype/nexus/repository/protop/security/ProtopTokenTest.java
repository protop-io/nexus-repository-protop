
package org.sonatype.nexus.repository.protop.security;

import javax.servlet.http.HttpServletRequest;

import org.sonatype.goodies.testsupport.TestSupport;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static com.google.common.net.HttpHeaders.AUTHORIZATION;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.Mockito.when;

public class ProtopTokenTest
    extends TestSupport
{
  private static final String TOKEN = randomUUID().toString();

  @Mock
  private HttpServletRequest request;

  ProtopToken underTest;

  @Before
  public void setup() throws Exception {
    underTest = new ProtopToken();
  }

  @Test
  public void extractProtopToken() throws Exception {
    when(request.getHeader(AUTHORIZATION)).thenReturn("Bearer ProtopToken." + TOKEN);
    String token = underTest.extract(request);
    assertThat(token, is(equalTo(TOKEN)));
  }

  @Test
  public void shouldNotMatchOtherFormat() throws Exception {
    when(request.getHeader(AUTHORIZATION)).thenReturn("Bearer NotProtop." + TOKEN);
    String token = underTest.extract(request);
    assertThat(token, is(nullValue()));
  }

  @Test
  public void extractProtopTokenWhenFormatNotPresent() throws Exception {
    when(request.getHeader(AUTHORIZATION)).thenReturn("Bearer " + TOKEN);
    String token = underTest.extract(request);
    assertThat(token, is(equalTo(TOKEN)));
  }
}
