package org.sonatype.nexus.repository.protop.internal.search;

import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.tophits.TopHits;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.sonatype.goodies.common.ComponentSupport;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

/**
 * Factory for creating protop V1 search responses from the appropriate source information. This is primarily intended for
 * use in marshaling search results from Elasticsearch into the JSON payloads to be returned to the client.
 *
 * @since 3.7
 */
@Named
@Singleton
public class ProtopSearchResponseFactory
        extends ComponentSupport {
    private static final String SEARCH_RESPONSE_PACKAGE_DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    private static final String SEARCH_RESPONSE_DATE_PATTERN = "EEE MMM dd yyyy HH:mm:ss 'GMT'Z '('z')'";

    private static final DateTimeFormatter SEARCH_RESPONSE_PACKAGE_DATE_FORMAT = DateTimeFormat
            .forPattern(SEARCH_RESPONSE_PACKAGE_DATE_PATTERN);

    private static final DateTimeFormatter SEARCH_RESPONSE_DATE_FORMAT = DateTimeFormat
            .forPattern(SEARCH_RESPONSE_DATE_PATTERN);

    private static final Double DEFAULT_SCORE = 0.0;

    private final ProtopSearchHitExtractor protopSearchHitExtractor;

    @Inject
    public ProtopSearchResponseFactory(final ProtopSearchHitExtractor protopSearchHitExtractor) {
        this.protopSearchHitExtractor = checkNotNull(protopSearchHitExtractor);
    }

    /**
     * Builds an empty search response (used in the scenario where no search text was provided, to mimic protop registry
     * behavior as of this writing.
     */
    public ProtopSearchResponse buildEmptyResponse() {
        ProtopSearchResponse response = new ProtopSearchResponse();
        response.setObjects(emptyList());
        response.setTime(formatSearchResponseDate(DateTime.now()));
        response.setTotal(0);
        return response;
    }

    /**
     * Builds a search response containing each of the included search buckets.
     */
    public ProtopSearchResponse buildResponseForResults(final List<Terms.Bucket> buckets, final int size, final int from) {
        List<ProtopSearchResponseObject> objects = buckets.stream()
                .map(bucket -> (TopHits) bucket.getAggregations().get("versions"))
                .map(TopHits::getHits)
                .map(searchHits -> searchHits.getAt(0))
                .map(this::buildSearchResponseObject)
                .skip(from)
                .limit(size)
                .collect(toList());

        return buildResponseForObjects(objects);
    }

    /**
     * Builds a search response containing the specified objects.
     */
    public ProtopSearchResponse buildResponseForObjects(final List<ProtopSearchResponseObject> objects) {
        ProtopSearchResponse response = new ProtopSearchResponse();
        response.setObjects(objects);
        response.setTime(formatSearchResponseDate(DateTime.now()));
        response.setTotal(objects.size());
        return response;
    }

    /**
     * Builds a single package's search response object based on a provided search hit.
     */
    private ProtopSearchResponseObject buildSearchResponseObject(final SearchHit searchHit) {
        ProtopSearchResponsePerson person = buildPerson(searchHit);
        ProtopSearchResponseScore score = buildPackageScore();
        ProtopSearchResponsePackageLinks links = buildPackageLinks(searchHit);

        ProtopSearchResponsePackage searchPackage = new ProtopSearchResponsePackage();
        searchPackage.setDate(formatSearchResponsePackageDate(protopSearchHitExtractor.extractLastModified(searchHit)));
        searchPackage.setName(protopSearchHitExtractor.extractName(searchHit));
        searchPackage.setVersion(protopSearchHitExtractor.extractVersion(searchHit));
        searchPackage.setDescription(protopSearchHitExtractor.extractDescription(searchHit));
        searchPackage.setKeywords(protopSearchHitExtractor.extractKeywords(searchHit));
        searchPackage.setPublisher(person);
        searchPackage.setMaintainers(person == null ? emptyList() : singletonList(person));
        searchPackage.setLinks(links);

        ProtopSearchResponseObject searchObject = new ProtopSearchResponseObject();
        searchObject.setPackageEntry(searchPackage);
        searchObject.setSearchScore((double) searchHit.getScore());
        searchObject.setScore(score);
        return searchObject;
    }

    /**
     * Builds the package links where available. Since we will not have a link to the protop registry, we just have the bugs,
     * homepage, and repository links to include (if present in the original protop.json for the tarball).
     */
    private ProtopSearchResponsePackageLinks buildPackageLinks(final SearchHit searchHit) {
        ProtopSearchResponsePackageLinks links = new ProtopSearchResponsePackageLinks();
        links.setBugs(protopSearchHitExtractor.extractBugsUrl(searchHit));
        links.setHomepage(protopSearchHitExtractor.extractHomepage(searchHit));
        links.setRepository(protopSearchHitExtractor.extractRepositoryUrl(searchHit));
        return links;
    }

    /**
     * Builds the score detail portion of a package included in the response response, substituting in the default score
     * for all values. We do not know these values for hosted since they are derived from a variety of metrics that aren't
     * available to us, so we just plug in zero values to indicate we have no particular information.
     */
    private ProtopSearchResponseScore buildPackageScore() {
        // We do not support these fields for hosted as they require information we cannot generate for uploaded tarballs
        // (such as Github stars, results of running various linters/checks, comparisons to known NSP vulnerabilities).
        // For hosted we just default to plugging in zeroes since we have no meaningful scores or rankings to return.
        ProtopSearchResponseScoreDetail scoreDetail = new ProtopSearchResponseScoreDetail();
        scoreDetail.setMaintenance(DEFAULT_SCORE);
        scoreDetail.setPopularity(DEFAULT_SCORE);
        scoreDetail.setQuality(DEFAULT_SCORE);

        // the final score will also be zero since we don't support/weight by the maintenance/popularity/quality fields
        ProtopSearchResponseScore score = new ProtopSearchResponseScore();
        score.setFinalScore(DEFAULT_SCORE);
        score.setDetail(scoreDetail);
        return score;
    }

    /**
     * Builds the person information for the search response if the author name or author email fields are present.
     */
    @Nullable
    private ProtopSearchResponsePerson buildPerson(final SearchHit searchHit) {
        // username is not available to us, so we substitute the actual name for these results
        String username = protopSearchHitExtractor.extractAuthorName(searchHit);
        String email = protopSearchHitExtractor.extractAuthorEmail(searchHit);

        // if we have at least one of the values, we should send something back, otherwise there's nothing meaningful here
        if (username == null && email == null) {
            return null;
        } else {
            ProtopSearchResponsePerson person = new ProtopSearchResponsePerson();
            person.setUsername(username);
            person.setEmail(email);
            return person;
        }
    }

    /**
     * Returns a formatted date string suitable for a search response package entry.
     */
    private String formatSearchResponsePackageDate(final DateTime dateTime) {
        return SEARCH_RESPONSE_PACKAGE_DATE_FORMAT.print(dateTime.toDateTime(DateTimeZone.UTC));
    }

    /**
     * Returns a formatted date string suitable for a search response entry.
     */
    private String formatSearchResponseDate(final DateTime dateTime) {
        return SEARCH_RESPONSE_DATE_FORMAT.print(dateTime.toDateTime(DateTimeZone.UTC));
    }
}
