package org.sonatype.nexus.repository.protop;

import com.google.common.collect.ImmutableMap;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.protop.internal.*;
import org.sonatype.nexus.repository.rest.UploadDefinitionExtension;
import org.sonatype.nexus.repository.security.ContentPermissionChecker;
import org.sonatype.nexus.repository.security.VariableResolverAdapter;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.TempBlob;
import org.sonatype.nexus.repository.upload.ComponentUpload;
import org.sonatype.nexus.repository.upload.UploadDefinition;
import org.sonatype.nexus.repository.upload.UploadHandlerSupport;
import org.sonatype.nexus.repository.upload.UploadResponse;
import org.sonatype.nexus.transaction.UnitOfWork;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

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
            final Map<String, Object> protopJson = protopPackageParser.parseProtopJson(tempBlob);
            final String org = (String) protopJson.get(ProtopAttributes.P_ORG);
            final String name = (String) protopJson.get(ProtopAttributes.P_NAME);
            final String version = (String) protopJson.get(ProtopAttributes.P_VERSION);
            final String path = ProtopMetadataUtils.createRepositoryPath(org, name, version);
            final Map<String, String> coordinates = toCoordinates(protopJson);

            ensurePermitted(repository.getName(), ProtopFormat.NAME, path, coordinates);

            UnitOfWork.begin(storageFacet.txSupplier());
            try {
                return new UploadResponse(facet.putPackage(protopJson, tempBlob));
            } finally {
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
