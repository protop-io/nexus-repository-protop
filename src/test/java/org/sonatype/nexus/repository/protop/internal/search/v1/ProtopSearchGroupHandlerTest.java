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
package org.sonatype.nexus.repository.protop.internal.search.v1;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.group.GroupFacet;
import org.sonatype.nexus.repository.group.GroupHandler.DispatchedRepositories;
import org.sonatype.nexus.repository.http.HttpStatus;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Parameters;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.Request;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.Status;
import org.sonatype.nexus.repository.view.ViewFacet;

import com.google.common.io.CharStreams;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.http.HttpStatus.OK;

public class ProtopSearchGroupHandlerTest
    extends TestSupport
{
  static final int MAX_SEARCH_RESULTS = 250;

  @Mock
  ProtopSearchParameterExtractor protopSearchParameterExtractor;

  @Mock
  ProtopSearchResponseFactory protopSearchResponseFactory;

  @Mock
  ProtopSearchResponseMapper protopSearchResponseMapper;

  @Mock
  Context context;

  @Mock
  Request request;

  @Mock
  Parameters parameters;

  @Mock
  Repository repository;

  @Mock
  Repository memberRepository1;

  @Mock
  Repository memberRepository2;

  @Mock
  ViewFacet viewFacet1;

  @Mock
  ViewFacet viewFacet2;

  @Mock
  Response response1;

  @Mock
  Response response2;

  @Mock
  Payload payload1;

  @Mock
  Payload payload2;

  @Mock
  Status status1;

  @Mock
  Status status2;

  @Mock
  InputStream payloadInputStream1;

  @Mock
  InputStream payloadInputStream2;

  @Mock
  ProtopSearchResponse protopSearchResponse1;

  @Mock
  ProtopSearchResponse protopSearchResponse2;

  @Mock
  ProtopSearchResponseObject protopSearchResponseObject1;

  @Mock
  ProtopSearchResponseObject protopSearchResponseObject2;

  @Mock
  ProtopSearchResponsePackage protopSearchResponsePackage1;

  @Mock
  ProtopSearchResponsePackage protopSearchResponsePackage2;

  @Mock
  GroupFacet groupFacet;

  @Mock
  DispatchedRepositories dispatchedRepositories;

  @Mock
  ProtopSearchResponse searchResponse;

  ProtopSearchGroupHandler underTest;

  @Before
  public void setUp() throws Exception {
    underTest = new ProtopSearchGroupHandler(protopSearchParameterExtractor, protopSearchResponseFactory,
        protopSearchResponseMapper, MAX_SEARCH_RESULTS);

    when(context.getRepository()).thenReturn(repository);
    when(context.getRequest()).thenReturn(request);
    when(request.getParameters()).thenReturn(parameters);

    when(repository.facet(GroupFacet.class)).thenReturn(groupFacet);

    when(groupFacet.members()).thenReturn(asList(memberRepository1, memberRepository2));

    when(memberRepository1.facet(ViewFacet.class)).thenReturn(viewFacet1);
    when(memberRepository2.facet(ViewFacet.class)).thenReturn(viewFacet2);

    when(protopSearchResponseFactory.buildEmptyResponse()).thenReturn(searchResponse);
    when(protopSearchResponseFactory.buildResponseForObjects(any(List.class))).then(invocation -> {
      List<ProtopSearchResponseObject> objects = (List<ProtopSearchResponseObject>) invocation.getArguments()[0];
      ProtopSearchResponse response = new ProtopSearchResponse();
      response.setObjects(objects);
      response.setTime("Wed Jan 25 2017 19:23:35 GMT+0000 (UTC)");
      response.setTotal(objects.size());
      return response;
    });

    when(protopSearchParameterExtractor.extractText(parameters)).thenReturn("text");
    when(protopSearchParameterExtractor.extractSize(parameters)).thenReturn(20);
    when(protopSearchParameterExtractor.extractFrom(parameters)).thenReturn(0);

    when(protopSearchResponseMapper.writeString(any(ProtopSearchResponse.class))).thenReturn("response");

    when(viewFacet1.dispatch(request, context)).thenReturn(response1);
    when(viewFacet2.dispatch(request, context)).thenReturn(response2);

    when(response1.getPayload()).thenReturn(payload1);
    when(response1.getStatus()).thenReturn(status1);

    when(response2.getPayload()).thenReturn(payload2);
    when(response2.getStatus()).thenReturn(status2);

    when(status1.getCode()).thenReturn(OK);
    when(status2.getCode()).thenReturn(OK);

    when(payload1.openInputStream()).thenReturn(payloadInputStream1);
    when(payload2.openInputStream()).thenReturn(payloadInputStream2);

    when(protopSearchResponseMapper.readFromInputStream(payloadInputStream1)).thenReturn(protopSearchResponse1);
    when(protopSearchResponseMapper.readFromInputStream(payloadInputStream2)).thenReturn(protopSearchResponse2);

    when(protopSearchResponse1.getObjects()).thenReturn(singletonList(protopSearchResponseObject1));
    when(protopSearchResponse2.getObjects()).thenReturn(singletonList(protopSearchResponseObject2));

    when(protopSearchResponseObject1.getSearchScore()).thenReturn(0.8);
    when(protopSearchResponseObject1.getPackageEntry()).thenReturn(protopSearchResponsePackage1);

    when(protopSearchResponseObject2.getSearchScore()).thenReturn(0.9);
    when(protopSearchResponseObject2.getPackageEntry()).thenReturn(protopSearchResponsePackage2);

    when(protopSearchResponsePackage1.getName()).thenReturn("package-1");
    when(protopSearchResponsePackage2.getName()).thenReturn("package-2");
  }

  @Test
  public void testSearchWithEmptyString() throws Exception {
    when(protopSearchResponseFactory.buildEmptyResponse()).thenReturn(searchResponse);
    when(protopSearchParameterExtractor.extractText(parameters)).thenReturn("");
    when(protopSearchResponseMapper.writeString(any(ProtopSearchResponse.class))).thenReturn("response");

    Response response = underTest.doGet(context, dispatchedRepositories);

    assertThat(response.getStatus().getCode(), is(OK));
    assertThat(response.getPayload(), not(nullValue()));
    try (InputStream in = response.getPayload().openInputStream()) {
      assertThat(CharStreams.toString(new InputStreamReader(in, UTF_8)), is("response"));
    }

    verify(protopSearchResponseFactory).buildEmptyResponse();

    verifyNoMoreInteractions(viewFacet1);
    verifyNoMoreInteractions(viewFacet2);
  }

  @Test
  public void testMergingResultsWithDifferentPackageNames() throws Exception {
    Response response = underTest.doGet(context, dispatchedRepositories);

    assertThat(response.getStatus().getCode(), is(OK));
    assertThat(response.getPayload(), not(nullValue()));
    try (InputStream in = response.getPayload().openInputStream()) {
      assertThat(CharStreams.toString(new InputStreamReader(in, UTF_8)), is("response"));
    }

    verify(viewFacet1).dispatch(request, context);
    verify(viewFacet2).dispatch(request, context);

    ArgumentCaptor<ProtopSearchResponse> searchResponseCaptor = ArgumentCaptor.forClass(ProtopSearchResponse.class);
    verify(protopSearchResponseMapper).writeString(searchResponseCaptor.capture());

    ProtopSearchResponse searchResponse = searchResponseCaptor.getValue();
    assertThat(searchResponse.getTotal(), is(2));
    assertThat(searchResponse.getObjects(), hasSize(2));
    assertThat(searchResponse.getObjects(), contains(protopSearchResponseObject2, protopSearchResponseObject1));
  }

  @Test
  public void testMergingResultsWithSamePackageName() throws Exception {
    when(protopSearchResponsePackage2.getName()).thenReturn("package-1");

    Response response = underTest.doGet(context, dispatchedRepositories);

    assertThat(response.getStatus().getCode(), is(OK));
    assertThat(response.getPayload(), not(nullValue()));
    try (InputStream in = response.getPayload().openInputStream()) {
      assertThat(CharStreams.toString(new InputStreamReader(in, UTF_8)), is("response"));
    }

    verify(viewFacet1).dispatch(request, context);
    verify(viewFacet2).dispatch(request, context);

    ArgumentCaptor<ProtopSearchResponse> searchResponseCaptor = ArgumentCaptor.forClass(ProtopSearchResponse.class);
    verify(protopSearchResponseMapper).writeString(searchResponseCaptor.capture());

    ProtopSearchResponse searchResponse = searchResponseCaptor.getValue();
    assertThat(searchResponse.getTotal(), is(1));
    assertThat(searchResponse.getObjects(), hasSize(1));
    assertThat(searchResponse.getObjects(), contains(protopSearchResponseObject1));
  }

  @Test
  public void testPropagateRequests() throws Exception {
    when(protopSearchParameterExtractor.extractSize(parameters)).thenReturn(100);
    when(protopSearchParameterExtractor.extractFrom(parameters)).thenReturn(50);

    Response response = underTest.doGet(context, dispatchedRepositories);

    assertThat(response.getStatus().getCode(), is(OK));
    assertThat(response.getPayload(), not(nullValue()));
    try (InputStream in = response.getPayload().openInputStream()) {
      assertThat(CharStreams.toString(new InputStreamReader(in, UTF_8)), is("response"));
    }

    verify(parameters).replace("from", "0");
    verify(parameters).replace("size", Integer.toString(MAX_SEARCH_RESULTS));

    verify(viewFacet1).dispatch(request, context);
    verify(viewFacet2).dispatch(request, context);
  }

  @Test
  public void testSkipResultWithMissingPackage() throws Exception {
    when(protopSearchResponseObject1.getPackageEntry()).thenReturn(null);

    Response response = underTest.doGet(context, dispatchedRepositories);

    assertThat(response.getStatus().getCode(), is(OK));
    assertThat(response.getPayload(), not(nullValue()));
    try (InputStream in = response.getPayload().openInputStream()) {
      assertThat(CharStreams.toString(new InputStreamReader(in, UTF_8)), is("response"));
    }

    ArgumentCaptor<ProtopSearchResponse> searchResponseCaptor = ArgumentCaptor.forClass(ProtopSearchResponse.class);
    verify(protopSearchResponseMapper).writeString(searchResponseCaptor.capture());

    ProtopSearchResponse searchResponse = searchResponseCaptor.getValue();
    assertThat(searchResponse.getTotal(), is(1));
    assertThat(searchResponse.getObjects(), hasSize(1));
    assertThat(searchResponse.getObjects(), contains(protopSearchResponseObject2));
  }

  @Test
  public void testSkipResultWithMissingPackageName() throws Exception {
    when(protopSearchResponsePackage1.getName()).thenReturn(null);

    Response response = underTest.doGet(context, dispatchedRepositories);

    assertThat(response.getStatus().getCode(), is(OK));
    assertThat(response.getPayload(), not(nullValue()));
    try (InputStream in = response.getPayload().openInputStream()) {
      assertThat(CharStreams.toString(new InputStreamReader(in, UTF_8)), is("response"));
    }

    ArgumentCaptor<ProtopSearchResponse> searchResponseCaptor = ArgumentCaptor.forClass(ProtopSearchResponse.class);
    verify(protopSearchResponseMapper).writeString(searchResponseCaptor.capture());

    ProtopSearchResponse searchResponse = searchResponseCaptor.getValue();
    assertThat(searchResponse.getTotal(), is(1));
    assertThat(searchResponse.getObjects(), hasSize(1));
    assertThat(searchResponse.getObjects(), contains(protopSearchResponseObject2));
  }

  @Test
  public void testSkipResultWithMissingScore() throws Exception {
    when(protopSearchResponseObject1.getSearchScore()).thenReturn(null);

    Response response = underTest.doGet(context, dispatchedRepositories);

    assertThat(response.getStatus().getCode(), is(OK));
    assertThat(response.getPayload(), not(nullValue()));
    try (InputStream in = response.getPayload().openInputStream()) {
      assertThat(CharStreams.toString(new InputStreamReader(in, UTF_8)), is("response"));
    }

    ArgumentCaptor<ProtopSearchResponse> searchResponseCaptor = ArgumentCaptor.forClass(ProtopSearchResponse.class);
    verify(protopSearchResponseMapper).writeString(searchResponseCaptor.capture());

    ProtopSearchResponse searchResponse = searchResponseCaptor.getValue();
    assertThat(searchResponse.getTotal(), is(1));
    assertThat(searchResponse.getObjects(), hasSize(1));
    assertThat(searchResponse.getObjects(), contains(protopSearchResponseObject2));
  }

  @Test
  public void testSkipResultWithBadResponse() throws Exception {
    when(status1.getCode()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);

    Response response = underTest.doGet(context, dispatchedRepositories);

    assertThat(response.getStatus().getCode(), is(OK));
    assertThat(response.getPayload(), not(nullValue()));
    try (InputStream in = response.getPayload().openInputStream()) {
      assertThat(CharStreams.toString(new InputStreamReader(in, UTF_8)), is("response"));
    }

    ArgumentCaptor<ProtopSearchResponse> searchResponseCaptor = ArgumentCaptor.forClass(ProtopSearchResponse.class);
    verify(protopSearchResponseMapper).writeString(searchResponseCaptor.capture());

    ProtopSearchResponse searchResponse = searchResponseCaptor.getValue();
    assertThat(searchResponse.getTotal(), is(1));
    assertThat(searchResponse.getObjects(), hasSize(1));
    assertThat(searchResponse.getObjects(), contains(protopSearchResponseObject2));
  }

  @Test
  public void testSkipResultWithMissingPayload() throws Exception {
    when(response1.getPayload()).thenReturn(null);

    Response response = underTest.doGet(context, dispatchedRepositories);

    assertThat(response.getStatus().getCode(), is(OK));
    assertThat(response.getPayload(), not(nullValue()));
    try (InputStream in = response.getPayload().openInputStream()) {
      assertThat(CharStreams.toString(new InputStreamReader(in, UTF_8)), is("response"));
    }

    ArgumentCaptor<ProtopSearchResponse> searchResponseCaptor = ArgumentCaptor.forClass(ProtopSearchResponse.class);
    verify(protopSearchResponseMapper).writeString(searchResponseCaptor.capture());

    ProtopSearchResponse searchResponse = searchResponseCaptor.getValue();
    assertThat(searchResponse.getTotal(), is(1));
    assertThat(searchResponse.getObjects(), hasSize(1));
    assertThat(searchResponse.getObjects(), contains(protopSearchResponseObject2));
  }
}
