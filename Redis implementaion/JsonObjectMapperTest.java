package com.adidas.next.shippingapi.util;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.adidas.next.shippingapi.TestHelper;
import com.adidas.next.shippingapi.entity.FulfillmentOption;
import com.adidas.next.shippingapi.entity.ShippingMethods;
import com.adidas.next.shippingapi.entity.SiteId;
import com.adidas.next.shippingapi.exception.ShippingApiException;
import com.adidas.next.shippingapi.resources.request.ShippingMethodPostRequest;
import com.adidas.next.shippingapi.resources.request.ShippingMethodPrice;
import com.adidas.next.shippingapi.resources.request.ShippingPatchRequest;
import com.adidas.next.shippingapi.resources.response.Location;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;

import io.r2dbc.postgresql.codec.Json;

@ExtendWith(MockitoExtension.class)
class JsonObjectMapperTest extends TestHelper {

  @InjectMocks
  JsonObjectMapper jsonObjectMapper;

  @Mock
  ObjectMapper objectMapper;

  @Test
  void testJsonStringToMapSuccess() {
    try {
      ObjectMapper mapper = new ObjectMapper();
      TypeFactory typeFactory = mapper.getTypeFactory();
      Mockito.when(objectMapper.getTypeFactory()).thenReturn(typeFactory);
      MapType mapType = typeFactory.constructMapType(Map.class, String.class, String.class);
      Mockito.when(objectMapper.readValue(Mockito.anyString(), Mockito.eq(mapType)))
          .thenReturn(Map.of("en_EN", "Click And Collect"));
      Map<String, String> nameMap = jsonObjectMapper.jsonStringToMap(Json.of("{en_EN : Click And Collect}"));
      Assertions.assertNotNull(nameMap);
      Assertions.assertNotNull(nameMap.get("en_EN"));
    } catch (JsonProcessingException e) {
      Assertions.fail(e.getMessage());
    }
  }

  @Test
  void testJsonStringToMapFailure() {
    try {
      ObjectMapper mapper = new ObjectMapper();
      TypeFactory typeFactory = mapper.getTypeFactory();
      Mockito.when(objectMapper.getTypeFactory()).thenReturn(typeFactory);
      MapType mapType = typeFactory.constructMapType(Map.class, String.class, String.class);
      Mockito.when(objectMapper.readValue(Mockito.anyString(), Mockito.eq(mapType)))
          .thenThrow(new JsonProcessingException("error") {
          });
      jsonObjectMapper.jsonStringToMap(Json.of("data"));
    } catch (ShippingApiException e) {
    } catch (JsonProcessingException e) {
      Assertions.fail(e.getMessage());
    }
  }
  
  @Test
  void testJsonStringToListSuccess() {
    try {
      ObjectMapper mapper = new ObjectMapper();
      TypeFactory typeFactory = mapper.getTypeFactory();
      Mockito.when(objectMapper.getTypeFactory()).thenReturn(typeFactory);
      CollectionType listType = typeFactory.constructCollectionType(List.class, ShippingMethodPrice.class);
      Mockito.when(objectMapper.readValue("{threshold : 123}", listType)).thenReturn(List.of("threshold", 123));
      List<ShippingMethodPrice> list = jsonObjectMapper.jsonStringToList(Json.of("{threshold : 123}"), ShippingMethodPrice.class);
      Assertions.assertNotNull(list);
    } catch (JsonProcessingException e) {
      Assertions.fail(e.getMessage());
    }
  }
  
  @Test
  void testJsonStringToListFailure() {
    try {
      ObjectMapper mapper = new ObjectMapper();
      TypeFactory typeFactory = mapper.getTypeFactory();
      Mockito.when(objectMapper.getTypeFactory()).thenReturn(typeFactory);
      CollectionType listType = objectMapper.getTypeFactory().constructCollectionType(List.class, ShippingMethodPrice.class);
          Mockito.when(objectMapper.readValue("data", listType))
          .thenThrow(new JsonProcessingException("error") {
          });
      jsonObjectMapper.jsonStringToList(Json.of("data"), ShippingMethodPrice.class);
    } catch (ShippingApiException e) {
    }catch (JsonProcessingException e) {
      Assertions.fail(e.getMessage());
    }
  }

  @Test
  public void testJsonSuccess() {
    try {
      Mockito.when(objectMapper.writeValueAsString(Mockito.anyMap()))
          .thenReturn(Map.of("en_EN", "Click And Collect").toString());
      Json nameJson = jsonObjectMapper.toJson(Map.of("en_EN", "Click And Collect"));
      Assertions.assertNotNull(nameJson);
      Assertions.assertEquals(nameJson.asString(), "{en_EN=Click And Collect}");
    } catch (JsonProcessingException e) {
      Assertions.fail(e.getMessage());
    }
  }

  @Test
  public void testJsonFailure() {
    try {
      Mockito.when(objectMapper.writeValueAsString(Mockito.anyMap()))
          .thenThrow(new JsonProcessingException("JsonProcessingException") {
          });
      Json nameJson = jsonObjectMapper.toJson(Map.of("en_EN", "Click And Collect"));
      Assertions.assertNotNull(nameJson);
      Assertions.assertEquals(nameJson.asString(), "{en_EN=Click And Collect}");
    } catch (ShippingApiException e) {
    } catch (JsonProcessingException e) {
      Assertions.fail(e.getMessage());
    }
  }
  
  @Test
  public void testObjectToMap() throws IllegalArgumentException {
    ShippingPatchRequest patchRequest = buildShippingPatchRequest();
    Map<String, Object> greaterMap = Map.of("type", "nonExistingType");
    ObjectMapper mapper = new ObjectMapper();
    Object spec = mapper.convertValue(greaterMap, Object.class);
    TypeFactory typeFactory = mapper.getTypeFactory();
    Mockito.when(objectMapper.getTypeFactory()).thenReturn(typeFactory);
    jsonObjectMapper.objectToMap(patchRequest);
    Assertions.assertNotNull(spec);
  }
  
  @Test
  public void testToJsonString() {
    try {
      Mockito.when(objectMapper.writeValueAsString(Mockito.any()))
          .thenReturn("{\"id\":11,\"name\":\"adidas-US\",\"enterpriseCode\":\"adidasUS\"");
      String siteId = jsonObjectMapper.toJsonString(prepareSiteId());
      Assertions.assertNotNull(siteId);
      Assertions.assertEquals("{\"id\":11,\"name\":\"adidas-US\",\"enterpriseCode\":\"adidasUS\"", siteId);
    } catch (JsonProcessingException e) {
      Assertions.fail(e.getMessage());
    }
  }
  
  @Test
  public void testToJsonStringFaliure() {
    try {
      Mockito.when(objectMapper.writeValueAsString(Mockito.any())).thenThrow(new JsonProcessingException("error") {
      });
      jsonObjectMapper.toJsonString(prepareSiteId());
    } catch (ShippingApiException e) {
    } catch (JsonProcessingException e) {
      Assertions.fail(e.getMessage());
    }
  }

  @Test
  public void testJsonStringToObject() {
    try {
      Mockito.when( objectMapper.readValue("{\"id\":11,\"name\":\"adidas-US\",\"enterpriseCode\":\"adidasUS\"", SiteId.class))
          .thenReturn(prepareSiteId());
      SiteId siteId = jsonObjectMapper.jsonStringToObject(SiteId.class, "{\"id\":11,\"name\":\"adidas-US\",\"enterpriseCode\":\"adidasUS\"");
      Assertions.assertNotNull(siteId);
      Assertions.assertEquals("adidas-US", siteId.getName());
      Assertions.assertEquals("adidasUS", siteId.getEnterpriseCode());
    } catch (JsonProcessingException e) {
      Assertions.fail(e.getMessage());
    }
  }
  
  @Test
  public void testJsonStringToObjectFailure() {
    try {
      Mockito.when( objectMapper.readValue("{\"id\":11,\"name\":\"adidas-US\",\"enterpriseCode\":\"adidasUS\"", SiteId.class)).thenThrow(new JsonProcessingException("error") {
      });
      jsonObjectMapper.jsonStringToObject(SiteId.class, "{\"id\":11,\"name\":\"adidas-US\",\"enterpriseCode\":\"adidasUS\"");
    } catch (ShippingApiException e) {
    } catch (JsonProcessingException e) {
      Assertions.fail(e.getMessage());
    }
  }

  @Test
  public void testJsonStringToListOfObject() {
    try {
      ObjectMapper mapper = new ObjectMapper();
      TypeFactory typeFactory = mapper.getTypeFactory();
      Mockito.when(objectMapper.getTypeFactory()).thenReturn(typeFactory);
      CollectionType listType = typeFactory.constructCollectionType(List.class, FulfillmentOption.class);
      Mockito.when(objectMapper.readValue(
          "[{\"fulfillmentType\":\"HomeDelivery\",\"name\":\"HomeDelivery\",\"description\":\"HomeDelivery\"}]", listType)).thenReturn(List.of(prepareShippingOption()));
      List<FulfillmentOption> list = jsonObjectMapper.jsonStringToListOfObject(
          "[{\"fulfillmentType\":\"HomeDelivery\",\"name\":\"HomeDelivery\",\"description\":\"HomeDelivery\"}]", FulfillmentOption.class);
      Assertions.assertNotNull(list);
      Assertions.assertEquals("HomeDelivery", list.get(0).getFulfillmentType());
      Assertions.assertEquals(Map.of("en-US", "HomeDelivery"), list.get(0).getDescription());
      Assertions.assertEquals(Map.of("en-US", "HomeDelivery"), list.get(0).getName());
    } catch (JsonProcessingException e) {
      Assertions.fail(e.getMessage());
    }
  }
  
  @Test
  public void testJsonStringToListOfObjectFailure() {
    try {
      ObjectMapper mapper = new ObjectMapper();
      TypeFactory typeFactory = mapper.getTypeFactory();
      Mockito.when(objectMapper.getTypeFactory()).thenReturn(typeFactory);
      CollectionType listType = typeFactory.constructCollectionType(List.class, FulfillmentOption.class);
      Mockito.when(objectMapper.readValue(
          "[{\"fulfillmentType\":\"HomeDelivery\",\"name\":\"HomeDelivery\",\"description\":\"HomeDelivery\"}]", listType)).thenThrow(new JsonProcessingException("error") {
          });
      jsonObjectMapper.jsonStringToListOfObject(
          "[{\"fulfillmentType\":\"HomeDelivery\",\"name\":\"HomeDelivery\",\"description\":\"HomeDelivery\"}]", FulfillmentOption.class);
    } catch (ShippingApiException e) {
    } catch (JsonProcessingException e) {
      Assertions.fail(e.getMessage());
    }
  }
  
  @Test
  public void testObjectToList() throws IllegalArgumentException {
    ShippingMethodPostRequest postRequest = buildShippingPostRequest();
    Object obj = new ObjectMapper().convertValue(postRequest, Object.class);
    jsonObjectMapper.objectToList(postRequest, null);
    Assertions.assertNotNull(obj);
  }

  @Test
  public void testObjectToListShippingMethod() throws IllegalArgumentException {
    List<ShippingMethods> postRequest = buildShippingMethodsList();
    Object obj = new ObjectMapper().convertValue(postRequest, Object.class);
    jsonObjectMapper.objectToListShippingMethod(postRequest, null);
    Assertions.assertNotNull(obj);
  }
  
  @Test
  public void testObjectToListLocation() throws IllegalArgumentException {
    List<Location> postRequest = prepareLocations();
    Object obj = new ObjectMapper().convertValue(postRequest, Object.class);
    jsonObjectMapper.objectToListLocation(postRequest, null);
    Assertions.assertNotNull(obj);
  }
}
