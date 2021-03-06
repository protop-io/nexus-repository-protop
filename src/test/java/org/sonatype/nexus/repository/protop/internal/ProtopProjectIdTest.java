
package org.sonatype.nexus.repository.protop.internal;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonatype.goodies.testsupport.TestSupport;

import com.google.common.base.Strings;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

/**
 * Test for {@link ProtopProjectId}.
 */
@RunWith(JUnitParamsRunner.class)
public class ProtopProjectIdTest
    extends TestSupport
{
  @Test
  @Parameters
  public void parseGood(final String idStr, final String expectedScope, final String expectedName) {
    ProtopProjectId id = ProtopProjectId.parse(idStr);
    assertThat(id.id(), equalTo(idStr));
    assertThat(id.org(), equalTo(expectedScope));
    assertThat(id.name(), equalTo(expectedName));

    ProtopProjectId id2 = ProtopProjectId.parse(idStr);

    assertThat(id2, equalTo(id));
    assertThat(id2.hashCode(), equalTo(id.hashCode()));
    assertThat(id2.compareTo(id), equalTo(0));
  }

  @SuppressWarnings("unused")
  private List<List<String>> parametersForParseGood() {
    return Arrays.asList(
        Arrays.asList("org/name", "org", "name"),
        Arrays.asList("org/n.ame", "org", "n.ame"),
        Arrays.asList("org/n_ame", "org", "n_ame"),
        Arrays.asList("o.rg/name", "o.rg", "name"),
        Arrays.asList("o_rg/name", "o_rg", "name"),
        Arrays.asList("sc.ope/na.me", "sc.ope", "na.me"),
        Arrays.asList("sc_ope/na_me", "sc_ope", "na_me"),
        Arrays.asList("sc.ope_/na.me_", "sc.ope_", "na.me_"),
        Arrays.asList("sc_ope./na_me.", "sc_ope.", "na_me.")
      );
  }
  
  
  @Test(expected = IllegalArgumentException.class)
  @Parameters
  public void parseBad(String id) {
    ProtopProjectId.parse(id);
  }

  @SuppressWarnings("unused")
  private List<String> parametersForParseBad() {
    return Arrays.asList(
        "",
        "-/",
        "./",
        ".org/",
        "_scope/",
        "org/",
        "#org/",
        "@sco/pe/name",
        "@scópe/name",
        ".@.org/name"
      );
  }

  @Test(expected = IllegalArgumentException.class)
  public void malformedNameTooLong() {
    ProtopProjectId.parse("ab/" + Strings.repeat("0123456789", 22));
  }

  @Test(expected = IllegalArgumentException.class)
  public void malformedOrgTooLong() {
    ProtopProjectId.parse(Strings.repeat("0123456789", 22) + "/ab");
  }
}
