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
package org.sonatype.nexus.repository.protop.internal.security;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.protop.security.ProtopToken;

import org.sonatype.nexus.security.UserPrincipalsHelper;
import org.sonatype.nexus.security.authc.apikey.ApiKeyStore;
import org.sonatype.nexus.security.token.BearerTokenRealm;

import org.apache.shiro.realm.AuthenticatingRealm;
import org.apache.shiro.subject.Subject;
import org.eclipse.sisu.Description;

/**
 * {@link AuthenticatingRealm} that maps protop tokens to valid {@link Subject}s.
 *
 * @since 3.0
 */
@Named(ProtopToken.NAME)
@Singleton
@Description("protop Bearer Token Realm")
public final class ProtopTokenRealm
    extends BearerTokenRealm
{
  @Inject
  public ProtopTokenRealm(final ApiKeyStore keyStore, final UserPrincipalsHelper principalsHelper) {
    super(keyStore, principalsHelper, ProtopToken.NAME);
  }
}
