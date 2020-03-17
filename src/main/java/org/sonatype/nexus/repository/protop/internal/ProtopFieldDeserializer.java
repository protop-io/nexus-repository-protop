package org.sonatype.nexus.repository.protop.internal;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;

import java.io.IOException;

import static com.fasterxml.jackson.core.JsonTokenId.ID_NULL;
import static java.util.Objects.nonNull;

/**
 * A protop specialized deserializer for an individual field.
 *
 * @since 3.16
 */
public class ProtopFieldDeserializer {
    /**
     * Deserialize object based on a default writing out of the field name and deserialized value. Implementers of this
     * class should override this to provide their own way of deserializing.
     *
     * @param fieldName    exact name of field as found by {@link JsonParser}
     * @param defaultValue {@link Object} that will provide the actual value to deserialize
     * @param parser       {@link JsonParser}
     * @param context      {@link DeserializationContext}
     * @param generator    {@link JsonGenerator}
     * @return Object deserialized through this field deserializer
     * @throws IOException from potential generating issues
     */
    public Object deserialize(final String fieldName,
                              final Object defaultValue,
                              final JsonParser parser,
                              final DeserializationContext context,
                              final JsonGenerator generator) throws IOException {
        generator.writeFieldName(fieldName);

        Object value;

        try {
            value = deserializeValue(defaultValue);
        } catch (RuntimeException e) {
            // handle the most likely exception from deserializing and throw as a normal deserializer would
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            }

            throw e;
        }

        if (nonNull(value) || parser.currentTokenId() == ID_NULL) {
            generator.writeObject(value);
        }

        return value;
    }

    /**
     * Method for explicitly deserilizing the value of supplied deserialized object.
     */
    protected Object deserializeValue(final Object defaultValue) {
        return defaultValue;
    }
}
