
package org.sonatype.nexus.repository.protop.internal


import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

import spock.lang.Specification

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.core.IsNot.not
import static org.hamcrest.core.IsNull.nullValue

/**
 * Tests {@link ProtopJsonUtils}
 */
class ProtopJsonUtilsTest
    extends Specification
{

  def 'normal utf-8 parsing'() {
    when: 'the string in the input is encoded using UTF-8'
      def parsed = parseWithProtopJsonUtils('{"name":"foo"}', StandardCharsets.UTF_8)

    then: 'everything should go fine'
      assertThat(parsed, not(nullValue()))
  }

  def 'parsing a fairly harmless iso-8859-1 string falls back to iso-8859-1'() {
    when: 'a fairly harmless string in the input is encoded using ISO-8859-1'
      def parsed = parseWithProtopJsonUtils('{"name":"foo"}', StandardCharsets.ISO_8859_1)

    then: 'everything should go fine'
      assertThat(parsed, not(nullValue()))
  }

  def 'parsing should fall back to iso-8859-1 when the bytes have an invalid middle byte in UTF-8'() {
    when: 'a string in the input is encoded using ISO-8859-1, but will have an invalid middle byte in UTF-8'
      def parsed = parseWithProtopJsonUtils('{"name":"foo","author":"bé"}', StandardCharsets.ISO_8859_1)

    then: 'everything should go fine'
      assertThat(parsed, not(nullValue()))
  }

  def 'parsing should fall back to iso-8859-1 when the bytes have an invalid start byte in UTF-8'() {
    when: 'a string in the input is encoded using ISO-8859-1, but will have an invalid start byte in UTF-8'
      def parsed = parseWithProtopJsonUtils('{"name":"foo","author":"¿foo?"}', StandardCharsets.ISO_8859_1)

    then: 'everything should go fine'
      assertThat(parsed, not(nullValue()))
  }

  def parseWithProtopJsonUtils(String toEncode, Charset charset) {
    ProtopJsonUtils.parse(
            ProtopJsonUtils.supplier(toEncode.getBytes(charset))
    )
  }

}
