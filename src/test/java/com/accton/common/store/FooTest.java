package com.accton.common.store;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

class SchemaValidationException extends Exception {
    public SchemaValidationException(String message) {
        super(message);
    }
}

class RequiredSchemaValidationException extends SchemaValidationException {
    public RequiredSchemaValidationException(String message) {
        super(message);
    }
}

class TypeSchemaValidationException extends SchemaValidationException {
    public TypeSchemaValidationException(String message) {
        super(message);
    }
}

class MinSchemaValidationException extends SchemaValidationException {
    public MinSchemaValidationException(String message) {
        super(message);
    }
}

class MaxSchemaValidationException extends SchemaValidationException {
    public MaxSchemaValidationException(String message) {
        super(message);
    }
}

// Schema
class Model {
    public enum Type {
        STRING,
        Number,
        DATE,
        Boolean
    }

    String name;
    Type type;
    boolean index;
    boolean unique;
    boolean required;
    int min;
    int max;

    Model(String name, Type type, boolean index, boolean unique, boolean required, int min, int max) {
        this.name = name;
        this.type = type;
        this.index = index;
        this.unique = unique;
        this.required = required;
        this.min = min;
        this.max = max;
    }

    public boolean validateRequired(JsonNode value) {
        return (required && value != null) || !required;
    }

    public boolean validateType(JsonNode value) {
        switch (type) {
            case STRING:
            case DATE:
                if (value.isTextual()) {
                    return true;
                }
                break;
        }

        return false;
    }

    public boolean validate(JsonNode value) {
        if (validateRequired(value) != true) {
            return false;
        }

        if (value != null) {
            if (validateType(value) != true) {
                return false;
            }
        }

        return true;
    }
}

class UserSerializer extends StdSerializer<User> {
    public UserSerializer() {
        this(null);
    }

    public UserSerializer(Class<User> t) {
        super(t);
    }

    @Override
    public void serialize(User value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonProcessingException {
        jgen.writeStartObject();
        jgen.writeStringField("username", value.getUsername());
        jgen.writeStringField("password", value.getPassword());
        jgen.writeStringField("created",
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(value.getCreated()));
        jgen.writeStringField("lastPasswordModified",
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(value.getLastPasswordModified()));
        jgen.writeEndObject();
    }
}

class UserDeserializer extends StdDeserializer<User> {
    public UserDeserializer() {
        this(null);
    }

    public UserDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public User deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException {
        JsonNode node = jp.getCodec().readTree(jp);
        if (!node.isObject()) {
            throw new IOException("json is not a object");
        }

        User user = User.create((ObjectNode) node);
        if (user == null) {
            throw new IOException("json is an invalid string");
        }

        return user;
//        int id = (Integer) ((IntNode) node.get("id")).numberValue();
//        String itemName = node.get("itemName").asText();
//        int userId = (Integer) ((IntNode) node.get("createdBy")).numberValue();
//
//        return new Item(id, itemName, new User(userId, null));
    }
}

@JsonSerialize(using = UserSerializer.class)
@JsonDeserialize(using = UserDeserializer.class)
class User {
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final String CREATED = "created";
    private static final String LAST_PASSWORD_MODIFIED = "lastPasswordModified";

    private static Map<String, Model> model = createModel();
    private static Map<String, Model> createModel() {
        Map<String, Model> model = new HashMap<>();

        model.put(USERNAME, new Model(USERNAME, Model.Type.STRING, true, true, true, 3, 10));
        model.put(PASSWORD, new Model(PASSWORD, Model.Type.STRING, false, true, true, 3, 10));
        model.put(CREATED, new Model(CREATED, Model.Type.DATE, false, false, false, -1, -1));
        model.put(LAST_PASSWORD_MODIFIED, new Model(LAST_PASSWORD_MODIFIED, Model.Type.DATE, false, false, false, -1, -1));

        return model;
    }

    public static Date date = Calendar.getInstance().getTime();

    private ObjectNode object;

    private User() {
        object = JsonNodeFactory.instance.objectNode();
        set(CREATED, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(date));
    }

    public User(String username, String password) {
        this();

        setUsername(username);
        setPassword(password);
    }

    // deserialize
    public static User create(ObjectNode objectNode) {
        for (Iterator<Map.Entry<String, JsonNode>> iterator = objectNode.fields(); iterator.hasNext();) {
            Map.Entry<String, JsonNode> attr = iterator.next();
            Model model = User.model.get(attr.getKey());

            if (model == null) {
                continue;
            }

            if (!model.validate(attr.getValue())) {
                return null;
            }
        }

        User user = new User();

        user.set(USERNAME, objectNode.get(USERNAME).textValue());
        user.set(PASSWORD, objectNode.get(PASSWORD).textValue());

        if (objectNode.get(CREATED) != null) {
            user.set(CREATED, objectNode.get(CREATED).textValue());
        }

        if (objectNode.get(LAST_PASSWORD_MODIFIED) != null) {
            user.set(LAST_PASSWORD_MODIFIED, objectNode.get(LAST_PASSWORD_MODIFIED).textValue());
        }

        return user;
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

    public void setUsername(String username) {
        set(USERNAME, username);
    }

    public String getUsername() {
        return object.get(USERNAME).textValue();
    }

    public void setPassword(String password) {
        set(PASSWORD, password);
        set(LAST_PASSWORD_MODIFIED, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(date));
    }

    public String getPassword() {
        JsonNode jsonNode = object.get(PASSWORD);

        if (jsonNode != null) {
            return jsonNode.textValue();
        }

        return null;
    }

    public Date getCreated() {
        String str = object.get(CREATED).textValue();
        try {
            return (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")).parse(str);
        } catch (ParseException e) {
            return null;
        }
    }

    public Date getLastPasswordModified() {
        String str = object.get(LAST_PASSWORD_MODIFIED).textValue();
        try {
            return (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")).parse(str);
        } catch (ParseException e) {
            return null;
        }
    }

    public boolean equals(User other) {
        return object.equals(other.object);
    }
}

class UserConfigSerializer extends StdSerializer<UserConfig> {
    public UserConfigSerializer() {
        this(null);
    }

    public UserConfigSerializer(Class<UserConfig> t) {
        super(t);
    }

    @Override
    public void serialize(UserConfig value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonProcessingException {

        jgen.writeStartObject();
        jgen.writeArrayFieldStart("users");

        for (Iterator<User> iterator = value.getUsers(); iterator.hasNext();) {
            User user = iterator.next();

            jgen.writeObject(user);
        }

        jgen.writeEndArray();
        jgen.writeEndObject();

//        jgen.writeStartObject();
//        jgen.writeStringField("username", value.getUsername());
//        jgen.writeStringField("password", value.getPassword());
//        jgen.writeStringField("created",
//                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(value.getCreated()));
//        jgen.writeStringField("lastPasswordModified",
//                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(value.getLastPasswordModified()));
//        jgen.writeEndObject();
    }
}

class UserConfigDeserializer extends StdDeserializer<UserConfig> {
    public UserConfigDeserializer() {
        this(null);
    }

    public UserConfigDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public UserConfig deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException {
        JsonNode node = jp.getCodec().readTree(jp);
        if (!node.isObject()) {
            throw new IOException("json is not a object");
        }

        UserConfig config = new UserConfig();

        if (node.get("users") != null && node.get("users").isArray()) {
            ArrayNode usersNode = (ArrayNode) node.get("users");

            for (Iterator<JsonNode> iterator = usersNode.iterator(); iterator.hasNext();) {
                JsonNode userNode = iterator.next();

                if (!userNode.isObject()) {
                    continue;
                }

                User user = User.create((ObjectNode) userNode);
                if (user == null) {
                    continue;
                }

                config.addUser(user);
            }
        }

        return config;
    }
}

@JsonSerialize(using = UserConfigSerializer.class)
@JsonDeserialize(using = UserConfigDeserializer.class)
class UserConfig {
    private ArrayList<User> users;

    public UserConfig() {
        users = new ArrayList<>();
    }

    public void addUser(User user) {
        users.add(user);
    }

    public void addUser(String username, String password) throws IllegalArgumentException {
        // TODO: check username and password

        for (Iterator<User> iterator = users.iterator(); iterator.hasNext();) {
            User user = iterator.next();

            if (user.getUsername().equalsIgnoreCase(username)) {
                throw new IllegalArgumentException(String.format("user (%s) is existed", username));
            }
        }

        users.add(new User(username, password));
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

        addUser(username.textValue(), password.textValue());
    }

    public void delUser(String username) {
        users.removeIf(s -> s.getUsername().equals(username));
    }

    public void setPassword(String username, String newPassword) {
        User user = getUser(username);
        if (user != null) {
            user.setPassword(newPassword);
        }
    }

    public User getUser(String username) {
        for (User user : users) {
            if (user.getUsername().equals(username)) {
                return user;
            }
        }

        return null;
    }

    public Iterator<User> getUsers() {
        return users.iterator();
    }

//    public boolean equals(UserConfig other) {
//        //ArrayList commonList = CollectionUtils.retainAll(list1,list2);
//        Set<User> set1 = new HashSet<User>(users);
//        for (User user : other.users) {
//            if (!set1.contains(user)) {
//                return false;
//            }
//        }
//
//        return true;
//    }
}

class Foo
{
    public String bar;

    Foo(String bar)
    {
        this.bar = bar;
    }
    Foo() {}
}

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

    ////
    // Jackson test ...
    //
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
        assertTrue(jsonNode.isTextual());
        assertTrue(jsonNode.textValue().equals("string value"));
    }

    public void testConvertIntToJsonNode() {
        int intValue = 123;

        ObjectMapper mapper = new ObjectMapper();

        JsonNode jsonNode = mapper.convertValue(intValue, JsonNode.class);
        assertTrue(jsonNode.isInt());
        assertTrue(jsonNode.intValue() == 123);
    }

    public void testConvertArrayToJsonNode() {
        int[] intArray = new int[2];
        intArray[0] = 11;
        intArray[1] = 22;

        ObjectMapper mapper = new ObjectMapper();

        JsonNode jsonNode = mapper.convertValue(intArray, JsonNode.class);
        assertTrue(jsonNode.isArray());

        assertTrue(jsonNode.get(0).isInt());
        assertTrue(jsonNode.get(0).intValue() == 11);
    }

    public void testConvertMapToJsonNode() {
        Map<String, Object> map = new HashMap<>();
        map.put("username", "myusername");
        map.put("age", 123);

        ObjectMapper mapper = new ObjectMapper();

        JsonNode jsonNode = mapper.convertValue(map, JsonNode.class);
        assertTrue(jsonNode.isObject());

        assertTrue(jsonNode.get("username").isTextual());
        assertTrue(jsonNode.get("username").textValue().equals("myusername"));

        assertTrue(jsonNode.get("age").isInt());
        assertTrue(jsonNode.get("age").intValue() == 123);
    }

    ////
    // user test ...
    //
    public void testCreateUser() {
        User user = new User("myusername", "mypassword");
        assertTrue(user.getUsername().equals("myusername"));
        assertTrue(user.getPassword().equals("mypassword"));

        Date created = user.getCreated();
        assertTrue(created != null);

        Date modified = user.getLastPasswordModified();
        assertTrue(modified != null);
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

    public void testToString() {
        User user = new User("myuser", "mypassword");

        ObjectMapper mapper = new ObjectMapper();

        try {
            String jsonString = mapper.writeValueAsString(user);
            ObjectNode object = (ObjectNode) mapper.readTree(jsonString);

            assertTrue(object.get("username").textValue().equals("myuser"));
            assertTrue(object.get("password").textValue().equals("mypassword"));
            assertTrue(object.get("created").isTextual());
            assertTrue(object.get("lastPasswordModified").isTextual());
        } catch (IOException e) {
            fail();
        }
    }

    public void testUserSerialize() {
        User user = new User("myuser", "mypassword");

        ObjectMapper mapper = new ObjectMapper();

        try {
            String jsonString = mapper.writeValueAsString(user);
            ObjectNode object = (ObjectNode) mapper.readTree(jsonString);

            User cloneUser = User.create(object);

            assertTrue(cloneUser.equals(user));
        } catch (IOException e) {
            fail();
        }
    }

    public void testUserSerializeByObjectMapper() {
        User user = new User("myuser", "mypassword");

        ObjectMapper mapper = new ObjectMapper();

        try {
            String jsonString = mapper.writeValueAsString(user);
            User cloneUser = mapper.readValue(jsonString, User.class);

            assertTrue(cloneUser.equals(user));
        } catch (IOException e) {
            fail();
        }
    }

    public void testUiAddUserViaUser() {
        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        UserConfig cfg = new UserConfig();

        payload.put("username", "myusername");
        payload.put("password", "mypassword");

        ObjectNode req = payload;

        User user = new User(req.get("username").textValue(), req.get("password").textValue());
        cfg.addUser(user);

        assertTrue(cfg.getUser("myusername").getUsername().equals("myusername"));
        assertTrue(cfg.getUser("myusername").getPassword().equals("mypassword"));
    }

    public void testUiAddUser() {
        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        UserConfig cfg = new UserConfig();

        payload.put("username", "myusername");
        payload.put("password", "mypassword");

        ObjectNode req = payload;

        cfg.addUser(req.get("username").textValue(), req.get("password").textValue());

        assertTrue(cfg.getUser("myusername").getUsername().equals("myusername"));
        assertTrue(cfg.getUser("myusername").getPassword().equals("mypassword"));
    }

    public void testUiAddUserViaObjectNode() {
        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        UserConfig cfg = new UserConfig();

        payload.put("username", "myusername");
        payload.put("password", "mypassword");

        ObjectNode req = payload;

        cfg.addUser(req);

        assertTrue(cfg.getUser("myusername").getUsername().equals("myusername"));
        assertTrue(cfg.getUser("myusername").getPassword().equals("mypassword"));
    }

    ////
    // user config test ...
    //
    public void testAddUser() {
        UserConfig cfg = new UserConfig();

        try {
            cfg.addUser("myuser", "mypassword");

            User user = cfg.getUser("myuser");
            assertTrue(user.getUsername().equals("myuser"));
            assertTrue(user.getPassword().equals("mypassword"));
            assertTrue(user.getCreated() != null);
            assertTrue(user.getLastPasswordModified() != null);
        } catch (IllegalArgumentException e) {
            fail();
        }
    }

    public void testCantAddUserIfHaveSameName() {
        UserConfig cfg = new UserConfig();

        try {
            cfg.addUser("myuser", "mypassword");
            cfg.addUser("MyUser", "mypassword");
            fail();
        } catch (IllegalArgumentException e) {
        }
    }

    public void testDeleteUser() {
        UserConfig cfg = new UserConfig();

        try {
            cfg.addUser("myuser", "mypassword");

            assertTrue(cfg.getUser("myuser") != null);

            cfg.delUser("myuser");

            assertTrue(cfg.getUser("myuser") == null);
        } catch (IllegalArgumentException e) {
            fail();
        }
    }

    public void testChangePassowd() {
        UserConfig cfg = new UserConfig();

        try {
            cfg.addUser("myuser", "mypassword");

            assertTrue(cfg.getUser("myuser").getPassword().equals("mypassword"));

            cfg.setPassword("myuser", "newPassword");

            assertTrue(cfg.getUser("myuser").getPassword().equals("newPassword"));
        } catch (IllegalArgumentException e) {
            fail();
        }
    }

    public void testUserConfigSerialize() {
        UserConfig cfg = new UserConfig();

        try {
            cfg.addUser("myuser", "mypassword");
            cfg.addUser("auser", "apassword");

            //String string = cfg.toJSON();

            ObjectMapper mapper = new ObjectMapper();

            String string = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(cfg);

            ObjectNode jsonConfig = (ObjectNode) mapper.readTree(string);
            ArrayNode array = (ArrayNode) jsonConfig.get("users");

            assertTrue(array.size() == 2);

            for (int i = 0; i < array.size(); ++i) {
                assertTrue(array.get(i).get("username").isTextual());
                assertTrue(array.get(i).get("password").isTextual());
                assertTrue(array.get(i).get("created").isTextual());
                assertTrue(array.get(i).get("lastPasswordModified").isTextual());
            }

            assertTrue(array.get(0).get("username").textValue().equals("myuser"));
            assertTrue(array.get(0).get("password").textValue().equals("mypassword"));

            assertTrue(array.get(1).get("username").textValue().equals("auser"));
            assertTrue(array.get(1).get("password").textValue().equals("apassword"));
        } catch (IllegalArgumentException e) {
            fail();
        } catch (IOException e) {
            fail();
        }
    }

    public void testUserConfigDeserialize() {
        UserConfig cfg = new UserConfig();

        try {
            cfg.addUser("myuser", "mypassword");
            cfg.addUser("auser", "apassword");

            //String string = cfg.toJSON();

            ObjectMapper mapper = new ObjectMapper();

            String string = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(cfg);

            UserConfig cloneCfg = mapper.readValue(string, UserConfig.class);

            //assertTrue(cloneCfg.equals(cfg) == true);
            assertTrue(cfg.getUser("myuser").equals(cloneCfg.getUser("myuser")));
            assertTrue(cfg.getUser("auser").equals(cloneCfg.getUser("auser")));

        } catch (IllegalArgumentException e) {
            fail();
        } catch (IOException e) {
            fail();
        }
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

    ObjectNode validateInputParameter(JsonNode argument, Schema schema) throws SchemaValidationException {
        ObjectNode error = JsonNodeFactory.instance.objectNode();

        if (schema.required == true && argument == null) {
            error.put("required", "missing required parameter");
            throw new RequiredSchemaValidationException("missing required parameter");
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

    ObjectNode validateUsername(ObjectNode req) throws SchemaValidationException {
        JsonNode username = req.get("username");
        return validateInputParameter(username, new Schema(true, "string", 4, 8));
    }

    ObjectNode validatePassword(ObjectNode req) throws SchemaValidationException {
        JsonNode password = req.get("password");
        return validateInputParameter(password, new Schema(true, "string", 4, 8));
    }

    public void testValidateInputParameterRequired() {
        ObjectNode payload = JsonNodeFactory.instance.objectNode();

        ObjectNode req = payload;

        ObjectNode error = JsonNodeFactory.instance.objectNode();
        JsonNode reqUsername = req.get("username");
        JsonNode reqPassword = req.get("password");

        try {
            ObjectNode errUsername = validateUsername(req);
            if (errUsername != null) {
                error.put("username", errUsername);
            }

            fail();
        } catch (RequiredSchemaValidationException e) {
            ObjectNode err = JsonNodeFactory.instance.objectNode();
            err.put("required", "missing required parameter");
            error.put("username", err);
            //error.put("required", "missing required parameter");
        } catch (SchemaValidationException e) {
            fail();
        }

        try {
            ObjectNode errPassword = validatePassword(req);
            if (errPassword != null) {
                error.put("password", errPassword);
            }

            fail();
        } catch (RequiredSchemaValidationException e) {
            ObjectNode err = JsonNodeFactory.instance.objectNode();
            err.put("required", "missing required parameter");
            error.put("password", err);
        } catch (SchemaValidationException e) {
            fail();
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

        try {
            ObjectNode errUsername = validateUsername(req);
            if (errUsername != null) {
                error.put("username", errUsername);
            }
        } catch (SchemaValidationException e) {
            fail();
        }

        try {

            ObjectNode errPassword = validatePassword(req);
            if (errPassword != null) {
                error.put("password", errPassword);
            }
        } catch (SchemaValidationException e) {
            fail();
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

        try {
            ObjectNode errUsername = validateUsername(req);
            if (errUsername != null) {
                error.put("username", errUsername);
            }
        } catch (SchemaValidationException e) {
            fail();
        }

        try {

            ObjectNode errPassword = validatePassword(req);
            if (errPassword != null) {
                error.put("password", errPassword);
            }
        } catch (SchemaValidationException e) {
            fail();
        }

        assertTrue(error.get("username").get("min") != null);
        assertTrue(error.get("password").get("max") != null);
    }

    public void testSerializationNull() {
        ObjectNode objectNode = JsonNodeFactory.instance.objectNode();

        objectNode.put("string", "");
        objectNode.put("int", 1);
        //objectNode.put("float", 1.0f);
        //objectNode.put("null", (String) null);

        ObjectMapper mapper = new ObjectMapper();
//        mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        try {
            String s = mapper.writeValueAsString(objectNode);
            System.out.println(s);

            ObjectWriter write = mapper.writer()
                    .with(SerializationFeature.WRITE_NULL_MAP_VALUES)
                    .with(SerializationFeature.INDENT_OUTPUT);
            String string = write.writeValueAsString(objectNode);

            //String string = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(objectNode);
            System.out.println(string);

            ObjectNode newNode = (ObjectNode) mapper.readTree(string);
            boolean x = newNode.equals(objectNode);

            assertTrue(x);

        } catch (JsonProcessingException e) {
            fail();
        } catch (IOException e) {
            fail();
        }
    }

    public void testFoo() {
        Map<String, Foo> foos = new HashMap<String, Foo>();
        foos.put("foo1", new Foo("foo1"));
        foos.put("foo2", new Foo(null));
        foos.put("foo3", null);
        //foos.put(null, new Foo("foo4"));

        // System.out.println(new ObjectMapper().writeValueAsString(foos));
        // Exception: Null key for a Map not allowed in JSON (use a converting NullKeySerializer?)

        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

            ObjectWriter write = mapper.writer()
                    .with(SerializationFeature.WRITE_NULL_MAP_VALUES)
                    .with(SerializationFeature.INDENT_OUTPUT);

            System.out.println(write.writeValueAsString(foos));
        } catch (JsonProcessingException e) {
            fail();
        }
    }

    public void testDeserializer() {
        ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
        objectNode.set("bar", JsonNodeFactory.instance.textNode("foo"));

        try {
            ObjectMapper mapper = new ObjectMapper();
            String json = objectNode.toString();
            Foo foo = mapper.readValue(json, Foo.class);

            assertTrue(foo.bar.equals("foo"));
        } catch (JsonGenerationException e) {
            System.out.println(e);
            fail();
        } catch (JsonMappingException e) {
            System.out.println(e);
            fail();
        } catch (IOException e) {
            System.out.println(e);
            fail();
        }
    }

    //
    // handle error:
    // error: {
    //    message: ""
    // }
    //
}
