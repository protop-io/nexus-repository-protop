
package org.sonatype.nexus.repository.protop.internal;

import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProtopPackageParserTest
    extends TestSupport
{
  private ProtopPackageParser underTest = new ProtopPackageParser();

  @Test
  public void testParsePackageJson() {
    Map<String, Object> results = underTest.parseProtopJson(() -> getClass().getResourceAsStream("protop.tar.gz"));
    assertThat(results, is(not(nullValue())));
    assertThat(results.isEmpty(), is(false));
    assertThat(results.size(), is(10));
  }

  @Test
  public void testParseMissingPackageJson() {
    Map<String, Object> results = underTest.parseProtopJson(() -> getClass().getResourceAsStream("package-without-json.tar.gz"));
    assertThat(results, is(not(nullValue())));
    assertThat(results.isEmpty(), is(true));
    assertThat(results.size(), is(0));
  }

  @Test
  public void testRecoveryFromExceptionalSituation() {
    Map<String, Object> results = underTest.parseProtopJson(() -> {
      throw new RuntimeException("test");
    });
    assertThat(results, is(not(nullValue())));
    assertThat(results.isEmpty(), is(true));
    assertThat(results.size(), is(0));
  }

  @Test
  public void testPackageJsonPathDeterminations() {
    // protop.json should always be under the main path in the tarball, whatever that is for a particular archive
    assertThat(underTest.isProtopJson(makeEntry("package/protop.json", false)), is(true));
    assertThat(underTest.isProtopJson(makeEntry("foo/protop.json", false)), is(true));

    // other paths we should just skip over or ignore, just in case someone put a "protop.json" somewhere else
    assertThat(underTest.isProtopJson(makeEntry("protop.json", false)), is(false));
    assertThat(underTest.isProtopJson(makeEntry("/protop.json", false)), is(false));
    assertThat(underTest.isProtopJson(makeEntry("/foo/bar/protop.json", false)), is(false));
    assertThat(underTest.isProtopJson(makeEntry("/foo/bar/baz/protop.json", false)), is(false));
    assertThat(underTest.isProtopJson(makeEntry("package/protop.json", true)), is(false));
  }

  private ArchiveEntry makeEntry(final String name, final boolean directory) {
    ArchiveEntry entry = mock(ArchiveEntry.class);
    when(entry.getName()).thenReturn(name);
    when(entry.isDirectory()).thenReturn(directory);
    return entry;
  }
}
