
package org.sonatype.nexus.repository.protop.internal;

import org.sonatype.goodies.testsupport.TestSupport;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.deser.ValueInstantiator;
import com.fasterxml.jackson.databind.deser.std.MapDeserializer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static java.util.Collections.emptyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ProtopMapDeserializerSerializerTest
    extends TestSupport
{
  @Mock
  private MapDeserializer rootDeserializer;

  @Mock
  private ValueInstantiator valueInstantiator;

  @Mock
  private JsonGenerator generator;

  @Before
  public void setUp() {
    when(rootDeserializer.getValueInstantiator()).thenReturn(valueInstantiator);
    when(valueInstantiator.canCreateUsingDefault()).thenReturn(true);

    new ProtopMapDeserializerSerializer(rootDeserializer, generator, emptyList());
  }

  @Test
  public void should_Use_ValueType_From_RootDeserializer() {
    verify(rootDeserializer).getValueType();
  }

  @Test
  public void should_Use_ValueInstantiator_From_RootDeserializer() {
    verify(rootDeserializer).getValueInstantiator();
  }
}
