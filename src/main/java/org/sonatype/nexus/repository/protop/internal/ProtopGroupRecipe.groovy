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

import org.sonatype.nexus.repository.Facet
import org.sonatype.nexus.repository.Format
import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.Type
import org.sonatype.nexus.repository.group.GroupHandler
import org.sonatype.nexus.repository.http.HttpHandlers
import org.sonatype.nexus.repository.protop.internal.search.legacy.ProtopSearchIndexFacetGroup
import org.sonatype.nexus.repository.protop.internal.search.v1.ProtopSearchGroupHandler
import org.sonatype.nexus.repository.types.GroupType
import org.sonatype.nexus.repository.view.ConfigurableViewFacet
import org.sonatype.nexus.repository.view.Router
import org.sonatype.nexus.repository.view.handlers.ContentHeadersHandler

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
  Provider<ProtopSearchIndexFacetGroup> protopSearchIndexFacet

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
    repository.attach(protopSearchIndexFacet.get())
    repository.attach(protopFacet.get())
    repository.attach(configure(viewFacet.get()))
  }

  Facet configure(final ConfigurableViewFacet facet) {
    Router.Builder builder = new Router.Builder()

    addBrowseUnsupportedRoute(builder)

    // GET /-/all (protop search)
    builder.route(searchIndexMatcher()
        .handler(timingHandler)
        .handler(securityHandler)
        .handler(unitOfWorkHandler)
        .handler(ProtopHandlers.searchIndex)
        .create())

    // GET /-/v1/search (protop v1 search)
    builder.route(searchV1Matcher()
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
