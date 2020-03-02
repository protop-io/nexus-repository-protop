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

import java.util.HashMap;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.app.BaseUrlHolder;
import org.sonatype.nexus.common.collect.NestedAttributesMap;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import org.hamcrest.Matchers;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertNull;

public class ProtopMetadataUtilsTest
    extends TestSupport
{
  @Before
  public void setUp() {
    BaseUrlHolder.unset();
  }

  @Test
  public void extractTarballName() {
    assertThat(ProtopMetadataUtils.extractTarballName("somename.tgz"), equalTo("somename.tgz"));
    assertThat(ProtopMetadataUtils.extractTarballName("package/-/tarball.tgz"), equalTo("tarball.tgz"));
    assertThat(ProtopMetadataUtils.extractTarballName("@org/package/-/tarball.tgz"), equalTo("tarball.tgz"));
    assertThat(ProtopMetadataUtils.extractTarballName(
        "http://localhost:8081/repository/protophosted/simple-protop-package/-/simple-protop-package-2.0.1.tgz"),
        equalTo("simple-protop-package-2.0.1.tgz"));
    assertThat(ProtopMetadataUtils.extractTarballName(
        "http://localhost:8081/repository/protophosted/@scope/simple-protop-package/-/simple-protop-package-2.0.1.tgz"),
        equalTo("simple-protop-package-2.0.1.tgz"));
  }

  @Test
  public void extractTarballName_packageMetadata() {
    NestedAttributesMap packageMetadata = new NestedAttributesMap("metadata", Maps.newHashMap());

    assertNull(ProtopMetadataUtils.extractTarballName(packageMetadata));

    packageMetadata.child("dist-tags").set("latest", "1.0");

    assertNull(ProtopMetadataUtils.extractTarballName(packageMetadata));

    packageMetadata.child("versions").child("1.0").child("dist").set("tarball",
        "http://localhost:8081/repository/protophosted/simple-protop-package/-/simple-protop-package-2.0.1.tgz");

    assertThat(ProtopMetadataUtils.extractTarballName(packageMetadata), equalTo("simple-protop-package-2.0.1.tgz"));
  }

  @Test
  public void overlayTest() {
    NestedAttributesMap recessive = new NestedAttributesMap("recessive", Maps.newHashMap());
    recessive.set("_id", "id");
    recessive.set("name", "recessive");
    recessive.child("versions").child("1.0").set("version", "1.0");
    recessive.child("versions").child("1.0").set("foo", "bar");
    recessive.child("versions").child("1.0").set("foo1", "bar1");
    recessive.child("versions").child("1.0").child("author").set("url", "http://www.example.com");
    recessive.child("versions").child("1.0").child("dependencies").set("a", "1.0");
    recessive.child("versions").child("1.0").child("devDependencies").set("m", "2.0");
    recessive.child("versions").child("1.0").child("publishConfig").set("a", "m");
    recessive.child("versions").child("1.0").child("scripts").set("cmd1", "command1");

    NestedAttributesMap dominant = new NestedAttributesMap("dominant", Maps.newHashMap());
    dominant.set("_id", "id");
    dominant.set("name", "dominant");
    dominant.child("versions").child("1.0").set("version", "1.0");
    dominant.child("versions").child("1.0").set("foo", "baz");
    dominant.child("versions").child("1.0").child("author").set("email", "none@example.com");
    dominant.child("versions").child("1.0").child("dependencies").set("b", "2.0");
    dominant.child("versions").child("1.0").child("devDependencies").set("n", "3.0");
    dominant.child("versions").child("1.0").child("publishConfig").set("b", "n");
    dominant.child("versions").child("1.0").child("scripts").set("cmd2", "command2");

    NestedAttributesMap result = ProtopMetadataUtils.overlay(recessive, dominant);

    assertThat(result, equalTo(recessive));
    assertThat(result.backing(), hasEntry("_id", "id"));
    assertThat(result.backing(), hasEntry("name", "dominant"));
    assertThat(result.child("versions").child("1.0").backing(), hasEntry("foo", "baz"));
    assertThat(result.child("versions").child("1.0").backing(), hasEntry("foo1", "bar1"));
    assertThat(result.child("versions").child("1.0").child("author").backing(),
        not(hasEntry("url", "http://www.example.com")));
    assertThat(result.child("versions").child("1.0").child("author").backing(), hasEntry("email", "none@example.com"));
    assertThat(result.child("versions").child("1.0").child("dependencies").backing(), not(hasEntry("a", "1.0")));
    assertThat(result.child("versions").child("1.0").child("dependencies").backing(), hasEntry("b", "2.0"));
    assertThat(result.child("versions").child("1.0").child("devDependencies").backing(), not(hasEntry("m", "2.0")));
    assertThat(result.child("versions").child("1.0").child("devDependencies").backing(), hasEntry("n", "3.0"));
    assertThat(result.child("versions").child("1.0").child("publishConfig").backing(), not(hasEntry("a", "m")));
    assertThat(result.child("versions").child("1.0").child("publishConfig").backing(), hasEntry("b", "n"));
    assertThat(result.child("versions").child("1.0").child("scripts").backing(), not(hasEntry("cmd1", "command1")));
    assertThat(result.child("versions").child("1.0").child("scripts").backing(), hasEntry("cmd2", "command2"));
  }

  @Test
  public void mergeTest() {
    NestedAttributesMap recessive = new NestedAttributesMap("recessive", Maps.newHashMap());
    recessive.set("_id", "id");
    recessive.set("name", "recessive");
    recessive.child("versions").child("1.0").set("version", "1.0");
    recessive.child("versions").child("1.0").set("foo", "bar");
    recessive.child("versions").child("1.0").set("foo1", "bar1");
    recessive.child("versions").child("2.0").set("version", "2.0");
    recessive.child("versions").child("2.0").set("foo", "bar");
    NestedAttributesMap dominant = new NestedAttributesMap("dominant", Maps.newHashMap());
    dominant.set("_id", "id");
    dominant.set("name", "dominant");
    dominant.child("versions").child("1.0").set("version", "1.0");
    dominant.child("versions").child("1.0").set("foo", "baz");
    dominant.child("versions").child("1.0").set("foo2", "bar2");
    dominant.child("versions").child("1.1").set("version", "1.1");
    dominant.child("versions").child("1.1").set("foo", "bar");

    NestedAttributesMap result = ProtopMetadataUtils.merge("theKey", ImmutableList.of(recessive, dominant));

    assertThat(result, not(equalTo(recessive)));
    assertThat(result.getKey(), Matchers.equalTo("theKey"));
    assertThat(result.backing(), not(hasEntry("_id", "id")));
    assertThat(result.backing(), hasEntry("name", "dominant"));
    assertThat(result.child("versions").child("1.0").backing(), hasEntry("foo", "baz"));
    assertThat(result.child("versions").child("1.0").backing(), not(hasEntry("foo1", "bar1")));
    assertThat(result.child("versions").child("1.0").backing(), hasEntry("foo2", "bar2"));
    assertThat(result.child("versions").child("1.1").backing(), hasEntry("foo", "bar"));
    assertThat(result.child("versions").child("2.0").backing(), hasEntry("foo", "bar"));
  }

  @Test
  public void shrinkSimple() {
    NestedAttributesMap packageRoot = new NestedAttributesMap("metadata", new HashMap<String, Object>());
    packageRoot.child("dist-tags").set("alpha", "1.0.0");
    packageRoot.child("dist-tags").set("gamma", "1.0.2");
    packageRoot.child("dist-tags").set("latest", "1.0.3");

    NestedAttributesMap versions = packageRoot.child("versions");
    versions.child("1.0.0").set("version", "1.0.0");
    versions.child("1.0.1").set("version", "1.0.1");
    versions.child("1.0.2").set("version", "1.0.2");
    versions.child("1.0.3").set("version", "1.0.3");

    ProtopMetadataUtils.shrink(packageRoot);

    // dist tags untouched
    assertThat(packageRoot.child("dist-tags").backing().entrySet(), hasSize(3));
    // versions shrinked
    assertThat(versions.get("1.0.0").toString(), equalTo("alpha"));
    assertThat(versions.get("1.0.1").toString(), equalTo("1.0.1"));
    assertThat(versions.get("1.0.2").toString(), equalTo("gamma"));
    assertThat(versions.get("1.0.3").toString(), equalTo("latest"));
  }

  @Test
  public void selectVersionByTarballNameTest() {
    NestedAttributesMap packageRoot = new NestedAttributesMap("package", Maps.newHashMap());
    packageRoot.set("_id", "id");
    packageRoot.set("name", "package");
    packageRoot.child("versions").child("1.0").set("name", "package");
    packageRoot.child("versions").child("1.0").set("version", "1.0");
    packageRoot.child("versions").child("1.0").child("dist").set("tarball", "http://example.com/path/package-1.0.tgz");
    packageRoot.child("versions").child("2.0").set("name", "package");
    packageRoot.child("versions").child("2.0").set("version", "2.0");
    packageRoot.child("versions").child("2.0").child("dist").set("tarball", "http://example.com/path/package-2.0.tgz");

    NestedAttributesMap v1 = ProtopMetadataUtils.selectVersionByTarballName(packageRoot, "package-1.0.tgz");
    NestedAttributesMap v2 = ProtopMetadataUtils.selectVersionByTarballName(packageRoot, "package-2.0.tgz");
    NestedAttributesMap v3 = ProtopMetadataUtils.selectVersionByTarballName(packageRoot, "package-3.0.tgz");

    assertThat(v1.child("dist").get("tarball"), equalTo("http://example.com/path/package-1.0.tgz"));
    assertThat(v2.child("dist").get("tarball"), equalTo("http://example.com/path/package-2.0.tgz"));
    assertThat(v3, nullValue());
  }

  @Test
  public void maintainTimeTest() {
    NestedAttributesMap package1 = new NestedAttributesMap("package1", Maps.newHashMap());
    package1.child("versions").set("1.0", "incomplete");

    String createdTimestamp = "2016-10-17T00:00:00.000Z";
    NestedAttributesMap package2 = new NestedAttributesMap("package2", Maps.newHashMap());
    package2.child("time").set("created", createdTimestamp);
    package2.child("time").set("1.0", createdTimestamp);
    package2.child("versions").set("1.0", "incomplete");
    package2.child("versions").set("2.0", "incomplete");

    DateTime package1Modified = ProtopMetadataUtils.maintainTime(package1);
    DateTime package2Modified = ProtopMetadataUtils.maintainTime(package2);

    String package1ModifiedTimestamp = ProtopMetadataUtils.PROTOP_TIMESTAMP_FORMAT.print(package1Modified);
    assertThat(package1.child("time").get("created"), equalTo(package1ModifiedTimestamp));
    assertThat(package1.child("time").get("modified"), equalTo(package1ModifiedTimestamp));
    assertThat(package1.child("time").get("1.0"), equalTo(package1ModifiedTimestamp));

    String package2ModifiedTimestamp = ProtopMetadataUtils.PROTOP_TIMESTAMP_FORMAT.print(package2Modified);
    assertThat(package2.child("time").get("created"), equalTo(createdTimestamp));
    assertThat(package2.child("time").get("modified"), equalTo(package2ModifiedTimestamp));
    assertThat(package2.child("time").get("1.0"), equalTo(createdTimestamp));
    assertThat(package2.child("time").get("2.0"), equalTo(package2ModifiedTimestamp));
  }

  @Test
  public void lastModifiedTest() {
    NestedAttributesMap package1 = new NestedAttributesMap("package1", Maps.newHashMap());

    String modifiedTimestamp = "2016-10-17T00:00:00.000Z";
    NestedAttributesMap package2 = new NestedAttributesMap("package2", Maps.newHashMap());
    package2.child("time").set("modified", modifiedTimestamp);

    DateTime package1Modified = ProtopMetadataUtils.lastModified(package1);
    DateTime package2Modified = ProtopMetadataUtils.lastModified(package2);

    DateTime expectedPackage2Modified = ProtopMetadataUtils.PROTOP_TIMESTAMP_FORMAT.parseDateTime(modifiedTimestamp);
    assertThat(package1Modified, nullValue());
    assertThat(package2Modified, equalTo(expectedPackage2Modified));
  }

  @Test
  public void rewriteTarballUrlTest() {
    try {
      assertThat(BaseUrlHolder.isSet(), is(false));
      BaseUrlHolder.set("http://localhost:8080/");

      NestedAttributesMap packageRoot = new NestedAttributesMap("package", Maps.newHashMap());
      packageRoot.set("_id", "id");
      packageRoot.set("name", "package");
      packageRoot.set("org", "org");
      packageRoot.child("versions").child("1.0").set("name", "package");
      packageRoot.child("versions").child("1.0").set("org", "org");
      packageRoot.child("versions").child("1.0").set("version", "1.0");
      packageRoot.child("versions").child("1.0").child("dist")
          .set("tarball", "http://example.com/path/org-package-1.0.tgz");
      packageRoot.child("versions").set("2.0", "incomplete");

      ProtopMetadataUtils.rewriteTarballUrl("testRepo", packageRoot);

      String rewritten = packageRoot.child("versions").child("1.0").child("dist").get("tarball", String.class);
      assertThat(rewritten, equalTo("http://localhost:8080/repository/testRepo/org/package/-/org-package-1.0.tgz"));
    }
    finally {
      BaseUrlHolder.unset();
    }
  }

  @Test
  public void createRepositoryPath() {
    assertThat(ProtopMetadataUtils.createRepositoryPath("org_a", "pkg", "1.2.3"), is("org_a/pkg/-/org_a-pkg-1.2.3.tgz"));
    assertThat(ProtopMetadataUtils.createRepositoryPath("foo", "pkg", "1.2.3"), is("foo/pkg/-/foo-pkg-1.2.3.tgz"));
  }
}
