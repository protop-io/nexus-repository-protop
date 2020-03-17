
package org.sonatype.nexus.repository.protop.internal;

import java.io.IOException;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.json.CurrentPathJsonParser;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class ProtopFieldMatcherTest
    extends TestSupport
{
  private static final String FIELD_NAME = "test";

  private static final String FIELD_PATH = "/test";

  @Mock
  private ProtopFieldDeserializer fieldDeserializer;

  @Mock
  private CurrentPathJsonParser parser;

  private ProtopFieldMatcher underTest;

  @Before
  public void setUp() throws IOException {
    when(parser.getCurrentName()).thenReturn(FIELD_NAME);
    when(parser.currentPath()).thenReturn(FIELD_PATH);
  }

  @Test(expected = NullPointerException.class)
  public void require_FieldName() {
    underTest = new ProtopFieldMatcher(null, "", fieldDeserializer);
  }

  @Test
  public void expect_fieldName_Set() {
    underTest = new ProtopFieldMatcher(FIELD_NAME, "", fieldDeserializer);
    assertThat(underTest.getFieldName(), equalTo(FIELD_NAME));
  }

  @Test(expected = NullPointerException.class)
  public void require_PathRegex() {
    underTest = new ProtopFieldMatcher("", null, fieldDeserializer);
  }

  @Test(expected = NullPointerException.class)
  public void require_FieldDeserializer() {
    underTest = new ProtopFieldMatcher("", "", null);
  }

  @Test
  public void should_Match_Current_Path() throws IOException {
    underTest = new ProtopFieldMatcher(FIELD_NAME, FIELD_PATH, fieldDeserializer);

    assertTrue(underTest.matches(parser));
  }

  @Test
  public void should_Match_By_Field_And_Regex_Path() throws IOException {
    underTest = new ProtopFieldMatcher(FIELD_NAME, "/t(.*)t", fieldDeserializer);

    assertTrue(underTest.matches(parser));
  }

  @Test
  public void should_NotMatch_Current_Path() throws IOException {
    underTest = new ProtopFieldMatcher("", "", fieldDeserializer);

    assertFalse(underTest.matches(parser));
  }

  @Test
  public void should_NotMatch_By_Name() throws IOException {
    underTest = new ProtopFieldMatcher("", FIELD_PATH, fieldDeserializer);

    assertFalse(underTest.matches(parser));
  }

  @Test
  public void should_NotMatch_By_Path() throws IOException {
    underTest = new ProtopFieldMatcher(FIELD_NAME, "", fieldDeserializer);

    assertFalse(underTest.matches(parser));
  }

  @Test
  public void should_Match_By_Field_But_Not_By_Regex_Path() throws IOException {
    underTest = new ProtopFieldMatcher(FIELD_NAME, "/b(.*)b", fieldDeserializer);

    assertFalse(underTest.matches(parser));
  }

  @Test
  public void should_AllowDeserializationOnMatched_By_Default() {
    assertTrue(new ProtopFieldMatcher(FIELD_NAME, "/b(.*)b", fieldDeserializer).allowDeserializationOnMatched());
  }
}
