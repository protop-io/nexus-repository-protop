package org.sonatype.nexus.repository.protop.internal.tasks;

import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.formfields.RepositoryCombobox;
import org.sonatype.nexus.repository.protop.internal.search.ProtopSearchFacet;
import org.sonatype.nexus.scheduling.TaskDescriptorSupport;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Task descriptor for {@link ReindexProtopRepositoryTask}.
 *
 * @since 3.7
 */
@Named
@Singleton
public class ReindexProtopRepositoryTaskDescriptor
        extends TaskDescriptorSupport {
    public static final String TYPE_ID = "repository.protop.reindex";

    public static final String REPOSITORY_NAME_FIELD_ID = "repositoryName";

    @Inject
    public ReindexProtopRepositoryTaskDescriptor(final NodeAccess nodeAccess) {
        super(TYPE_ID,
                ReindexProtopRepositoryTask.class,
                "Repair - Reconcile protop /-/v1/search metadata",
                VISIBLE,
                EXPOSED,
                new RepositoryCombobox(
                        REPOSITORY_NAME_FIELD_ID,
                        "Repository",
                        "Select the protop repository to reconcile",
                        true
                ).includingAnyOfFacets(ProtopSearchFacet.class).includeAnEntryForAllRepositories(),

                nodeAccess.isClustered() ? newMultinodeFormField().withInitialValue(true) : null
        );
    }
}
