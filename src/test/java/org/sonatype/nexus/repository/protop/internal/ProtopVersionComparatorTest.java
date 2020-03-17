
package org.sonatype.nexus.repository.protop.internal;

import org.sonatype.goodies.testsupport.TestSupport;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.sonatype.nexus.repository.protop.internal.ProtopVersionComparator.extractAlwaysPackageVersion;
import static org.sonatype.nexus.repository.protop.internal.ProtopVersionComparator.extractNewestVersion;
import static org.sonatype.nexus.repository.protop.internal.ProtopVersionComparator.extractPackageRootVersionUnlessEmpty;

public class ProtopVersionComparatorTest
    extends TestSupport
{
  @Test
  public void shouldReturnPackage() throws Exception {
    assertThat(extractAlwaysPackageVersion.apply("1.0.0", "2.0.0"), is("2.0.0"));
  }

  @Test
  public void shouldReturnPackageRootWhenNotEmpty() throws Exception {
    assertThat(extractPackageRootVersionUnlessEmpty.apply("1.0.0", "2.0.0"), is("1.0.0"));
  }

  @Test
  public void shouldReturnPackageWhenPackageRootIsEmpty() throws Exception {
    assertThat(extractPackageRootVersionUnlessEmpty.apply("", "2.0.0"), is("2.0.0"));
  }

  @Test
  public void shouldReturnPackageWhenPackageRootIsNull() throws Exception {
    assertThat(extractPackageRootVersionUnlessEmpty.apply(null, "2.0.0"), is("2.0.0"));
  }

  @Test
  public void shouldReturnPackageWhenLatest() throws Exception {
    assertThat(extractNewestVersion.apply("1.0.0", "2.0.0"), is("2.0.0"));
  }

  @Test
  public void shouldReturnPackageRootWhenLatest() throws Exception {
    assertThat(extractNewestVersion.apply("2.0.0", "1.0.0"), is("2.0.0"));
  }

  @Test
  public void shouldReturnReleasePackageWhenLatest() throws Exception {
    assertThat(extractNewestVersion.apply("1.0.0", "1.0.0-SNAPSHOT"), is("1.0.0"));
  }
}
