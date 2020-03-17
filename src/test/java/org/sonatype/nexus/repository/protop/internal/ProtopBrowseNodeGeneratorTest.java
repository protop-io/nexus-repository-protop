
package org.sonatype.nexus.repository.protop.internal;

import java.util.List;

import org.sonatype.nexus.repository.browse.BrowseNodeGenerator;
import org.sonatype.nexus.repository.browse.BrowsePaths;
import org.sonatype.nexus.repository.browse.BrowseTestSupport;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.DefaultComponent;

import org.junit.Test;

import static java.util.Arrays.asList;

public class ProtopBrowseNodeGeneratorTest
    extends BrowseTestSupport
{
  private BrowseNodeGenerator generator = new ProtopBrowseNodeGenerator();

  @Test
  public void computeAssetPathScopedComponent() {
    Asset asset = createAsset("@types/jquery/-/jquery-1.0.0.tar.gz");
    Component component = new DefaultComponent();

    List<BrowsePaths> assetPaths = generator.computeAssetPaths(asset, component);

    assertPaths(asList("@types", "jquery", "jquery-1.0.0.tar.gz"), asList("@types", "jquery", "-/jquery-1.0.0.tar.gz"), assetPaths);
  }

  @Test
  public void computeAssetPathComponent() {
    Asset asset = createAsset("jquery/-/jquery-1.0.0.tar.gz");
    Component component = new DefaultComponent();

    List<BrowsePaths> assetPaths = generator.computeAssetPaths(asset, component);

    assertPaths(asList("jquery", "jquery-1.0.0.tar.gz"), asList("jquery", "-/jquery-1.0.0.tar.gz"), assetPaths);
  }

  @Test
  public void computeComponentPathReturnsAssetPath() {
    Asset asset = createAsset("jquery/-/jquery-1.0.0.tar.gz");
    Component component = new DefaultComponent();

    List<BrowsePaths> assetPaths = generator.computeComponentPaths(asset, component);

    assertPaths(asList("jquery", "jquery-1.0.0.tar.gz"), asList("jquery", "-/jquery-1.0.0.tar.gz"), assetPaths);
  }
}
