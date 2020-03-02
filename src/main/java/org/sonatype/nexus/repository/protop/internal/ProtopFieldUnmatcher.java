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

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;

/**
 * A protop Json Field Matcher to test whether we never matched a field.
 *
 * @since 3.17
 */
public class ProtopFieldUnmatcher
    extends ProtopFieldMatcher
{
  private int matched = 0;

  public ProtopFieldUnmatcher(final String fieldName,
                           final String pathRegex,
                           final ProtopFieldDeserializer deserializer)
  {
    super(fieldName, pathRegex, deserializer);
  }

  @Override
  public boolean allowDeserializationOnMatched() {
    return false;
  }

  /**
   * Test for matches using the super class and marks if it was matched.
   *
   * @see ProtopFieldMatcher#matches(JsonParser)
   */
  @Override
  public boolean matches(final JsonParser parser) throws IOException {
    boolean matches = super.matches(parser);

    if (matched == 0 && matches) {
      matched = 1;
    }

    return matches;
  }

  /**
   * Test whether at the current parsed state of a {@link ProtopFieldMatcher} it was matched
   * by <code>fieldName</code> and <code>pathRegex</code>.
   */
  public boolean wasNeverMatched() {
    return matched == 0;
  }
}
