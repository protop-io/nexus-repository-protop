package org.sonatype.nexus.repository.protop.internal.search;

import org.elasticsearch.search.SearchHit;
import org.joda.time.DateTime;
import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.protop.internal.ProtopFormat;
import org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.sonatype.nexus.repository.protop.internal.ProtopAttributes.*;

/**
 * Extractor object allowing easy extraction of information from an Elasticsearch {@code SearchHit} as used by protop
 * search.
 *
 * @since 3.7
 */
@Named
@Singleton
public class ProtopSearchHitExtractor
        extends ComponentSupport {
    private static final String P_ASSETS = "assets";

    private static final String P_LAST_MODIFIED = "last_modified";

    @Nullable
    public String extractName(final SearchHit searchHit) {
        return extractProtopAttribute(searchHit, P_NAME, String.class);
    }

    @Nullable
    public String extractVersion(final SearchHit searchHit) {
        return extractProtopAttribute(searchHit, P_VERSION, String.class);
    }

    @Nullable
    public String extractDescription(final SearchHit searchHit) {
        return extractProtopAttribute(searchHit, P_DESCRIPTION, String.class);
    }

    @Nullable
    public String extractAuthorName(final SearchHit searchHit) {
        return parseAuthorName(extractProtopAttribute(searchHit, P_AUTHOR, String.class));
    }

    @Nullable
    public String extractAuthorEmail(final SearchHit searchHit) {
        return parseAuthorEmail(extractProtopAttribute(searchHit, P_AUTHOR, String.class));
    }

    public List<String> extractKeywords(final SearchHit searchHit) {
        String keywords = extractProtopAttribute(searchHit, P_KEYWORDS, String.class);
        if (keywords == null) {
            return emptyList();
        }
        keywords = keywords.trim();
        if (keywords.isEmpty()) {
            return emptyList();
        }
        return asList(keywords.split("\\s"));
    }

    @Nullable
    public String extractHomepage(final SearchHit searchHit) {
        return extractProtopAttribute(searchHit, P_HOMEPAGE, String.class);
    }

    @Nullable
    public String extractRepositoryUrl(final SearchHit searchHit) {
        return extractProtopAttribute(searchHit, P_REPOSITORY_URL, String.class);
    }

    @Nullable
    public String extractBugsUrl(final SearchHit searchHit) {
        return extractProtopAttribute(searchHit, P_BUGS_URL, String.class);
    }

    @Nullable
    public DateTime extractLastModified(final SearchHit searchHit) {
        Number timestamp = extractAttribute(searchHit, "content", P_LAST_MODIFIED, Number.class);
        if (timestamp != null) {
            return new DateTime(timestamp.longValue());
        } else {
            return null;
        }
    }

    /**
     * Attempts to extract the protop attribute with the specified name, returning null if it is not found.
     */
    @Nullable
    private <T> T extractProtopAttribute(final SearchHit searchHit, final String name, final Class<T> klass) {
        return extractAttribute(searchHit, ProtopFormat.NAME, name, klass);
    }

    /**
     * Attempts to (safely) extract an attribute from the specified attribute map, returning null if it is not found.
     */
    @Nullable
    private <T> T extractAttribute(final SearchHit searchHit,
                                   final String mapName,
                                   final String fieldName,
                                   final Class<T> klass) {

        Map<String, Object> source = searchHit.getSource();

        Collection<Map<String, Object>> assets = (Collection<Map<String, Object>>) source.get(P_ASSETS);
        if (assets == null || assets.isEmpty()) {
            return null;
        }

        Map<String, Object> asset = assets.iterator().next();
        Map<String, Object> entityAttributes = (Map<String, Object>) asset.get(MetadataNodeEntityAdapter.P_ATTRIBUTES);
        if (entityAttributes == null) {
            return null;
        }

        Map<String, Object> attributes = (Map<String, Object>) entityAttributes.get(mapName);
        if (attributes == null) {
            return null;
        }

        return (T) attributes.get(fieldName);
    }

    /**
     * Attempts to parse the name from the author field. Since we do not have username information for hosted, we use
     * the actual name of the user.
     */
    @Nullable
    private String parseAuthorName(@Nullable final String author) {

        // if no string provided, then no author
        if (author == null) {
            return null;
        }

        // find the start of the email address
        int endIndex = author.indexOf('<');

        // if no email address, try looking for the start of the url
        if (endIndex == -1) {
            endIndex = author.indexOf('(');
        }

        // determine the username portion based on the end position
        String username;
        if (endIndex == -1) {
            // if we don't have a start of another portion, then just trim the entire string and use it as the "username"
            username = author.trim();
        } else {
            // otherwise if we do, get everything before that and use that as the "username"
            username = author.substring(0, endIndex).trim();
        }

        // did we not actually get anything worth sending back?
        if (username.isEmpty()) {
            return null;
        }

        // if so, return the username
        return username;
    }

    /**
     * Attempts to parse the email address from the author field. This is the only piece of information on a user that we
     * can provide that could be considered equivalent to the person-related fields mentioned in an protop V1 search result.
     */
    @Nullable
    private String parseAuthorEmail(@Nullable final String author) {

        // if no string provided, then no author
        if (author == null) {
            return null;
        }

        // find the start of the email address
        int startIndex = author.indexOf('<');

        // if no email address start, then we have no email address
        if (startIndex == -1) {
            return null;
        }

        int endIndex = author.indexOf('>', startIndex);

        // if we have no end email address, then we don't have a valid email address string anyway
        if (endIndex == -1) {
            return null;
        }

        // extract the email address
        String email = author.substring(startIndex + 1, endIndex).trim();

        // did we not actually get anything worth sending back?
        if (email.isEmpty()) {
            return null;
        }

        // if so, return the username
        return email;
    }
}
