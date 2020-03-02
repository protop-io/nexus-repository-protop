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
import java.util.Map;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.protop.ProtopFacet;
import org.sonatype.nexus.repository.protop.internal.ProtopAttributes.AssetKind;
import org.sonatype.nexus.repository.protop.internal.search.legacy.ProtopSearchIndexInvalidatedEvent;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetBlob;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.transaction.UnitOfWork;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.protop.ProtopCoordinateUtil.extractVersion;
import static org.sonatype.nexus.repository.protop.internal.ProtopAttributes.AssetKind.PACKAGE_ROOT;
import static org.sonatype.nexus.repository.protop.internal.ProtopAttributes.AssetKind.REPOSITORY_ROOT;
import static org.sonatype.nexus.repository.protop.internal.ProtopAttributes.AssetKind.TARBALL;
import static org.sonatype.nexus.repository.protop.internal.ProtopFacetUtils.REPOSITORY_ROOT_ASSET;
import static org.sonatype.nexus.repository.protop.internal.ProtopFacetUtils.getOrCreateTarballComponent;
import static org.sonatype.nexus.repository.protop.internal.ProtopFacetUtils.tarballAssetName;
import static org.sonatype.nexus.repository.storage.AssetEntityAdapter.P_ASSET_KIND;

/**
 * @since 3.6.1
 */
@Named
public class ProtopFacetImpl
    extends FacetSupport
    implements ProtopFacet
{
  private final ProtopPackageParser protopPackageParser;

  @Inject
  public ProtopFacetImpl(final ProtopPackageParser protopPackageParser) {
    this.protopPackageParser = checkNotNull(protopPackageParser);
  }

  @Nullable
  @Override
  public Asset findRepositoryRootAsset() {
    final StorageTx tx = UnitOfWork.currentTx();
    return ProtopFacetUtils.findRepositoryRootAsset(tx, tx.findBucket(getRepository()));
  }

  @Nullable
  @Override
  public Asset putRepositoryRoot(final AssetBlob assetBlob, @Nullable final AttributesMap contentAttributes)
      throws IOException
  {
    final Repository repository = getRepository();
    final StorageTx tx = UnitOfWork.currentTx();
    final Bucket bucket = tx.findBucket(repository);
    Asset asset = ProtopFacetUtils.findRepositoryRootAsset(tx, bucket);

    if (asset == null) {
      asset = tx.createAsset(bucket, repository.getFormat()).name(REPOSITORY_ROOT_ASSET);
      getEventManager().post(new ProtopSearchIndexInvalidatedEvent(repository));
      saveAsset(tx, asset, assetBlob, REPOSITORY_ROOT, contentAttributes);
    }

    return asset;
  }

  @Nullable
  @Override
  public Asset findPackageRootAsset(final String packageId) {
    final StorageTx tx = UnitOfWork.currentTx();
    return ProtopFacetUtils.findPackageRootAsset(tx, tx.findBucket(getRepository()), ProtopProjectId.parse(packageId));
  }

  @Nullable
  @Override
  public Asset putPackageRoot(final String projectId,
                              final AssetBlob assetBlob,
                              @Nullable final AttributesMap contentAttributes) throws IOException {
    final Repository repository = getRepository();
    final StorageTx tx = UnitOfWork.currentTx();
    final Bucket bucket = tx.findBucket(repository);
    Asset asset = ProtopFacetUtils.findPackageRootAsset(tx, bucket, ProtopProjectId.parse(projectId));

    if (asset == null) {
      asset = tx.createAsset(bucket, repository.getFormat()).name(projectId);
      saveAsset(tx, asset, assetBlob, PACKAGE_ROOT, contentAttributes);
    }

    return asset;
  }

  @Nullable
  @Override
  public Asset findTarballAsset(final String packageId,
                                final String tarballName)
  {
    final StorageTx tx = UnitOfWork.currentTx();
    return ProtopFacetUtils
        .findTarballAsset(tx, tx.findBucket(getRepository()), ProtopProjectId.parse(packageId), tarballName);
  }

  @Nullable
  @Override
  public Asset putTarball(final String packageId,
                          final String tarballName,
                          final AssetBlob assetBlob,
                          @Nullable final AttributesMap contentAttributes) throws IOException {
    final Repository repository = getRepository();
    final StorageTx tx = UnitOfWork.currentTx();
    final Bucket bucket = tx.findBucket(repository);
    ProtopProjectId protopProjectId = ProtopProjectId.parse(packageId);
    Asset asset = ProtopFacetUtils.findTarballAsset(tx, bucket, protopProjectId, tarballName);

    if (asset == null) {
      String version = extractVersion(tarballName);
      Component tarballComponent = getOrCreateTarballComponent(tx, repository, protopProjectId, version);
      asset = tx.firstAsset(tarballComponent);

      if (asset == null) {
        asset = tx.createAsset(bucket, tarballComponent).name(tarballAssetName(protopProjectId, tarballName));
      }
    }

    maybeExtractFormatAttributes(tx, packageId, asset, assetBlob);
    saveAsset(tx, asset, assetBlob, TARBALL, contentAttributes);

    return asset;
  }

  private void saveAsset(final StorageTx tx,
                         final Asset asset,
                         final AssetBlob assetBlob,
                         final AssetKind kind,
                         @Nullable final AttributesMap contentAttributes)
  {
    asset.formatAttributes().set(P_ASSET_KIND, kind.name());
    tx.attachBlob(asset, assetBlob);
    Content.applyToAsset(asset, Content.maintainLastModified(asset, contentAttributes));
    tx.saveAsset(asset);
  }

  private void maybeExtractFormatAttributes(final StorageTx tx,
                                            final String packageId,
                                            final Asset asset,
                                            final AssetBlob assetBlob)
  {
    Blob blob = tx.requireBlob(assetBlob.getBlobRef());
    Map<String, Object> formatAttributes = protopPackageParser.parseProtopJson(blob::getInputStream);
    if (formatAttributes.isEmpty()) {
      log.warn("No format attributes found in protop.json for protop package ID {}, will not be searchable", packageId);
    }
    else {
      ProtopFormatAttributesExtractor formatAttributesExtractor = new ProtopFormatAttributesExtractor(formatAttributes);
      formatAttributesExtractor.copyFormatAttributes(asset);
    }
  }
}
