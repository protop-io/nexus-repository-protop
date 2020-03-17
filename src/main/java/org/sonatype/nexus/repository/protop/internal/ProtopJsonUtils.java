package org.sonatype.nexus.repository.protop.internal;

import com.fasterxml.jackson.core.JsonGenerator.Feature;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Supplier;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.InvalidContentException;

import javax.annotation.Nonnull;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * protop helper for serializing JSON protop metadata.
 *
 * @since 3.0
 */
public final class ProtopJsonUtils {
    public static final TypeReference<HashMap<String, Object>> rawMapJsonTypeRef;

    static final TypeReference<List<Object>> rawListJsonTypeRef;

    public static final ObjectMapper mapper;

    static {
        rawMapJsonTypeRef = new TypeReference<HashMap<String, Object>>() {
            // nop
        };
        rawListJsonTypeRef = new TypeReference<List<Object>>() {
            // nop
        };

        mapper = new ObjectMapper();
        mapper.disable(Feature.AUTO_CLOSE_TARGET);
    }

    private ProtopJsonUtils() {
        // nop
    }

    /**
     * Parses JSON content into map.
     */
    @Nonnull
    static NestedAttributesMap parse(final Supplier<InputStream> streamSupplier) throws IOException {
        try {
            final Map<String, Object> backing =
                    mapper.readValue(streamSupplier.get(), rawMapJsonTypeRef);
            return new NestedAttributesMap(String.valueOf(backing.get(ProtopMetadataUtils.NAME)), backing);
        } catch (JsonParseException e) {
            // fallback
            if (e.getMessage().contains("Invalid UTF-8")) {
                // try again, but assume ISO8859-1 encoding now, that is illegal for JSON
                final Map<String, Object> backing =
                        mapper.readValue(
                                new InputStreamReader(streamSupplier.get(), StandardCharsets.ISO_8859_1),
                                rawMapJsonTypeRef
                        );
                return new NestedAttributesMap(String.valueOf(backing.get(ProtopMetadataUtils.NAME)), backing);
            }
            throw new InvalidContentException("Invalid JSON input", e);
        }
    }

    /**
     * Serializes input map as JSON into given {@link Writer}.
     */
    static void serialize(final Writer out, final NestedAttributesMap packageRoot) {
        try {
            mapper.writeValue(out, packageRoot.backing());
        } catch (IOException e) {
            // mapping broken? we do not use mapping
            throw new RuntimeException(e);
        }
    }

    /**
     * Serializes input map as JSON into byte array.
     */
    @Nonnull
    static byte[] bytes(final NestedAttributesMap packageRoot) {
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        final OutputStreamWriter writer = new OutputStreamWriter(byteArrayOutputStream, StandardCharsets.UTF_8);
        serialize(writer, packageRoot);
        return byteArrayOutputStream.toByteArray();
    }

    /**
     * Creates input stream supplier out of passed in byte array content.
     */
    @Nonnull
    static Supplier<InputStream> supplier(final byte[] content) throws IOException {
        return () -> new ByteArrayInputStream(content);
    }
}
