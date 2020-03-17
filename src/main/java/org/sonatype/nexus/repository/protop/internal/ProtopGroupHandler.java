package org.sonatype.nexus.repository.protop.internal;

import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.group.GroupFacet;
import org.sonatype.nexus.repository.group.GroupHandler;
import org.sonatype.nexus.repository.http.HttpStatus;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Response;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @since 3.19
 */
public class ProtopGroupHandler extends GroupHandler {

    protected Map<Repository, Response> getResponses(final Context context,
                                                     final DispatchedRepositories dispatched,
                                                     final ProtopGroupFacet groupFacet) throws Exception {

        // get all and filter for HTTP OK responses
        Map<Repository, Response> responses = getAll(context, groupFacet.members(), dispatched);
        Map<Repository, Response> okResponses = new LinkedHashMap<>();
        responses.forEach((repository, response) -> {
            if (response.getStatus().getCode() == HttpStatus.OK) {
                okResponses.put(repository, response);
            }
        });
        return okResponses;
    }

    protected ProtopGroupFacet getGroupFacet(final Context context) {
        return DefaultGroovyMethods.asType(context.getRepository().facet(GroupFacet.class), ProtopGroupFacet.class);
    }
}
