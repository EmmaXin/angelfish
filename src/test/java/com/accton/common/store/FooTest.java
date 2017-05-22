package com.accton.common.store;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Unit test for simple App.
 */
public class FooTest
        extends TestCase {

    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public FooTest(String testName) {
        super(testName);
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite(FooTest.class);
    }

    public void testObjectNodeForEach() {
        ObjectNode objectNode = JsonNodeFactory.instance.objectNode();

        objectNode.put("username", "myusername");
        objectNode.put("password", "mypassword");

        Map<String, String> map = new HashMap<>();

        for (Iterator<Map.Entry<String, JsonNode>> iterator = objectNode.fields(); iterator.hasNext();) {
            Map.Entry<String, JsonNode> field = iterator.next();

            map.put(field.getKey(), field.getValue().textValue());
        }

        assertTrue(map.size() == 2);
        assertTrue(map.get("username").equals("myusername"));
        assertTrue(map.get("password").equals("mypassword"));
    }

    public void testObjectNodeGetTextValue() {
        ObjectNode objectNode = JsonNodeFactory.instance.objectNode();

        objectNode.put("username", "myusername");

        JsonNode username = objectNode.get("username");
        assertTrue(username.isTextual());
        assertTrue(username.textValue().equals("myusername"));
    }

    public void testObjectNodeGetIntegerValue() {
        ObjectNode objectNode = JsonNodeFactory.instance.objectNode();

        objectNode.put("age", 100);

        JsonNode age = objectNode.get("age");
        assertTrue(age.isIntegralNumber());
        assertTrue(age.intValue() == 100);
    }

    public void testObjectNodeTestKey() {
        ObjectNode objectNode = JsonNodeFactory.instance.objectNode();

        objectNode.put("age", 100);

        JsonNode abc = objectNode.get("abc");
        assertTrue(abc == null);
    }

    public void testConvertStringToJsonNode() {
        String string = "string value";

        ObjectMapper mapper = new ObjectMapper();

        JsonNode jsonNode = mapper.convertValue(string, JsonNode.class);
        assertTrue(jsonNode.isTextual() == true);
        assertTrue(jsonNode.textValue().equals("string value"));
    }

    public void testConvertIntToJsonNode() {
        int intValue = 123;

        ObjectMapper mapper = new ObjectMapper();

        JsonNode jsonNode = mapper.convertValue(intValue, JsonNode.class);
        assertTrue(jsonNode.isInt() == true);
        assertTrue(jsonNode.intValue() == 123);
    }

    public void testConvertArrayToJsonNode() {
        int[] intArray = new int[2];
        intArray[0] = 11;
        intArray[1] = 22;

        ObjectMapper mapper = new ObjectMapper();

        JsonNode jsonNode = mapper.convertValue(intArray, JsonNode.class);
        assertTrue(jsonNode.isArray() == true);

        assertTrue(jsonNode.get(0).isInt() == true);
        assertTrue(jsonNode.get(0).intValue() == 11);
    }

    public void testConvertMapToJsonNode() {
        Map<String, Object> map = new HashMap<>();
        map.put("username", "myusername");
        map.put("age", 123);

        ObjectMapper mapper = new ObjectMapper();

        JsonNode jsonNode = mapper.convertValue(map, JsonNode.class);
        assertTrue(jsonNode.isObject() == true);

        assertTrue(jsonNode.get("username").isTextual() == true);
        assertTrue(jsonNode.get("username").textValue().equals("myusername"));

        assertTrue(jsonNode.get("age").isInt() == true);
        assertTrue(jsonNode.get("age").intValue() == 123);
    }

    class User {
        static final String USERNAME = "username";
        static final String PASSWORD = "password";

        private ObjectNode object;

        private User() {
            object = JsonNodeFactory.instance.objectNode();
        }

        public User(String username, String password) {
            this();

            set(USERNAME, username);
            set(PASSWORD, password);
        }

        private void set(String fieldName, Object value) {
            if (value != null) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode jsonNode = mapper.convertValue(value, JsonNode.class);

                object.put(fieldName, jsonNode);
            } else {
                object.remove(fieldName);
            }
        }

        public String getUsername() {
            return object.get(USERNAME).asText();
        }

        public void setPassword(String password) {
            set(PASSWORD, password);
        }

        public String getPassword() {
            JsonNode jsonNode = object.get(PASSWORD);

            if (jsonNode != null) {
                return jsonNode.asText();
            }

            return null;
        }

        public String toString() {
            return object.toString();
        }

        public ObjectNode toObjectNode() {
            return object.deepCopy();
        }
    }

    public void testCreateUser() {
        User user = new User("myusername", "mypassword");
        assertTrue(user.getUsername().equals("myusername"));
        assertTrue(user.getPassword().equals("mypassword"));
    }

    public void testChangePassword() {
        User user = new User("myusername", "mypassword");
        assertTrue(user.getUsername().equals("myusername"));
        assertTrue(user.getPassword().equals("mypassword"));

        user.setPassword("newpassword");
        assertTrue(user.getPassword().equals("newpassword"));

        user.setPassword(null);
        assertTrue(user.getPassword() == null);

        user.setPassword("renewpassword");
        assertTrue(user.getPassword().equals("renewpassword"));
    }

    class AccountingCfg {
        private ArrayNode array;

        public AccountingCfg() {
            array = JsonNodeFactory.instance.arrayNode();
        }

        @Deprecated
        public void addUser(User user) {
            array.add(user.toObjectNode());
        }

        public void addUser(String username, String password) {
            ObjectNode user = JsonNodeFactory.instance.objectNode();
            user.put("username", username);
            user.put("password", password);
            array.add(user);
        }

        public void addUser(ObjectNode user) {
            JsonNode username = user.get("username");
            JsonNode password = user.get("password");

            if (username == null) {
                throw new IllegalArgumentException("username is required");
            }

            if (!username.isTextual()) {
                throw new IllegalArgumentException("username is invalid type");
            }

            if (password == null) {
                throw new IllegalArgumentException("password is required");
            }

            if (!password.isTextual()) {
                throw new IllegalArgumentException("password is invalid type");
            }

            ObjectNode clone = JsonNodeFactory.instance.objectNode();
            clone.put("username", username.textValue());
            clone.put("password", password.textValue());
            array.add(clone);
        }

        public Iterator<JsonNode> getUsers() {
            return array.elements();
        }

        public JsonNode getUser(String username) {
            for (Iterator<JsonNode> iterator = getUsers(); iterator.hasNext(); ) {
                JsonNode jsonNode = iterator.next();

                if (jsonNode.get("username").textValue().equals(username)) {
                    return jsonNode;
                }
            }

            return null;
        }
    }

    public void testAddUserViaUser() {
        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        AccountingCfg cfg = new AccountingCfg();

        payload.put("username", "myusername");
        payload.put("password", "mypassword");

        ObjectNode req = payload;

        User user = new User(req.get("username").textValue(), req.get("password").textValue());
        cfg.addUser(user);

        assertTrue(cfg.getUser("myusername").get("username").textValue().equals("myusername"));
        assertTrue(cfg.getUser("myusername").get("password").textValue().equals("mypassword"));
    }

    public void testAddUser() {
        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        AccountingCfg cfg = new AccountingCfg();

        payload.put("username", "myusername");
        payload.put("password", "mypassword");

        ObjectNode req = payload;

        cfg.addUser(req.get("username").textValue(), req.get("password").textValue());
        assertTrue(cfg.getUser("myusername").get("username").textValue().equals("myusername"));
        assertTrue(cfg.getUser("myusername").get("password").textValue().equals("mypassword"));
    }

    public void testAddUserViaObjectNode() {
        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        AccountingCfg cfg = new AccountingCfg();

        payload.put("username", "myusername");
        payload.put("password", "mypassword");

        ObjectNode req = payload;

        cfg.addUser(req);
        assertTrue(cfg.getUser("myusername").get("username").textValue().equals("myusername"));
        assertTrue(cfg.getUser("myusername").get("password").textValue().equals("mypassword"));
    }

    JsonNode putIfAbsent(ObjectNode object, String field, JsonNode defaultValue) {
        JsonNode objectNode = object.get(field);
        if (objectNode != null) {
            return objectNode;
        }

        object.put(field, defaultValue);
        return defaultValue;
    }

    class Schema {
        public boolean required;
        public String type;
        public Integer min;
        public Integer max;

        Schema(boolean required, String type, int min, int max) {
            this.required = required;
            this.type = type;
            this.min = min;
            this.max = max;
        }
    }

    ObjectNode validateInputParameter(JsonNode argument, Schema schema) {
        ObjectNode error = JsonNodeFactory.instance.objectNode();

        if (schema.required == true && argument == null) {
            error.put("required", "missing required parameter");
        }

        if (argument != null) {
            if (schema.type.equals("string")) {
                if (!argument.isTextual()) {
                    error.put("type", "required string format");
                }
            }

            if (argument.isTextual()) {
                if (argument.textValue().length() < schema.min) {
                    error.put("min", String.format("min %d character", schema.min));
                }

                if (schema.max < argument.textValue().length()) {
                    error.put("max", String.format("max %d character", schema.max));
                }
            }
        }

        if (error.size() == 0) {
            return null;
        }

        return error;
    }

    ObjectNode validateUsername(ObjectNode req) {
        JsonNode username = req.get("username");
        return validateInputParameter(username, new Schema(true, "string", 4, 8));
    }

    ObjectNode validatePassword(ObjectNode req) {
        JsonNode password = req.get("password");
        return validateInputParameter(password, new Schema(true, "string", 4, 8));
    }

    public void testValidateInputParameterRequired() {
        ObjectNode payload = JsonNodeFactory.instance.objectNode();

        ObjectNode req = payload;

        ObjectNode error = JsonNodeFactory.instance.objectNode();
        JsonNode reqUsername = req.get("username");
        JsonNode reqPassword = req.get("password");

        ObjectNode errUsername = validateUsername(req);
        if (errUsername != null) {
            error.put("username", errUsername);
        }

        ObjectNode errPassword = validatePassword(req);
        if (errUsername != null) {
            error.put("password", errPassword);
        }

        assertTrue(error.get("username").get("required") != null);
        assertTrue(error.get("password").get("required") != null);
    }

    public void testValidateInputParameterStringType() {
        ObjectNode payload = JsonNodeFactory.instance.objectNode();

        payload.put("username", 123);
        payload.put("password", 456);

        ObjectNode req = payload;

        ObjectNode error = JsonNodeFactory.instance.objectNode();
        JsonNode reqUsername = req.get("username");
        JsonNode reqPassword = req.get("password");

        ObjectNode errUsername = validateUsername(req);
        if (errUsername != null) {
            error.put("username", errUsername);
        }

        ObjectNode errPassword = validatePassword(req);
        if (errUsername != null) {
            error.put("password", errPassword);
        }

        assertTrue(error.get("username").get("type") != null);
        assertTrue(error.get("password").get("type") != null);
    }

    public void testValidateInputParameterMinMax() {
        ObjectNode payload = JsonNodeFactory.instance.objectNode();

        payload.put("username", "123");
        payload.put("password", "12345678901");

        ObjectNode req = payload;

        ObjectNode error = JsonNodeFactory.instance.objectNode();
        JsonNode reqUsername = req.get("username");
        JsonNode reqPassword = req.get("password");

        ObjectNode errUsername = validateUsername(req);
        if (errUsername != null) {
            error.put("username", errUsername);
        }

        ObjectNode errPassword = validatePassword(req);
        if (errUsername != null) {
            error.put("password", errPassword);
        }

        assertTrue(error.get("username").get("min") != null);
        assertTrue(error.get("password").get("max") != null);
    }

    //
    // handle error:
    // error: {
    //    message: ""
    // }
    //
}
