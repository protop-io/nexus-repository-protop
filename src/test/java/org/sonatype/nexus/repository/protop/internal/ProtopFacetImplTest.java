
package org.sonatype.nexus.repository.protop.internal;

import com.google.common.collect.ImmutableMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.storage.*;
import org.sonatype.nexus.transaction.UnitOfWork;

import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Supplier;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.protop.internal.ProtopFacetUtils.REPOSITORY_ROOT_ASSET;

public class ProtopFacetImplTest extends TestSupport {
  private static final String ASSET_KIND_KEY = "asset_kind";

  private static final String ASSET_KIND_VALUE = "TARBALL";

  private static final String NAME_ATTRIBUTE_KEY = "name";

  private static final String NAME_ATTRIBUTE_VALUE = "foo";

  private static final String ORG_ATTRIBUTE_KEY = "org";

  private static final String ORG_ATTRIBUTE_VALUE = "someorg";

  private final Map<String, Object> FORMAT_ATTRIBUTES = ImmutableMap.of(
          NAME_ATTRIBUTE_KEY, NAME_ATTRIBUTE_VALUE,
          ORG_ATTRIBUTE_KEY, ORG_ATTRIBUTE_VALUE);

  private ProtopFacetImpl underTest;

  private ProtopProjectId projectId;

  @Mock
  private ProtopPackageParser protopPackageParser;

  @Mock
  private EventManager eventManager;

  @Mock
  private Repository repository;

  @Mock
  private ProtopFormat format;

  @Mock
  private StorageTx tx;

  @Mock
  private Iterable<Component> iterable;

  @Mock
  private Iterator<Component> iterator;

  @Mock
  private Bucket bucket;

  @Mock
  private AssetBlob assetBlob;

  @Mock
  private BlobRef blobRef;

  @Mock
  private Blob blob;

  @Mock
  private Component component;

  @Mock
  private Asset asset;

  @Mock
  private NestedAttributesMap attributesMap;

  @Mock
  private NestedAttributesMap formatAttributes;

  @Mock
  private InputStream inputStream;

  @Before
  public void setUp() throws Exception {
    underTest = new ProtopFacetImpl(protopPackageParser);
    underTest.installDependencies(eventManager);
    underTest.attach(repository);

    projectId = new ProtopProjectId("idk", "query-string");

    UnitOfWork.beginBatch(tx);

    when(repository.getFormat()).thenReturn(format);
    when(tx.findBucket(repository)).thenReturn(bucket);
    when(tx.createAsset(any(), any(Component.class))).thenReturn(asset);
    when(tx.createAsset(any(), any(Format.class))).thenReturn(asset);
    when(tx.createComponent(eq(bucket), any())).thenReturn(component);
    when(tx.findComponents(any(), any())).thenReturn(iterable);
    when(tx.requireBlob(blobRef)).thenReturn(blob);
    when(iterable.iterator()).thenReturn(iterator);
    when(iterator.hasNext()).thenReturn(false);
    when(component.group(any())).thenReturn(component);
    when(component.name(any())).thenReturn(component);
    when(component.version(any())).thenReturn(component);
    when(asset.formatAttributes()).thenReturn(formatAttributes);
    when(asset.attributes()).thenReturn(attributesMap);
    when(attributesMap.child(any())).thenReturn(attributesMap);
    when(asset.name(any())).thenReturn(asset);
    when(assetBlob.getBlobRef()).thenReturn(blobRef);
    when(blob.getInputStream()).thenReturn(inputStream);
    when(protopPackageParser.parseProtopJson(any(Supplier.class))).thenReturn(FORMAT_ATTRIBUTES);
  }

  @After
  public void tearDown() throws Exception {
    UnitOfWork.end();
  }

  @Test
  public void testPutPackageRoot() throws Exception {
    underTest.putPackageRoot(projectId.id(), assetBlob, null);

    verifyAssetCreated(projectId.id());
  }

  @Test
  public void testPutRepositoryRoot() throws Exception {
    underTest.putRepositoryRoot(assetBlob, null);

    verifyAssetCreated(REPOSITORY_ROOT_ASSET);
//    verify(eventManager).post(any(ProtopSearchIndexInvalidatedEvent.class));
  }

  @Test
  public void testPutTarball() throws Exception {
    String tarball = "idk-query-string.tar.gz";
    underTest.putTarball(projectId.id(), tarball, assetBlob, null);

    verify(tx).createAsset(eq(bucket), eq(component));
    verify(tx).saveAsset(asset);

    verifyTarballAndVersion(projectId, tarball, "");
  }

  @Test
  public void testReleaseVersion() throws Exception {
    String version = "1.0.0";
    String tarball = "idk-query-string-" + version + ".tar.gz";

    underTest.putTarball(projectId.id(), tarball, assetBlob, null);

    verifyTarballAndVersion(projectId, tarball, version);
  }

  @Test
  public void testReleaseVersionWithMultiDigit() throws Exception {
    String version = "12.345.6789-beta.15";
    String tarball = "query-string-" + version + ".tar.gz";

    underTest.putTarball(projectId.id(), tarball, assetBlob, null);

    verifyTarballAndVersion(projectId, tarball, version);
  }

  @Test
  public void testPrereleaseVersion() throws Exception {
    ProtopProjectId packageId = new ProtopProjectId("idk", "query-string");
    String version = "1.0.0-alpha." + System.currentTimeMillis() + ".500-beta";
    String tarball = "idk-query-string-" + version + ".tar.gz";

    underTest.putTarball(packageId.id(), tarball, assetBlob, null);

    verifyTarballAndVersion(packageId, tarball, version);
  }

  @Test
  public void testVersionWithMetadata() throws Exception {
    ProtopProjectId packageId = new ProtopProjectId("idk", "query-string");
    String version = "1.0.0-alpha." + System.currentTimeMillis() + ".500-beta+meta.data";
    String tarball = "idk-query-string-" + version + ".tar.gz";

    underTest.putTarball(packageId.id(), tarball, assetBlob, null);

    verifyTarballAndVersion(packageId, tarball, version);
  }

  private void verifyAssetCreated(final String name) throws Exception {
    verify(tx).createAsset(eq(bucket), eq(format));
    verify(tx).saveAsset(asset);
    verify(asset).name(eq(name));
  }

  private void verifyTarballAndVersion(final ProtopProjectId projectId, final String tarball, final String version) throws Exception {
    verify(tx).createComponent(bucket, repository.getFormat());
    verify(component).name(eq(projectId.name()));
    verify(component).version(eq(version));

    verify(asset).name(eq(ProtopFacetUtils.tarballAssetName(projectId, tarball)));
    verify(formatAttributes).set(ASSET_KIND_KEY, ASSET_KIND_VALUE);
    verify(formatAttributes).set(NAME_ATTRIBUTE_KEY, NAME_ATTRIBUTE_VALUE);
    verify(formatAttributes).set(ORG_ATTRIBUTE_KEY, ORG_ATTRIBUTE_VALUE);
  }
}
