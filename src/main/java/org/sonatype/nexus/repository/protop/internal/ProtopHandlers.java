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
package org.sonatype.nexus.repository.protop.internal;

import java.io.IOException;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.sonatype.nexus.repository.IllegalOperationException;
import org.sonatype.nexus.repository.InvalidContentException;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.protop.internal.search.legacy.ProtopSearchIndexFacet;
import org.sonatype.nexus.repository.protop.internal.search.v1.ProtopSearchFacet;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Handler;
import org.sonatype.nexus.repository.view.Parameters;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher.State;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.text.Strings2.isBlank;
import static org.sonatype.nexus.repository.http.HttpMethods.PUT;

/**
 * protop protocol handlers.
 *
 * @since 3.0
 */
public final class ProtopHandlers {

  private ProtopHandlers() {
    // no op
  }

  private static final Logger log = LoggerFactory.getLogger(ProtopHandlers.class);

  static final String T_PACKAGE_ORG = "packageOrg";

  static final String T_PACKAGE_NAME = "packageName";

  static final String T_PACKAGE_VERSION = "packageVersion";

  static final String T_PACKAGE_TAG = "packageTag";

  static final String T_REVISION = "revision";

  static final String T_TARBALL_NAME = "tarballName";

  static final String T_USERNAME = "userName";

  static final String USER_LOGIN_PREFIX = "/-/user/org.couchdb.user:";

  static final String T_TOKEN = "token";

  @Nonnull
  static ProtopProjectId projectId(final TokenMatcher.State state) {
    checkNotNull(state);

    String projectOrg = state.getTokens().get(T_PACKAGE_ORG);
    checkNotNull(projectOrg);

    String projectName = state.getTokens().get(T_PACKAGE_NAME);
    checkNotNull(projectName);

    // TODO figure out how to re-integrate this
    String version = state.getTokens().get(T_PACKAGE_VERSION);
    if (!isBlank(version)) {
//      name += "-" + version;
      log.info("TODO - use the version requested: " + version);
    }

    return new ProtopProjectId(projectOrg, projectName);
  }

  @Nonnull
  static String tarballName(final TokenMatcher.State state) {
    checkNotNull(state);
    String tarballName = state.getTokens().get(T_TARBALL_NAME);
    checkNotNull(tarballName);
    return tarballName;
  }

  @Nullable
  static DateTime indexSince(final Parameters parameters) {
    // protop "incremental" index support: tells when it did last updated index
    // GET /-/all/since?stale=update_after&startkey=1441712501000
    if (parameters != null && "update_after".equals(parameters.get("stale"))) {
      String tsStr = parameters.get("startkey");
      if (!isBlank(tsStr)) {
        try {
          return new DateTime(Long.parseLong(tsStr));
        }
        catch (NumberFormatException e) {
          // ignore
        }
      }
    }
    return null;
  }

  @Nullable
  private static String version(final TokenMatcher.State state) {
    checkNotNull(state);
    return state.getTokens().get(T_PACKAGE_VERSION);
  }

  @Nullable
  private static String revision(final TokenMatcher.State state) {
    checkNotNull(state);
    return state.getTokens().get(T_REVISION);
  }

  static Handler protopErrorHandler = new Handler()
  {
    @Nonnull
    @Override
    public Response handle(@Nonnull final Context context) throws Exception {
      try {
        return context.proceed();
      }
      catch (IllegalOperationException | IllegalArgumentException e) {
        Response error = ProtopResponses.badRequest(e.getMessage());
        log.warn("Error: {} {}: {} - {}",
            context.getRequest().getAction(),
            context.getRequest().getPath(),
            error.getStatus(),
            e.getMessage(),
            e);
        return error;
      }
      catch (InvalidContentException e) {
        Response error;
        if (PUT.equals(context.getRequest().getAction())) {
          error = ProtopResponses.badRequest(e.getMessage());
        } else {
          error = ProtopResponses.notFound(e.getMessage());
        }
        log.warn("Error: {} {}: {} - {}",
            context.getRequest().getAction(),
            context.getRequest().getPath(),
            error.getStatus(),
            e.getMessage(),
            e);
        return error;
      }
    }
  };

  static Handler getPackage = new Handler() {

    @Nonnull
    @Override
    public Response handle(@Nonnull final Context context) throws Exception {
      log.info("Get package");

      State state = context.getAttributes().require(TokenMatcher.State.class);
      log.info("State: {}", state.getTokens());

      Repository repository = context.getRepository();
      log.debug("[getPackage] repository: {} tokens: {}", repository.getName(), state.getTokens());

      ProtopProjectId packageId = projectId(state);
      Content content = repository.facet(ProtopHostedFacet.class)
          .getPackage(packageId);
      if (content != null) {
        return ProtopResponses.ok(content);
      }
      else {
        return ProtopResponses.packageNotFound(packageId);
      }
    }
  };

  static Handler postPackage = new Handler() {

    @Nonnull
    @Override
    public Response handle(@Nonnull final Context context) throws Exception {
      State state = context.getAttributes().require(TokenMatcher.State.class);
      Repository repository = context.getRepository();
      log.debug("[putProject] repository: {} tokens: {}", repository.getName(), state.getTokens());

      repository.facet(ProtopHostedFacet.class)
              .postProject(projectId(state), version(state), revision(state), context.getRequest().getPayload());
      return ProtopResponses.ok();
    }
  };

  static Handler putPackage = new Handler() {

    @Nonnull
    @Override
    public Response handle(@Nonnull final Context context) throws Exception {
      State state = context.getAttributes().require(TokenMatcher.State.class);
      Repository repository = context.getRepository();
      log.debug("[putProject] repository: {} tokens: {}", repository.getName(), state.getTokens());

      repository.facet(ProtopHostedFacet.class)
          .putProject(projectId(state), revision(state), context.getRequest().getPayload());
      return ProtopResponses.ok();
    }
  };

  static Handler deletePackage = new Handler() {

    @Nonnull
    @Override
    public Response handle(@Nonnull final Context context) throws Exception {
      State state = context.getAttributes().require(TokenMatcher.State.class);
      Repository repository = context.getRepository();
      log.debug("[deletePackage] repository: {} tokens: {}", repository.getName(), state.getTokens());

      ProtopProjectId packageId = projectId(state);
      Set<String> deleted = repository.facet(ProtopHostedFacet.class).deletePackage(packageId, revision(state));
      if (!deleted.isEmpty()) {
        return ProtopResponses.ok();
      }
      else {
        return ProtopResponses.packageNotFound(packageId);
      }
    }
  };

  static Handler getTarball = new Handler() {

    @Nonnull
    @Override
    public Response handle(@Nonnull final Context context) throws Exception {
      State state = context.getAttributes().require(TokenMatcher.State.class);
      Repository repository = context.getRepository();
      log.debug("[getTarball] repository: {} tokens: {}", repository.getName(), state.getTokens());

      ProtopProjectId packageId = projectId(state);
      String tarballName = tarballName(state);
      Content content = repository.facet(ProtopHostedFacet.class).getTarball(packageId, tarballName);
      if (content != null) {
        return ProtopResponses.ok(content);
      }
      else {
        return ProtopResponses.tarballNotFound(packageId, tarballName);
      }
    }
  };

  static Handler deleteTarball = new Handler() {

    @Nonnull
    @Override
    public Response handle(@Nonnull final Context context) throws Exception {
      State state = context.getAttributes().require(TokenMatcher.State.class);
      Repository repository = context.getRepository();
      log.debug("[deleteTarball] repository: {} tokens: {}", repository.getName(), state.getTokens());

      ProtopProjectId packageId = projectId(state);
      String tarballName = tarballName(state);
      Set<String> deleted = repository.facet(ProtopHostedFacet.class).deleteTarball(packageId, tarballName);
      if (!deleted.isEmpty()) {
        return ProtopResponses.ok();
      }
      else {
        return ProtopResponses.tarballNotFound(packageId, tarballName);
      }
    }
  };

  /**
   * @deprecated No longer actively used by protop upstream, replaced by v1 search api (NEXUS-13150).
   */
  @Deprecated
  static Handler searchIndex = new Handler() {

    @Nonnull
    @Override
    public Response handle(@Nonnull final Context context) throws Exception {
      Repository repository = context.getRepository();
      Parameters parameters = context.getRequest().getParameters();
      log.debug("[searchIndex] repository: {} parameters: {}", repository.getName(), parameters);

      return ProtopResponses.ok(repository.facet(ProtopSearchIndexFacet.class).searchIndex(indexSince(parameters)));
    }
  };

  static Handler searchV1 = new Handler() {

    @Nonnull
    @Override
    public Response handle(@Nonnull final Context context) throws Exception {
      Repository repository = context.getRepository();
      Parameters parameters = context.getRequest().getParameters();
      log.debug("[searchV1] repository: {} parameters: {}", repository.getName(), parameters);

      return ProtopResponses.ok(repository.facet(ProtopSearchFacet.class).searchV1(parameters));
    }
  };

  static Handler createToken = new Handler() {

    @Nonnull
    @Override
    public Response handle(@Nonnull final Context context) throws Exception {
      State state = context.getAttributes().require(TokenMatcher.State.class);
      Repository repository = context.getRepository();
      log.debug("[createToken] repository: {} tokens: {}", repository.getName(), state.getTokens());

      return repository.facet(ProtopTokenFacet.class).login(context);
    }
  };

  static Handler deleteToken = new Handler() {

    @Nonnull
    @Override
    public Response handle(@Nonnull final Context context) throws Exception {
      State state = context.getAttributes().require(TokenMatcher.State.class);
      Repository repository = context.getRepository();
      log.debug("[deleteToken] repository: {} tokens: {}", repository.getName(), state.getTokens());

      return repository.facet(ProtopTokenFacet.class).logout(context);
    }
  };

  static Handler getDistTags = new Handler() {

    @Nonnull
    @Override
    public Response handle(@Nonnull final Context context) throws Exception {
      State state = context.getAttributes().require(TokenMatcher.State.class);
      Repository repository = context.getRepository();
      log.debug("[getPackage] repository: {} tokens: {}", repository.getName(), state.getTokens());

      ProtopProjectId packageId = projectId(state);
      Content content = repository.facet(ProtopHostedFacet.class)
          .getDistTags(packageId);
      if (content != null) {
        return ProtopResponses.ok(content);
      }
      else {
        return ProtopResponses.packageNotFound(packageId);
      }
    }
  };

  static Handler putDistTags = new Handler() {

    @Nonnull
    @Override
    public Response handle(@Nonnull final Context context) throws Exception {
      State state = context.getAttributes().require(TokenMatcher.State.class);
      Repository repository = context.getRepository();
      log.debug("[putDistTags] repository: {} tokens: {}", repository.getName(), state.getTokens());

      try {
        repository.facet(ProtopHostedFacet.class)
            .putDistTags(projectId(state), state.getTokens().get(T_PACKAGE_TAG), context.getRequest().getPayload());
        return ProtopResponses.ok();
      }
      catch (IOException e) { //NOSONAR
        return ProtopResponses.badRequest(e.getMessage());
      }
    }
  };

  static Handler deleteDistTags = new Handler() {

    @Nonnull
    @Override
    public Response handle(@Nonnull final Context context) throws Exception {
      State state = context.getAttributes().require(TokenMatcher.State.class);
      Repository repository = context.getRepository();
      log.debug("[putDistTags] repository: {} tokens: {}", repository.getName(), state.getTokens());

      try {
        repository.facet(ProtopHostedFacet.class)
            .deleteDistTags(projectId(state), state.getTokens().get(T_PACKAGE_TAG), context.getRequest().getPayload());
        return ProtopResponses.ok();
      }
      catch (IOException e) { //NOSONAR
        return ProtopResponses.badRequest(e.getMessage());
      }
    }
  };
}
