package com.cherokeelessons.converter;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.TimeZone;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;

public class JsonConverter {
	final protected ObjectMapper mapper;

	public JsonConverter() {
		mapper = new ObjectMapper();
		init();
	}

	public void addMixInAnnotations(Class<?> target, Class<?> mixinSource) {
		mapper.addMixIn(target, mixinSource);
	}

	protected void init() {
		mapper.getVisibilityChecker().withFieldVisibility(Visibility.ANY);
		// General Settings
		mapper.setTimeZone(TimeZone.getTimeZone("EST5EDT"));
		mapper.enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY);

		// Serialization Settings
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
		mapper.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
		mapper.enable(SerializationFeature.WRITE_DATE_KEYS_AS_TIMESTAMPS);
		mapper.enable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		mapper.disable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
		mapper.disable(SerializationFeature.WRITE_CHAR_ARRAYS_AS_JSON_ARRAYS);
		mapper.enable(SerializationFeature.WRITE_NULL_MAP_VALUES);
		mapper.enable(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS);
		mapper.setSerializationInclusion(Include.NON_NULL);

		// include java specific type info as part of json
		// mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);

		// Deserialization Settings
		mapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
		mapper.enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL);
		mapper.disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);
		mapper.disable(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS);
		mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		mapper.disable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);
		mapper.disable(DeserializationFeature.USE_JAVA_ARRAY_FOR_JSON_ARRAY);
		mapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
	}

	public String toJson(Object object) {
		ObjectWriter writer;
		writer = mapper.writer();
		try {
			return writer.writeValueAsString(object);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			return null;
		}
	}

	public void toJson(File dest, Object object) {
		ObjectWriter writer;
		writer = mapper.writer();
		try {
			writer.writeValue(dest, object);
		} catch (JsonProcessingException e) {
		} catch (IOException e) {
		}
	}

	public <T> T fromJson(String json, Class<T> classOfT) {
		if (json == null) {
			return null;
		}
		T result = null;
		ObjectReader reader;
		reader = mapper.readerFor(classOfT);
		try {
			result = reader.readValue(json);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}

	public <T> T fromJson(File src, Class<T> classOfT) {
		if (src == null || !src.isFile() || !src.canRead()) {
			return null;
		}
		ObjectReader reader;
		reader = mapper.readerFor(classOfT);
		try {
			return reader.readValue(src);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public <T> T fromJson(URL src, Class<T> classOfT) {
		if (src == null) {
			return null;
		}
		ObjectReader reader;
		reader = mapper.readerFor(classOfT);
		try {
			return reader.readValue(src);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
}
