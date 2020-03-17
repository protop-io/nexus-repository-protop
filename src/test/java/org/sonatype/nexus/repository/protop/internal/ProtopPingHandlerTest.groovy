

package org.sonatype.nexus.repository.protop.internal

import org.sonatype.nexus.repository.view.Context

import spock.lang.Specification

class ProtopPingHandlerTest
    extends Specification
{

  def 'it will respond with an empty object when invoked'() {
    given:
      def subject = new ProtopPingHandler()
    when:
      def response = subject.handle(Mock(Context))
    then:
      response.getPayload().openInputStream().text == '{}'
  }
}
