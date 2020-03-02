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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreException;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.orient.entity.AttachedEntityHelper;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.protop.internal.ProtopAttributes.AssetKind;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetBlob;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.MissingAssetBlobException;
import org.sonatype.nexus.repository.storage.Query;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.storage.TempBlob;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.payloads.BlobPayload;
import org.sonatype.nexus.repository.view.payloads.BytesPayload;
import org.sonatype.nexus.repository.view.payloads.StreamPayload;
import org.sonatype.nexus.repository.view.payloads.StreamPayload.InputStreamSupplier;
import org.sonatype.nexus.thread.io.StreamCopier;

import com.google.common.base.Supplier;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.orientechnologies.orient.core.metadata.schema.OType;

import static com.google.common.collect.Maps.newHashMap;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonList;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toList;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA1;
import static org.sonatype.nexus.repository.http.HttpStatus.OK;
import static org.sonatype.nexus.repository.protop.internal.ProtopAttributes.AssetKind.TARBALL;
import static org.sonatype.nexus.repository.protop.internal.ProtopJsonUtils.mapper;
import static org.sonatype.nexus.repository.protop.internal.ProtopJsonUtils.serialize;
import static org.sonatype.nexus.repository.protop.internal.ProtopMetadataUtils.DIST_TAGS;
import static org.sonatype.nexus.repository.protop.internal.ProtopVersionComparator.extractNewestVersion;
import static org.sonatype.nexus.repository.storage.AssetEntityAdapter.P_ASSET_KIND;
import static org.sonatype.nexus.repository.storage.ComponentEntityAdapter.P_GROUP;
import static org.sonatype.nexus.repository.storage.ComponentEntityAdapter.P_VERSION;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_ATTRIBUTES;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_BUCKET;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_NAME;
import static org.sonatype.nexus.repository.view.ContentTypes.APPLICATION_JSON;
import static org.sonatype.nexus.repository.view.Status.success;

/**
 * Shared code of protop facets.
 *
 * Payloads being stored and their mapping:
 *
 * protop package metadata (JSON)
 * Component: none
 * Asset: N = ProtopProjectId.id()
 *
 * protop tarball (binary)
 * Component: G = ProtopProjectId.org(), N = ProtopProjectId.name(), V = version that tarball belongs to
 * Asset: N = ProtopPackage.id() + "/-/" + tarballName (see #tarballAssetName)
 *
 * @since 3.0
 */
public final class ProtopFacetUtils
{
  private ProtopFacetUtils() {
    // nop
  }

  private static final String SQL_FIND_ALL_PACKAGE_NAMES = String
      .format("SELECT DISTINCT(%s) AS %s FROM asset WHERE %s = :bucketRid AND %s.%s.%s = :kind ORDER BY name",
          P_NAME,
          P_NAME,
          P_BUCKET,
          P_ATTRIBUTES,
          ProtopFormat.NAME,
          P_ASSET_KIND
      );

  public static final List<HashAlgorithm> HASH_ALGORITHMS = Lists.newArrayList(SHA1);

  public static final String REPOSITORY_ROOT_ASSET = "-/all";

  public static final String REPOSITORY_SEARCH_ASSET = "-/v1/search";

  /**
   * Parses JSON content into map.
   */
  @Nonnull
  static NestedAttributesMap parse(final Supplier<InputStream> streamSupplier) throws IOException {
    return ProtopJsonUtils.parse(streamSupplier);
  }

  /**
   * Creates an {@link AssetBlob} out of passed in content and attaches it to passed in {@link Asset}.
   */
  @Nonnull
  static AssetBlob storeContent(final StorageTx tx,
                                final Asset asset,
                                final Supplier<InputStream> content,
                                final AssetKind assetKind) throws IOException {
    asset.formatAttributes().set(P_ASSET_KIND, assetKind.name());

    final AssetBlob result = tx.createBlob(
        asset.name(),
        content,
        HASH_ALGORITHMS,
        null,
        assetKind.getContentType(),
        assetKind.isSkipContentVerification());
    tx.attachBlob(asset, result);
    return result;
  }

  /**
   * Creates an {@link AssetBlob} out of passed in temporary blob and attaches it to passed in {@link Asset}.
   */
  @Nonnull
  static AssetBlob storeContent(final StorageTx tx,
                                final Asset asset,
                                final TempBlob tempBlob,
                                final AssetKind assetKind) throws IOException
  {
    asset.formatAttributes().set(P_ASSET_KIND, assetKind.name());
    AssetBlob result = tx.createBlob(
        asset.name(),
        tempBlob,
        null,
        assetKind.getContentType(),
        assetKind.isSkipContentVerification()
    );
    tx.attachBlob(asset, result);
    return result;
  }

  /**
   * Convert an asset blob to {@link Content}.
   *
   * @return content of asset blob
   */
  public static Content toContent(final Asset asset, final Blob blob) {
    Content content = new Content(new BlobPayload(blob, asset.requireContentType()));
    Content.extractFromAsset(asset, HASH_ALGORITHMS, content.getAttributes());
    return content;
  }

  /**
   * Convert an {@link Asset} representing a package root to a {@link Content} via a {@link StreamPayload}.
   *
   * @param repository       {@link Repository} to look up package root from.
   * @param packageRootAsset {@link Asset} associated with blob holding package root.
   * @return Content of asset blob
   */
  public static ProtopContent toContent(final Repository repository, final Asset packageRootAsset)
  {
    ProtopContent content = new ProtopContent(toPayload(repository, packageRootAsset));
    Content.extractFromAsset(packageRootAsset, HASH_ALGORITHMS, content.getAttributes());
    return content;
  }

  /**
   * Build a {@link ProtopStreamPayload} out of the {@link InputStream} representing the package root.
   *
   * @param repository       {@link Repository} to look up package root from.
   * @param packageRootAsset {@link Asset} associated with blob holding package root.
   */
  public static ProtopStreamPayload toPayload(final Repository repository,
                                           final Asset packageRootAsset)
  {
    return new ProtopStreamPayload(loadPackageRoot(repository, packageRootAsset));
  }

  /**
   * Save repository root asset & create blob from an input stream.
   *
   * @return blob content
   */
  @Nonnull
  public static Content saveRepositoryRoot(final StorageTx tx,
                                           final Asset asset,
                                           final Supplier<InputStream> contentSupplier,
                                           final Content content) throws IOException
  {
    Content.applyToAsset(asset, Content.maintainLastModified(asset, content.getAttributes()));
    final AssetBlob assetBlob = storeContent(tx, asset, contentSupplier, AssetKind.REPOSITORY_ROOT);
    tx.saveAsset(asset);
    return toContent(asset, assetBlob.getBlob());
  }

  /**
   * Save repository root asset & create blob from a temporary blob.
   *
   * @return blob content
   */
  @Nonnull
  static Content saveRepositoryRoot(final StorageTx tx,
                                    final Asset asset,
                                    final TempBlob tempBlob,
                                    final Content content) throws IOException
  {
    Content.applyToAsset(asset, Content.maintainLastModified(asset, content.getAttributes()));
    AssetBlob assetBlob = storeContent(tx, asset, tempBlob, AssetKind.REPOSITORY_ROOT);
    tx.saveAsset(asset);
    return toContent(asset, assetBlob.getBlob());
  }

  /**
   * Formats an asset name for a tarball out of package name and tarball filename.
   */
  @Nonnull
  static String tarballAssetName(final ProtopProjectId projectId, final String tarballName) {
    return projectId.id() + "/-/" + tarballName;
  }

  /**
   * Builds query builder for {@link Component} based on passed in {@link ProtopProjectId}.
   */
  @Nonnull
  private static Query.Builder query(final ProtopProjectId projectId) {
    return Query.builder()
            .where(P_NAME).eq(projectId.name())
            .and(P_GROUP).eq(projectId.org());
  }

  /**
   * Find all tarball component by package name in repository.
   */
  @Nonnull
  static Iterable<Component> findPackageTarballComponents(final StorageTx tx,
                                                          final Repository repository,
                                                          final ProtopProjectId packageId) {
    return tx.findComponents(query(packageId).build(), singletonList(repository));
  }

  /**
   * Find a tarball component by package name and version in repository.
   */
  @Nullable
  static Component findPackageTarballComponent(final StorageTx tx,
                                               final Repository repository,
                                               final ProtopProjectId packageId,
                                               final String version) {
    Iterable<Component> components = tx.findComponents(
        query(packageId)
            .and(P_VERSION).eq(version)
            .build(),
        singletonList(repository)
    );
    if (components.iterator().hasNext()) {
      return components.iterator().next();
    }
    return null;
  }

  /**
   * Find a repository root asset by package name in repository.
   */
  @Nullable
  public static Asset findRepositoryRootAsset(final StorageTx tx, final Bucket bucket) {
    return tx.findAssetWithProperty(P_NAME, REPOSITORY_ROOT_ASSET, bucket);
  }

  /**
   * Find a package root asset by package name in repository.
   */
  @Nullable
  public static Asset findPackageRootAsset(final StorageTx tx,
                                           final Bucket bucket,
                                           final ProtopProjectId packageId) {
    return tx.findAssetWithProperty(P_NAME, packageId.id(), bucket);
  }

  /**
   * Find a tarball asset by package name and tarball filename in repository.
   */
  @Nullable
  static Asset findTarballAsset(final StorageTx tx,
                                final Bucket bucket,
                                final ProtopProjectId packageId,
                                final String tarballName) {
    return tx.findAssetWithProperty(P_NAME, tarballAssetName(packageId, tarballName), bucket);
  }

  /**
   * Returns iterable that contains all the package names that exists in repository. Optional filtering possible with
   * nullable {@code modifiedSince} timestamp, that will return package names modified since.
   */
  @Nonnull
  public static Iterable<ProtopProjectId> findAllPackageNames(final StorageTx tx,
                                                              final Bucket bucket) {
    Map<String, Object> sqlParams = new HashMap<>();
    sqlParams.put("bucketRid", AttachedEntityHelper.id(bucket));
    sqlParams.put("kind", AssetKind.PACKAGE_ROOT);
    return Iterables.transform(
        tx.browse(SQL_FIND_ALL_PACKAGE_NAMES, sqlParams),
        input -> ProtopProjectId.parse(input.<String>field(P_NAME, OType.STRING))
    );
  }

  /**
   * Returns the package root JSON content by reading up package root asset's blob and parsing it. It also decorates
   * the JSON document with some fields.
   */
  public static NestedAttributesMap loadPackageRoot(final StorageTx tx,
                                                    final Asset packageRootAsset) throws IOException {
    final Blob blob = tx.requireBlob(packageRootAsset.requireBlobRef());
    NestedAttributesMap metadata = ProtopJsonUtils.parse(() -> blob.getInputStream());
    // add _id
    metadata.set(ProtopMetadataUtils.META_ID, packageRootAsset.name());
    return metadata;
  }

  /**
   * Returns a {@link Supplier} that will get the {@link InputStream} for the package root associated with the given
   * {@link Asset}.
   *
   * return {@link InputStreamSupplier}
   */
  public static InputStreamSupplier loadPackageRoot(final Repository repository,
                                                    final Asset packageRootAsset) {
    return () -> packageRootAssetToInputStream(repository, packageRootAsset);
  }

  /**
   * Returns a new {@link InputStream} that returns an error object. Mostly useful for protop Responses that have already
   * been written with a successful status (like a 200) but just before streaming out content found an issue preventing
   * the intended content to be streamed out.
   *
   * @return InputStream
   */
  public static InputStream errorInputStream(final String message) {
    NestedAttributesMap errorObject = new NestedAttributesMap("error", newHashMap());
    errorObject.set("success", false);
    errorObject.set("error", "Failed to stream response due to: " + message);
    return new ByteArrayInputStream(ProtopJsonUtils.bytes(errorObject));
  }

  /**
   * Saves the package root JSON content by persisting content into root asset's blob. It also removes some transient
   * fields from JSON document.
   */
  static void savePackageRoot(final StorageTx tx,
                              final Asset packageRootAsset,
                              final NestedAttributesMap packageRoot) throws IOException {
    packageRoot.remove(ProtopMetadataUtils.META_ID);
    packageRoot.remove("_attachments");
    packageRootAsset.formatAttributes().set(
        ProtopAttributes.P_protop_LAST_MODIFIED, ProtopMetadataUtils.maintainTime(packageRoot).toDate()
    );
    storeContent(
        tx,
        packageRootAsset,
        new StreamCopier<Supplier<InputStream>>(
            outputStream -> serialize(new OutputStreamWriter(outputStream, UTF_8), packageRoot),
            inputStream -> () -> inputStream).read(),
        AssetKind.PACKAGE_ROOT
    );
    tx.saveAsset(packageRootAsset);
  }

  /**
   * Deletes the package root and all related tarballs too.
   */
  static Set<String> deletePackageRoot(final StorageTx tx,
                                       final Repository repository,
                                       final ProtopProjectId packageId,
                                       final boolean deleteBlobs) {
    // find package asset -> delete
    Asset packageRootAsset = findPackageRootAsset(tx, tx.findBucket(repository), packageId);
    if (packageRootAsset == null) {
      return Collections.emptySet();
    }
    tx.deleteAsset(packageRootAsset, deleteBlobs);
    // find all tarball components -> delete
    Iterable<Component> protopTarballs = findPackageTarballComponents(tx, repository, packageId);
    Set<String> deletedAssetNames = new HashSet<>();
    for (Component protopTarball : protopTarballs) {
      deletedAssetNames.addAll(tx.deleteComponent(protopTarball, deleteBlobs));
    }
    return deletedAssetNames;
  }

  /**
   * Returns the tarball content.
   */
  @Nullable
  static Content getTarballContent(final StorageTx tx,
                                   final Bucket bucket,
                                   final ProtopProjectId packageId,
                                   final String tarballName) {
    Asset asset = findTarballAsset(tx, bucket, packageId, tarballName);
    if (asset == null) {
      return null;
    }

    Blob blob = tx.requireBlob(asset.requireBlobRef());
    Content content = new Content(new BlobPayload(blob, asset.requireContentType()));
    Content.extractFromAsset(asset, HASH_ALGORITHMS, content.getAttributes());
    return content;
  }

  /**
   * Gets or creates a {@link Component} for protop package tarball.
   */
  @Nonnull
  static Component getOrCreateTarballComponent(final StorageTx tx,
                                               final Repository repository,
                                               final ProtopProjectId packageId,
                                               final String version) {
    Component tarballComponent = findPackageTarballComponent(tx, repository, packageId, version);
    if (tarballComponent == null) {
      tarballComponent = tx.createComponent(tx.findBucket(repository), repository.getFormat())
          .group(packageId.org())
          .name(packageId.name())
          .version(version);
      tx.saveComponent(tarballComponent);
    }
    return tarballComponent;
  }

  /**
   * Creates an {@code AssetBlob} from a tarball's {@code TempBlob}.
   *
   * @since 3.7
   */
  static AssetBlob createTarballAssetBlob(final StorageTx tx,
                                          final ProtopProjectId packageId,
                                          final String tarballName,
                                          final TempBlob tempBlob) throws IOException {
    return tx.createBlob(
        tarballAssetName(packageId, tarballName),
        tempBlob,
        null,
        TARBALL.getContentType(),
        TARBALL.isSkipContentVerification()
    );
  }

  private static InputStream packageRootAssetToInputStream(final Repository repository, final Asset packageRootAsset) {
    BlobStore blobStore = repository.facet(StorageFacet.class).blobStore();
    if (isNull(blobStore)) {
      throw new MissingAssetBlobException(packageRootAsset);
    }

    BlobRef blobRef = packageRootAsset.requireBlobRef();
    Blob blob = blobStore.get(blobRef.getBlobId());
    if (isNull(blob)) {
      throw new MissingAssetBlobException(packageRootAsset);
    }

    try {
      return blob.getInputStream();
    }
    catch (BlobStoreException ignore) { // NOSONAR
      // we want any issue with the blob store stream to be caught during the getting of the input stream as throw the
      // the same type of exception as a missing asset blob, so that we can pass the associated asset around.
      throw new MissingAssetBlobException(packageRootAsset);
    }
  }

  /**
   * Converts the tags to a {@link Content} containing the tags as a json object
   */
  public static Content distTagsToContent(final NestedAttributesMap distTags) throws IOException {
    final byte[] bytes = mapper.writeValueAsBytes(distTags.backing());
    return new Content(new BytesPayload(bytes, APPLICATION_JSON));
  }

  /**
   * Updates the packageRoot with this set of dist-tags
   */
  public static void updateDistTags(final StorageTx tx,
                                    final Asset packageRootAsset,
                                    final String tag,
                                    final Object version) throws IOException {
    NestedAttributesMap packageRoot = loadPackageRoot(tx, packageRootAsset);
    NestedAttributesMap distTags = packageRoot.child(DIST_TAGS);
    distTags.set(tag, version);

    savePackageRoot(tx, packageRootAsset, packageRoot);
  }

  /**
   * Deletes the {@param tag} from the packageRoot
   */
  public static void deleteDistTags(final StorageTx tx,
                                    final Asset packageRootAsset,
                                    final String tag) throws IOException {
    NestedAttributesMap packageRoot = ProtopFacetUtils.loadPackageRoot(tx, packageRootAsset);
    if (packageRoot.contains(DIST_TAGS)) {
      NestedAttributesMap distTags = packageRoot.child(DIST_TAGS);
      distTags.remove(tag);
      ProtopFacetUtils.savePackageRoot(tx, packageRootAsset, packageRoot);
    }
  }

  /**
   * Removes all tags that are associated with a given {@param version}. If that version is also set as the
   * latest, the new latest is also populated from the remaining package versions stored
   */
  public static void removeDistTagsFromTagsWithVersion(final NestedAttributesMap packageRoot, final String version) {
    if (packageRoot.contains(DIST_TAGS)) {
      packageRoot.child(ProtopMetadataUtils.DIST_TAGS).entries().removeIf(e -> version.equals(e.getValue()));
    }
  }

  /**
   * Merges the dist-tag responses from all members and merges the values
   */
  public static Response mergeDistTagResponse(final Map<Repository, Response> responses) throws IOException {
    final List<NestedAttributesMap> collection = responses
        .values().stream()
        .map(response -> (Content) response.getPayload())
        .filter(Objects::nonNull)
        .map(ProtopFacetUtils::readDistTagResponse)
        .filter(Objects::nonNull)
        .collect(toList());

    final NestedAttributesMap merged = collection.get(0);
    if (collection.size() > 1) {
      collection.subList(1, collection.size())
          .forEach(response -> response.backing().forEach(populateLatestVersion(merged)));
    }

    return new Response.Builder()
        .status(success(OK))
        .payload(new BytesPayload(ProtopJsonUtils.bytes(merged), APPLICATION_JSON))
        .build();
  }

  private static BiConsumer<String, Object> populateLatestVersion(final NestedAttributesMap merged) {
    return (k, v) -> {
      if (!merged.contains(k)) {
        merged.set(k, v);
      }
      else {
        final String newestVersion = extractNewestVersion.apply(merged.get(k, String.class), (String) v);
        merged.set(k, newestVersion);
      }
    };
  }

  private static NestedAttributesMap readDistTagResponse(final Content content) {
    try (InputStream is = content.openInputStream()) {
      return ProtopJsonUtils.parse(() -> is);
    }
    catch (IOException ignore) { //NOSONAR
    }
    return null;
  }
}
