package org.sonatype.nexus.repository.protop.internal;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.sonatype.goodies.common.ComponentSupport;

import javax.inject.Named;
import javax.inject.Singleton;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.function.Supplier;

import static java.util.Collections.emptyMap;
import static org.apache.commons.compress.archivers.ArchiveStreamFactory.TAR;
import static org.apache.commons.compress.compressors.CompressorStreamFactory.GZIP;

/**
 * Parser for protop packages that will open up the tarball, extract the protop.json if present, and return a map with the
 * attributes parsed from the protop package.
 */
@Named
@Singleton
public class ProtopPackageParser extends ComponentSupport {
    private static final String SEPARATOR = "/";

    private static final String PROTOP_JSON_SUBPATH = SEPARATOR + "protop.json";

    /**
     * Parses the protop.json in the supplied tar.gz if present and extractable. In all other situations, an empty map
     * will be returned indicating the absence of (or inability to extract) a valid protop.json file and its contents.
     */
    public Map<String, Object> parseProtopJson(final Supplier<InputStream> supplier) {
        try (InputStream is = new BufferedInputStream(supplier.get())) {
            final CompressorStreamFactory compressorStreamFactory = new CompressorStreamFactory();
            try (InputStream cis = compressorStreamFactory.createCompressorInputStream(GZIP, is)) {
                final ArchiveStreamFactory archiveFactory = new ArchiveStreamFactory();
                try (ArchiveInputStream ais = archiveFactory.createArchiveInputStream(TAR, cis)) {
                    return parseProtopJsonInternal(ais);
                }
            }
        } catch (Exception e) {
            log.debug("Error occurred while processing protop.json, returning empty map to continue", e);
            return emptyMap();
        }
    }

    /**
     * Performs the actual parsing of the protop.json file if it exists.
     */
    private Map<String, Object> parseProtopJsonInternal(final ArchiveInputStream archiveInputStream)
            throws IOException {
        ArchiveEntry entry = archiveInputStream.getNextEntry();
        while (entry != null) {
            if (isProtopJson(entry)) {
                return ProtopJsonUtils.parse(() -> archiveInputStream).backing();
            }
            entry = archiveInputStream.getNextEntry();
        }
        return emptyMap();
    }

    /**
     * Determines if the specified archive entry's name could represent a valid protop.json file. Typically these would
     * be under {@code package/protop.json}, but there are tarballs out there that have the protop.json in a different
     * directory.
     */
    @VisibleForTesting
    boolean isProtopJson(final ArchiveEntry entry) {
        if (entry.isDirectory()) {
            return false;
        }
        String name = entry.getName();
        if (name == null) {
            return false;
        } else if (!name.endsWith(PROTOP_JSON_SUBPATH)) {
            return false; // not a protop.json file
        } else if (name.startsWith(PROTOP_JSON_SUBPATH)) {
            return false; // should not be at the root, should be under the path containing the actual package, whatever it is
        } else {
            return name.indexOf(PROTOP_JSON_SUBPATH) == name.indexOf(SEPARATOR); // path should only be one directory deep
        }
    }
}
