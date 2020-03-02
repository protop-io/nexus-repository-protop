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

import org.sonatype.nexus.repository.storage.MissingAssetBlobException;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.payloads.StreamPayload.InputStreamFunction;

import static java.util.Collections.singletonList;

/**
 * protop focused {@link Content} allowing for setting {@link ProtopStreamPayload} fields after creation.
 *
 * @since 3.16
 */
public class ProtopContent
    extends Content
{
  private ProtopStreamPayload payload;

  public ProtopContent(final ProtopStreamPayload payload) {
    super(payload);
    this.payload = payload;
  }

  public ProtopContent packageId(final String packageId) {
    payload.packageId(packageId);
    return this;
  }

  public ProtopContent revId(final String revId) {
    payload.revId(revId);
    return this;
  }

  public ProtopContent fieldMatchers(final ProtopFieldMatcher fieldMatcher) {
    return fieldMatchers(singletonList(fieldMatcher));
  }

  public ProtopContent fieldMatchers(final List<ProtopFieldMatcher> fieldMatchers) {
    payload.fieldMatchers(fieldMatchers);
    return this;
  }

  public ProtopContent missingBlobInputStreamSupplier(
      final InputStreamFunction<MissingAssetBlobException> missingBlobInputStreamSupplier)
  {
    payload.missingBlobInputStreamSupplier(missingBlobInputStreamSupplier);
    return this;
  }
}
