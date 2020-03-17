package org.sonatype.nexus.repository.protop.internal.cleanup;

import com.google.common.collect.ImmutableMap;
import org.sonatype.nexus.cleanup.config.CleanupPolicyConfiguration;
import org.sonatype.nexus.repository.protop.internal.ProtopFormat;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Map;

import static org.sonatype.nexus.repository.search.DefaultComponentMetadataProducer.*;

/**
 * Defines which cleanup policy fields to display for protop.
 */
@Named(ProtopFormat.NAME)
@Singleton
public class ProtopCleanupPolicyConfiguration
        implements CleanupPolicyConfiguration {
    @Override
    public Map<String, Boolean> getConfiguration() {
        return ImmutableMap.of(LAST_BLOB_UPDATED_KEY, true,
                LAST_DOWNLOADED_KEY, true,
                IS_PRERELEASE_KEY, true,
                REGEX_KEY, true);
    }
}
