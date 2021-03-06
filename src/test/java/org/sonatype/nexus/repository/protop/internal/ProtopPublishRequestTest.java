
package org.sonatype.nexus.repository.protop.internal;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.storage.TempBlob;

import com.google.common.collect.ImmutableMap;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;

public class ProtopPublishRequestTest
    extends TestSupport
{
  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Mock
  NestedAttributesMap packageRoot;

  @Mock
  TempBlob tempBlobA;

  @Mock
  TempBlob tempBlobB;

  @Test
  public void manageAndDeleteTempBlobsCorrectly() {
    try (ProtopPublishRequest request = newProtopPublishRequest()) {
      assertThat(request.getPackageRoot(), is(packageRoot));
      assertThat(request.requireBlob("a"), is(tempBlobA));
      assertThat(request.requireBlob("b"), is(tempBlobB));
    }
    verify(tempBlobA).close();
    verify(tempBlobB).close();
  }

  @Test
  public void throwExceptionOnMissingTempBlob() {
    exception.expectMessage("blob-z");
    exception.expect(IllegalStateException.class);
    try (ProtopPublishRequest request = newProtopPublishRequest()) {
      request.requireBlob("blob-z");
    }
  }

  private ProtopPublishRequest newProtopPublishRequest() {
    return new ProtopPublishRequest(packageRoot, ImmutableMap.of("a", tempBlobA, "b", tempBlobB));
  }
}
