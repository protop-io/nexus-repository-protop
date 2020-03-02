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
package org.sonatype.nexus.repository.protop.internal.search.v1;

import java.util.List;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Response for an protop V1 search request.
 *
 * @since 3.7
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProtopSearchResponse
{
  @Nullable
  private List<ProtopSearchResponseObject> objects;

  @Nullable
  private Integer total;

  @Nullable
  private String time;

  @Nullable
  public List<ProtopSearchResponseObject> getObjects() {
    return objects;
  }

  public void setObjects(@Nullable final List<ProtopSearchResponseObject> objects) {
    this.objects = objects;
  }

  @Nullable
  public Integer getTotal() {
    return total;
  }

  public void setTotal(@Nullable final Integer total) {
    this.total = total;
  }

  @Nullable
  public String getTime() {
    return time;
  }

  public void setTime(@Nullable final String time) {
    this.time = time;
  }
}
