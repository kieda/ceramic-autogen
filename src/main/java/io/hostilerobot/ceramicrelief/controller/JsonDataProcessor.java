package io.hostilerobot.ceramicrelief.controller;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.UnaryOperator;

public class JsonDataProcessor<T> implements DataProcessor<T>{
    private static final ObjectMapper mapper;
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
    private final UnaryOperator<JsonNode> extract;

    private JsonNode previous = null;

    private JsonDataProcessor(Class<T> clazz, UnaryOperator<JsonNode> extract) {
        this.clazz = clazz;
        this.extract = extract;
    }

    private JsonNode extract(InputStream in) throws IOException{
        JsonNode startingJson = mapper.readTree(in);
        if(extract != null) return extract.apply(startingJson);
        return startingJson;
    }

    @Override
    public T transform(InputStream in) throws IOException{
        JsonNode input = extract(in);
        previous = input;
        return mapper.treeToValue(input, clazz);
    }

    @Override
    public boolean merge(T current, InputStream in) throws IOException {
        JsonNode input = extract(in);
        boolean same = input.equals(previous);
        previous = input;
        mapper.readerForUpdating(current)
                .readValue(input);

        return !same;
    }

    public static <T> Builder<T> builder(Class<T> clazz) {
        return new Builder<>(clazz);
    }

    public static final class Builder<T> {
        private final Class<T> clazz;
        private UnaryOperator<JsonNode> extract = null;

        private Builder(Class<T> clazz) {
            this.clazz = clazz;
        }
        public Builder<T> extractFunction(UnaryOperator<JsonNode> extractFunction) {
            this.extract = extractFunction;
            return this;
        }
        public JsonDataProcessor<T> build() {
            return new JsonDataProcessor<>(clazz, extract);
        }
    }

    @Override
    public boolean mergeSupported() {
        return true;
    }
}
