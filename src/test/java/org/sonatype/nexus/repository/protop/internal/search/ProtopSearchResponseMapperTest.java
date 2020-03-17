
package org.sonatype.nexus.repository.protop.internal.search;

import com.google.common.io.CharStreams;
import net.javacrumbs.jsonunit.JsonMatchers;
import org.junit.Test;
import org.sonatype.goodies.testsupport.TestSupport;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ProtopSearchResponseMapperTest
    extends TestSupport
{
  /**
   * Tests that a string generated from populated JSON data carriers produces JSON equivalent to the sample search
   * response in the protop V1 search API documentation.
   *
   * @see <a href="https://github.com/protop/registry/blob/master/docs/REGISTRY-API.md#get-v1search">GET·/-/v1/search</a>
   */
  @Test
  public void testWriteString() throws Exception {
    ProtopSearchResponseMapper underTest = new ProtopSearchResponseMapper();
    ProtopSearchResponse response = buildSearchResponse();
    String result = underTest.writeString(response);

    try (InputStream in = getClass().getResourceAsStream("sample-search-response.json")) {
      String expected = CharStreams.toString(new InputStreamReader(in, StandardCharsets.UTF_8));
      assertThat(result, JsonMatchers.jsonEquals(expected));
    }
  }

  private ProtopSearchResponse buildSearchResponse() {
    ProtopSearchResponseScoreDetail detail = new ProtopSearchResponseScoreDetail();
    detail.setQuality(0.9270640902288084);
    detail.setPopularity(0.8484861649808381);
    detail.setMaintenance(0.9962706951777409);

    ProtopSearchResponseScore score = new ProtopSearchResponseScore();
    score.setFinalScore(0.9237841281241451);
    score.setDetail(detail);

    ProtopSearchResponsePackageLinks links = new ProtopSearchResponsePackageLinks();
    links.setProtop("https://www.protop.io/package/foo");
    links.setHomepage("http://foo.org/proto");
    links.setRepository("https://github.com/foo/foo");
    links.setBugs("https://github.com/foo/foo/issues");

    ProtopSearchResponsePerson publisher = new ProtopSearchResponsePerson();
    publisher.setUsername("idk");
    publisher.setEmail("idk@protop.io");

    ProtopSearchResponsePerson maintainer1 = new ProtopSearchResponsePerson();
    maintainer1.setUsername("idk");
    maintainer1.setEmail("idk@protop.io");

    ProtopSearchResponsePerson maintainer2 = new ProtopSearchResponsePerson();
    maintainer2.setUsername("ok");
    maintainer2.setEmail("ok@protop.io");

    ProtopSearchResponsePerson maintainer3 = new ProtopSearchResponsePerson();
    maintainer3.setUsername("hmm");
    maintainer3.setEmail("hmm@protop.io");

    ProtopSearchResponsePerson maintainer4 = new ProtopSearchResponsePerson();
    maintainer4.setUsername("nylen");
    maintainer4.setEmail("jnylen@gmail.com");

    ProtopSearchResponsePackage packageEntry = new ProtopSearchResponsePackage();
    packageEntry.setName("yargs");
    packageEntry.setVersion("6.6.0");
    packageEntry.setDescription("yargs the modern, pirate-themed, successor to optimist.");
    packageEntry.setKeywords(asList("argument", "args", "option", "parser", "parsing", "cli", "command"));
    packageEntry.setDate("2016-12-30T16:53:16.023Z");
    packageEntry.setLinks(links);
    packageEntry.setPublisher(publisher);
    packageEntry.setMaintainers(asList(maintainer1, maintainer2, maintainer3, maintainer4));

    ProtopSearchResponseObject responseObject = new ProtopSearchResponseObject();
    responseObject.setPackageEntry(packageEntry);
    responseObject.setScore(score);
    responseObject.setSearchScore(100000.914);

    ProtopSearchResponse response = new ProtopSearchResponse();
    response.setObjects(Collections.singletonList(responseObject));
    response.setTotal(1);
    response.setTime("Fri Feb 28 2020 15:15:15 GMT+0000 (UTC)");
    return response;
  }

  /**
   * Tests that reading the sample search response in the protop V1 search API documentation produces semantically correct
   * Java objects containing the information.
   *
   * @see <a href="https://github.com/protop/registry/blob/master/docs/REGISTRY-API.md#get-v1search">GET·/-/v1/search</a>
   */
  @Test
  public void testReadInputStream() throws Exception {
    try (InputStream in = getClass().getResourceAsStream("sample-search-response.json")) {

      ProtopSearchResponseMapper underTest = new ProtopSearchResponseMapper();
      ProtopSearchResponse response = underTest.readFromInputStream(in);

      assertThat(response.getObjects(), hasSize(1));
      assertThat(response.getTotal(), is(1));
      assertThat(response.getTime(), is("Fri Feb 28 2020 15:15:15 GMT+0000 (UTC)"));

      ProtopSearchResponseObject object = response.getObjects().get(0);
      assertThat(object.getSearchScore(), is(closeTo(100000.914, 0.01)));

      ProtopSearchResponseScore score = object.getScore();
      assertThat(score.getFinalScore(), is(closeTo(0.9237841281241451, 0.01)));

      ProtopSearchResponseScoreDetail detail = score.getDetail();
      assertThat(detail.getQuality(), is(closeTo(0.9270640902288084, 0.01)));
      assertThat(detail.getPopularity(), is(closeTo(0.8484861649808381, 0.01)));
      assertThat(detail.getMaintenance(), is(closeTo(0.9962706951777409, 0.01)));

      ProtopSearchResponsePackage packageEntry = object.getPackageEntry();
      assertThat(packageEntry.getName(), is("yargs"));
      assertThat(packageEntry.getVersion(), is("6.6.0"));
      assertThat(packageEntry.getDescription(), is("yargs the modern, pirate-themed, successor to optimist."));
      assertThat(packageEntry.getKeywords(),
          containsInAnyOrder("argument", "args", "option", "parser", "parsing", "cli", "command"));
      assertThat(packageEntry.getDate(), is("2016-12-30T16:53:16.023Z"));

      ProtopSearchResponsePackageLinks links = packageEntry.getLinks();
      assertThat(links.getProtop(), is("https://www.protop.io/package/foo"));
      assertThat(links.getHomepage(), is("http://foo.org/proto"));
      assertThat(links.getRepository(), is("https://github.com/foo/foo"));
      assertThat(links.getBugs(), is("https://github.com/foo/foo/issues"));

      ProtopSearchResponsePerson publisher = packageEntry.getPublisher();
      assertThat(publisher.getUsername(), is("idk"));
      assertThat(publisher.getEmail(), is("idk@protop.io"));

      List<ProtopSearchResponsePerson> maintainers = packageEntry.getMaintainers();
      assertThat(maintainers, hasSize(4));
      assertThat(maintainers.get(0).getUsername(), is("idk"));
      assertThat(maintainers.get(0).getEmail(), is("idk@protop.io"));
      assertThat(maintainers.get(1).getUsername(), is("ok"));
      assertThat(maintainers.get(1).getEmail(), is("ok@protop.io"));
      assertThat(maintainers.get(2).getUsername(), is("hmm"));
      assertThat(maintainers.get(2).getEmail(), is("hmm@protop.io"));
      assertThat(maintainers.get(3).getUsername(), is("nylen"));
      assertThat(maintainers.get(3).getEmail(), is("jnylen@gmail.com"));
    }
  }
}
