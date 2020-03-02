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

import java.util.List;
import java.util.function.Supplier;

import org.sonatype.nexus.repository.Repository;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;

import static java.util.Arrays.asList;
import static org.sonatype.nexus.repository.protop.internal.ProtopMetadataUtils.META_ID;
import static org.sonatype.nexus.repository.protop.internal.ProtopMetadataUtils.META_REV;
import static org.sonatype.nexus.repository.protop.internal.ProtopMetadataUtils.rewriteTarballUrl;

/**
 * Simple factory class for providing handlers that are common for manipulating protop JSON Fields.
 *
 * @since 3.16
 */
public class ProtopFieldFactory
{
  public static final ProtopFieldDeserializer NULL_DESERIALIZER = new ProtopFieldDeserializer()
  {
    @Override
    public Object deserialize(final String fieldName,
                              final Object defaultValue,
                              final JsonParser parser,
                              final DeserializationContext context,
                              final JsonGenerator generator)
    {
      return null;
    }
  };

  public static final ProtopFieldMatcher REMOVE_ID_MATCHER = removeFieldMatcher(META_ID, "/" + META_ID);

  public static final ProtopFieldMatcher REMOVE_REV_MATCHER = removeFieldMatcher(META_REV, "/" + META_REV);

  public static final List<ProtopFieldMatcher> REMOVE_DEFAULT_FIELDS_MATCHERS = asList(REMOVE_ID_MATCHER,
      REMOVE_REV_MATCHER);

  private ProtopFieldFactory() {
    // factory constructor
  }

  private static ProtopFieldMatcher removeFieldMatcher(final String fieldName, final String pathRegex) {
    return new ProtopFieldMatcher(fieldName, pathRegex, NULL_DESERIALIZER);
  }

  public static ProtopFieldUnmatcher missingFieldMatcher(final String fieldName,
                                                      final String pathRegex,
                                                      final Supplier<Object> supplier)
  {
    return new ProtopFieldUnmatcher(fieldName, pathRegex, missingFieldDeserializer(supplier));
  }

  public static ProtopFieldDeserializer missingFieldDeserializer(final Supplier<Object> supplier) {
    ProtopFieldDeserializer deserializer = new ProtopFieldDeserializer()
    {
      @Override
      public Object deserializeValue(final Object defaultValue) {
        return supplier.get();
      }
    };
    return deserializer;
  }

  public static ProtopFieldUnmatcher missingRevFieldMatcher(final Supplier<Object> supplier) {
    return missingFieldMatcher(META_REV, "/" + META_REV, supplier);
  }

  public static ProtopFieldMatcher rewriteTarballUrlMatcher(final Repository repository, final String packageId) {
    return rewriteTarballUrlMatcher(repository.getName(), packageId);
  }

  public static ProtopFieldMatcher rewriteTarballUrlMatcher(final String repositoryName, final String packageId) {
    return new ProtopFieldMatcher("tarball", "/versions/(.*)/dist/tarball",
        rewriteTarballUrlDeserializer(repositoryName, packageId));
  }

  public static ProtopFieldDeserializer rewriteTarballUrlDeserializer(final String repositoryName,
                                                                   final String packageId)
  {
    return new ProtopFieldDeserializer()
    {
      @Override
      public Object deserializeValue(final Object defaultValue) {
        return rewriteTarballUrl(repositoryName, packageId, super.deserializeValue(defaultValue).toString());
      }
    };
  }
}
