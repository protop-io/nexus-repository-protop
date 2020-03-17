package org.sonatype.nexus.repository.protop.internal.search;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import org.sonatype.goodies.common.ComponentSupport;

import javax.inject.Named;
import javax.inject.Singleton;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Utility class for abstracting away the actual generation of JSON strings/content from the actual data objects. This
 * class delegates to a Jackson {@code JsonMapper} internally with custom configuration to handle Joda {@code DateTime}
 * objects. Primarily extracted here to make unit testing a bit easier by obviating the need for valid JSON responses.
 *
 * @since 3.7
 */
@Named
@Singleton
public class ProtopSearchResponseMapper
        extends ComponentSupport {
    private final ObjectMapper mapper;

    public ProtopSearchResponseMapper() {
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JodaModule());
    }

    /**
     * Writes the provided {@code ProtopSearchResponse} into a string in memory suitable for returning as part of a response.
     * This should be fine for the JSON sizes encountered in search requests (given the typical and maximum size limits).
     */
    public String writeString(final ProtopSearchResponse searchResponse) throws JsonProcessingException {
        return mapper.writeValueAsString(searchResponse);
    }

    /**
     * Reads an input stream, marshaling the contents into a {@code ProtopSearchResponse} if syntactically valid. Note that
     * this method makes no attempt to ensure that the response is semantically valid, so this must be done by the caller
     * as part of processing the results.
     */
    public ProtopSearchResponse readFromInputStream(final InputStream searchResponseStream) throws IOException {
        try (InputStream in = new BufferedInputStream(searchResponseStream)) {
            return mapper.readValue(in, ProtopSearchResponse.class);
        }
    }
}
