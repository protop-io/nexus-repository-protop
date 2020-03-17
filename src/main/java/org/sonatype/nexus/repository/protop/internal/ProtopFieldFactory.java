package org.sonatype.nexus.repository.protop.internal;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import org.sonatype.nexus.repository.Repository;

import java.util.List;
import java.util.function.Supplier;

import static java.util.Arrays.asList;
import static org.sonatype.nexus.repository.protop.internal.ProtopMetadataUtils.*;

/**
 * Simple factory class for providing handlers that are common for manipulating protop JSON Fields.
 *
 * @since 3.16
 */
public class ProtopFieldFactory {
    public static final ProtopFieldDeserializer NULL_DESERIALIZER = new ProtopFieldDeserializer() {
        @Override
        public Object deserialize(final String fieldName,
                                  final Object defaultValue,
                                  final JsonParser parser,
                                  final DeserializationContext context,
                                  final JsonGenerator generator) {
            return null;
        }
    };

    public static final ProtopFieldMatcher REMOVE_ID_MATCHER = removeFieldMatcher(META_ID, "/" + META_ID);

    public static final ProtopFieldMatcher REMOVE_REV_MATCHER = removeFieldMatcher(META_REV, "/" + META_REV);

    public static final List<ProtopFieldMatcher> REMOVE_DEFAULT_FIELDS_MATCHERS = asList(REMOVE_ID_MATCHER,
            REMOVE_REV_MATCHER);

    private ProtopFieldFactory() {
        // factory constructor
    }

    private static ProtopFieldMatcher removeFieldMatcher(final String fieldName, final String pathRegex) {
        return new ProtopFieldMatcher(fieldName, pathRegex, NULL_DESERIALIZER);
    }

    public static ProtopFieldUnmatcher missingFieldMatcher(final String fieldName,
                                                           final String pathRegex,
                                                           final Supplier<Object> supplier) {
        return new ProtopFieldUnmatcher(fieldName, pathRegex, missingFieldDeserializer(supplier));
    }

    public static ProtopFieldDeserializer missingFieldDeserializer(final Supplier<Object> supplier) {
        ProtopFieldDeserializer deserializer = new ProtopFieldDeserializer() {
            @Override
            public Object deserializeValue(final Object defaultValue) {
                return supplier.get();
            }
        };
        return deserializer;
    }

    public static ProtopFieldUnmatcher missingRevFieldMatcher(final Supplier<Object> supplier) {
        return missingFieldMatcher(META_REV, "/" + META_REV, supplier);
    }

    public static ProtopFieldMatcher rewriteTarballUrlMatcher(final Repository repository, final String packageId) {
        return rewriteTarballUrlMatcher(repository.getName(), packageId);
    }

    public static ProtopFieldMatcher rewriteTarballUrlMatcher(final String repositoryName, final String packageId) {
        return new ProtopFieldMatcher("tarball", "/versions/(.*)/dist/tarball",
                rewriteTarballUrlDeserializer(repositoryName, packageId));
    }

    public static ProtopFieldDeserializer rewriteTarballUrlDeserializer(final String repositoryName,
                                                                        final String packageId) {
        return new ProtopFieldDeserializer() {
            @Override
            public Object deserializeValue(final Object defaultValue) {
                return rewriteTarballUrl(repositoryName, packageId, super.deserializeValue(defaultValue).toString());
            }
        };
    }
}
