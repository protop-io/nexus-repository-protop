/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
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
    Map<String, Object> results = underTest.parseProtopJson(() -> getClass().getResourceAsStream("protop.tgz"));
    assertThat(results, is(not(nullValue())));
    assertThat(results.isEmpty(), is(false));
    assertThat(results.size(), is(10));
  }

  @Test
  public void testParseMissingPackageJson() {
    Map<String, Object> results = underTest.parseProtopJson(() -> getClass().getResourceAsStream("package-without-json.tgz"));
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
