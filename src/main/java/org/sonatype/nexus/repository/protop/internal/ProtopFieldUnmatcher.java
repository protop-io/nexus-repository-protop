package org.sonatype.nexus.repository.protop.internal;

import com.fasterxml.jackson.core.JsonParser;

import java.io.IOException;

/**
 * A protop Json Field Matcher to test whether we never matched a field.
 *
 * @since 3.17
 */
public class ProtopFieldUnmatcher
        extends ProtopFieldMatcher {
    private int matched = 0;

    public ProtopFieldUnmatcher(final String fieldName,
                                final String pathRegex,
                                final ProtopFieldDeserializer deserializer) {
        super(fieldName, pathRegex, deserializer);
    }

    @Override
    public boolean allowDeserializationOnMatched() {
        return false;
    }

    /**
     * Test for matches using the super class and marks if it was matched.
     *
     * @see ProtopFieldMatcher#matches(JsonParser)
     */
    @Override
    public boolean matches(final JsonParser parser) throws IOException {
        boolean matches = super.matches(parser);

        if (matched == 0 && matches) {
            matched = 1;
        }

        return matches;
    }

    /**
     * Test whether at the current parsed state of a {@link ProtopFieldMatcher} it was matched
     * by <code>fieldName</code> and <code>pathRegex</code>.
     */
    public boolean wasNeverMatched() {
        return matched == 0;
    }
}
