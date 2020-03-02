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
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.entity.EntityHelper;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.protop.ProtopFacet;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetBlob;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.storage.TempBlob;
import org.sonatype.nexus.repository.transaction.TransactionalDeleteBlob;
import org.sonatype.nexus.repository.transaction.TransactionalStoreBlob;
import org.sonatype.nexus.repository.transaction.TransactionalStoreMetadata;
import org.sonatype.nexus.repository.transaction.TransactionalTouchBlob;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.common.eventbus.Subscribe;
import org.apache.commons.io.IOUtils;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Arrays.asList;
import static org.sonatype.nexus.repository.protop.internal.ProtopFacetUtils.findPackageRootAsset;
import static org.sonatype.nexus.repository.protop.internal.ProtopFacetUtils.findPackageTarballComponent;
import static org.sonatype.nexus.repository.protop.internal.ProtopFacetUtils.savePackageRoot;
import static org.sonatype.nexus.repository.protop.internal.ProtopFacetUtils.toContent;
import static org.sonatype.nexus.repository.protop.internal.ProtopFieldFactory.missingRevFieldMatcher;
import static org.sonatype.nexus.repository.protop.internal.ProtopFieldFactory.rewriteTarballUrlMatcher;
import static org.sonatype.nexus.repository.protop.internal.ProtopMetadataUtils.DIST_TAGS;
import static org.sonatype.nexus.repository.protop.internal.ProtopMetadataUtils.META_ID;
import static org.sonatype.nexus.repository.protop.internal.ProtopMetadataUtils.META_REV;
import static org.sonatype.nexus.repository.protop.internal.ProtopMetadataUtils.selectVersionByTarballName;
import static org.sonatype.nexus.repository.protop.internal.ProtopPackageRootMetadataUtils.createFullPackageMetadata;
import static org.sonatype.nexus.repository.protop.internal.ProtopVersionComparator.extractAlwaysPackageVersion;

/**
 * {@link ProtopHostedFacet} implementation.
 *
 * @since 3.0
 */
@Named
public class ProtopHostedFacetImpl
    extends FacetSupport
    implements ProtopHostedFacet
{
  private final ProtopRequestParser protopRequestParser;

  @Inject
  public ProtopHostedFacetImpl(final ProtopRequestParser protopRequestParser) {
    this.protopRequestParser = checkNotNull(protopRequestParser);
  }

  @Override
  protected void doInit(final Configuration configuration) throws Exception {
    super.doInit(configuration);
    getRepository().facet(StorageFacet.class).registerWritePolicySelector(new ProtopWritePolicySelector());
  }

  @Nullable
  @Override
  @TransactionalTouchBlob
  public Content getPackage(final ProtopProjectId packageId) throws IOException {
    checkNotNull(packageId);
    log.debug("Getting package: {}", packageId);
    StorageTx tx = UnitOfWork.currentTx();
    Asset packageRootAsset = findPackageRootAsset(tx, tx.findBucket(getRepository()), packageId);
    if (packageRootAsset == null) {
      return null;
    }

    return toContent(getRepository(), packageRootAsset)
        .fieldMatchers(asList(
            missingRevFieldMatcher(() -> generateNewRevId(packageRootAsset)),
            rewriteTarballUrlMatcher(getRepository().getName(), packageId.id())))
        .packageId(packageRootAsset.name());
  }

  protected String generateNewRevId(final Asset packageRootAsset) {
    String newRevision = EntityHelper.version(packageRootAsset).getValue();

    // For NEXUS-18094 we gonna request to upgrade the actual asset
    getEventManager().post(new ProtopRevisionUpgradeRequestEvent(packageRootAsset, newRevision));

    return newRevision;
  }

  /**
   * For NEXUS-18094 we moved the revision number to live in the package root file so that the revision number doesn't
   * change as the database record changes (previously it used the Orient Document Version number). This method allows
   * us to avoid the need for an upgrade step by upgrading package roots without a rev as they are fetched.
   */
  @Subscribe
  public void on(final ProtopRevisionUpgradeRequestEvent event) {
    UnitOfWork.begin(getRepository().facet(StorageFacet.class).txSupplier());
    try {
      upgradeRevisionOnPackageRoot(event.getPackageRootAsset(), event.getRevision());
    }
    finally {
      UnitOfWork.end();
    }
  }

  @TransactionalTouchBlob
  protected void upgradeRevisionOnPackageRoot(final Asset packageRootAsset, final String revision) {
    StorageTx tx = UnitOfWork.currentTx();

    ProtopProjectId packageId = ProtopProjectId.parse(packageRootAsset.name());
    Asset asset = findPackageRootAsset(tx, tx.findBucket(getRepository()), packageId);

    if (asset == null) {
      log.error("Failed to update revision on package root. Asset for id '{}' didn't exist", packageId.id());
      return;
    }

    // if there is a transaction failure and we fail to upgrade the package root with _rev
    // then the user who fetched the package root will not be able to run a delete command
    try {
      NestedAttributesMap packageRoot = ProtopFacetUtils.loadPackageRoot(tx, asset);
      packageRoot.set(META_REV, revision);
      savePackageRoot(UnitOfWork.currentTx(), packageRootAsset, packageRoot);
    }
    catch (IOException e) {
      log.warn("Failed to update revision in package root. Revision '{}' was not set" +
              " and might cause delete for that revision to fail for Asset {}",
          revision, packageRootAsset, e);
    }
  }

  @Override
  public void putProject(final ProtopProjectId packageId, @Nullable final String revision, final Payload payload) throws IOException {
    checkNotNull(packageId);
    checkNotNull(payload);
    try (ProtopPublishRequest request = protopRequestParser.parsePublish(getRepository(), payload)) {
      putPublishRequest(packageId, revision, request);
    }
  }

  @Override
  public Asset putProject(final Map<String, Object> packageJson, final TempBlob tempBlob) throws IOException {
    checkNotNull(packageJson);
    checkNotNull(tempBlob);

    log.debug("Storing package: {}", packageJson);

    String org = (String) checkNotNull(packageJson.get(ProtopAttributes.P_ORG),
            "Uploaded project org is invalid, or is missing protop.json");
    String name = (String) checkNotNull(packageJson.get(ProtopAttributes.P_NAME),
            "Uploaded project name is invalid, or is missing protop.json");
    String version = (String) checkNotNull(packageJson.get(ProtopAttributes.P_VERSION),
            "Uploaded project version is invalid, or is missing protop.json");

    NestedAttributesMap metadata = createFullPackageMetadata(
        new NestedAttributesMap("metadata", packageJson),
        getRepository().getName(),
        tempBlob.getHashes().get(HashAlgorithm.SHA1).toString(),
        null,
        extractAlwaysPackageVersion);

    ProtopProjectId projectId = new ProtopProjectId(org, name);

    return putProject(projectId, metadata, tempBlob);
  }

  @TransactionalStoreBlob
  protected Asset putProject(final ProtopProjectId projectId,
                             final NestedAttributesMap requestPackageRoot,
                             final TempBlob tarballTempBlob) throws IOException {
    checkNotNull(projectId);
    checkNotNull(requestPackageRoot);
    checkNotNull(tarballTempBlob);

    log.debug("Storing package: {}", projectId);

    StorageTx tx = UnitOfWork.currentTx();

    String tarballName = ProtopMetadataUtils.extractTarballName(requestPackageRoot);
    AssetBlob assetBlob = ProtopFacetUtils.createTarballAssetBlob(tx, projectId, tarballName, tarballTempBlob);

    ProtopFacet protopFacet = facet(ProtopFacet.class);
    Asset asset = protopFacet.putTarball(projectId.id(), tarballName, assetBlob, new AttributesMap());

    putProjectRoot(projectId, null, requestPackageRoot);

    return asset;
  }

  @TransactionalStoreBlob
  protected void putPublishRequest(final ProtopProjectId packageId,
                                   @Nullable final String revision,
                                   final ProtopPublishRequest request) throws IOException {

    log.debug("Storing package: {}", packageId);
    StorageTx tx = UnitOfWork.currentTx();

    NestedAttributesMap packageRoot = request.getPackageRoot();

    // process attachments, if any
    NestedAttributesMap attachments = packageRoot.child("_attachments");
    if (!attachments.isEmpty()) {
      for (String name : attachments.keys()) {
        NestedAttributesMap attachment = attachments.child(name);
        NestedAttributesMap packageVersion = selectVersionByTarballName(packageRoot, name);
        putTarball(tx, packageId, packageVersion, attachment, request);
      }
    }

    putProjectRoot(packageId, revision, packageRoot);
  }

  /**
   * Note: transactional method cannot be private, must be protected (as CGLIB will extend it).
   */
  @TransactionalStoreBlob
  public void putProjectRoot(final ProtopProjectId projectId,
                             @Nullable final String revision,
                             final NestedAttributesMap newPackageRoot) throws IOException {
    log.debug("Storing package root: {}", projectId);
    StorageTx tx = UnitOfWork.currentTx();
    Bucket bucket = tx.findBucket(getRepository());
    boolean update = false;

    NestedAttributesMap projectRoot = newPackageRoot;
    Asset packageRootAsset = findPackageRootAsset(tx, bucket, projectId);
    if (packageRootAsset != null) {
      NestedAttributesMap oldPackageRoot = ProtopFacetUtils.loadPackageRoot(tx, packageRootAsset);

      String rev = revision;
      if (rev == null) {
        rev = projectRoot.get(META_REV, String.class);
      }

      // ensure revision is expected, client updates package that is in expected state
      if (rev != null) {
        // if revision is present, full document is being sent, no overlay must occur
        checkArgument(rev.equals(oldPackageRoot.get(META_REV, String.class)));
        update = true;
      } else {
        // if no revision present, snippet is being sent, overlay it (if old exists)
        projectRoot = ProtopMetadataUtils.overlay(oldPackageRoot, projectRoot);
      }
    }

    boolean createdPackageRoot = false;
    if (packageRootAsset == null) {
      packageRootAsset = tx.createAsset(bucket, getRepository().getFormat()).name(projectId.id());
      createdPackageRoot = true;
    }

    updateRevision(projectRoot, packageRootAsset, createdPackageRoot);

    savePackageRoot(tx, packageRootAsset, projectRoot);
    if (update) {
      updateDeprecationFlags(tx, projectId, projectRoot);
    }
  }

  private void updateRevision(final NestedAttributesMap packageRoot,
                              final Asset packageRootAsset,
                              final boolean createdPackageRoot) {
    String newRevision = "1";

    if (!createdPackageRoot) {
      if (packageRoot.contains(META_REV)) {
        String rev = packageRoot.get(META_REV, String.class);
        newRevision = Integer.toString(Integer.parseInt(rev) + 1);
      }
      else {
        /*
          This is covering the edge case when a new package is uploaded to a repository where the packageRoot already 
          exists.
          
          If that packageRoot was created using an earlier version of NXRM where we didn't store the rev then we need
          to add it in. We also add the rev in on download but it is possible that someone is uploading a package where
          the packageRoot has never been downloaded before.
         */
        newRevision = EntityHelper.version(packageRootAsset).getValue();
      }
    }

    packageRoot.set(META_ID, packageRootAsset.name());
    packageRoot.set(META_REV, newRevision);
  }

  /**
   * Updates all the tarball components that belong to given package, updating their deprecated flags. Only changed
   * {@link Component}s are modified and saved.
   */
  private void updateDeprecationFlags(final StorageTx tx,
                                      final ProtopProjectId packageId,
                                      final NestedAttributesMap packageRoot) {
    final NestedAttributesMap versions = packageRoot.child(ProtopMetadataUtils.VERSIONS);
    for (Component tarballComponent : ProtopFacetUtils.findPackageTarballComponents(tx, getRepository(), packageId)) {
      // integrity check: package doc must contain the tarball version
      checkState(versions.contains(tarballComponent.version()), "Package %s lacks tarball version %s", packageId,
          tarballComponent.version());
      final NestedAttributesMap version = versions.child(tarballComponent.version());
      final String deprecationMessage = version.get(ProtopMetadataUtils.DEPRECATED, String.class);
      // in protop JSON, deprecated with non-empty string means deprecated, with empty or not present is not deprecated
      final boolean deprecated = !Strings2.isBlank(deprecationMessage);
      if (deprecated && !deprecationMessage
          .equals(tarballComponent.formatAttributes().get(ProtopAttributes.P_DEPRECATED, String.class))) {
        tarballComponent.formatAttributes().set(ProtopAttributes.P_DEPRECATED, deprecationMessage);
        tx.saveComponent(tarballComponent);
      }
      else if (!deprecated && tarballComponent.formatAttributes().contains(ProtopAttributes.P_DEPRECATED)) {
        tarballComponent.formatAttributes().remove(ProtopAttributes.P_DEPRECATED);
        tx.saveComponent(tarballComponent);
      }
    }
  }

  private void putTarball(final StorageTx tx,
                          final ProtopProjectId packageId,
                          final NestedAttributesMap packageVersion,
                          final NestedAttributesMap attachment,
                          final ProtopPublishRequest request) throws IOException {
    String tarballName = ProtopMetadataUtils.extractTarballName(attachment.getKey());
    log.debug("Storing tarball: {}@{} ({})",
        packageId,
        packageVersion.get(ProtopMetadataUtils.VERSION, String.class),
        tarballName);

    TempBlob tempBlob = request.requireBlob(attachment.require("data", String.class));
    AssetBlob assetBlob = ProtopFacetUtils.createTarballAssetBlob(tx, packageId, tarballName, tempBlob);

    ProtopFacet protopFacet = facet(ProtopFacet.class);
    protopFacet.putTarball(packageId.id(), tarballName, assetBlob, new AttributesMap());
  }

  @Override
  @TransactionalDeleteBlob
  public Set<String> deletePackage(final ProtopProjectId packageId, @Nullable final String revision) throws IOException {
    return deletePackage(packageId, revision, true);
  }

  @Override
  @TransactionalDeleteBlob
  public Set<String> deletePackage(final ProtopProjectId packageId,
                                   @Nullable final String revision,
                                   final boolean deleteBlobs) throws IOException {
    checkNotNull(packageId);
    StorageTx tx = UnitOfWork.currentTx();
    if (revision != null) {
      Asset packageRootAsset = findPackageRootAsset(tx, tx.findBucket(getRepository()), packageId);
      if (packageRootAsset != null) {
        NestedAttributesMap oldPackageRoot = ProtopFacetUtils.loadPackageRoot(tx, packageRootAsset);
        checkArgument(revision.equals(oldPackageRoot.get(META_REV, String.class)));
      }
    }

    return ProtopFacetUtils.deletePackageRoot(tx, getRepository(), packageId, deleteBlobs);
  }

  @Nullable
  @Override
  @TransactionalTouchBlob
  public Content getTarball(final ProtopProjectId packageId, final String tarballName) throws IOException {
    checkNotNull(packageId);
    checkNotNull(tarballName);
    StorageTx tx = UnitOfWork.currentTx();
    return ProtopFacetUtils.getTarballContent(tx, tx.findBucket(getRepository()), packageId, tarballName);
  }

  @Override
  @TransactionalDeleteBlob
  public Set<String> deleteTarball(final ProtopProjectId packageId, final String tarballName) {
    return deleteTarball(packageId, tarballName, true);
  }

  @Override
  @TransactionalDeleteBlob
  public Set<String> deleteTarball(final ProtopProjectId packageId, final String tarballName, final boolean deleteBlob) {
    checkNotNull(packageId);
    checkNotNull(tarballName);
    StorageTx tx = UnitOfWork.currentTx();
    Bucket bucket = tx.findBucket(getRepository());

    Asset tarballAsset = ProtopFacetUtils.findTarballAsset(tx, bucket, packageId, tarballName);
    if (tarballAsset == null) {
      return Collections.emptySet();
    }
    Component tarballComponent = tx.findComponentInBucket(tarballAsset.componentId(), bucket);
    if (tarballComponent == null) {
      return Collections.emptySet();
    }
    return tx.deleteComponent(tarballComponent, deleteBlob);
  }

  @Nullable
  @Override
  @TransactionalTouchBlob
  public Content getDistTags(final ProtopProjectId projectId) {
    checkNotNull(projectId);
    log.debug("Getting package: {}", projectId);
    StorageTx tx = UnitOfWork.currentTx();
    Asset packageRootAsset = findPackageRootAsset(tx, tx.findBucket(getRepository()), projectId);

    if (packageRootAsset == null) {
      log.debug("returning null");
      return null;
    }

    log.debug("still going");

    try {
      final NestedAttributesMap packageRoot = ProtopFacetUtils.loadPackageRoot(tx, packageRootAsset);
      final NestedAttributesMap distTags = packageRoot.child(DIST_TAGS);
      return ProtopFacetUtils.distTagsToContent(distTags);
    } catch (IOException e) {
      log.info("Unable to obtain dist-tags for {}", projectId.id(), e);
    }
    return null;
  }

  @Override
  public void putDistTags(final ProtopProjectId packageId, final String tag, final Payload payload) throws IOException {
    checkNotNull(packageId);
    checkNotNull(tag);
    log.debug("Updating distTags: {}", packageId);

    if ("latest".equals(tag)) {
      throw new IOException("Unable to update latest tag");
    }

    String version = parseVersionToTag(packageId, tag, payload);
    doPutDistTags(packageId, tag, version);
  }

  @TransactionalStoreMetadata
  protected void doPutDistTags(final ProtopProjectId packageId, final String tag, final String version) throws IOException {
    StorageTx tx = UnitOfWork.currentTx();
    Asset packageRootAsset = findPackageRootAsset(tx, tx.findBucket(getRepository()), packageId);
    if (packageRootAsset == null) {
      return;
    }

    if (findPackageTarballComponent(tx, getRepository(), packageId, version) == null) {
      throw new IOException(String
          .format("version %s of package %s is not present in repository %s", version, packageId.id(),
              getRepository().getName()));
    }

    try {
      ProtopFacetUtils.updateDistTags(tx, packageRootAsset, tag, version);
    }
    catch (IOException e) {
      log.error("Unable to update dist-tags for {}", packageId.id(), e);
    }
  }

  @TransactionalStoreMetadata
  @Override
  public void deleteDistTags(final ProtopProjectId packageId, final String tag, final Payload payload) throws IOException {
    checkNotNull(packageId);
    checkNotNull(tag);
    log.debug("Deleting distTags: {}", packageId);

    if ("latest".equals(tag)) {
      throw new IOException("Unable to delete latest");
    }

    StorageTx tx = UnitOfWork.currentTx();
    Asset packageRootAsset = findPackageRootAsset(tx, tx.findBucket(getRepository()), packageId);
    if (packageRootAsset == null) {
      return;
    }

    try {
      ProtopFacetUtils.deleteDistTags(tx, packageRootAsset, tag);
    }
    catch (IOException e) {
      log.info("Unable to obtain dist-tags for {}", packageId.id(), e);
    }
  }

  private String parseVersionToTag(final ProtopProjectId packageId,
                                   @Nullable final String tag,
                                   final Payload payload) throws IOException {
    String version;
    try (InputStream is = payload.openInputStream()) {
      version = IOUtils.toString(is).replaceAll("\"", "");
      log.debug("Adding tag {}:{} to {}", tag, version, packageId);
    }
    return version;
  }
}
