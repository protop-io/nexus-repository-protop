package org.sonatype.nexus.repository.protop.internal;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.deser.std.MapDeserializer;
import org.sonatype.nexus.repository.json.MapDeserializerSerializer;

import java.util.List;

/**
 * protop Specialized {@link MapDeserializerSerializer} that uses a {@link ProtopUntypedObjectDeserializerSerializer} for
 * it object deserialization and serializing out.
 *
 * @since 3.16
 */
public class ProtopMapDeserializerSerializer
        extends MapDeserializerSerializer {
    public ProtopMapDeserializerSerializer(final MapDeserializer rootDeserializer,
                                           final JsonGenerator generator,
                                           final List<ProtopFieldMatcher> matchers) {
        super(rootDeserializer, new ProtopUntypedObjectDeserializerSerializer(generator, matchers));
    }
}
