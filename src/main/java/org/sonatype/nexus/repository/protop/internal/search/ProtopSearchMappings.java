package org.sonatype.nexus.repository.protop.internal.search;

import com.google.common.collect.ImmutableList;
import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.rest.SearchMapping;
import org.sonatype.nexus.repository.rest.SearchMappings;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.List;

/**
 * @since 3.7
 */
@Named("protop")
@Singleton
public class ProtopSearchMappings
        extends ComponentSupport
        implements SearchMappings {
    private static final List<SearchMapping> MAPPINGS = ImmutableList.of(
            new SearchMapping("protop.org", "group", "protop org")
    );

    @Override
    public Iterable<SearchMapping> get() {
        return MAPPINGS;
    }
}
