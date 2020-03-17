
package org.sonatype.nexus.repository.protop.internal

import org.sonatype.goodies.testsupport.TestSupport
import org.sonatype.nexus.common.collect.AttributesMap
import org.sonatype.nexus.common.collect.NestedAttributesMap
import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.protop.internal.ProtopProxyFacetImpl.ProxyTarget
import org.sonatype.nexus.repository.protop.ProtopFacet
import org.sonatype.nexus.repository.view.Content
import org.sonatype.nexus.repository.view.Context
import org.sonatype.nexus.repository.view.Parameters
import org.sonatype.nexus.repository.view.Payload
import org.sonatype.nexus.repository.view.Request
import org.sonatype.nexus.repository.view.Response
import org.sonatype.nexus.repository.view.ViewFacet
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher

import org.apache.shiro.authz.AuthorizationException
import org.junit.Before
import org.junit.Test
import org.mockito.Mock

import static java.nio.charset.StandardCharsets.UTF_8
import static org.hamcrest.Matchers.is
import static org.junit.Assert.assertThat
import static org.mockito.Matchers.any
import static org.mockito.Matchers.eq
import static org.mockito.Matchers.same
import static org.mockito.Mockito.doAnswer
import static org.mockito.Mockito.doReturn
import static org.mockito.Mockito.doThrow
import static org.mockito.Mockito.never
import static org.mockito.Mockito.spy
import static org.mockito.Mockito.verify
import static org.sonatype.nexus.repository.protop.internal.ProtopProxyFacetImpl.ProxyTarget.SEARCH_RESULTS
import static org.sonatype.nexus.repository.protop.internal.ProtopProxyFacetImpl.ProxyTarget.TARBALL

class ProtopProxyFacetImplTest extends TestSupport {

  static final String PATH = '/some/path'

  static final String DIST_TARBALL = '/org/pkg/org-pkg-1.0.0.tar.gz'

  static final String TARBALL_NAME = 'org-pkg-1.0.0.tar.gz'

  static final String PACKAGE_ORG = 'org'
  static final String PACKAGE_NAME = 'pkg'

  static final String SEARCH_TEXT = 'jquery hello world'

  static final String SEARCH_ENCODED_TEXT = 'jquery+hello+world'

  static final String SEARCH_RESULT_SIZE = '20'

  static final TOKENS = [packageOrg: PACKAGE_ORG, packageName: PACKAGE_NAME, tarballName: TARBALL_NAME]

  static final PACKAGE_VERSION = [name: [dist: [tarball: DIST_TARBALL]]]

  @Mock
  Repository repository

  @Mock
  ViewFacet viewFacet

  @Mock
  ProtopFacet protopFacet

  @Mock
  TokenMatcher.State state

  @Mock
  Request request

  @Mock
  AttributesMap contextAttributes

  @Mock
  Response response

  @Mock
  Payload payload

  @Mock
  Context context

  @Mock
  Content content

  ProtopProxyFacetImpl underTest

  @Before
  void setUp() {
    underTest = spy(new ProtopProxyFacetImpl())
  }

  @Test
  void 'when a repository makes a request to itself to retrieve package root information, it should use the context'() {
    doReturn(repository).when(underTest).getRepository()
    doReturn(null).when(underTest).getCachedContent(context)
    doReturn(content).when(underTest).fetch(any(String), same(context), any(Content))
    doReturn(content).when(underTest).store(same(context), same(content))
    doAnswer({ new NestedAttributesMap('name', PACKAGE_VERSION) }).when(underTest).
        retrievePackageVersionTx(any(ProtopProjectId), eq(TARBALL_NAME))

    doReturn(request).when(context).getRequest()
    doReturn(PATH).when(request).getPath()

    doReturn(protopFacet).when(repository).facet(ProtopFacet)
    doReturn(viewFacet).when(repository).facet(ViewFacet)
    doReturn(response).when(viewFacet).dispatch(any(Request), same(context))
    doReturn(payload).when(response).getPayload()
    doAnswer({ return new ByteArrayInputStream('{}'.getBytes(UTF_8)) }).when(payload).openInputStream()

    doReturn(contextAttributes).when(context).getAttributes()
    doReturn(TARBALL).when(contextAttributes).require(ProxyTarget)
    doReturn(state).when(contextAttributes).require(TokenMatcher.State)
    doReturn(TOKENS).when(state).getTokens()

    assertThat(underTest.get(context), is(content))
    verify(viewFacet).dispatch(any(Request), same(context))
    verify(viewFacet, never()).dispatch(any(Request))
  }

  @Test(expected = AuthorizationException)
  void 'AuthorizationExceptions from dispatch requests are not incorrectly mapped as another kind of Exception'() {
    doReturn(repository).when(underTest).getRepository()
    doReturn(null).when(underTest).getCachedContent(context)

    doReturn(request).when(context).getRequest()
    doReturn(PATH).when(request).getPath()

    doReturn(viewFacet).when(repository).facet(ViewFacet)
    doThrow(new AuthorizationException()).when(viewFacet).dispatch(any(Request), any(Context))

    doReturn(contextAttributes).when(context).getAttributes()
    doReturn(TARBALL).when(contextAttributes).require(ProxyTarget)
    doReturn(state).when(contextAttributes).require(TokenMatcher.State)
    doReturn(TOKENS).when(state).getTokens()

    underTest.get(context)
  }

  @Test
  void 'Ensure correctly-encoded parameters are added to URL via getUrl'() {
    String baseUrl = "/repository/protop-proxy/-/v1/search"
    String fullUrl = (baseUrl + "?size=${SEARCH_RESULT_SIZE}&text=${SEARCH_ENCODED_TEXT}").substring(1)
    Parameters parameters = new Parameters()
    parameters.set("text", SEARCH_TEXT)
    parameters.set("size", SEARCH_RESULT_SIZE)
    doReturn(repository).when(underTest).getRepository()
    doReturn(request).when(context).getRequest()
    doReturn(contextAttributes).when(context).getAttributes()
    doReturn(SEARCH_RESULTS).when(contextAttributes).require(ProxyTarget)
    doReturn(parameters).when(request).getParameters()
    doReturn(baseUrl).when(request).getPath()
    assertThat(underTest.getUrl(context), is(fullUrl))
  }
}
