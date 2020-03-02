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

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.protop.internal.ProtopAttributes;
import org.sonatype.nexus.repository.protop.internal.ProtopFacetUtils;
import org.sonatype.nexus.repository.protop.internal.ProtopFormat;
import org.sonatype.nexus.repository.protop.internal.ProtopHostedFacet;
import org.sonatype.nexus.repository.protop.internal.ProtopMetadataUtils;
import org.sonatype.nexus.repository.protop.internal.ProtopProjectId;
import org.sonatype.nexus.repository.protop.internal.ProtopPackageParser;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.rest.UploadDefinitionExtension;
import org.sonatype.nexus.repository.security.ContentPermissionChecker;
import org.sonatype.nexus.repository.security.VariableResolverAdapter;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.TempBlob;
import org.sonatype.nexus.repository.upload.UploadHandlerSupport;
import org.sonatype.nexus.repository.upload.ComponentUpload;
import org.sonatype.nexus.repository.upload.UploadDefinition;
import org.sonatype.nexus.repository.upload.UploadResponse;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.common.collect.ImmutableMap;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Support for uploading components via UI & API
 */
@Singleton
@Named(ProtopFormat.NAME)
public class ProtopUploadHandler extends UploadHandlerSupport {

  private UploadDefinition definition;

  private final ContentPermissionChecker contentPermissionChecker;

  private final VariableResolverAdapter variableResolverAdapter;

  private final ProtopPackageParser protopPackageParser;

  @Inject
  public ProtopUploadHandler(final ContentPermissionChecker contentPermissionChecker,
                          @Named("simple") final VariableResolverAdapter variableResolverAdapter,
                          final ProtopPackageParser protopPackageParser,
                          final Set<UploadDefinitionExtension> uploadDefinitionExtensions) {
    super(uploadDefinitionExtensions);
    this.contentPermissionChecker = contentPermissionChecker;
    this.variableResolverAdapter = checkNotNull(variableResolverAdapter);
    this.protopPackageParser = checkNotNull(protopPackageParser);
  }

  @Override
  public UploadResponse handle(final Repository repository, final ComponentUpload upload) throws IOException {
    ProtopHostedFacet facet = repository.facet(ProtopHostedFacet.class);

    StorageFacet storageFacet = repository.facet(StorageFacet.class);

    try (TempBlob tempBlob = storageFacet.createTempBlob(upload.getAssetUploads().get(0).getPayload(),
        ProtopFacetUtils.HASH_ALGORITHMS)) {
      final Map<String, Object> packageJson = protopPackageParser.parseProtopJson(tempBlob);
      final String org = (String) packageJson.get(ProtopAttributes.P_ORG);
      final String name = (String) packageJson.get(ProtopAttributes.P_NAME);
      final String version = (String) packageJson.get(ProtopAttributes.P_VERSION);
      final String path = ProtopMetadataUtils.createRepositoryPath(org, name, version);
      final Map<String, String> coordinates = toCoordinates(packageJson);

      ensurePermitted(repository.getName(), ProtopFormat.NAME, path, coordinates);

      UnitOfWork.begin(storageFacet.txSupplier());
      try {
        return new UploadResponse(facet.putProject(packageJson, tempBlob));
      }
      finally {
        UnitOfWork.end();
      }
    }
  }

  private Map<String, String> toCoordinates(final Map<String, Object> packageJson) {
    String org = (String) checkNotNull(packageJson.get(ProtopAttributes.P_ORG));
    String name = (String) checkNotNull(packageJson.get(ProtopAttributes.P_NAME));
    ProtopProjectId packageId = new ProtopProjectId(org, name);
    String version = (String) checkNotNull(packageJson.get(ProtopAttributes.P_VERSION));

    return ImmutableMap.of("packageOrg", packageId.org(), "packageName", packageId.name(),
            ProtopAttributes.P_VERSION, version);
  }

  @Override
  public UploadDefinition getDefinition() {
    if (definition == null) {
      definition = getDefinition(ProtopFormat.NAME, false);
    }
    return definition;
  }

  @Override
  public VariableResolverAdapter getVariableResolverAdapter() {
    return variableResolverAdapter;
  }

  @Override
  public ContentPermissionChecker contentPermissionChecker() {
    return contentPermissionChecker;
  }
}
