package io.yggdrash.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonUtil {
    private static final Logger log = LoggerFactory.getLogger(JsonUtil.class);

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final JsonParser jsonParser = new JsonParser();

    public static JsonObject convertMapToJson(Map map) {
        String json = convertObjToString(map);
        return parseJsonObject(json);
    }

    public static HashMap convertJsonToMap(JsonElement json) {
        try {
            return mapper.readValue(json.toString(), HashMap.class);
        } catch (IOException e) {
            log.warn("convert fail json to map err={}", e);
            return null;
        }
    }

    public static String convertObjToString(Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("convert fail obj to string err={}", e);
            return null;
        }
    }

    public static JsonArray parseJsonArray(String json) {
        return (JsonArray) jsonParser.parse(json);
    }

    public static JsonObject parseJsonObject(String json) {
        return (JsonObject) jsonParser.parse(json);
    }

    public static JsonObject parseJsonObject(Reader json) {
        return (JsonObject) jsonParser.parse(json);
    }

    public static List<String> convertJsonArrayToStringList(JsonArray array) {
        List<String> stringList = new ArrayList<>();
        Iterator<JsonElement> elementIterator = array.iterator();
        while (elementIterator.hasNext()) {
            JsonElement element = elementIterator.next();
            stringList.add(element.getAsString());
        }
        return stringList;
    }


}
