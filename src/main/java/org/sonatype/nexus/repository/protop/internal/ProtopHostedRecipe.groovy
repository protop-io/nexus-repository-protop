package org.sonatype.nexus.repository.protop.internal

import org.sonatype.nexus.repository.Format
import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.Type
import org.sonatype.nexus.repository.http.HttpHandlers
import org.sonatype.nexus.repository.protop.internal.search.ProtopSearchFacetHosted
import org.sonatype.nexus.repository.types.HostedType
import org.sonatype.nexus.repository.view.ConfigurableViewFacet
import org.sonatype.nexus.repository.view.Router
import org.sonatype.nexus.repository.view.ViewFacet
import org.sonatype.nexus.repository.view.handlers.ContentHeadersHandler

import javax.annotation.Nonnull
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton

import static org.sonatype.nexus.repository.http.HttpMethods.*
/**
 * protop hosted repository recipe.
 *
 * @since 3.0
 */
@Named(ProtopHostedRecipe.NAME)
@Singleton
class ProtopHostedRecipe extends ProtopRecipeSupport {

    public static final String NAME = 'protop-hosted'

    @Inject
    ContentHeadersHandler contentHeadersHandler

    @Inject
    Provider<ProtopHostedFacet> protopHostedFacet

    @Inject
    Provider<ProtopHostedComponentMaintenanceImpl> protopHostedComponentMaintenanceProvider

    @Inject
    Provider<ProtopSearchFacetHosted> protopSearchFacet

    @Inject
    ProtopWhoamiHandler protopWhoamiHandler

    @Inject
    ProtopPingHandler pingHandler

    @Inject
    ProtopHostedRecipe(@Named(HostedType.NAME) final Type type,
                       @Named(ProtopFormat.NAME) final Format format) {
        super(type, format)
    }

    @Override
    void apply(@Nonnull final Repository repository) throws Exception {
        repository.attach(securityFacet.get())
        repository.attach(tokenFacet.get())
        repository.attach(configure(viewFacet.get()))
        repository.attach(searchFacet.get())
        repository.attach(storageFacet.get())
        repository.attach(attributesFacet.get())
        repository.attach(protopFacet.get())
        repository.attach(protopHostedFacet.get())
        repository.attach(protopHostedComponentMaintenanceProvider.get())
        repository.attach(protopSearchFacet.get())
    }

    /**
     * Configure {@link ViewFacet}.
     */
    private ViewFacet configure(final ConfigurableViewFacet facet) {
        Router.Builder builder = new Router.Builder()

        addBrowseUnsupportedRoute(builder)

        createUserRoutes(builder)

//        // GET /-/all (protop search)
//        builder.route(searchIndexMatcher()
//                .handler(timingHandler)
//                .handler(securityHandler)
//                .handler(ProtopHandlers.protopErrorHandler)
//                .handler(conditionalRequestHandler)
//                .handler(contentHeadersHandler)
//                .handler(unitOfWorkHandler)
//                .handler(lastDownloadedHandler)
//                .handler(ProtopHandlers.searchIndex)
//                .create())

        // GET /-/search (protop search)
        builder.route(searchMatcher()
                .handler(timingHandler)
                .handler(securityHandler)
                .handler(ProtopHandlers.protopErrorHandler)
                .handler(partialFetchHandler)
                .handler(conditionalRequestHandler)
                .handler(contentHeadersHandler)
                .handler(unitOfWorkHandler)
                .handler(ProtopHandlers.searchV1)
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

        // GET /packageOrg/packageName (protop install)
        builder.route(maybeVersionedPackageMatcher(GET, HEAD)
                .handler(timingHandler)
                .handler(securityHandler)
                .handler(ProtopHandlers.protopErrorHandler)
                .handler(conditionalRequestHandler)
                .handler(contentHeadersHandler)
                .handler(unitOfWorkHandler)
                .handler(lastDownloadedHandler)
                .handler(ProtopHandlers.getPackage)
                .create())

        // PUT /packageOrg/packageName (protop publish + protop deprecate)
        builder.route(maybeVersionedPackageMatcher(PUT)
                .handler(timingHandler)
                .handler(securityHandler)
                .handler(ProtopHandlers.protopErrorHandler)
                .handler(handlerContributor)
                .handler(conditionalRequestHandler)
                .handler(contentHeadersHandler)
                .handler(unitOfWorkHandler)
                .handler(ProtopHandlers.putPackage)
                .create())

        // PUT /packageOrg/packageName/-rev/revision (protop unpublish)
        builder.route(packageMatcherWithRevision(PUT)
                .handler(timingHandler)
                .handler(securityHandler)
                .handler(ProtopHandlers.protopErrorHandler)
                .handler(handlerContributor)
                .handler(conditionalRequestHandler)
                .handler(contentHeadersHandler)
                .handler(unitOfWorkHandler)
                .handler(ProtopHandlers.putPackage)
                .create())

        // DELETE /packageOrg/packageName (protop un-publish when last version deleted, protop 1.x)
        builder.route(maybeVersionedPackageMatcher(DELETE)
                .handler(timingHandler)
                .handler(securityHandler)
                .handler(ProtopHandlers.protopErrorHandler)
                .handler(conditionalRequestHandler)
                .handler(contentHeadersHandler)
                .handler(unitOfWorkHandler)
                .handler(ProtopHandlers.deletePackage)
                .create())

        // DELETE /packageOrg/packageName/-rev/revision (protop un-publish when last version deleted, newer protops)
        builder.route(packageMatcherWithRevision(DELETE)
                .handler(timingHandler)
                .handler(securityHandler)
                .handler(ProtopHandlers.protopErrorHandler)
                .handler(conditionalRequestHandler)
                .handler(contentHeadersHandler)
                .handler(unitOfWorkHandler)
                .handler(ProtopHandlers.deletePackage)
                .create())

        // GET /packageOrg/packageName/-/tarballName (protop install)
        builder.route(tarballMatcher(GET, HEAD)
                .handler(timingHandler)
                .handler(securityHandler)
                .handler(ProtopHandlers.protopErrorHandler)
                .handler(handlerContributor)
                .handler(conditionalRequestHandler)
                .handler(contentHeadersHandler)
                .handler(unitOfWorkHandler)
                .handler(lastDownloadedHandler)
                .handler(ProtopHandlers.getTarball)
                .create())

        // DELETE /packageOrg/packageName/-/tarballName (protop un-publish when some versions are left in place)
        builder.route(tarballMatcher(DELETE)
                .handler(timingHandler)
                .handler(securityHandler)
                .handler(ProtopHandlers.protopErrorHandler)
                .handler(conditionalRequestHandler)
                .handler(contentHeadersHandler)
                .handler(unitOfWorkHandler)
                .handler(ProtopHandlers.deleteTarball)
                .create())

        // DELETE /packageOrg/packageName/-/tarballName/-rev/revision (protop un-publish when some versions are left in place)
        builder.route(tarballMatcherWithRevision(DELETE)
                .handler(timingHandler)
                .handler(securityHandler)
                .handler(ProtopHandlers.protopErrorHandler)
                .handler(conditionalRequestHandler)
                .handler(contentHeadersHandler)
                .handler(unitOfWorkHandler)
                .handler(ProtopHandlers.deleteTarball)
                .create())

        // GET /-/package/packageName/dist-tags (protop dist-tag ls pkg)
        builder.route(distTagsMatcher(GET)
                .handler(timingHandler)
                .handler(securityHandler)
                .handler(ProtopHandlers.protopErrorHandler)
                .handler(unitOfWorkHandler)
                .handler(ProtopHandlers.getDistTags)
                .create())

        // PUT /-/package/packageName/dist-tags (protop dist-tag add pkg@version tag)
        builder.route(distTagsUpdateMatcher(PUT)
                .handler(timingHandler)
                .handler(securityHandler)
                .handler(ProtopHandlers.protopErrorHandler)
                .handler(unitOfWorkHandler)
                .handler(ProtopHandlers.putDistTags)
                .create())

        // DELETE /-/package/packageName/dist-tags (protop dist-tag rm pkg tag)
        builder.route(distTagsUpdateMatcher(DELETE)
                .handler(timingHandler)
                .handler(securityHandler)
                .handler(ProtopHandlers.protopErrorHandler)
                .handler(unitOfWorkHandler)
                .handler(ProtopHandlers.deleteDistTags)
                .create())

//    createUserRoutes(builder)

        builder.defaultHandlers(HttpHandlers.badRequest())

        facet.configure(builder.create())

        return facet
    }
}
