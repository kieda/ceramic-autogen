package io.hostilerobot.ceramicrelief.controller;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.function.Consumer;

public class JsonDataController<T> extends AbstractDataController<T> {

    protected static ObjectMapper mapper;
    static {
        mapper = JsonMapper.builder()
                .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
                .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
                .enable(JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES)
                .enable(JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER)
                .enable(JsonReadFeature.ALLOW_LEADING_ZEROS_FOR_NUMBERS)
                .enable(JsonReadFeature.ALLOW_LEADING_PLUS_SIGN_FOR_NUMBERS)
                .enable(JsonReadFeature.ALLOW_LEADING_DECIMAL_POINT_FOR_NUMBERS)
                .enable(JsonReadFeature.ALLOW_TRAILING_DECIMAL_POINT_FOR_NUMBERS)
                .build();
    }
    private final Class<T> clazz;

    protected JsonDataController(Class<T> clazz) {
        this.clazz = clazz;
    }

    public static <T> Builder<T> builder(final Class<T> clazz){
        return new Builder<>(() -> new JsonDataController<>(clazz));
    }

    @Override
    public T transform(InputStream input) throws IOException {
        return mapper.readValue(input, clazz);
    }
}
