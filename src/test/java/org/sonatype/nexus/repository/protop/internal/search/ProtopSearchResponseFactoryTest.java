
package org.sonatype.nexus.repository.protop.internal.search;

import java.util.ArrayList;
import java.util.List;

import org.sonatype.goodies.testsupport.TestSupport;

import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms.Bucket;
import org.elasticsearch.search.aggregations.metrics.tophits.TopHits;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.sonatype.nexus.repository.protop.internal.search.*;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProtopSearchResponseFactoryTest
    extends TestSupport
{
  @Mock
  ProtopSearchHitExtractor protopSearchHitExtractor;

  ProtopSearchResponseFactory underTest;

  @Before
  public void setUp() {
    underTest = new ProtopSearchResponseFactory(protopSearchHitExtractor);
  }

  @Test
  public void testBuildEmptyResponse() {
    ProtopSearchResponse response = underTest.buildEmptyResponse();

    assertThat(response.getObjects(), is(empty()));
    assertThat(response.getTime(), not(nullValue()));
    assertThat(response.getTotal(), is(0));
  }

  @Test
  public void testBuildResponseWithResult() {
    ProtopSearchResponse response = underTest.buildResponseForResults(generateBuckets(1, true, true), 20, 0);

    assertThat(response.getObjects(), hasSize(1));
    assertThat(response.getTime(), not(nullValue()));
    assertThat(response.getTotal(), is(1));

    ProtopSearchResponseObject object = response.getObjects().get(0);
    ProtopSearchResponsePackage entry = object.getPackageEntry();

    assertThat(entry.getName(), is("name-0"));
    assertThat(entry.getVersion(), is("version-0"));
    assertThat(entry.getDescription(), is("description-0"));
    assertThat(entry.getKeywords(), is(singletonList("keyword-0")));
    assertThat(entry.getPublisher().getUsername(), is("author-name-0"));
    assertThat(entry.getPublisher().getEmail(), is("author-email-0"));
    assertThat(entry.getDate(), not(nullValue()));

    assertThat(entry.getMaintainers(), hasSize(1));
    assertThat(entry.getMaintainers().get(0).getUsername(), is("author-name-0"));
    assertThat(entry.getMaintainers().get(0).getEmail(), is("author-email-0"));

    assertThat(entry.getLinks(), not(nullValue()));
    assertThat(entry.getLinks().getBugs(), is("bugs-url-0"));
    assertThat(entry.getLinks().getHomepage(), is("homepage-url-0"));
    assertThat(entry.getLinks().getRepository(), is("repository-url-0"));
    assertThat(entry.getLinks().getProtop(), is(nullValue()));

    assertThat(object.getSearchScore(), is(1.0));
    assertThat(object.getScore(), not(nullValue()));
    assertThat(object.getScore().getFinalScore(), is(0.0));
    assertThat(object.getScore().getDetail(), not(nullValue()));
    assertThat(object.getScore().getDetail().getQuality(), is(0.0));
    assertThat(object.getScore().getDetail().getMaintenance(), is(0.0));
    assertThat(object.getScore().getDetail().getPopularity(), is(0.0));
  }

  @Test
  public void testBuildResponseWithSize() {
    ProtopSearchResponse response = underTest.buildResponseForResults(generateBuckets(5, true, true), 3, 0);

    assertThat(response.getObjects(), hasSize(3));
    assertThat(response.getTime(), not(nullValue()));
    assertThat(response.getTotal(), is(3));

    assertThat(response.getObjects().get(0).getPackageEntry().getName(), is("name-0"));
    assertThat(response.getObjects().get(1).getPackageEntry().getName(), is("name-1"));
    assertThat(response.getObjects().get(2).getPackageEntry().getName(), is("name-2"));
  }

  @Test
  public void testBuildResponseWithFrom() {
    ProtopSearchResponse response = underTest.buildResponseForResults(generateBuckets(10, true, true), 20, 5);

    assertThat(response.getObjects(), hasSize(5));
    assertThat(response.getTime(), not(nullValue()));
    assertThat(response.getTotal(), is(5));

    assertThat(response.getObjects().get(0).getPackageEntry().getName(), is("name-5"));
    assertThat(response.getObjects().get(1).getPackageEntry().getName(), is("name-6"));
    assertThat(response.getObjects().get(2).getPackageEntry().getName(), is("name-7"));
    assertThat(response.getObjects().get(3).getPackageEntry().getName(), is("name-8"));
    assertThat(response.getObjects().get(4).getPackageEntry().getName(), is("name-9"));
  }

  @Test
  public void testBuildResponseWithoutAuthorInformation() {
    ProtopSearchResponse response = underTest.buildResponseForResults(generateBuckets(1, false, false), 20, 0);

    assertThat(response.getObjects(), hasSize(1));
    assertThat(response.getTime(), not(nullValue()));
    assertThat(response.getTotal(), is(1));

    ProtopSearchResponseObject object = response.getObjects().get(0);
    ProtopSearchResponsePackage entry = object.getPackageEntry();

    assertThat(entry.getPublisher(), is(nullValue()));
    assertThat(entry.getMaintainers(), hasSize(0));
  }

  @Test
  public void testBuildResponseWithAuthorNameOnly() {
    ProtopSearchResponse response = underTest.buildResponseForResults(generateBuckets(1, true, false), 20, 0);

    assertThat(response.getObjects(), hasSize(1));
    assertThat(response.getTime(), not(nullValue()));
    assertThat(response.getTotal(), is(1));

    ProtopSearchResponseObject object = response.getObjects().get(0);
    ProtopSearchResponsePackage entry = object.getPackageEntry();

    assertThat(entry.getPublisher().getUsername(), is("author-name-0"));
    assertThat(entry.getPublisher().getEmail(), is(nullValue()));

    assertThat(entry.getMaintainers(), hasSize(1));
    assertThat(entry.getMaintainers().get(0).getUsername(), is("author-name-0"));
    assertThat(entry.getMaintainers().get(0).getEmail(), is(nullValue()));
  }

  @Test
  public void testBuildResponseWithAuthorEmailOnly() {
    ProtopSearchResponse response = underTest.buildResponseForResults(generateBuckets(1, false, true), 20, 0);

    assertThat(response.getObjects(), hasSize(1));
    assertThat(response.getTime(), not(nullValue()));
    assertThat(response.getTotal(), is(1));

    ProtopSearchResponseObject object = response.getObjects().get(0);
    ProtopSearchResponsePackage entry = object.getPackageEntry();

    assertThat(entry.getPublisher().getUsername(), is(nullValue()));
    assertThat(entry.getPublisher().getEmail(), is("author-email-0"));

    assertThat(entry.getMaintainers(), hasSize(1));
    assertThat(entry.getMaintainers().get(0).getUsername(), is(nullValue()));
    assertThat(entry.getMaintainers().get(0).getEmail(), is("author-email-0"));
  }

  @Test
  public void testBuildResponseFromObjects() {
    ProtopSearchResponseObject object = new ProtopSearchResponseObject();

    ProtopSearchResponse response = underTest.buildResponseForObjects(singletonList(object));

    assertThat(response.getObjects(), hasSize(1));
    assertThat(response.getTime(), not(nullValue()));
    assertThat(response.getTotal(), is(1));
  }

  private List<Terms.Bucket> generateBuckets(final int count,
                                             final boolean includeAuthorName,
                                             final boolean includeAuthorEmail)
  {
    List<Bucket> buckets = new ArrayList<>();
    for (int index = 0; index < count; index++) {
      Terms.Bucket bucket = mock(Terms.Bucket.class);
      Aggregations aggregations = mock(Aggregations.class);
      TopHits topHits = mock(TopHits.class);
      SearchHits searchHits = mock(SearchHits.class);
      SearchHit searchHit = mock(SearchHit.class);

      when(bucket.getAggregations()).thenReturn(aggregations);
      when(aggregations.get("versions")).thenReturn(topHits);
      when(topHits.getHits()).thenReturn(searchHits);
      when(searchHits.getAt(0)).thenReturn(searchHit);
      when(searchHit.getScore()).thenReturn(1.0F);

      if (includeAuthorEmail) {
        when(protopSearchHitExtractor.extractAuthorEmail(searchHit)).thenReturn("author-email-" + index);
      }
      if (includeAuthorName) {
        when(protopSearchHitExtractor.extractAuthorName(searchHit)).thenReturn("author-name-" + index);
      }
      when(protopSearchHitExtractor.extractBugsUrl(searchHit)).thenReturn("bugs-url-" + index);
      when(protopSearchHitExtractor.extractDescription(searchHit)).thenReturn("description-" + index);
      when(protopSearchHitExtractor.extractHomepage(searchHit)).thenReturn("homepage-url-" + index);
      when(protopSearchHitExtractor.extractRepositoryUrl(searchHit)).thenReturn("repository-url-" + index);
      when(protopSearchHitExtractor.extractKeywords(searchHit)).thenReturn(singletonList("keyword-" + index));
      when(protopSearchHitExtractor.extractLastModified(searchHit)).thenReturn(DateTime.now());
      when(protopSearchHitExtractor.extractName(searchHit)).thenReturn("name-" + index);
      when(protopSearchHitExtractor.extractVersion(searchHit)).thenReturn("version-" + index);

      buckets.add(bucket);
    }
    return buckets;
  }
}
