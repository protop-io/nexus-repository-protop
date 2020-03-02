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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.base.Strings;
import org.apache.commons.lang3.StringUtils;
import org.sonatype.nexus.common.app.VersionComparator;

import com.google.common.escape.Escaper;
import com.google.common.net.UrlEscapers;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * protop package identifier. Rules applied:
 * <ul>
 * <li>must be shorter that 214 characters (gross length, w/ org)</li>
 * <li>can't start with '.' (dot) or '_' (underscore)</li>
 * <li>only URL-safe characters can be used</li>
 * </ul>
 *
 * Allowed formats:
 * <ul>
 * <li>"org_name:artifact_name"</li>
 * </ul>
 *
 * @see <a href="https://docs.protopjs.com/files/protop.json#name">Protop JSON 'name'</a>
 * @since 3.0
 */
public final class ProtopProjectId implements Comparable<ProtopProjectId> {

  private static final VersionComparator comparator = ProtopVersionComparator.versionComparator;

  private static final Escaper escaper = UrlEscapers.urlPathSegmentEscaper();

  private final String org;

  private final String name;

  private final String id;

  public ProtopProjectId(final String org, final String name) {

    checkArgument(org.length() > 0, "Org cannot be empty string");
    checkArgument(org.equals(escaper.escape(org)), "Non URL-safe org: %s", org);
    checkArgument(!org.startsWith(".") && !org.startsWith("_"), "Org starts with '.' or '_': %s", org);

    checkArgument(name.length() > 0, "Name cannot be empty string");
    checkArgument(name.equals(escaper.escape(name)), "Non URL-safe name: %s", name);
    checkArgument(!name.startsWith(".") && !name.startsWith("_"), "Name starts with '.' or '_': %s", name);

    this.id = org + "/" + name;
    checkArgument(this.id.length() < 214, "Must be shorter than 214 characters: %s", id);

    this.org = org;
    this.name = name;
  }

  /**
   * Returns the org name, never {@code null}.
   */
  @Nonnull
  public String org() {
    return org;
  }

  /**
   * Returns the project name, never {@code null}.
   */
  @Nonnull
  public String name() {
    return name;
  }

  /**
   * Returns the raw name of project (org:name).
   */
  @Nonnull
  public String id() {
    return id;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }

    if (!(o instanceof ProtopProjectId)) {
      return false;
    }

    ProtopProjectId that = (ProtopProjectId) o;
    return id.equals(that.id);
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }

  @Override
  public int compareTo(final ProtopProjectId o) {
    return comparator.compare(id, o.id);
  }

  @Override
  public String toString() {
    return id();
  }

  /**
   * Parses string like <code>org/name</code> into a {@link ProtopProjectId}.
   */
  public static ProtopProjectId parse(final String string) {
    checkNotNull(string);
    checkArgument(StringUtils.countMatches(string, "/") == 1, "Path should contain one \"/\".");

    int slashIndex = string.indexOf('/');

    String org = string.substring(0, slashIndex);
    String name = string.substring(slashIndex + 1);

    return new ProtopProjectId(org, name);
  }
}
