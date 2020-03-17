package org.sonatype.nexus.repository.protop.internal;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.MapDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdValueInstantiator;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.json.MergeObjectMapper;
import org.sonatype.nexus.repository.json.NestedAttributesMapJsonParser;
import org.sonatype.nexus.repository.json.NestedAttributesMapStdValueInstantiator;
import org.sonatype.nexus.repository.json.SourceMapDeserializer;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;

import static com.google.common.collect.Maps.newHashMap;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.sonatype.nexus.repository.protop.internal.ProtopMetadataUtils.*;
import static org.sonatype.nexus.repository.protop.internal.ProtopVersionComparator.versionComparator;

/**
 * {@link ObjectMapper} implementation for protop Merging (the NXRM way) from {@link InputStream}s
 *
 * @since 3.16
 */
public class ProtopMergeObjectMapper
        extends MergeObjectMapper {
    /**
     * Same as {@link #merge(List, Charset)} but no {@link Charset} needed to be provided.
     *
     * @see #merge(List, Charset)
     */
    @Override
    public NestedAttributesMap merge(final List<InputStream> inputStreams) throws IOException {
        return merge(inputStreams, null);
    }

    /**
     * Merge the given {@link InputStream}s into a {@link NestedAttributesMap}. The merging is done according the
     * following rules :
     * <br/>
     * <br/>
     * 1) The order of the {@link InputStream}s dictate which values will be considered the dominant and
     * preserved (the last is the most dominant) value.
     * <br/>
     * 2) The versions in the protop "versions" field will be consolidated together.
     * <br/>
     * 3) We set the "dist-tags/latest" by tracking all versions and comparing which one is the latest.
     * <br/>
     * 4) We remove the "_id" and "_rev" fields as they have no meaning after merge
     *
     * @param inputStreams {@link List} of {@link InputStream}s
     * @param charset      {@link Charset} used for changing from default UTF-8
     * @return NestedAttributesMap
     */
    @Override
    public NestedAttributesMap merge(final List<InputStream> inputStreams,
                                     @Nullable final Charset charset)
            throws IOException {
        String latestVersion = null;
        NestedAttributesMap result = new NestedAttributesMap("mergeMap", newHashMap());

        for (InputStream inputStream : inputStreams) {
            merge(result, inputStream, charset);

            latestVersion = updateLatestVersion(result, latestVersion);
        }

        // after all has been parsed mark what we considered the latest version, note that
        // latestVersion might be null leaving a empty dist_tags in the json output
        result.child(DIST_TAGS).set(LATEST, latestVersion);

        // clean up meta id and rev as they have no meaning after merge
        result.remove(META_ID);
        result.remove(META_REV);

        return result;
    }

    @Override
    protected void deserialize(final NestedAttributesMapJsonParser parser,
                               final DeserializationContext context,
                               final MapDeserializer rootDeserializer) throws IOException {
        StdValueInstantiator instantiator = (StdValueInstantiator) rootDeserializer.getValueInstantiator();

        SourceMapDeserializer.of(rootDeserializer.getValueType(),
                new NestedAttributesMapStdValueInstantiator(instantiator, parser.getRoot()),
                new ProtopNestedAttributesMapUntypedObjectDeserializer(parser))
                .deserialize(parser, context);
    }

    private String updateLatestVersion(final NestedAttributesMap result, final String latestVersion) {
        String pkgLatestVersion = result.child(DIST_TAGS).get(LATEST, String.class);

        if (nonNull(pkgLatestVersion) && (isNull(latestVersion) ||
                versionComparator.compare(pkgLatestVersion, latestVersion) > 0)) {
            return pkgLatestVersion;
        }

        return latestVersion;
    }
}
