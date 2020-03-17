package org.sonatype.nexus.repository.protop.internal

import org.sonatype.nexus.repository.Facet
import org.sonatype.nexus.repository.Format
import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.Type
import org.sonatype.nexus.repository.group.GroupHandler
import org.sonatype.nexus.repository.http.HttpHandlers
import org.sonatype.nexus.repository.protop.internal.search.ProtopSearchGroupHandler
import org.sonatype.nexus.repository.types.GroupType
import org.sonatype.nexus.repository.view.ConfigurableViewFacet
import org.sonatype.nexus.repository.view.Router
import org.sonatype.nexus.repository.view.handlers.ContentHeadersHandler

import javax.annotation.Nonnull
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton

import static org.sonatype.nexus.repository.http.HttpMethods.GET
import static org.sonatype.nexus.repository.http.HttpMethods.HEAD

/**
 * protop group repository recipe.
 */
@Named(ProtopGroupRecipe.NAME)
@Singleton
class ProtopGroupRecipe extends ProtopRecipeSupport {

    public static final String NAME = 'protop-group'

    @Inject
    Provider<ProtopGroupFacet> groupFacet

    @Inject
    ProtopGroupPackageHandler packageHandler

    @Inject
    ProtopGroupDistTagsHandler distTagsHandler

    @Inject
    GroupHandler tarballHandler

    @Inject
    ProtopSearchGroupHandler searchHandler

    @Inject
    ContentHeadersHandler contentHeadersHandler

    @Inject
    ProtopWhoamiHandler protopWhoamiHandler

    @Inject
    ProtopPingHandler pingHandler

    @Inject
    ProtopGroupRecipe(@Named(GroupType.NAME) final Type type,
                      @Named(ProtopFormat.NAME) final Format format) {
        super(type, format)
    }

    @Override
    void apply(@Nonnull final Repository repository) throws Exception {
        repository.attach(groupFacet.get())
        repository.attach(storageFacet.get())
        repository.attach(attributesFacet.get())
        repository.attach(securityFacet.get())
        repository.attach(tokenFacet.get())
//        repository.attach(protopSearchIndexFacet.get())
        repository.attach(protopFacet.get())
        repository.attach(configure(viewFacet.get()))
    }

    Facet configure(final ConfigurableViewFacet facet) {
        Router.Builder builder = new Router.Builder()

        addBrowseUnsupportedRoute(builder)

//        // GET /-/all (protop search)
//        builder.route(searchIndexMatcher()
//                .handler(timingHandler)
//                .handler(securityHandler)
//                .handler(unitOfWorkHandler)
//                .handler(ProtopHandlers.searchIndex)
//                .create())

        // GET /-/search (protop search)
        builder.route(searchMatcher()
                .handler(timingHandler)
                .handler(securityHandler)
                .handler(unitOfWorkHandler)
                .handler(searchHandler)
                .create())

        // GET /-/whoami
        builder.route(whoamiMatcher()
                .handler(timingHandler)
                .handler(protopWhoamiHandler)
                .create())

        // GET /-/ping
        builder.route(pingMatcher()
                .handler(timingHandler)
                .handler(pingHandler)
                .create())

        // GET /packageName (protop install)
        builder.route(maybeVersionedPackageMatcher(GET, HEAD)
                .handler(timingHandler)
                .handler(securityHandler)
                .handler(unitOfWorkHandler)
                .handler(conditionalRequestHandler)
                .handler(contentHeadersHandler)
                .handler(lastDownloadedHandler)
                .handler(packageHandler)
                .create())

        // GET /packageName/-/tarballName (protop install)
        builder.route(tarballMatcher(GET, HEAD)
                .handler(timingHandler)
                .handler(securityHandler)
                .handler(handlerContributor)
                .handler(tarballHandler)
                .create())

        // GET /-/package/packageName/dist-tags (protop dist-tag ls pkg)
        builder.route(distTagsMatcher(GET)
                .handler(timingHandler)
                .handler(securityHandler)
                .handler(unitOfWorkHandler)
                .handler(lastDownloadedHandler)
                .handler(distTagsHandler)
                .create())

        createUserRoutes(builder)

        builder.defaultHandlers(HttpHandlers.badRequest())

        facet.configure(builder.create())

        return facet
    }
}
