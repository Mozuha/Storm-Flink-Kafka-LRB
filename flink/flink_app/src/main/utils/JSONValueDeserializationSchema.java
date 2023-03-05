package main.utils;

import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.typeutils.TypeExtractor;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;

public class JSONValueDeserializationSchema implements DeserializationSchema<ObjectNode> {

  private static final long serialVersionUID = -1L;

  private ObjectMapper mapper;

  @Override
  public ObjectNode deserialize(byte[] message) throws IOException {
    if (mapper == null) {
      mapper = new ObjectMapper();
    }
    ObjectNode node = mapper.createObjectNode();
    if (message != null) {
      node.set("value", mapper.readValue(message, JsonNode.class));
    }
    return node;
  }

  @Override
  public boolean isEndOfStream(ObjectNode nextElement) {
    return false;
  }

  @Override
  public TypeInformation<ObjectNode> getProducedType() {
    return TypeExtractor.getForClass(ObjectNode.class);
  }
}
