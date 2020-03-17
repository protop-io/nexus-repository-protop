package org.sonatype.nexus.repository.protop.internal

import org.sonatype.nexus.repository.Format
import org.sonatype.nexus.repository.RecipeSupport
import org.sonatype.nexus.repository.Type
import org.sonatype.nexus.repository.attributes.AttributesFacet
import org.sonatype.nexus.repository.http.PartialFetchHandler
import org.sonatype.nexus.repository.routing.RoutingRuleHandler
import org.sonatype.nexus.repository.search.SearchFacet
import org.sonatype.nexus.repository.security.SecurityHandler
import org.sonatype.nexus.repository.storage.StorageFacet
import org.sonatype.nexus.repository.storage.UnitOfWorkHandler
import org.sonatype.nexus.repository.view.ConfigurableViewFacet
import org.sonatype.nexus.repository.view.Route.Builder
import org.sonatype.nexus.repository.view.Router
import org.sonatype.nexus.repository.view.handlers.ConditionalRequestHandler
import org.sonatype.nexus.repository.view.handlers.HandlerContributor
import org.sonatype.nexus.repository.view.handlers.LastDownloadedHandler
import org.sonatype.nexus.repository.view.handlers.TimingHandler
import org.sonatype.nexus.repository.view.matchers.ActionMatcher
import org.sonatype.nexus.repository.view.matchers.LiteralMatcher
import org.sonatype.nexus.repository.view.matchers.logic.LogicMatchers
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher

import javax.inject.Inject
import javax.inject.Provider

import static org.sonatype.nexus.repository.http.HttpMethods.*

/**
 * Common configuration aspects for protop repositories.
 * TODO - jeffery fix all of these
 */
abstract class ProtopRecipeSupport extends RecipeSupport {

    @Inject
    Provider<ProtopSecurityFacet> securityFacet

    @Inject
    Provider<ProtopFacetImpl> protopFacet

    @Inject
    Provider<ProtopTokenFacet> tokenFacet

    @Inject
    Provider<ConfigurableViewFacet> viewFacet

    @Inject
    Provider<StorageFacet> storageFacet

    @Inject
    Provider<AttributesFacet> attributesFacet

    @Inject
    Provider<SearchFacet> searchFacet

    @Inject
    TimingHandler timingHandler

    @Inject
    RoutingRuleHandler routingHandler

    @Inject
    SecurityHandler securityHandler

    @Inject
    PartialFetchHandler partialFetchHandler

    @Inject
    ConditionalRequestHandler conditionalRequestHandler

    @Inject
    UnitOfWorkHandler unitOfWorkHandler

    @Inject
    HandlerContributor handlerContributor

    @Inject
    LastDownloadedHandler lastDownloadedHandler

    protected ProtopRecipeSupport(final Type type, final Format format) {
        super(type, format)
    }

    /**
     * Creates common user related routes to support {@code protop adduser} and {@code protop logout} commands.
     */
    void createUserRoutes(Router.Builder builder) {
        // PUT /-/user/org.couchdb.user:userName (protop adduser)
        // Note: this happens as anon! No securityHandler here
        builder.route(userMatcher(PUT)
                .handler(timingHandler)
                .handler(ProtopHandlers.protopErrorHandler)
                .handler(ProtopHandlers.createToken)
                .create())

        // DELETE /-/user/token/{token} (protop logout)
        builder.route(tokenMatcher(DELETE)
                .handler(timingHandler)
                .handler(securityHandler)
                .handler(ProtopHandlers.protopErrorHandler)
                .handler(ProtopHandlers.deleteToken)
                .create())
    }

//    /**
//     * Matcher for protop package search index.
//     */
//    static Builder searchIndexMatcher() {
//        new Builder().matcher(
//                LogicMatchers.and(
//                        new ActionMatcher(GET),
//                        LogicMatchers.or(
//                                new LiteralMatcher('/-/all'),
//                                new LiteralMatcher('/-/all/since')
//                        )
//                )
//        )
//    }

    /**
     * Matcher for protop package search.
     */
    static Builder searchMatcher() {
        new Builder().matcher(
                LogicMatchers.and(
                        new ActionMatcher(GET),
                        new LiteralMatcher('/-/search')
                )
        )
    }

    /**
     * Matcher for protop whoami command.
     */
    static Builder whoamiMatcher() {
        new Builder().matcher(
                LogicMatchers.and(
                        new ActionMatcher(GET),
                        new LiteralMatcher('/-/whoami')
                )
        )
    }

    /**
     * Matcher for protop ping command.
     */
    static Builder pingMatcher() {
        new Builder().matcher(
                LogicMatchers.and(
                        new ActionMatcher(GET),
                        new LiteralMatcher('/-/ping')
                )
        )
    }

    /**
     * Matcher for protop package metadata.
     */
    static Builder maybeVersionedPackageMatcher(String... httpMethod) {
        new Builder().matcher(
                LogicMatchers.and(
                        new ActionMatcher(httpMethod),
                        LogicMatchers.or(
                                new TokenMatcher('/{' + ProtopHandlers.T_PACKAGE_ORG + '}/{' + ProtopHandlers.T_PACKAGE_NAME + '}'),
                                new TokenMatcher('/{' + ProtopHandlers.T_PACKAGE_ORG + '}/{' + ProtopHandlers.T_PACKAGE_NAME + '}/{' +
                                        ProtopHandlers.T_PACKAGE_VERSION + '}')
                        )
                )
        )
    }

    /**
     * Matcher for protop package metadata.
     */
    static Builder packageMatcherWithRevision(String httpMethod) {
        new Builder().matcher(
                LogicMatchers.and(
                        new ActionMatcher(httpMethod),
                        new TokenMatcher('/{' + ProtopHandlers.T_PACKAGE_ORG + '}/{' + ProtopHandlers.T_PACKAGE_NAME + '}/-rev/{' +
                                ProtopHandlers.T_REVISION + '}')
                )
        )
    }

    /**
     * Matcher for protop package tarballs.
     */
    static Builder tarballMatcher(String... httpMethod) {
        new Builder().matcher(
                LogicMatchers.and(
                        new ActionMatcher(httpMethod),
                        new TokenMatcher('/{' + ProtopHandlers.T_PACKAGE_ORG + '}/{' + ProtopHandlers.T_PACKAGE_NAME + '}/-/{' +
                                ProtopHandlers.T_TARBALL_NAME + '}')
                )
        )
    }

    /**
     * Matcher for protop package dist-tags.
     */
    static Builder distTagsMatcher(String httpMethod) {
        new Builder().matcher(
                LogicMatchers.and(
                        new ActionMatcher(httpMethod),
                        new TokenMatcher('/-/package/{' + ProtopHandlers.T_PACKAGE_ORG + '}/{' +
                                ProtopHandlers.T_PACKAGE_NAME + '}/dist-tags')
                )
        )
    }

    /**
     * Matcher for protop package dist-tags.
     */
    static Builder distTagsUpdateMatcher(String httpMethod) {
        new Builder().matcher(
                LogicMatchers.and(
                        new ActionMatcher(httpMethod),
                        new TokenMatcher('/-/package/{' + ProtopHandlers.T_PACKAGE_ORG + '}/{' + ProtopHandlers.T_PACKAGE_NAME +
                                '}/dist-tags/{' + ProtopHandlers.T_PACKAGE_TAG + '}'),
                )
        )
    }

    /**
     * Matcher for protop package tarballs.
     */
    static Builder tarballMatcherWithRevision(String httpMethod) {
        new Builder().matcher(
                LogicMatchers.and(
                        new ActionMatcher(httpMethod),
                        new TokenMatcher('/{' + ProtopHandlers.T_PACKAGE_ORG + '}/{' + ProtopHandlers.T_PACKAGE_NAME + '}/-/{' +
                                ProtopHandlers.T_TARBALL_NAME + '}/-rev/{' + ProtopHandlers.T_REVISION + '}')
                )
        )
    }

    /**
     * Matcher for {@code protop adduser}.
     */
    static Builder userMatcher(String httpMethod) {
        new Builder().matcher(
                LogicMatchers.and(
                        new ActionMatcher(httpMethod),
                        new TokenMatcher(ProtopHandlers.USER_LOGIN_PREFIX + '{' + ProtopHandlers.T_USERNAME + '}')
                )
        )
    }

    /**
     * Matcher for {@code protop logout}.
     */
    static Builder tokenMatcher(String httpMethod) {
        new Builder().matcher(
                LogicMatchers.and(
                        new ActionMatcher(httpMethod),
                        new TokenMatcher('/-/user/token/{' + ProtopHandlers.T_TOKEN + '}')
                )
        )
    }
}
