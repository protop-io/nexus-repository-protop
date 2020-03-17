package org.sonatype.nexus.repository.protop.internal.search;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.group.GroupFacet;
import org.sonatype.nexus.repository.group.GroupHandler;
import org.sonatype.nexus.repository.http.HttpResponses;
import org.sonatype.nexus.repository.http.HttpStatus;
import org.sonatype.nexus.repository.view.*;
import org.sonatype.nexus.repository.view.payloads.StringPayload;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.Map.Entry;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Comparator.comparingDouble;
import static java.util.stream.Collectors.toList;

/**
 * Support for merging protop V1 search results together.
 *
 * @since 3.7
 */
@Named
@Singleton
public class ProtopSearchGroupHandler
        extends GroupHandler {
    private final ProtopSearchParameterExtractor protopSearchParameterExtractor;

    private final ProtopSearchResponseFactory protopSearchResponseFactory;

    private final ProtopSearchResponseMapper protopSearchResponseMapper;

    private final int v1SearchMaxResults;

    @Inject
    public ProtopSearchGroupHandler(final ProtopSearchParameterExtractor protopSearchParameterExtractor,
                                    final ProtopSearchResponseFactory protopSearchResponseFactory,
                                    final ProtopSearchResponseMapper protopSearchResponseMapper,
                                    @Named("${nexus.protop.v1SearchMaxResults:-250}") final int v1SearchMaxResults) {
        this.protopSearchParameterExtractor = checkNotNull(protopSearchParameterExtractor);
        this.protopSearchResponseFactory = checkNotNull(protopSearchResponseFactory);
        this.protopSearchResponseMapper = checkNotNull(protopSearchResponseMapper);
        this.v1SearchMaxResults = v1SearchMaxResults;
    }

    @Override
    protected Response doGet(@Nonnull final Context context,
                             @Nonnull final DispatchedRepositories dispatched)
            throws Exception {
        checkNotNull(context);
        checkNotNull(dispatched);

        Request request = context.getRequest();
        Parameters parameters = request.getParameters();
        String text = protopSearchParameterExtractor.extractText(parameters);

        ProtopSearchResponse response;
        if (text.isEmpty()) {
            response = protopSearchResponseFactory.buildEmptyResponse();
        } else {
            response = searchMembers(context, dispatched, parameters);
        }

        String content = protopSearchResponseMapper.writeString(response);
        return HttpResponses.ok(new StringPayload(content, ContentTypes.APPLICATION_JSON));
    }

    /**
     * Searches the member repositories, returning the merged response for all contacted member repositories that returned
     * a valid search response.
     */
    private ProtopSearchResponse searchMembers(final Context context,
                                               final DispatchedRepositories dispatched,
                                               final Parameters parameters) throws Exception {
        // preserve original from and size for repeated queries
        int from = protopSearchParameterExtractor.extractFrom(parameters);
        int size = protopSearchParameterExtractor.extractSize(parameters);

        // if the text is empty, then we override the original parameters to return a full set from each upstream source
        // we could make multiple queries to make this more efficient, but this is the simplest implementation for now
        parameters.replace("from", "0");
        parameters.replace("size", Integer.toString(v1SearchMaxResults));

        // sort all the merged results by normalized search score, then build the result responses to send back
        GroupFacet groupFacet = context.getRepository().facet(GroupFacet.class);
        Set<Entry<Repository, Response>> entries = getAll(context, groupFacet.members(), dispatched).entrySet();
        List<ProtopSearchResponseObject> mergedResponses = mergeAndNormalizeResponses(entries);
        mergedResponses.sort(comparingDouble(ProtopSearchResponseObject::getSearchScore).reversed());
        List<ProtopSearchResponseObject> mergedResponseObjects = mergedResponses.stream()
                .skip(from)
                .limit(size)
                .collect(toList());

        return protopSearchResponseFactory.buildResponseForObjects(mergedResponseObjects);
    }

    /**
     * Merges the responses from all the specified repositories, normalizing the search scores. Each package name is only
     * returned once for the first time it is encountered in the search results, with scores for each retained entry being
     * normalized to a scale of [0, 1] and sorted descending.
     */
    private List<ProtopSearchResponseObject> mergeAndNormalizeResponses(final Set<Entry<Repository, Response>> entries) {

        Map<String, ProtopSearchResponseObject> results = new LinkedHashMap<>();
        for (Entry<Repository, Response> entry : entries) {

            // do NOT ignore the rest of the search results just because we had an issue with ONE response
            ProtopSearchResponse searchResponse = parseSearchResponse(entry.getKey(), entry.getValue());
            if (searchResponse == null) {
                continue;
            }

            // should never happen, but if there are no actual objects in the response, just continue with the next one
            List<ProtopSearchResponseObject> searchResponseObjects = searchResponse.getObjects();
            if (searchResponseObjects == null) {
                continue;
            }

            // normalize each incoming score based on the first store we obtain from the package entries in the results, since
            // that's going to be the highest score for each batch.
            Double highestScore = null;
            for (ProtopSearchResponseObject searchResponseObject : searchResponseObjects) {

                // ensure that we only grab existing objects that have names and scores (should always be present, but just to
                // be safe in the event of semantically incorrect but syntactically valid JSON, we should check and filter them)
                if (!isValidResponseObject(searchResponseObject)) {
                    continue;
                }

                // if we do not already have a highest score, we should obtain one from the first valid search response we
                // encounter, using it to normalize the subsequent search scores using this one as the "highest" search score
                if (highestScore == null) {
                    highestScore = searchResponseObject.getSearchScore();
                }

                // add this result to the list with a normalized search score from 0 to 1 based on the highest score we first
                // encountered at the start of the responses, assuming we have not already encountered the same package name
                searchResponseObject.setSearchScore(searchResponseObject.getSearchScore() / highestScore);
                results.putIfAbsent(searchResponseObject.getPackageEntry().getName(), searchResponseObject);

            }
        }

        return new ArrayList<>(results.values());
    }

    /**
     * Determines whether or not a response object is valid enough to be processed, i.e. that the appropriate score and
     * name fields, at a minimum, were populated by the sender. This should ensure that we can process the a particular
     * response object without encountering NPEs later on.
     */
    private boolean isValidResponseObject(@Nullable final ProtopSearchResponseObject responseObject) {
        return responseObject != null &&
                responseObject.getSearchScore() != null &&
                responseObject.getPackageEntry() != null &&
                responseObject.getPackageEntry().getName() != null;
    }

    /**
     * Parses a search response, returning the marshaled {@link ProtopSearchResponse}.
     */
    @Nullable
    private ProtopSearchResponse parseSearchResponse(final Repository repository, final Response response) {
        Payload payload = response.getPayload();
        if (response.getStatus().getCode() == HttpStatus.OK && payload != null) {
            try (InputStream in = payload.openInputStream()) {
                return protopSearchResponseMapper.readFromInputStream(in);
            } catch (IOException e) {
                if (log.isDebugEnabled()) {
                    log.warn("Unable to process search response for repository {}, skipping", repository.getName(), e);
                } else {
                    log.warn("Unable to process search response for repository {}, cause: {}, skipping", repository.getName(),
                            e.getMessage());
                }
            }
        }
        return null;
    }
}
