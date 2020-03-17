
package org.sonatype.nexus.repository.protop.internal;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.app.BaseUrlHolder;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.apache.commons.io.IOUtils.toInputStream;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.fail;
import static org.sonatype.nexus.repository.protop.internal.ProtopFieldFactory.REMOVE_DEFAULT_FIELDS_MATCHERS;
import static org.sonatype.nexus.repository.protop.internal.ProtopFieldFactory.missingFieldMatcher;
import static org.sonatype.nexus.repository.protop.internal.ProtopFieldFactory.rewriteTarballUrlMatcher;
import static org.sonatype.nexus.repository.protop.internal.ProtopMetadataUtils.META_REV;

public class ProtopStreamingObjectMapperTest
    extends TestSupport
{
  private final static String REPO_NAME = "protop-repository";

  private final static String ID_FIELD_NAME = "\"_id\"";

  private final static String REV_FIELD_NAME = "\"_rev\"";

  private final static String PACKAGE_REV = "1234567890";

  private final static String PACKAGE_ID = "array-first";

  private final static String PACKAGE_ID_JSON = ID_FIELD_NAME + ":\"" + PACKAGE_ID + "\"";

  private final static String PACKAGE_REV_JSON = REV_FIELD_NAME + ":\"" + PACKAGE_REV + "\"";

  @Before
  public void setUp() {
    BaseUrlHolder.unset();
  }

  @Test
  public void verify_MultiType_Json_StreamsOut_Same_JSON() throws IOException {
    try (InputStream packageRoot = getResource("streaming-payload.json");
         InputStream packageRoot2 = getResource("streaming-payload.json")) {

      ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      new ProtopStreamingObjectMapper().readAndWrite(packageRoot, byteArrayOutputStream);

      assertThat(byteArrayOutputStream.toString(), equalTo(IOUtils.toString(packageRoot2)));
    }
  }

  @Test
  public void verify_TarballUrl_Manipulation_Of_Json_While_Streaming_Out() throws IOException {
    String nxrmUrl = "http://localhost:8080/";
    String remoteUrl = "https://registry.protopjs.org";
    String distTarballPath = "\"dist\":{\"tarball\":\"";
    String packageId = "array-first";
    String packagePath = "/" + packageId + "/-/" + packageId;

    // these are not the complete tarball urls but are unique enough to identify that it was changed
    String normDistTarball = distTarballPath + nxrmUrl + "repository/" + REPO_NAME + packagePath;
    String remoteDistTarball = distTarballPath + remoteUrl + packagePath;

    assertThat(BaseUrlHolder.isSet(), is(false));

    BaseUrlHolder.set(nxrmUrl);

    try (InputStream packageRoot = getResource("streaming-payload-manipulate-while-streaming-out.json");
         InputStream packageRoot2 = getResource("streaming-payload-manipulate-while-streaming-out.json")) {

      String original = IOUtils.toString(packageRoot);

      assertThat(original, containsString(remoteDistTarball));
      assertThat(original, not(containsString(normDistTarball)));

      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      new ProtopStreamingObjectMapper(singletonList(rewriteTarballUrlMatcher(REPO_NAME, packageId)))
          .readAndWrite(packageRoot2, outputStream);

      String streamed = outputStream.toString();
      assertThat(streamed, not(containsString(remoteDistTarball)));
      assertThat(streamed, containsString(normDistTarball));
    }
  }

  @Test
  public void verify_Remove_Id_and_Rev_Manipulation_Of_Json_While_Streaming_Out() throws IOException {
    try (InputStream packageRoot = getResource("streaming-payload-manipulate-while-streaming-out.json");
         InputStream packageRoot2 = getResource("streaming-payload-manipulate-while-streaming-out.json")) {

      String original = IOUtils.toString(packageRoot);

      assertThat(original, containsString(PACKAGE_ID_JSON));
      assertThat(original, containsString(PACKAGE_REV));

      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      new ProtopStreamingObjectMapper(REMOVE_DEFAULT_FIELDS_MATCHERS).readAndWrite(packageRoot2, outputStream);

      String streamed = outputStream.toString();
      assertThat(streamed, not(containsString(PACKAGE_ID_JSON)));
      assertThat(streamed, not(containsString(PACKAGE_REV_JSON)));
    }
  }

  @Test
  public void verify_Add_Id_and_Rev_Manipulation_Of_Json_While_Streaming_Out() throws IOException {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    new ProtopStreamingObjectMapper(PACKAGE_ID, null, emptyList()).readAndWrite(toInputStream("{}"), outputStream);

    String streamed = outputStream.toString();
    assertThat(streamed, containsString(PACKAGE_ID_JSON));
    assertThat(streamed, not(containsString(REV_FIELD_NAME)));

    outputStream = new ByteArrayOutputStream();
    new ProtopStreamingObjectMapper(null, PACKAGE_REV, emptyList()).readAndWrite(toInputStream("{}"), outputStream);

    streamed = outputStream.toString();
    assertThat(streamed, not(containsString(ID_FIELD_NAME)));
    assertThat(streamed, containsString(PACKAGE_REV_JSON));

    outputStream = new ByteArrayOutputStream();
    new ProtopStreamingObjectMapper(PACKAGE_ID, PACKAGE_REV, emptyList())
        .readAndWrite(toInputStream("{}"), outputStream);

    streamed = outputStream.toString();
    assertThat(streamed, containsString(PACKAGE_ID_JSON));
    assertThat(streamed, containsString(PACKAGE_REV_JSON));

    outputStream = new ByteArrayOutputStream();
    new ProtopStreamingObjectMapper().readAndWrite(toInputStream("{}"), outputStream);

    streamed = outputStream.toString();
    assertThat(streamed, not(containsString(ID_FIELD_NAME)));
    assertThat(streamed, not(containsString(REV_FIELD_NAME)));

    outputStream = new ByteArrayOutputStream();
    new ProtopStreamingObjectMapper(REMOVE_DEFAULT_FIELDS_MATCHERS)
        .readAndWrite(toInputStream("{" + PACKAGE_ID_JSON + "," + PACKAGE_REV_JSON + "}"), outputStream);

    streamed = outputStream.toString();
    assertThat(streamed, not(containsString(ID_FIELD_NAME)));
    assertThat(streamed, not(containsString(REV_FIELD_NAME)));
  }

  @Test
  public void verify_Appending_Of_Fields_If_Never_Matched() throws IOException {
    try (InputStream packageRoot = getResource("streaming-payload-manipulate-while-streaming-out.json");
         InputStream packageRoot2 = getResource("streaming-payload-manipulate-while-streaming-out.json")) {

      String original = IOUtils.toString(packageRoot);

      assertThat(original, containsString(PACKAGE_ID_JSON));
      assertThat(original, containsString(PACKAGE_REV));

      String randomUUID = randomUUID().toString();
      String randomUUIDName = "randomUid";
      String randomUUIDFieldName = "\"" + randomUUIDName + "\"";

      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

      new ProtopStreamingObjectMapper(
          asList(
              missingFieldMatcher(randomUUIDName, "/" + randomUUIDName, () -> randomUUID),
              missingFieldMatcher(META_REV, "/" + META_REV, () -> randomUUID)))
          .readAndWrite(packageRoot2, outputStream);

      String streamed = outputStream.toString();

      // proof we added a field if never matched
      assertThat(streamed, containsString(randomUUIDFieldName + ":\"" + randomUUID + "\""));

      // proof that an existing field stays the same and doesn't get overwritten by a missing field matcher
      assertThat(streamed, containsString(PACKAGE_REV_JSON));
    }
  }

  private InputStream getResource(final String fileName) {
    return getClass().getResourceAsStream(fileName);
  }
}
