package org.sonatype.nexus.repository.protop.internal;

import com.fasterxml.jackson.core.JsonParser;
import org.sonatype.nexus.repository.json.CurrentPathJsonParser;

import java.io.IOException;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Objects.nonNull;
import static java.util.regex.Pattern.compile;

/**
 * A protop Json Field Matcher
 *
 * @since 3.16
 */
public class ProtopFieldMatcher {
    private final String fieldName;

    private final Pattern pattern;

    private final ProtopFieldDeserializer deserializer;

    /**
     * Constructor.
     *
     * @param fieldName String exact field name, e.g. : "tarball"
     * @param pathRegex String that matches by regex, e.g. : "/versions/(.*)/dist/tarball"
     */
    public ProtopFieldMatcher(final String fieldName, final String pathRegex, final ProtopFieldDeserializer deserializer) {
        this.fieldName = checkNotNull(fieldName);
        this.pattern = compile(checkNotNull(pathRegex));
        this.deserializer = checkNotNull(deserializer);
    }

    /**
     * Indicate whether the field matcher is allowed to be deserialized if it matched.
     *
     * @return true by default, implementers can override default behaviour.
     */
    public boolean allowDeserializationOnMatched() {
        return true;
    }

    /**
     * Test if the {@link #fieldName} and {@link #pattern} matches the current path on the {@link JsonParser}
     *
     * @param parser {@link JsonParser}
     * @return true if the field name and regex matches the current path on the {@link JsonParser}, false otherwise.
     */
    public boolean matches(final JsonParser parser) throws IOException {
        String currentName = parser.getCurrentName();
        // match on field name first, as it's much faster then matching on path, we eliminate fields that don't match fast.
        return nonNull(currentName) && matchesFieldName(currentName) && matchesPath(parser);
    }

    private boolean matchesFieldName(final String fieldName) {
        return this.fieldName.endsWith(fieldName);
    }

    private boolean matchesPath(final JsonParser parser) {
        if (parser instanceof CurrentPathJsonParser) {
            return matchesPath(((CurrentPathJsonParser) parser).currentPath());
        }

        return false;
    }

    private boolean matchesPath(final String path) {
        return this.pattern.matcher(path).matches();
    }

    public ProtopFieldDeserializer getDeserializer() {
        return deserializer;
    }

    public String getFieldName() {
        return fieldName;
    }
}
