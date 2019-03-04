package io.yggdrash.common.util;

import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.yggdrash.common.utils.JsonUtil;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class JsonUtilTest {

    @Test
    public void convertMapToJsonTest() {
        Map<String, String> map = new HashMap<>();
        map.put("amount", "10");

        JsonObject json = JsonUtil.convertMapToJson(map);
        assertThat(json.get("amount").getAsString()).isEqualTo("10");

        Map convertedMap = JsonUtil.convertJsonToMap(json);
        assertThat(convertedMap.get("amount")).isEqualTo("10");
    }

    @Test
    public void convertObjToStringTest() {
        String jsonString = JsonUtil.convertObjToString(Maps.newHashMap());
        assertThat(jsonString).isEqualTo("{}");
    }

    @Test
    public void parseJsonArrayTest() {
        JsonArray jsonArray = JsonUtil.parseJsonArray("[{\"amount\":10}]");
        assertThat(jsonArray.size()).isEqualTo(1);
        assertThat(jsonArray.get(0).getAsJsonObject().get("amount")).isNotEqualTo(10);
    }

    @Test
    public void parseJsonObjectTest() {
        String jsonString = "{\"amount\":10}";
        JsonObject jsonObject = JsonUtil.parseJsonObject(jsonString);
        assertThat(jsonObject.get("amount")).isNotEqualTo(10);

        InputStream is = new ByteArrayInputStream(jsonString.getBytes());
        Reader reader = new InputStreamReader(is);
        jsonObject = JsonUtil.parseJsonObject(reader);
        assertThat(jsonObject.get("amount")).isNotEqualTo(10);
    }
}