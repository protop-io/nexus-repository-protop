package org.sonatype.nexus.repository.protop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.repository.protop.internal.ProtopProjectId;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Since 3.16
 */
public class ProtopCoordinateUtil {

    private static final Logger logger = LoggerFactory.getLogger(ProtopCoordinateUtil.class);

    private static final Pattern PROTOP_VERSION_PATTERN = Pattern
            .compile("-(\\d+\\.\\d+\\.\\d+[A-Za-z\\d\\-.+]*)\\.(?:tar\\.gz)");


    public static String extractVersion(final String protopPath) {
        Matcher matcher = PROTOP_VERSION_PATTERN.matcher(protopPath);

        return matcher.find() ? matcher.group(1) : "";
    }

    public static String getPackageIdOrg(final String protopPath) {
        logger.info("Getting org for {}.", protopPath);
        return ProtopProjectId.parse(protopPath).org();
    }

    public static String getPackageIdName(final String protopPath) {
        logger.info("Getting name for {}.", protopPath);
        return ProtopProjectId.parse(protopPath).name();
    }

    private ProtopCoordinateUtil() {
        // no op
    }
}
