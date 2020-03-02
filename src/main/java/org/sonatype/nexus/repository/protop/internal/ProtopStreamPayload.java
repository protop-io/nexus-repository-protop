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

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Objects;

import org.sonatype.nexus.repository.storage.MissingAssetBlobException;
import org.sonatype.nexus.repository.view.payloads.StreamPayload;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.fasterxml.jackson.core.JsonGenerator.Feature.AUTO_CLOSE_TARGET;
import static com.fasterxml.jackson.databind.SerializationFeature.FLUSH_AFTER_WRITE_VALUE;
import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;
import static org.sonatype.nexus.repository.protop.internal.ProtopFacetUtils.errorInputStream;
import static org.sonatype.nexus.repository.view.ContentTypes.APPLICATION_JSON;

/**
 * protop Specific {@link StreamPayload} that implements its own {@link #copy(InputStream, OutputStream)} method
 * to allow for streaming out the {@link #openInputStream()} directly to a given {@link OutputStream}.
 *
 * @since 3.16
 */
public class ProtopStreamPayload
    extends StreamPayload
{
  private static final Logger log = LoggerFactory.getLogger(ProtopStreamPayload.class);

  private String revId;

  private String packageId;

  private List<ProtopFieldMatcher> fieldMatchers;

  private InputStreamFunction<MissingAssetBlobException> missingBlobInputStreamSupplier;

  public ProtopStreamPayload(final InputStreamSupplier supplier)
  {
    super(supplier, UNKNOWN_SIZE, APPLICATION_JSON);
  }

  public ProtopStreamPayload packageId(final String packageId) {
    this.packageId = packageId;
    return this;
  }

  public ProtopStreamPayload revId(final String revId) {
    this.revId = revId;
    return this;
  }

  public ProtopStreamPayload fieldMatchers(final List<ProtopFieldMatcher> fieldMatchers) {
    this.fieldMatchers = fieldMatchers;
    return this;
  }

  public ProtopStreamPayload missingBlobInputStreamSupplier(
      final InputStreamFunction<MissingAssetBlobException> missingBlobInputStreamSupplier)
  {
    this.missingBlobInputStreamSupplier = missingBlobInputStreamSupplier;
    return this;
  }

  @Override
  public InputStream openInputStream() throws IOException {
    try {
      return super.openInputStream();
    }
    catch (MissingAssetBlobException e) { // NOSONAR
      if (nonNull(missingBlobInputStreamSupplier)) {
        return missingBlobInputStreamSupplier.apply(e);
      }

      // NEXUS-20040 - add logging detailing the missing handler and missing blob.
      log.warn("Missing blob for asset {} and no handler set to recover, for package '{}' and rev '{}'",
          e.getAsset(), Objects.toString(packageId, "unknown"), Objects.toString(revId, "unknown"),
          log.isDebugEnabled() ? e : null);

      return errorInputStream("Missing blob and no handler set to recover.");
    }
  }

  @Override
  public void copy(final InputStream input, final OutputStream output) throws IOException {
    new ProtopStreamingObjectMapper(packageId, revId, nonNull(fieldMatchers) ? fieldMatchers : emptyList())
        .configure(FLUSH_AFTER_WRITE_VALUE, false) // we don't want the object mapper to flush all the time
        .disable(AUTO_CLOSE_TARGET)
        .readAndWrite(input, new BufferedOutputStream(output));
  }
}
