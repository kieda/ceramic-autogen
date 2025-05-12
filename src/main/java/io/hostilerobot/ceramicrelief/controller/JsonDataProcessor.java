package io.hostilerobot.ceramicrelief.controller;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.UnaryOperator;

public class JsonDataProcessor<T> implements DataProcessor<T> {
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


    /**
     * registers a global module that will be applied to all deserialization/serialization for a specific type
     *
     * @param module
     */
    public static void registerModule(Module module) {
        mapper.registerModule(module);
    }

    private ObjectMapper localMapper;
    private final Class<T> clazz;
    private final UnaryOperator<JsonNode> extract;

    private JsonNode previous = null;

    private JsonDataProcessor(Class<T> clazz, UnaryOperator<JsonNode> extract, ObjectMapper m) {
        this.clazz = clazz;
        this.extract = extract;
        this.localMapper = m == null ? mapper : m;
    }

    private JsonNode extract(InputStream in) throws IOException{
        JsonNode startingJson = localMapper.readTree(in);
        if(extract != null) return extract.apply(startingJson);
        return startingJson;
    }

    @Override
    public T transform(InputStream in) throws IOException{
        // todo - is there a better way to do before/after ?
        // since we can get the actual T before/after with transform, but will only get raw json before/after with merge
        // is there anything useful in doing it the other way? We only really care if the json node changed after all to see if we should fire invalidation triggers
        JsonNode input = extract(in);
        previous = input;
        return localMapper.treeToValue(input, clazz);
    }

    @Override
    public boolean merge(T current, InputStream in) throws IOException {
        JsonNode input = extract(in);
        localMapper.readerForUpdating(current)
                .readValue(input);
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
        // these taken over by Modules
//        private BiConsumer<T, JsonNode> mergingFunction = null;
//        private Function<JsonNode, T> mappingFunction = null;
        private ObjectMapper mapper = null;

        private Builder(Class<T> clazz) {
            this.clazz = clazz;
        }


        /**
         * registers a module that will be used strictly for this instance
         * @param module
         */
        public Builder<T> registerModule(Module module) {
            if(mapper == null)
                mapper = JsonDataProcessor.mapper.copy();
            mapper.registerModule(module);
            return this;
        }

        public Builder<T> extractFunction(UnaryOperator<JsonNode> extractFunction) {
            this.extract = extractFunction;
            return this;
        }

        public JsonDataProcessor<T> build() {
            return new JsonDataProcessor<>(clazz, extract, mapper);
        }
    }

    @Override
    public boolean mergeSupported() {
        return true;
    }
}
