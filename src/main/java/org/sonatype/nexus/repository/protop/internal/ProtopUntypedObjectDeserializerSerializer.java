package org.sonatype.nexus.repository.protop.internal;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.UntypedObjectDeserializer;
import org.sonatype.nexus.repository.json.UntypedObjectDeserializerSerializer;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link UntypedObjectDeserializer} that is protop specific by instantly writing out to the provided generator,
 * rather then maintaining references in a map until all values have been deserialized.
 *
 * @since 3.16
 */
public class ProtopUntypedObjectDeserializerSerializer
        extends UntypedObjectDeserializerSerializer {
    private final List<ProtopFieldMatcher> matchers;

    public ProtopUntypedObjectDeserializerSerializer(final JsonGenerator generator,
                                                     final List<ProtopFieldMatcher> matchers) {
        super(generator);
        this.matchers = checkNotNull(matchers);
    }

    /**
     * Overwritten from {@link UntypedObjectDeserializer} allowing the deserialized JSON to be streamed out directly and
     * preventing the deserialized object from being kept in memory.
     *
     * @param parser  {@link JsonParser}
     * @param context {@link DeserializationContext}
     * @return an {@link Object} of any type, if needing to temporary keep it in memory, otherwise null.
     * @throws IOException if unable to properly read and parse given {@link JsonGenerator}.
     */
    @Override
    @Nullable
    public Object deserialize(final JsonParser parser, final DeserializationContext context) throws IOException {
        String fieldName = parser.getCurrentName();

        for (ProtopFieldMatcher matcher : matchers) {
            if (matcher.matches(parser) && matcher.allowDeserializationOnMatched()) {
                // first matcher wins
                return matcher.getDeserializer().deserialize(fieldName, defaultValueDeserialize(parser, context), parser, context, generator);
            }
        }

        return defaultDeserialize(fieldName, parser, context);
    }
}
