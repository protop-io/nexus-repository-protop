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

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.protop.internal.ProtopFacetUtils;
import org.sonatype.nexus.repository.protop.internal.ProtopFormat;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.entity.EntityBatchEvent;
import org.sonatype.nexus.common.entity.EntityEvent;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.common.event.EventAware.Asynchronous;
import org.sonatype.nexus.common.event.EventHelper;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetEvent;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Subscriber of batched component/asset events, which are used to maintain cached protop index documents.
 *
 * @since 3.0
 * @deprecated No longer actively used by protop upstream, replaced by v1 search api (NEXUS-13150).
 */
@Deprecated
@Named
@Singleton
public class ProtopSearchIndexSubscriber
    extends ComponentSupport
    implements EventAware, Asynchronous
{
  private final RepositoryManager repositoryManager;

  @Inject
  public ProtopSearchIndexSubscriber(final RepositoryManager repositoryManager) {
    this.repositoryManager = checkNotNull(repositoryManager);
  }

  /**
   * On package root {@link Asset} change event (any change, CREATE, UPDATE or DELETE), the owning repository's cached
   * index document should be invalidated.
   */
  @Subscribe
  @AllowConcurrentEvents
  public void on(final EntityBatchEvent batchEvent) {
    // skip when replicating, origin node will delete any search indexes
    if (!EventHelper.isReplicating()) {
      for (final EntityEvent event : batchEvent.getEvents()) {
        final Repository protopAssetRepository = filterProtopAssetRepository(event);
        if (protopAssetRepository != null) {
          invalidateCachedSearchIndex(protopAssetRepository);
        }
      }
    }
  }

  /**
   * Returns passed in event only if: a) it is {@link AssetEvent}, b) is about {@link Asset} without component
   * (package metadata) and c) is NOT having name of {@link ProtopFacetUtils#REPOSITORY_ROOT_ASSET} (is not index document
   * itself).
   */
  @Nullable
  private Repository filterProtopAssetRepository(final EntityEvent entityEvent) {
    if (entityEvent instanceof AssetEvent) {
      final AssetEvent assetEvent = (AssetEvent) entityEvent;
      if (assetEvent.getComponentId() == null
          && !ProtopFacetUtils.REPOSITORY_ROOT_ASSET.equals(assetEvent.getAsset().name())) {
        return findProtopRepository(assetEvent.getRepositoryName());
      }
    }
    return null;
  }

  /**
   * Returns the repository if it have format of {@link ProtopFormat} only, {@code null} otherwise.
   */
  @Nullable
  private Repository findProtopRepository(final String repositoryName) {
    final Repository repository = repositoryManager.get(repositoryName);
    if (repository != null && ProtopFormat.NAME.equals(repository.getFormat().getValue())) {
      return repository;
    }
    return null;
  }

  /**
   * Invalidates the cached search document of repository.
   */
  private void invalidateCachedSearchIndex(final Repository repository) {
    repository.facet(ProtopSearchIndexFacet.class).invalidateCachedSearchIndex();
  }
}
