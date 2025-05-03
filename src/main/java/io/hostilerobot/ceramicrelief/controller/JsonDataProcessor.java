package io.hostilerobot.ceramicrelief.controller;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
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
    private final BiConsumer<T, JsonNode> merging;
    private final Function<JsonNode, T> mapping;

    private JsonNode previous = null;

    private JsonDataProcessor(Class<T> clazz, UnaryOperator<JsonNode> extract, BiConsumer<T, JsonNode> merging, Function<JsonNode, T> mapping) {
        this.clazz = clazz;
        this.extract = extract;
        this.merging = merging;
        this.mapping = mapping;
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
        if(mapping != null)
            return mapping.apply(input);
        else
            return mapper.treeToValue(input, clazz);
    }

    @Override
    public boolean merge(T current, InputStream in) throws IOException {
        JsonNode input = extract(in);
        if(merging != null) {
            merging.accept(current, input);
        } else {
            mapper.readerForUpdating(current)
                    .readValue(input);
        }
        boolean same = input.equals(previous);
        previous = input;

        return !same;
    }

    public static <T> Builder<T> builder(Class<T> clazz) {
        return new Builder<>(clazz);
    }

    public static final class Builder<T> {
        private final Class<T> clazz;
        private UnaryOperator<JsonNode> extract = null;
        private BiConsumer<T, JsonNode> mergingFunction = null;
        private Function<JsonNode, T> mappingFunction = null;

        private Builder(Class<T> clazz) {
            this.clazz = clazz;
        }

        public Builder<T> customMapping(Function<JsonNode, T> mappingFunction) {
            this.mappingFunction = mappingFunction;
            return this;
        }

        public Builder<T> customMerging(BiConsumer<T, JsonNode> mergingFunction) {
            this.mergingFunction = mergingFunction;
            return this;
        }

        public Builder<T> extractFunction(UnaryOperator<JsonNode> extractFunction) {
            this.extract = extractFunction;
            return this;
        }

        public JsonDataProcessor<T> build() {
            return new JsonDataProcessor<>(clazz, extract, mergingFunction, mappingFunction);
        }
    }

    @Override
    public boolean mergeSupported() {
        return true;
    }
}
