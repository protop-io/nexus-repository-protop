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
package org.sonatype.nexus.repository.protop.internal.search.legacy;

import java.io.IOException;

import javax.annotation.Nullable;

import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.view.Content;

import org.joda.time.DateTime;

/**
 * protop search index facet.
 *
 * @since 3.0
 * @deprecated No longer actively used by protop upstream, replaced by v1 search api (NEXUS-13150).
 */
@Deprecated
@Facet.Exposed
public interface ProtopSearchIndexFacet
    extends Facet
{
  /**
   * Fetches the index document.
   */
  Content searchIndex(@Nullable final DateTime since) throws IOException;

  /**
   * Invalidates cached index document, if applicable.
   */
  void invalidateCachedSearchIndex();
}