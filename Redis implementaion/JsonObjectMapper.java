package com.adidas.next.shippingapi.util;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.adidas.next.shippingapi.constant.ErrorConstants;
import com.adidas.next.shippingapi.exception.ShippingApiException;
import com.adidas.next.shippingapi.resources.response.CarrierStringRecord;
import com.adidas.next.shippingapi.resources.response.ShippingMethod;
import com.adidas.next.shippingapi.resources.response.pudo.LocationPUDO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.r2dbc.postgresql.codec.Json;

@Component
public class JsonObjectMapper {

  private final ObjectMapper objectMapper;
  
  public JsonObjectMapper(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public Map<String, String> jsonStringToMap(Json json) {
    try {
      return objectMapper.readValue(json.asString(), objectMapper.getTypeFactory().constructMapType(Map.class, String.class, String.class));
    } catch (JsonProcessingException e) {
      throw new ShippingApiException(e, ErrorConstants.INTERNAL_ERROR_CODE);
    }
  }
  
  public Map<String, Object> jsonStringToMapObject(Json json) {
    try {
      return objectMapper.readValue(json.asString(), objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class));
    } catch (JsonProcessingException e) {
      throw new ShippingApiException(e, ErrorConstants.INTERNAL_ERROR_CODE);
    }
  }
  
  public <T> Object jsonToObject(Json json, Class<T> className) {
    try {
      return objectMapper.readValue(json.asString(), objectMapper.getTypeFactory().constructType(className));
    } catch (JsonProcessingException e) {
      throw new ShippingApiException(e, ErrorConstants.INTERNAL_ERROR_CODE);
    }
  }

  public <T> Map<String, T> jsonStringToMap(Json json, Class<T> className) {
    try {
      return objectMapper.readValue(json.asString(), objectMapper.getTypeFactory().constructMapType(Map.class, String.class, className));
    } catch (JsonProcessingException e) {
      throw new ShippingApiException(e, ErrorConstants.INTERNAL_ERROR_CODE);
    }
  }

  public <T> List<T> jsonStringToList(Json json, Class<T> className) {
    try {
      return objectMapper.readValue(json.asString(),
          objectMapper.getTypeFactory().constructCollectionType(List.class, className));
    } catch (JsonProcessingException e) {
      throw new ShippingApiException(e, ErrorConstants.INTERNAL_ERROR_CODE);
    }
  }

  public <T> Json toJson(T data) {
    try {
      return Json.of(objectMapper.writeValueAsString(data));
    } catch (JsonProcessingException e) {
      throw new ShippingApiException(e, ErrorConstants.INTERNAL_ERROR_CODE);
    }
  }
  
  public <T> Map<String, Object> objectToMap(T object) {
      return objectMapper.convertValue(object, objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class));
  }
  
  public <T> String toJsonString(T data) {
    try {
     return  objectMapper.writeValueAsString(data);
    } catch (JsonProcessingException e) {
      throw new ShippingApiException(e, ErrorConstants.INTERNAL_ERROR_CODE);
    }
  }
  
  public <T> T jsonStringToObject(Class<T> clazz, String jsonString) {
    try {
      return objectMapper.readValue(jsonString, clazz);
    } catch (JsonProcessingException e) {
      throw new ShippingApiException(e, ErrorConstants.INTERNAL_ERROR_CODE);
    }
  }

  public <T> List<T> jsonStringToListOfObject(String jsonString, Class<T> className) {
    try {
      return objectMapper.readValue(jsonString,
          objectMapper.getTypeFactory().constructCollectionType(List.class, className));
    } catch (JsonProcessingException e) {
      throw new ShippingApiException(e, ErrorConstants.INTERNAL_ERROR_CODE);
    }
  }
 
  public List<CarrierStringRecord> objectToList(Object object, TypeReference<List<CarrierStringRecord>> typeReference) {
    return objectMapper.convertValue(object, typeReference);
  }

  public List<ShippingMethod> objectToListShippingMethod(Object object,
    TypeReference<List<ShippingMethod>> typeReference) {
   return objectMapper.convertValue(object, typeReference);
  }

  public List<LocationPUDO> objectToListLocation(Object object, TypeReference<List<LocationPUDO>> typeReference) {
   return objectMapper.convertValue(object, typeReference);
  }

}
