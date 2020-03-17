
package org.sonatype.nexus.repository.protop.internal;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.entity.DetachedEntityId;
import org.sonatype.nexus.common.entity.DetachedEntityMetadata;
import org.sonatype.nexus.common.entity.DetachedEntityVersion;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.protop.ProtopFacet;
import org.sonatype.nexus.repository.protop.ProtopUploadHandlerTest;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetBlob;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.storage.TempBlob;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.common.base.Supplier;
import com.google.common.hash.HashCode;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Matchers;
import org.mockito.Mock;

import static java.util.Collections.emptyMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyCollection;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_NAME;

public class ProtopHostedFacetImplTest extends TestSupport
{
  @Rule
  public TemporaryFolder tempFolderRule = new TemporaryFolder();

  private ProtopHostedFacetImpl underTest;

  @Mock
  private ProtopFacet protopFacet;

  @Mock
  private ProtopRequestParser protopRequestParser;

  @Mock
  private Repository repository;

  @Mock
  private StorageFacet storageFacet;

  @Mock
  private StorageTx storageTx;

  @Mock
  private TempBlob tempBlob;

  @Mock
  private Asset mockAsset;

  @Mock
  private Asset packageRootAsset;

  @Mock
  private AssetBlob assetBlob;

  @Captor
  private ArgumentCaptor<Supplier<InputStream>> captor;

  @Before
  public void setup() throws Exception {
    underTest = new ProtopHostedFacetImpl(protopRequestParser);
    underTest.attach(repository);

    when(protopFacet.putTarball(any(), any(), any(), any())).thenReturn(mockAsset);

    when(storageFacet.createTempBlob(any(Payload.class), any())).thenAnswer(invocation -> {
      when(tempBlob.get()).thenReturn(((Payload) invocation.getArguments()[0]).openInputStream());
      return tempBlob;
    });
    when(storageFacet.txSupplier()).thenReturn(() -> storageTx);
    when(repository.facet(StorageFacet.class)).thenReturn(storageFacet);

    when(storageTx.createAsset(any(), any(Format.class))).thenReturn(packageRootAsset);
    when(packageRootAsset.formatAttributes()).thenReturn(new NestedAttributesMap("metadata", new HashMap<>()));
    when(packageRootAsset.name(any())).thenReturn(packageRootAsset);

    when(repository.facet(ProtopFacet.class)).thenReturn(protopFacet);

    when(tempBlob.getHashes())
        .thenReturn(Collections.singletonMap(HashAlgorithm.SHA1, HashCode.fromBytes("abcd".getBytes())));
    when(storageTx.createBlob(anyString(), Matchers.<Supplier<InputStream>> any(), anyCollection(), anyMap(),
        anyString(), anyBoolean()))
        .thenReturn(assetBlob);

    UnitOfWork.beginBatch(storageTx);
  }

  @After
  public void tearDown() {
    UnitOfWork.end();
  }

  @Test
  public void putPackage_payload_newPackage() throws IOException, URISyntaxException {
    try (InputStream is = ArchiveUtils.pack(tempFolderRule.newFile(),
        new File(ProtopUploadHandlerTest.class.getResource("internal/protop.json").toURI()),
            "package/protop.json")) {
      Map<String, Object> packageJson = new ProtopPackageParser().parseProtopJson(() -> is);
      Asset asset = underTest.putPackage(packageJson, tempBlob);
      assertThat(asset, is(mockAsset));
    }
  }

  @Test
  public void putPackage_payload_overlayPackage() throws IOException, URISyntaxException {
    mockPackageMetadata();
    try (InputStream is = ArchiveUtils.pack(tempFolderRule.newFile(),
        new File(ProtopUploadHandlerTest.class.getResource("internal/protop.json").toURI()), "package/protop.json")) {
      Map<String, Object> packageJson = new ProtopPackageParser().parseProtopJson(() -> is);
      Asset asset = underTest.putPackage(packageJson, tempBlob);
      assertThat(asset, is(mockAsset));

      verify(storageTx, times(1)).createBlob(eq("@foo/bar"), captor.capture(), any(), any(), any(), eq(true));

      String packageMetadata = IOUtils.toString(captor.getValue().get());
      assertTrue(packageMetadata.contains("0.1")); // existing
      assertTrue(packageMetadata.contains("1.0")); // added
    }
  }

  @Test
  public void putPackage_payload_notProtop() throws IOException, URISyntaxException {
    try {
      TempBlob tempBlob = mock(TempBlob.class);
      when(tempBlob.get())
          .thenAnswer(i -> ProtopHostedFacetImplTest.class.getResource("package-without-json.tar.gz").toURI());

      underTest.putPackage(emptyMap(), tempBlob);
      fail("Expected exception not thrown");
    }
    catch (NullPointerException e) {
      assertThat(e.getMessage(), is("Uploaded project org is invalid, or is missing protop.json"));
    }
  }

  @Test
  public void getDistTagNoResponseWhenPackageRootNotFound() {
    Content content = underTest.getDistTags(ProtopProjectId.parse("org/package"));

    assertThat(content, is(nullValue()));
  }

  @Test
  public void getDistTagWhenPackageRootFound() throws Exception {
    Bucket bucket = mock(Bucket.class);
    Asset asset = mock(Asset.class);
    Blob blob = mock(Blob.class);
    ByteArrayInputStream bis = new ByteArrayInputStream("{\"dist-tags\":{\"latest\":\"1.0.0\"}}".getBytes());
    when(storageTx.findBucket(repository)).thenReturn(bucket);
    when(storageTx.findAssetWithProperty("name", "org/package", bucket)).thenReturn(asset);
    when(storageTx.requireBlob(asset.requireBlobRef())).thenReturn(blob);
    when(blob.getInputStream()).thenReturn(bis);

    final Content content = underTest.getDistTags(ProtopProjectId.parse("org/package"));

    final String actual = IOUtils.toString(content.openInputStream());
    assertThat(actual, is("{\"latest\":\"1.0.0\"}"));
  }

  @Test(expected = IOException.class)
  public void updateTagLatestIsInvalid() throws Exception {
    ProtopProjectId packageId = new ProtopProjectId("myOrg", "myName");

    underTest.putDistTags(packageId, "latest", null);
  }

  private void mockPackageMetadata() {
    Asset packageAssetRoot = mock(Asset.class);
    when(packageAssetRoot.name()).thenReturn("@foo/bar");
    when(packageAssetRoot.getEntityMetadata())
        .thenReturn(new DetachedEntityMetadata(new DetachedEntityId("@foo/bar"), new DetachedEntityVersion("a")));
    when(packageAssetRoot.formatAttributes()).thenReturn(new NestedAttributesMap("metadata", new HashMap<>()));
    BlobRef blobRef = mock(BlobRef.class);
    when(packageAssetRoot.requireBlobRef()).thenReturn(blobRef);
    Blob blob = mock(Blob.class);
    when(storageTx.requireBlob(blobRef)).thenReturn(blob);

    when(blob.getInputStream())
        .thenReturn(new ByteArrayInputStream("{\"name\": \"@foo/bar\",\"versions\": {\"0.1\": {}}}".getBytes()));

    when(storageTx.findAssetWithProperty(eq(P_NAME), eq("@foo/bar"), any(Bucket.class))).thenReturn(packageAssetRoot);
  }
}
