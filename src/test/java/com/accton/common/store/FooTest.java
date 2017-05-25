package com.accton.common.store;

import com.fasterxml.jackson.annotation.*;
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
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.github.fge.jsonschema.main.JsonValidator;
import com.google.gson.Gson;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

class JsonSchemaManager {
    private final JsonValidator validator = JsonSchemaFactory.byDefault().getValidator();
    private Map<Class<?>, JsonNode> jsonNodeMap = new HashMap<>();

    public void load(Class<?> className, String schema) throws IOException {
        JsonNode schemaFromDisk = JsonLoader.fromURL(this.getClass().getResource(schema));
        jsonNodeMap.put(className, schemaFromDisk);
    }


    public ProcessingReport check(Class<?> className, JsonNode toBeValidate) {

        ProcessingReport report = null;
        try {
            report = validator.validate(jsonNodeMap.get(className), toBeValidate);
            return report;
//            if (!report.isSuccess()) {
//                StringBuilder stringBuilder = new StringBuilder();
//                stringBuilder.append(" Oops!! failed JSON validation ");
//                stringBuilder.append(":").append("\n");
//                List<ProcessingMessage> messages = Lists.newArrayList(report);
//
//                ObjectMapper mapper = new ObjectMapper();
//                ArrayNode errors = mapper.createArrayNode();
//
//                for (int i = 0; i < messages.size(); i++) {
//                    stringBuilder.append("- ");
//                    stringBuilder.append(messages.get(i).toString());
//                    stringBuilder.append((i == (messages.size()) - 1) ? "" : "\r");
//                }
//
//                for (Iterator<ProcessingMessage> iterator = messages.iterator(); iterator.hasNext();) {
//                    ProcessingMessage processingMessage = iterator.next();
//                    errors.add(processingMessage.asJson());
//                }
//
//                throw new RuntimeException(stringBuilder.toString());
//            }
        } catch (ProcessingException e) {
            throw new RuntimeJsonMappingException("ERROR -->" + e.toString());
        }
    }
}

enum Currency {
    INR, HKD, EUR, USD
}

@JsonPropertyOrder(value = {"id", "amount", "currency"})
@JsonRootName("transaction")
class Transaction implements Serializable {

    @JsonProperty
    private String id;
    @JsonProperty
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    private BigDecimal amount;
    @JsonProperty
    private Currency currency;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public double getAmount() {
        return amount.doubleValue();
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("Transaction{");
        sb.append("id='").append(id).append('\'');
        sb.append(", amount=").append(amount);
        sb.append(", currency=").append(currency);
        sb.append('}');
        return sb.toString();
    }
}

@JsonPropertyOrder(value = {"ip", "mac", "subnet"})
@JsonRootName("dhcp")
class DHCP implements Serializable {
    @JsonProperty
    private String ip;
    @JsonProperty
    private String mac;
    @JsonProperty
    private String subnet;

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public String getSubnet() {
        return subnet;
    }

    public void setSubnet(String subnet) {
        this.subnet = subnet;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("DHCP{");
        sb.append("ip='").append(ip).append('\'');
        sb.append(", mac=").append(mac);
        sb.append(", subnet=").append(subnet);
        sb.append('}');
        return sb.toString();
    }
}

class JSONConverter {

    protected static final ThreadLocal<ObjectMapper> OBJECT_MAPPER_THREAD_LOCAL = ThreadLocal.withInitial(() -> new ObjectMapper());

    /**
     * Convert String as JSON to JsonNode.
     *
     * @param payload
     * @return
     */
    public static JsonNode getJsonFromString(String payload) {
        try {
            return OBJECT_MAPPER_THREAD_LOCAL.get().readTree(payload);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Convert JsonNode Object to Map
     *
     * @param payload
     * @return
     */
    public static Map getMapFromJson(JsonNode payload) {
        try {
            return OBJECT_MAPPER_THREAD_LOCAL.get().readerFor(Map.class).readValue(payload);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Convert Map Object to JsonNode.
     *
     * @param payload
     * @return
     */
    public static JsonNode getJsonFromMap(Map payload) {
        try {
            return getJsonFromString(OBJECT_MAPPER_THREAD_LOCAL.get().writeValueAsString(payload));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Create a Empty Object Node. i.e {}
     *
     * @return
     */
    public static ObjectNode createObjectNode() {
        return OBJECT_MAPPER_THREAD_LOCAL.get().createObjectNode();
    }

    /**
     * Create a Empty Array Json Object i.e [{},{}]
     *
     * @return
     */
    public static ArrayNode createArrayNode() {
        return OBJECT_MAPPER_THREAD_LOCAL.get().createArrayNode();
    }

    /**
     * Remove particular key elements from JsonNode.
     *
     * @param payload
     * @param key
     * @return
     */
    public static JsonNode remove(JsonNode payload, String key) {
        ((ObjectNode) payload).remove(key);
        return payload;
    }
}

class CSVReader {

    protected final CsvMapper csvMapper = new CsvMapper();

    public List<JsonNode> load(Class<?> zlass, URL resourceCSVFile) throws IOException {

        CsvSchema csvSchema = csvMapper.typedSchemaFor(zlass).withHeader();
        MappingIterator it = new CsvMapper().readerFor(zlass)
                .with(csvSchema.withColumnSeparator(CsvSchema.DEFAULT_COLUMN_SEPARATOR))
                .readValues(resourceCSVFile);
        List<JsonNode> listOfJson = new ArrayList<>();
        while (it.hasNext()) {
            listOfJson.add(JSONConverter.getJsonFromString(new Gson().toJson(it.next())));
        }
        return listOfJson;
    }
}

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
    static final int NO_LIMIT = -1;

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

    Model(String name, Type type, boolean required, int min, int max) {
        this.name = name;
        this.type = type;
        //this.index = index;
        //this.unique = unique;
        this.required = required;
        this.min = min;
        this.max = max;
    }

    Model(String name, Type type, boolean required) {
        this(name, type, required, NO_LIMIT, NO_LIMIT);
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

        ObjectNode userObject = User.sanitize((ObjectNode) node);
        if (userObject == null) {
            throw new IOException("json is an invalid string");
        }

        return new User(userObject);
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

    public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";

    public static final int MIN_LENGTH_OF_USERNAME = 3;
    public static final int MAX_LENGTH_OF_USERNAME = 10;

    public static final int MIN_LENGTH_OF_PASSWORD = 3;
    public static final int MAX_LENGTH_OF_PASSWORD = 10;

    private static Map<String, Model> model = createModel();
    private static Map<String, Model> createModel() {
        Map<String, Model> model = new HashMap<>();

        model.put(USERNAME, new Model(USERNAME,
                Model.Type.STRING,
                true,
                MIN_LENGTH_OF_USERNAME,
                MAX_LENGTH_OF_USERNAME));

        model.put(PASSWORD, new Model(PASSWORD,
                Model.Type.STRING,
                true,
                MIN_LENGTH_OF_PASSWORD,
                MAX_LENGTH_OF_PASSWORD));

        model.put(CREATED, new Model(CREATED,
                Model.Type.DATE,
                false));

        model.put(LAST_PASSWORD_MODIFIED, new Model(LAST_PASSWORD_MODIFIED,
                Model.Type.DATE,
                false));

        return model;
    }

    private ObjectNode object;

    public User(ObjectNode objectNode) {
        object = objectNode;

        if (object.get(CREATED) == null || !object.get(CREATED).isTextual()) {
            set(CREATED, new SimpleDateFormat(DATE_FORMAT).format(currentTime()));
        }
    }

    public User(ObjectNode objectNode, String username, String password) {
        this(objectNode);

        setUsername(username);
        setPassword(password);
    }

    public User(String username, String password) {
        this(JsonNodeFactory.instance.objectNode(), username, password);
    }

    public static ObjectNode sanitize(ObjectNode objectNode) {
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

        ObjectNode innerObject = JsonNodeFactory.instance.objectNode();
        User user = new User(innerObject, objectNode.get(USERNAME).textValue(), objectNode.get(PASSWORD).textValue());

        if (objectNode.get(CREATED) != null) {
            user.set(CREATED, objectNode.get(CREATED).textValue());
        }

        if (objectNode.get(LAST_PASSWORD_MODIFIED) != null) {
            user.set(LAST_PASSWORD_MODIFIED, objectNode.get(LAST_PASSWORD_MODIFIED).textValue());
        }

        return innerObject;
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

    private static Date parseDateString(String str) {
        try {
            return (new SimpleDateFormat(DATE_FORMAT)).parse(str);
        } catch (ParseException e) {
            return null;
        }
    }

    private static Date currentTime() {
        return new java.util.Date();
    }

    public void setUsername(String username) {
        set(USERNAME, username);
    }

    public String getUsername() {
        return object.get(USERNAME).textValue();
    }

    public void setPassword(String password) {
        set(PASSWORD, password);
        set(LAST_PASSWORD_MODIFIED, new SimpleDateFormat(DATE_FORMAT).format(currentTime()));
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
        return parseDateString(str);
    }

    public Date getLastPasswordModified() {
        String str = object.get(LAST_PASSWORD_MODIFIED).textValue();
        return parseDateString(str);
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

//        for (Iterator<User> iterator = value.getUsers(); iterator.hasNext();) {
        for (User user : value.getUsers()) {
//            User user = iterator.next();

            jgen.writeObject(user);
        }

        jgen.writeEndArray();
        jgen.writeEndObject();
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
                JsonNode unsafeNode = iterator.next();

                if (!unsafeNode.isObject()) {
                    continue;
                }

                ObjectNode userNode = User.sanitize((ObjectNode) unsafeNode);
                if (userNode == null) {
                    continue;
                }

                config.addRawUser(userNode);
            }
        }

        return config;
    }
}

@JsonSerialize(using = UserConfigSerializer.class)
@JsonDeserialize(using = UserConfigDeserializer.class)
class UserConfig {
    private ArrayNode users;

    protected UserConfig() {
        users = JsonNodeFactory.instance.arrayNode();
    }

    public void addUser(String username, String password) throws IllegalArgumentException {
        // TODO: check username and password

        for (Iterator<JsonNode> iterator = users.iterator(); iterator.hasNext();) {
            JsonNode user = iterator.next();

            if (user.get("username").textValue().equalsIgnoreCase(username)) {
                throw new IllegalArgumentException(String.format("user (%s) is existed", username));
            }
        }

        ObjectNode userNode = JsonNodeFactory.instance.objectNode();
        new User(userNode, username, password);
        addRawUser(userNode);
    }

    @Deprecated
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

    public void addRawUser(ObjectNode userNode) {
        users.add(userNode);
    }

    public void delUser(String username) {
        for (int i = 0; i < users.size(); ++i) {
            ObjectNode userNode = (ObjectNode) users.get(i);

            if (userNode.get("username").textValue().equals(username)) {
                users.remove(i);
                return;
            }
        }
    }

    public void setPassword(String username, String newPassword) {
        User user = getUser(username);
        if (user != null) {
            user.setPassword(newPassword);
        }
    }

    public User getUser(String username) {
        for (int i = 0; i < users.size(); ++i) {
            ObjectNode userNode = (ObjectNode) users.get(i);

            if (userNode.get("username").textValue().equals(username)) {
                return new User(userNode);
            }
        }

        return null;
    }

//    public Iterator<User> getUsers() {
//        return users.iterator();
//    }

    public ArrayList<User> getUsers() {
        ArrayList<User> arrayList = new ArrayList<>();

        for (int i = 0; i < users.size(); ++i) {
            ObjectNode objectNode = (ObjectNode) users.get(i);

            arrayList.add(new User(objectNode));
        }

        return arrayList;
    }

    public boolean equals(UserConfig other) {
        return users.equals(other.users);
    }
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
        ObjectNode userObject = JsonNodeFactory.instance.objectNode();
        User user = new User(userObject, "myuser", "mypassword");

        ObjectMapper mapper = new ObjectMapper();

        try {
            String jsonString = mapper.writeValueAsString(user);
            ObjectNode object = (ObjectNode) mapper.readTree(jsonString);

            assertTrue(object.equals(userObject));

//            User cloneUser = User.deserialize(object);
//            assertTrue(cloneUser.equals(user));
        } catch (IOException e) {
            fail();
        }
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

            ObjectMapper mapper = new ObjectMapper();

            String string = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(cfg);

            UserConfig cloneCfg = mapper.readValue(string, UserConfig.class);

            assertTrue(cfg.getUser("myuser").equals(cloneCfg.getUser("myuser")));
            assertTrue(cfg.getUser("auser").equals(cloneCfg.getUser("auser")));

            assertTrue(cloneCfg.equals(cfg) == true);

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

    public void testJsonValidate() {
        try {
            JsonSchemaManager jsonSchemaManager = new JsonSchemaManager();
            jsonSchemaManager.load(Transaction.class, "/schema/trade-schema.json");

            CSVReader csvReader = new CSVReader();
            List<JsonNode> lists = csvReader.load(Transaction.class, FooTest.class.getClassLoader().getResource("trade.csv"));
            for (int i = 0; i < lists.size(); i++) {
                //validate each JsonNode of Type Transaction to Schema file.
                jsonSchemaManager.check(Transaction.class, lists.get(i));
                lists.get(i);
            }
        } catch (IOException e) {
            fail();
        }
    }

    ArrayNode processReportToResErrors(ProcessingReport report) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode jsonNodes = mapper.createArrayNode();

        for (Iterator<ProcessingMessage> iterator = report.iterator(); iterator.hasNext();) {
            ProcessingMessage processingMessage = iterator.next();
            jsonNodes.add(processingMessage.asJson());
        }

        return jsonNodes;
    }

    void checkErrors(ArrayNode errors) {
        for (Iterator<JsonNode> it = errors.iterator(); it.hasNext();) {
            JsonNode error = it.next();
            assertTrue(error.isObject());
            assertTrue(((ObjectNode)error).get("level") != null);
            assertTrue(((ObjectNode)error).get("schema") != null);
            assertTrue(((ObjectNode)error).get("instance") != null);
            assertTrue(((ObjectNode)error).get("keyword") != null);
            assertTrue(((ObjectNode)error).get("message") != null);
            assertTrue(((ObjectNode)error).get("found") != null);
            assertTrue(((ObjectNode)error).get("expected") != null);
            //assertTrue(((ObjectNode)error).get("keyword").textValue().equals("type"));
        }
    }

    public void testJsonDHCPValidate_Sucess() {
        try {
            JsonSchemaManager jsonSchemaManager = new JsonSchemaManager();
            jsonSchemaManager.load(DHCP.class, "/schema/dhcp-schema.json");

            ObjectNode dhcpReq = JsonNodeFactory.instance.objectNode();
            dhcpReq.put("ip", "1.2.3.4");
            dhcpReq.put("mac", "ca:fe:ca:fe:ca:fe");
            dhcpReq.put("subnet", "255.255.252.0");

            ProcessingReport report = jsonSchemaManager.check(DHCP.class, dhcpReq);
            assertTrue(report.isSuccess());

            ObjectMapper mapper = new ObjectMapper();
            DHCP dhcp = mapper.readValue(dhcpReq.toString(), DHCP.class);
            System.out.printf("");

        } catch (RuntimeException e) {
            fail();
        }  catch (IOException e) {
            fail();
        }
    }

    public void testJsonDHCPValidate_InvalidType() {
        try {
            JsonSchemaManager jsonSchemaManager = new JsonSchemaManager();
            jsonSchemaManager.load(DHCP.class, "/schema/dhcp-schema.json");

            ObjectNode dhcpReq = JsonNodeFactory.instance.objectNode();
            //dhcpReq.put("ip", "1.2.3.4");
            dhcpReq.put("ip", 123);
            //dhcpReq.put("mac", "ca:fe:ca:fe:ca:fe");
            dhcpReq.put("mac", 123);
            dhcpReq.put("subnet", "255.255.252.0");

            ProcessingReport report = jsonSchemaManager.check(DHCP.class, dhcpReq);
            assertTrue(report.isSuccess() != true);

            ArrayNode errors = processReportToResErrors(report);
            checkErrors(errors);

            assertTrue(errors.size() == 2);
            for (Iterator<JsonNode> it = errors.iterator(); it.hasNext();) {
                JsonNode error = it.next();
//                assertTrue(error.isObject());
//                assertTrue(((ObjectNode)error).get("level") != null);
//                assertTrue(((ObjectNode)error).get("schema") != null);
//                assertTrue(((ObjectNode)error).get("instance") != null);
//                assertTrue(((ObjectNode)error).get("keyword") != null);
//                assertTrue(((ObjectNode)error).get("message") != null);
//                assertTrue(((ObjectNode)error).get("found") != null);
//                assertTrue(((ObjectNode)error).get("expected") != null);

                assertTrue(((ObjectNode)error).get("keyword").textValue().equals("type"));
            }
        } catch (RuntimeException e) {
            fail();
        }  catch (IOException e) {
            fail();
        }
    }

    public void testJsonDHCPValidate_MissRequired() {
        try {
            JsonSchemaManager jsonSchemaManager = new JsonSchemaManager();
            jsonSchemaManager.load(DHCP.class, "/schema/dhcp-schema.json");

            ObjectNode dhcpReq = JsonNodeFactory.instance.objectNode();
            //dhcpReq.put("ip", "1.2.3.4");
            //dhcpReq.put("mac", "ca:fe:ca:fe:ca:fe");
            dhcpReq.put("subnet", "255.255.252.0");

            ProcessingReport report = jsonSchemaManager.check(DHCP.class, dhcpReq);
            assertTrue(report.isSuccess() != true);

            ArrayNode errors = processReportToResErrors(report);
            assertTrue(errors.size() == 1);
        } catch (RuntimeException e) {
            fail();
        }  catch (IOException e) {
            fail();
        }
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
