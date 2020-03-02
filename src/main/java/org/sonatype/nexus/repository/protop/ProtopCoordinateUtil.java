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
package org.sonatype.nexus.repository.protop;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.repository.protop.internal.ProtopProjectId;

/**
 * Since 3.16
 */
public class ProtopCoordinateUtil {

  private static final Logger logger = LoggerFactory.getLogger(ProtopCoordinateUtil.class);

  private static final Pattern PROTOP_VERSION_PATTERN = Pattern
      .compile("-(\\d+\\.\\d+\\.\\d+[A-Za-z\\d\\-.+]*)\\.(?:tar\\.gz|tgz)");


  public static String extractVersion(final String protopPath) {
    Matcher matcher = PROTOP_VERSION_PATTERN.matcher(protopPath);

    return matcher.find() ? matcher.group(1) : "";
  }

  public static String getPackageIdOrg(final String protopPath) {
    logger.info("Getting org for {}.", protopPath);
    return ProtopProjectId.parse(protopPath).org();
  }

  public static String getPackageIdName(final String protopPath) {
    logger.info("Getting name for {}.", protopPath);
    return ProtopProjectId.parse(protopPath).name();
  }

  private ProtopCoordinateUtil() {
    // no op
  }
}
