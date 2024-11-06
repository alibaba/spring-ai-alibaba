package org.bsc.langgraph4j;

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.JsonSchemaGenerator;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.github.victools.jsonschema.generator.Option.MAP_VALUES_AS_ADDITIONAL_PROPERTIES;
import static com.github.victools.jsonschema.generator.Option.STRICT_TYPE_INFO;


public class GenerateJsonSchemaTest {


    @Test
    public void generateWithVictools() {
        SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON);
        SchemaGeneratorConfig config = configBuilder
                .with(MAP_VALUES_AS_ADDITIONAL_PROPERTIES, STRICT_TYPE_INFO)
                .build();
        SchemaGenerator generator = new SchemaGenerator(config);
        JsonNode jsonSchema = generator.generateSchema(NodeOutput.class);

        System.out.println(jsonSchema.toPrettyString());
    }

    @Test
    public void generateWithJackson() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
// configure mapper, if necessary, then create schema generator
        JsonSchemaGenerator schemaGen = new JsonSchemaGenerator(mapper);
        JsonSchema schema = schemaGen.generateSchema(NodeOutput.class);

        System.out.println(mapper.writeValueAsString(schema));
    }


}

class GPTJsonSchemaGenerator {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static ObjectNode generateSchema(Object data) {
        return generateSchema(data, new HashSet<>());
    }

    private static ObjectNode generateSchema(Object data, Set<Object> visited) {
        ObjectNode schema = mapper.createObjectNode();
        if (data == null) {
            schema.put("type", "null");
        } else if (data instanceof Map) {
            if (visited.contains(data)) {
                schema.put("type", "object");
                return schema;
            }
            visited.add(data);
            schema.put("type", "object");
            ObjectNode properties = mapper.createObjectNode();
            Map<?, ?> map = (Map<?, ?>) data;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = String.valueOf(entry.getKey());
                Object value = entry.getValue();
                properties.set(key, generateSchema(value, visited));
            }
            schema.set("properties", properties);
        } else if (data instanceof List) {
            if (visited.contains(data)) {
                schema.put("type", "array");
                return schema;
            }
            visited.add(data);
            schema.put("type", "array");
            ArrayNode itemsArray = mapper.createArrayNode();
            List<?> list = (List<?>) data;
            for (Object item : list) {
                itemsArray.add(generateSchema(item, visited));
            }
            if (itemsArray.size() > 0) {
                // Simplify the schema if all items have the same schema
                JsonNode firstItemSchema = itemsArray.get(0);
                boolean allSame = true;
                for (int i = 1; i < itemsArray.size(); i++) {
                    if (!itemsArray.get(i).equals(firstItemSchema)) {
                        allSame = false;
                        break;
                    }
                }
                if (allSame) {
                    schema.set("items", firstItemSchema);
                } else {
                    schema.set("items", itemsArray);
                }
            } else {
                schema.set("items", mapper.createObjectNode());
            }
        } else if (data instanceof String) {
            schema.put("type", "string");
        } else if (data instanceof Integer || data instanceof Long) {
            schema.put("type", "integer");
        } else if (data instanceof Float || data instanceof Double) {
            schema.put("type", "number");
        } else if (data instanceof Boolean) {
            schema.put("type", "boolean");
        } else {
            schema.put("type", "string");
        }
        return schema;
    }

    public static void main(String[] args) {
        Map<String, Object> inputMap = new HashMap<>();

        inputMap.put("name", "Alice");
        inputMap.put("age", 30);
        inputMap.put("isMember", true);
        inputMap.put("preferences", Map.of(
                "notifications", "email",
                "theme", "dark"
        ));
        inputMap.put("tags", List.of("user", "admin"));
        inputMap.put("scores", List.of(95, 85, 76));

        ObjectNode schema = generateSchema(inputMap);

        try {
            String jsonSchema = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(schema);
            System.out.println(jsonSchema);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

class ClaudeJsonSchemaGenerator {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String generateSchema(Map<String, Object> map) {
        ObjectNode schemaNode = objectMapper.createObjectNode();
        schemaNode.put("$schema", "http://json-schema.org/draft-07/schema#");
        schemaNode.put("type", "object");

        ObjectNode propertiesNode = schemaNode.putObject("properties");

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            propertiesNode.set(key, generateSchemaForValue(value));
        }

        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(schemaNode);
        } catch (Exception e) {
            throw new RuntimeException("Error generating JSON schema", e);
        }
    }

    private static ObjectNode generateSchemaForValue(Object value) {
        ObjectNode propertySchema = objectMapper.createObjectNode();

        if (value == null) {
            propertySchema.put("type", "null");
        } else if (value instanceof String) {
            propertySchema.put("type", "string");
        } else if (value instanceof Integer || value instanceof Long) {
            propertySchema.put("type", "integer");
        } else if (value instanceof Float || value instanceof Double) {
            propertySchema.put("type", "number");
        } else if (value instanceof Boolean) {
            propertySchema.put("type", "boolean");
        } else if (value instanceof List) {
            propertySchema.put("type", "array");
            List<?> list = (List<?>) value;
            if (!list.isEmpty()) {
                propertySchema.set("items", generateSchemaForValue(list.get(0)));
            }
        } else if (value instanceof Map) {
            propertySchema.put("type", "object");
            @SuppressWarnings("unchecked")
            Map<String, Object> nestedMap = (Map<String, Object>) value;
            ObjectNode nestedProperties = propertySchema.putObject("properties");
            for (Map.Entry<String, Object> entry : nestedMap.entrySet()) {
                nestedProperties.set(entry.getKey(), generateSchemaForValue(entry.getValue()));
            }
        } else {
            propertySchema.put("type", "object");
        }

        return propertySchema;
    }

    public static void main(String[] args) {
        // Example usage
        Map<String, Object> testMap = Map.of(
                "name", "John Doe",
                "age", 30,
                "isStudent", false,
                "grades", List.of(85, 90, 78),
                "address", Map.of(
                        "street", "123 Main St",
                        "city", "Anytown"
                )
        );

        String jsonSchema = generateSchema(testMap);
        System.out.println(jsonSchema);
    }
}

class CopilotJsonSchemaGenerator {

    public static void main(String[] args) {
        // Sample input map
        Map<String, Object> inputMap = Map.of(
                "name", "John Doe",
                "age", 30,
                "address", Map.of("street", "123 Main St", "city", "Anytown")
        );

        // Generate JSON schema
        String schema = generateJsonSchema(inputMap);
        System.out.println(schema);
    }

    public static String generateJsonSchema(Map<String, Object> inputMap) {
        ObjectMapper objectMapper = new ObjectMapper();
        SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(
                SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON);
        SchemaGenerator generator = new SchemaGenerator(configBuilder.build());

        // Convert the map to a JSON node
        var jsonNode = objectMapper.valueToTree(inputMap);

        // Generate the schema
        var jsonSchema = generator.generateSchema(jsonNode.getClass());

        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonSchema);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
                // Handle error during nested schema generation