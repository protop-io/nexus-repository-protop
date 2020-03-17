package org.sonatype.nexus.repository.protop.internal;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.MapDeserializer;
import org.sonatype.nexus.repository.json.SourceMapDeserializer;
import org.sonatype.nexus.repository.json.StreamingObjectMapper;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;
import static org.sonatype.nexus.repository.protop.internal.ProtopMetadataUtils.META_ID;
import static org.sonatype.nexus.repository.protop.internal.ProtopMetadataUtils.META_REV;

/**
 * {@link ObjectMapper} implementation for protop streaming in and out of JSON.
 *
 * @since 3.16
 */
public class ProtopStreamingObjectMapper
        extends StreamingObjectMapper {
    private final String packageId;

    private final String packageRev;

    private List<ProtopFieldMatcher> matchers;

    public ProtopStreamingObjectMapper() {
        this(null, null, emptyList());
    }

    public ProtopStreamingObjectMapper(final List<ProtopFieldMatcher> matchers) {
        this(null, null, matchers);
    }

    public ProtopStreamingObjectMapper(@Nullable final String packageId,
                                       @Nullable final String packageRev,
                                       final List<ProtopFieldMatcher> matchers) {
        this.packageId = packageId;
        this.packageRev = packageRev;
        this.matchers = matchers;
    }

    @Override
    protected void deserializeAndSerialize(final JsonParser parser,
                                           final DeserializationContext context,
                                           final MapDeserializer deserializer,
                                           final JsonGenerator generator) throws IOException {
        SourceMapDeserializer
                .of(new ProtopMapDeserializerSerializer(deserializer, generator, matchers))
                .deserialize(parser, context);
    }

    @Override
    protected void beforeDeserialize(final JsonGenerator generator) throws IOException {
        super.beforeDeserialize(generator);
        maybeWritePackageAndRevID(generator);
    }

    @Override
    protected void afterDeserialize(final JsonGenerator generator) throws IOException {
        super.afterDeserialize(generator);
        appendFieldsIfNeverMatched(generator);
    }

    private void appendFieldsIfNeverMatched(final JsonGenerator generator) throws IOException {
        for (ProtopFieldMatcher matcher : unmatched()) {
            ProtopFieldDeserializer deserializer = matcher.getDeserializer();
            generator.writeFieldName(matcher.getFieldName());
            generator.writeObject(deserializer.deserializeValue(null));
        }
    }

    private void maybeWritePackageAndRevID(final JsonGenerator generator) throws IOException {
        if (nonNull(packageId)) {
            generator.writeFieldName(META_ID);
            generator.writeObject(packageId);
        }

        if (nonNull(packageRev)) {
            generator.writeFieldName(META_REV);
            generator.writeObject(packageRev);
        }
    }

    private List<ProtopFieldUnmatcher> unmatched() {
        return matchers.stream().map(this::protopFieldUnmatchedFilter)
                .filter(Objects::nonNull)
                .filter(ProtopFieldUnmatcher::wasNeverMatched)
                .collect(toList());
    }

    private ProtopFieldUnmatcher protopFieldUnmatchedFilter(final ProtopFieldMatcher fieldMatcher) {
        return fieldMatcher instanceof ProtopFieldUnmatcher ? (ProtopFieldUnmatcher) fieldMatcher : null;
    }
}
