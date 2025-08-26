package org.knowm.xchange.bitstamp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import si.mazi.rescu.HttpStatusExceptionSupport;

@JsonDeserialize(using = BitstampException.BitstampExceptionDeserializer.class)
public class BitstampException extends HttpStatusExceptionSupport {

  private Map<String, Collection<String>> errors;

  public BitstampException(String message) {
    super(message);
  }

  public BitstampException(
      @JsonProperty("error") Object error, @JsonProperty("reason") Object reason) {
    super(getMessage(error == null ? reason : error));

    if (error == null) {
      error = reason;
    }
    if (error instanceof Map) {
      try {
        errors = (Map<String, Collection<String>>) error;
      } catch (Exception ignore) {
      }
    }
  }

  private static String getMessage(Object errors) {
    if (errors instanceof Map) {
      try {
        Map<String, Iterable> map = (Map<String, Iterable>) errors;
        final StringBuilder sb = new StringBuilder();
        for (String key : map.keySet()) {
          for (Object msg : map.get(key)) {
            if (sb.length() > 0) {
              sb.append(" -- ");
            }
            sb.append(msg);
          }
        }
        return sb.toString();
      } catch (Exception ignore) {
      }
    }
    return String.valueOf(errors);
  }

  public Map<String, Collection<String>> getErrors() {
    return errors;
  }

  public Collection<String> getErrors(String key) {
    return errors.get(key);
  }

  /**
   * Custom deserializer to handle both object and array error responses from
   * Bitstamp API
   */
  public static class BitstampExceptionDeserializer extends JsonDeserializer<BitstampException> {

    @Override
    public BitstampException deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
      JsonToken token = p.getCurrentToken();

      if (token == JsonToken.START_ARRAY) {
        // Handle array format: ["error message"]
        StringBuilder message = new StringBuilder();
        while (p.nextToken() != JsonToken.END_ARRAY) {
          if (message.length() > 0) {
            message.append(" -- ");
          }
          message.append(p.getValueAsString());
        }
        return new BitstampException(message.toString());
      } else if (token == JsonToken.START_OBJECT) {
        // Handle object format: {"error": "message"} or {"reason": "message"}
        String error = null;
        String reason = null;

        while (p.nextToken() != JsonToken.END_OBJECT) {
          String fieldName = p.getCurrentName();
          p.nextToken();

          if ("error".equals(fieldName)) {
            error = p.getValueAsString();
          } else if ("reason".equals(fieldName)) {
            reason = p.getValueAsString();
          }
        }

        return new BitstampException(error, reason);
      } else {
        // Handle simple string format
        return new BitstampException(p.getValueAsString());
      }
    }
  }
}
