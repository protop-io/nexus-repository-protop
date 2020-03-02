/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.repository.protop.internal

import javax.annotation.Nonnull
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton

import org.sonatype.nexus.repository.Format
import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.Type
import org.sonatype.nexus.repository.cache.NegativeCacheFacet
import org.sonatype.nexus.repository.http.HttpHandlers
import org.sonatype.nexus.repository.httpclient.HttpClientFacet
import org.sonatype.nexus.repository.protop.internal.ProtopProxyFacetImpl.ProxyTarget
import org.sonatype.nexus.repository.protop.internal.search.legacy.ProtopSearchIndexFacetProxy
import org.sonatype.nexus.repository.protop.internal.search.v1.ProtopSearchFacetProxy
import org.sonatype.nexus.repository.purge.PurgeUnusedFacet
import org.sonatype.nexus.repository.storage.SingleAssetComponentMaintenance
import org.sonatype.nexus.repository.types.ProxyType
import org.sonatype.nexus.repository.view.ConfigurableViewFacet
import org.sonatype.nexus.repository.view.Context
import org.sonatype.nexus.repository.view.Router
import org.sonatype.nexus.repository.view.ViewFacet
import org.sonatype.nexus.repository.view.handlers.ContentHeadersHandler

import static org.sonatype.nexus.repository.http.HttpMethods.GET
import static org.sonatype.nexus.repository.http.HttpMethods.HEAD

/**
 * protop proxy repository recipe.
 *
 * @since 3.0
 */
@Named(ProtopProxyRecipe.NAME)
@Singleton
class ProtopProxyRecipe
    extends ProtopRecipeSupport
{
  public static final String NAME = 'protop-proxy'

  @Inject
  Provider<ProtopProxyFacetImpl> proxyFacet

  @Inject
  Provider<HttpClientFacet> httpClientFacet

  @Inject
  Provider<NegativeCacheFacet> negativeCacheFacet

  @Inject
  Provider<ProtopSearchIndexFacetProxy> protopSearchIndexFacet

  @Inject
  Provider<ProtopSearchFacetProxy> protopSearchFacet

  @Inject
  Provider<PurgeUnusedFacet> purgeUnusedFacet

  @Inject
  Provider<ProtopProxyCacheInvalidatorFacetImpl> protopProxyCacheInvalidatorFacet

  @Inject
  Provider<SingleAssetComponentMaintenance> singleAssetComponentMaintenanceProvider

  @Inject
  ProtopNegativeCacheHandler negativeCacheHandler

  @Inject
  ContentHeadersHandler contentHeadersHandler

  @Inject
  ProtopProxyHandler proxyHandler

  @Inject
  ProtopWhoamiHandler protopWhoamiHandler

  @Inject
  ProtopPingHandler pingHandler

  @Inject
  ProtopProxyRecipe(@Named(ProxyType.NAME) final Type type,
                 @Named(ProtopFormat.NAME) final Format format)
  {
    super(type, format)
  }

  @Override
  void apply(@Nonnull final Repository repository) throws Exception {
    repository.attach(securityFacet.get())
    repository.attach(tokenFacet.get())
    repository.attach(configure(viewFacet.get()))
    repository.attach(httpClientFacet.get())
    repository.attach(negativeCacheFacet.get())
    repository.attach(protopFacet.get())
    repository.attach(proxyFacet.get())
    repository.attach(storageFacet.get())
    repository.attach(attributesFacet.get())
    repository.attach(searchFacet.get())
    repository.attach(protopSearchIndexFacet.get())
    repository.attach(protopSearchFacet.get())
    repository.attach(singleAssetComponentMaintenanceProvider.get())
    repository.attach(purgeUnusedFacet.get())
    repository.attach(protopProxyCacheInvalidatorFacet.get());
  }

  /**
   * Configure {@link ViewFacet}.
   */
  private ViewFacet configure(final ConfigurableViewFacet facet) {
    Router.Builder builder = new Router.Builder()

    addBrowseUnsupportedRoute(builder)

    // GET /-/all (protop search)
    builder.route(searchIndexMatcher()
        .handler(timingHandler)
        .handler(securityHandler)
        .handler(ProtopHandlers.protopErrorHandler)
        .handler(negativeCacheHandler)
        .handler(partialFetchHandler)
        .handler(conditionalRequestHandler)
        .handler(contentHeadersHandler)
        .handler(proxyTargetHandler.rcurry(ProxyTarget.SEARCH_INDEX))
        .handler(unitOfWorkHandler)
        .handler(lastDownloadedHandler)
        .handler(ProtopHandlers.searchIndex)
        .create())

    // GET /-/v1/search (protop v1 search)
    builder.route(searchV1Matcher()
        .handler(timingHandler)
        .handler(securityHandler)
        .handler(ProtopHandlers.protopErrorHandler)
        .handler(negativeCacheHandler)
        .handler(partialFetchHandler)
        .handler(conditionalRequestHandler)
        .handler(contentHeadersHandler)
        .handler(proxyTargetHandler.rcurry(ProxyTarget.SEARCH_V1_RESULTS))
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

    // GET /packageName (protop install)
    builder.route(maybeVersionedPackageMatcher(GET, HEAD)
        .handler(timingHandler)
        .handler(securityHandler)
        .handler(routingHandler)
        .handler(ProtopHandlers.protopErrorHandler)
        .handler(negativeCacheHandler)
        .handler(partialFetchHandler)
        .handler(conditionalRequestHandler)
        .handler(contentHeadersHandler)
        .handler(proxyTargetHandler.rcurry(ProxyTarget.PACKAGE))
        .handler(unitOfWorkHandler)
        .handler(lastDownloadedHandler)
        .handler(proxyHandler)
        .create())

    // GET /packageName/-/tarballName (protop install)
    builder.route(tarballMatcher(GET, HEAD)
        .handler(timingHandler)
        .handler(securityHandler)
        .handler(routingHandler)
        .handler(ProtopHandlers.protopErrorHandler)
        .handler(handlerContributor)
        .handler(negativeCacheHandler)
        .handler(partialFetchHandler)
        .handler(conditionalRequestHandler)
        .handler(contentHeadersHandler)
        .handler(proxyTargetHandler.rcurry(ProxyTarget.TARBALL))
        .handler(unitOfWorkHandler)
        .handler(lastDownloadedHandler)
        .handler(proxyHandler)
        .create())

    // GET /-/package/packageName/dist-tags
    builder.route(distTagsMatcher(GET)
        .handler(timingHandler)
        .handler(securityHandler)
        .handler(routingHandler)
        .handler(ProtopHandlers.protopErrorHandler)
        .handler(handlerContributor)
        .handler(contentHeadersHandler)
        .handler(proxyTargetHandler.rcurry(ProxyTarget.DIST_TAGS))
        .handler(unitOfWorkHandler)
        .handler(proxyHandler)
        .create())

    createUserRoutes(builder)

    builder.defaultHandlers(HttpHandlers.notFound())

    facet.configure(builder.create())

    return facet
  }

  Closure proxyTargetHandler = {
    Context context, ProxyTarget value ->
      context.attributes.set(ProxyTarget, value)
      return context.proceed()
  }
}
