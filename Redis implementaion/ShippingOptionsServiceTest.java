package com.adidas.next.shippingapi.service;

import static com.adidas.next.shippingapi.constant.ErrorConstants.NOT_FOUND_CODE;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.HttpClientErrorException;

import com.adidas.next.shippingapi.TestHelper;
import com.adidas.next.shippingapi.client.DeliveryPromiseMSClient;
import com.adidas.next.shippingapi.client.DeliveryPromiseServiceClient;
import com.adidas.next.shippingapi.converter.ShippingMethodPostRequestConverter;
import com.adidas.next.shippingapi.converter.hd.DPEDefaultResponseConverter;
import com.adidas.next.shippingapi.converter.hd.DPERequestConverter;
import com.adidas.next.shippingapi.converter.hd.DPEResponseConverter;
import com.adidas.next.shippingapi.exception.NotFoundException;
import com.adidas.next.shippingapi.handler.cnc.CNCDPEServiceHandler;
import com.adidas.next.shippingapi.handler.hd.DPEDefaultResponseHandler;
import com.adidas.next.shippingapi.handler.hd.DPEServiceHandler;
import com.adidas.next.shippingapi.handler.pudo.PUDODPEServiceBaseHandler;
import com.adidas.next.shippingapi.handler.pudo.PUDODPEServiceHandler;
import com.adidas.next.shippingapi.handler.pudo.PUDOSiteIdDefaultResponseHandler;
import com.adidas.next.shippingapi.repository.FulfillmentReadRepository;
import com.adidas.next.shippingapi.repository.SiteIdReadRepository;
import com.adidas.next.shippingapi.resources.request.ShippingOptionsPostRequest;
import com.adidas.next.shippingapi.resources.response.ShippingOptionsResponse;
import com.adidas.next.shippingapi.util.JsonObjectMapper;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(SpringExtension.class)
public class ShippingOptionsServiceTest extends TestHelper {

  @Mock
  DPEDefaultResponseConverter shippingOptionsResponseConverter;

  @Mock
  ShippingMethodPostRequestConverter shippingMethodPostRequestConverter;
  
  @Mock
  DPERequestConverter deliveryPromiseRequestConverter;
  
  @Mock
  DeliveryPromiseServiceClient deliveryPromiseServiceClient;

  @Mock
  DeliveryPromiseMSClient deliveryPromiseMSClient;
  
  @Mock
  DPEResponseConverter shippingOptionResponseConverter;

  @Mock
  DPEServiceHandler dpeHandler;

  @InjectMocks
  ShippingOptionsService shippingOptionsService;
  
  @Mock
  FulfillmentReadRepository fulfillmentReadRepository;
  
  @Mock
  CNCDPEServiceHandler cncdpeHandler;
  
  @Mock
  SiteIdReadRepository siteIdReadRepository;
  
  @Mock
  PUDODPEServiceHandler pudodpeServiceHandler;
  
  @Mock
  ShippingOptionsValidationService shippingOptionsValidationService;
  
  @Mock
  PUDODPEServiceBaseHandler pudoDPEServiceBaseHandler;
  
  @Mock
  ReactiveRedisOperations<String, String> redisOperations;
  
  @Mock
  ReactiveValueOperations<String, String> reactiveValueOperations;
  
  @Mock
  JsonObjectMapper jsonObjectMapper;
  
  @Mock
  PUDOSiteIdDefaultResponseHandler pudoSiteIdDefaultResponseHandler;
  
  @Mock
  DPEDefaultResponseHandler dpeDefaultResponseHandler;

  @Test
  void testGetShippingOptionsForHomeDeliverySuccess() {
    List<String> embed = new ArrayList<>();
    embed.add("HomeDelivery");
    ShippingOptionsPostRequest shippingOptionsPostRequest = buildShippingOptionsPostRequest();
    Mockito.when(siteIdReadRepository.findByName(Mockito.anyString())).thenReturn(Mono.just(prepareSiteId()));
    Mockito.when(shippingOptionsValidationService.validateCNCDPEShippingOptionsRequest(Mockito.any(), Mockito.any())).thenReturn(Mono.just(buildGeoCodingResponse()));
    Mockito.when(fulfillmentReadRepository.findBySiteIdAndFulfilmentType(Mockito.any(), Mockito.anyList())).thenReturn(Flux.just(prepareShippingOption()));
    Mockito.when(shippingOptionsValidationService.validateDPEHDRequest(Mockito.any())).thenReturn(Mono.just(buildShippingAddress()));
    Mockito.when(dpeHandler.createDPEHDEmptyResponse(Mockito.any(), Mockito.any())).thenReturn(Mono.just(buildShippingOptionsResponseForPostwithNull()));
    Mockito.when(dpeHandler.createDPE(Mockito.any(), Mockito.any(),Mockito.any(),Mockito.any())).thenReturn(Mono.just(buildShippingOptionsResponseForPost()));
    Mockito.when(redisOperations.opsForValue()).thenReturn(reactiveValueOperations);
    Mockito.when(reactiveValueOperations.get(Mockito.any())).thenReturn(Mono.empty());
    Mockito.when(reactiveValueOperations.set(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(true));
    
    Mono<List<ShippingOptionsResponse>> shippingOptionsResponse = shippingOptionsService
        .createShippingOptions(shippingOptionsPostRequest, embed);
    StepVerifier.create(shippingOptionsResponse).thenConsumeWhile(shippingResponse -> {
      Assertions.assertEquals("HomeDelivery", shippingResponse.get(0).getFulfillmentType());
      Assertions.assertEquals("HomeDelivery", shippingResponse.get(0).getName());
      Assertions.assertEquals("HomeDelivery", shippingResponse.get(0).getDescription());
      Assertions.assertEquals("M20323_530", shippingResponse.get(0).getShipments().get(0).getProductLines().get(0).getSku());
      return true;
    }).verifyComplete();
  }
  
  @Test
  void testGetShippingOptionsForCNCSuccess() {
    ShippingOptionsPostRequest shippingOptionsPostRequest = prepareShippingOptionsPostRequestForCNCUS();
    Mockito.when(siteIdReadRepository.findByName(Mockito.anyString())).thenReturn(Mono.just(prepareSiteId()));
    Mockito.when(shippingOptionsValidationService.validateCNCDPEShippingOptionsRequest(Mockito.any(), Mockito.any())).thenReturn(Mono.just(buildGeoCodingResponse()));
    Mockito.when(fulfillmentReadRepository.findBySiteIdAndFulfilmentType(Mockito.any(), Mockito.anyList())).thenReturn(Flux.just(prepareShippingOptionForCNC()));
    Mockito.when(cncdpeHandler.createCNCDPE(Mockito.any(), Mockito.any(),Mockito.any(), Mockito.any())).thenReturn(Mono.just(buildShippingOptionsResponseForCNCPost()));
    Mockito.when(redisOperations.opsForValue()).thenReturn(reactiveValueOperations);
    Mockito.when(reactiveValueOperations.get(Mockito.any())).thenReturn(Mono.empty());
    Mockito.when(reactiveValueOperations.set(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(true));
    
    Mono<List<ShippingOptionsResponse>> shippingOptionsResponse = shippingOptionsService
        .createShippingOptions(shippingOptionsPostRequest, List.of("ClickAndCollect"));
    StepVerifier.create(shippingOptionsResponse).thenConsumeWhile(shippingResponse -> {
      Assertions.assertEquals("ClickAndCollect", shippingResponse.get(0).getFulfillmentType());
      Assertions.assertEquals("Click And Collect", shippingResponse.get(0).getName());
      Assertions.assertEquals("Click And Collect", shippingResponse.get(0).getDescription());      
      Assertions.assertEquals("PearlStreet47", shippingResponse.get(0).getLocations().get(0).getId());
      Assertions.assertEquals("Pearl Street", shippingResponse.get(0).getLocations().get(0).getLocationName());
      Assertions.assertEquals("US", shippingResponse.get(0).getLocations().get(0).getAddress().getCountry());      
      Assertions.assertEquals(404233.12, shippingResponse.get(0).getLocations().get(0).getGeoLocation().getLatitude());
      Assertions.assertEquals(74006.84, shippingResponse.get(0).getLocations().get(0).getGeoLocation().getLongitude());     
      Assertions.assertEquals("17", shippingResponse.get(0).getLocations().get(0).getOpeningHours().getSunday().getEndHours());
      Assertions.assertEquals("30", shippingResponse.get(0).getLocations().get(0).getOpeningHours().getSunday().getEndMinutes());
      Assertions.assertEquals("08", shippingResponse.get(0).getLocations().get(0).getOpeningHours().getSunday().getStartHours());
      Assertions.assertEquals("20", shippingResponse.get(0).getLocations().get(0).getOpeningHours().getSunday().getStartMinutes());      
      Assertions.assertEquals("EG4958_550", shippingResponse.get(0).getShipments().get(0).getProductLines().get(0).getSku());
      Assertions.assertEquals("1", shippingResponse.get(0).getShipments().get(0).getProductLines().get(0).getId());
      Assertions.assertEquals(2, shippingResponse.get(0).getShipments().get(0).getProductLines().get(0).getQuantity());
      Assertions.assertEquals("inline", shippingResponse.get(0).getShipments().get(0).getProductLines().get(0).getLineType());     
      Assertions.assertEquals("inline", shippingResponse.get(0).getShipments().get(0).getProductLines().get(0).getLineType());
      return true;
    }).verifyComplete();
  }
  
  @Test
  void testGetShippingOptionsWhenEmbedIsEmpty() {
    ShippingOptionsPostRequest shippingOptionsPostRequest = buildShippingOptionsPostRequest();
    Mockito.when(siteIdReadRepository.findByName(Mockito.anyString())).thenReturn(Mono.just(prepareSiteId()));
    Mockito.when(shippingOptionsValidationService.validateCNCDPEShippingOptionsRequest(Mockito.any(), Mockito.any())).thenReturn(Mono.just(buildGeoCodingResponse()));
    Mockito.when(fulfillmentReadRepository.findBySiteIdAndFulfilmentType(Mockito.any(), Mockito.anyList())).thenReturn(Flux.just(prepareShippingOption()));
    Mockito.when(shippingOptionsValidationService.validateDPEHDRequest(Mockito.any())).thenReturn(Mono.just(buildShippingAddress()));
    Mockito.when(dpeHandler.createDPEHDEmptyResponse(Mockito.any(), Mockito.any())).thenReturn(Mono.just(buildShippingOptionsResponseForPostwithNull()));
    Mockito.when(dpeHandler.createDPE(Mockito.any(), Mockito.any(),Mockito.any(),Mockito.any())).thenReturn(Mono.just(buildShippingOptionsResponseForPost()));
    Mockito.when(cncdpeHandler.createCNCDPE(Mockito.any(), Mockito.any(),Mockito.any(), Mockito.any())).thenReturn(Mono.just(buildShippingOptionsResponseForCNCPost()));
    Mockito.when(redisOperations.opsForValue()).thenReturn(reactiveValueOperations);
    Mockito.when(reactiveValueOperations.get(Mockito.any())).thenReturn(Mono.empty());
    Mockito.when(reactiveValueOperations.set(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(true));
    
    Mono<List<ShippingOptionsResponse>> shippingOptionsResponse = shippingOptionsService
        .createShippingOptions(shippingOptionsPostRequest, new ArrayList<>());
    StepVerifier.create(shippingOptionsResponse).thenConsumeWhile(shippingResponse -> {
      Assertions.assertEquals("HomeDelivery", shippingResponse.get(0).getFulfillmentType());
      Assertions.assertEquals("HomeDelivery", shippingResponse.get(0).getName());
      Assertions.assertEquals("HomeDelivery", shippingResponse.get(0).getDescription());
      Assertions.assertEquals("M20323_530", shippingResponse.get(0).getShipments().get(0).getProductLines().get(0).getSku());
      return true;
    }).verifyComplete();
  }

  @Test
  void testGetShippingOptionsWhenEmbedIsEmptyForCNC() {
    ShippingOptionsPostRequest shippingOptionsPostRequest = buildShippingOptionsPostRequest();
    Mockito.when(siteIdReadRepository.findByName(Mockito.anyString())).thenReturn(Mono.just(prepareSiteId()));
    Mockito.when(shippingOptionsValidationService.validateCNCDPEShippingOptionsRequest(Mockito.any(), Mockito.any())).thenReturn(Mono.just(buildGeoCodingResponse()));
    Mockito.when(fulfillmentReadRepository.findBySiteIdAndFulfilmentType(Mockito.any(), Mockito.anyList())).thenReturn(Flux.just(prepareShippingOptionForCNC()));
    Mockito.when(dpeHandler.createDPE(Mockito.any(), Mockito.any(),Mockito.any(),Mockito.any())).thenReturn(Mono.just(buildShippingOptionsResponseForPost()));
    Mockito.when(cncdpeHandler.createCNCDPE(Mockito.any(), Mockito.any(),Mockito.any(), Mockito.any())).thenReturn(Mono.just(buildShippingOptionsResponseForCNCPost()));
    Mockito.when(redisOperations.opsForValue()).thenReturn(reactiveValueOperations);
    Mockito.when(reactiveValueOperations.get(Mockito.any())).thenReturn(Mono.empty());
    Mockito.when(reactiveValueOperations.set(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(true));
    
    Mono<List<ShippingOptionsResponse>> shippingOptionsResponse = shippingOptionsService
        .createShippingOptions(shippingOptionsPostRequest, new ArrayList<>());
    StepVerifier.create(shippingOptionsResponse).thenConsumeWhile(shippingResponse -> {
      Assertions.assertEquals("ClickAndCollect", shippingResponse.get(0).getFulfillmentType());
      Assertions.assertEquals("Click And Collect", shippingResponse.get(0).getName());
      Assertions.assertEquals("Click And Collect", shippingResponse.get(0).getDescription());
      Assertions.assertEquals("Pearl Street", shippingResponse.get(0).getLocations().get(0).getLocationName());
      Assertions.assertEquals("US", shippingResponse.get(0).getLocations().get(0).getAddress().getCountry());
      Assertions.assertEquals("Hudson Valley", shippingResponse.get(0).getLocations().get(0).getAddress().getCity());
      Assertions.assertEquals("10001", shippingResponse.get(0).getLocations().get(0).getAddress().getPostalCode());
      Assertions.assertEquals("Central New York", shippingResponse.get(0).getLocations().get(0).getAddress().getState());
      Assertions.assertEquals("EG4958_550", shippingResponse.get(0).getShipments().get(0).getProductLines().get(0).getSku());
      return true;
    }).verifyComplete();
  }

  @Test
  void testGetShippingOptionsForCNCWithStoreIdSuccess() {
    ShippingOptionsPostRequest shippingOptionsPostRequest = prepareShippingOptionsPostRequestForCNCUSWithStoreId();
    Mockito.when(siteIdReadRepository.findByName(Mockito.anyString())).thenReturn(Mono.just(prepareSiteId()));
    Mockito.when(shippingOptionsValidationService.validateCNCDPEShippingOptionsRequest(Mockito.any(), Mockito.any())).thenReturn(Mono.just(buildGeoCodingResponse()));
    Mockito.when(fulfillmentReadRepository.findBySiteIdAndFulfilmentType(Mockito.any(), Mockito.anyList())).thenReturn(Flux.just(prepareShippingOptionForCNC()));
    Mockito.when(cncdpeHandler.createCNCDPE(Mockito.any(), Mockito.any(),Mockito.any(), Mockito.any())).thenReturn(Mono.just(buildShippingOptionsResponseForCNCPost()));
    Mockito.when(redisOperations.opsForValue()).thenReturn(reactiveValueOperations);
    Mockito.when(reactiveValueOperations.get(Mockito.any())).thenReturn(Mono.empty());
    Mockito.when(reactiveValueOperations.set(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(true));
    
    Mono<List<ShippingOptionsResponse>> shippingOptionsResponse = shippingOptionsService
        .createShippingOptions(shippingOptionsPostRequest, List.of("ClickAndCollect"));
    StepVerifier.create(shippingOptionsResponse).thenConsumeWhile(shippingResponse -> {
      Assertions.assertEquals("ClickAndCollect", shippingResponse.get(0).getFulfillmentType());
      Assertions.assertEquals("Click And Collect", shippingResponse.get(0).getName());
      Assertions.assertEquals("Click And Collect", shippingResponse.get(0).getDescription());     
      Assertions.assertEquals("Pearl Street", shippingResponse.get(0).getLocations().get(0).getLocationName());
      Assertions.assertEquals("US", shippingResponse.get(0).getLocations().get(0).getAddress().getCountry());      
      Assertions.assertEquals(404233.12, shippingResponse.get(0).getLocations().get(0).getGeoLocation().getLatitude());
      Assertions.assertEquals(74006.84, shippingResponse.get(0).getLocations().get(0).getGeoLocation().getLongitude());     
      Assertions.assertEquals("17", shippingResponse.get(0).getLocations().get(0).getOpeningHours().getSunday().getEndHours());
      Assertions.assertEquals("30", shippingResponse.get(0).getLocations().get(0).getOpeningHours().getSunday().getEndMinutes());
      Assertions.assertEquals("08", shippingResponse.get(0).getLocations().get(0).getOpeningHours().getSunday().getStartHours());
      Assertions.assertEquals("20", shippingResponse.get(0).getLocations().get(0).getOpeningHours().getSunday().getStartMinutes());      
      Assertions.assertEquals("EG4958_550", shippingResponse.get(0).getShipments().get(0).getProductLines().get(0).getSku());
      Assertions.assertEquals("1", shippingResponse.get(0).getShipments().get(0).getProductLines().get(0).getId());
      Assertions.assertEquals(2, shippingResponse.get(0).getShipments().get(0).getProductLines().get(0).getQuantity());
      Assertions.assertEquals("inline", shippingResponse.get(0).getShipments().get(0).getProductLines().get(0).getLineType());      
      Assertions.assertEquals("inline", shippingResponse.get(0).getShipments().get(0).getProductLines().get(0).getLineType());
      return true;
    }).verifyComplete();
  }

  
  @Test
  void testGetShippingOptionsWithDisabledSiteId() {
    ShippingOptionsPostRequest shippingOptionsPostRequest = buildShippingOptionsPostRequest();
    Mockito.when(fulfillmentReadRepository.findBySiteIdAndFulfilmentType(Mockito.any(), Mockito.anyList())).thenReturn(Flux.empty());
    Mono<List<ShippingOptionsResponse>> shippingOptionsResponse = shippingOptionsService
        .createShippingOptions(shippingOptionsPostRequest, new ArrayList<>());
    StepVerifier.create(shippingOptionsResponse)
        .consumeErrorWith(throwable -> new NotFoundException(new HttpClientErrorException(HttpStatus.NOT_FOUND), NOT_FOUND_CODE))
        .verify();
  }

  @Test
  void testcheckforHDwithSiteIdFail() {
    ShippingOptionsPostRequest shippingOptionsRequest = buildShippingOptionsPostRequest();
    Mockito.when(dpeHandler.createDPE(Mockito.any(), Mockito.any(), Mockito.any(),Mockito.any()))
        .thenReturn(Mono.just(buildShippingOptionsResponseForPost()));
    Assertions.assertEquals("US", shippingOptionsRequest.getShippingAddress().getCountry());
  }

  @Test
  void testcheckforHDwithSiteIdSuccess() {
    ShippingOptionsPostRequest shippingOptionsRequest = buildShippingOptionsPostRequestwithShipNull();
    Mockito.when(dpeHandler.createDPEHDEmptyResponse(Mockito.any(), Mockito.any()))
        .thenReturn(Mono.just(buildShippingOptionsResponseForPost()));
    Assertions.assertNotEquals("Germany", shippingOptionsRequest.getShippingAddress());
  }
  
  @Test
  void testGetShippingOptionsForCNCWithStoreIdLocationNull() {
    ShippingOptionsPostRequest shippingOptionsPostRequest = buildShippingOptionsPostRequestlocaltionIsNull();
    Mockito.when(siteIdReadRepository.findByName(Mockito.anyString())).thenReturn(Mono.just(prepareSiteId()));
    Mockito.when(shippingOptionsValidationService.validateCNCDPEShippingOptionsRequest(Mockito.any(), Mockito.any())).thenReturn(Mono.just(buildGeoCodingResponse()));
    Mockito.when(fulfillmentReadRepository.findBySiteIdAndFulfilmentType(Mockito.any(), Mockito.anyList())).thenReturn(Flux.just(prepareShippingOptionForCNC()));
    Mockito.when(cncdpeHandler.createCNCDPE(Mockito.any(), Mockito.any(),Mockito.any(), Mockito.any())).thenReturn(Mono.just(buildShippingOptionsResponseForCNCGeoLocation()));
    Mockito.when(redisOperations.opsForValue()).thenReturn(reactiveValueOperations);
    Mockito.when(reactiveValueOperations.get(Mockito.any())).thenReturn(Mono.empty());
    Mockito.when(reactiveValueOperations.set(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(true));
    
    Mono<List<ShippingOptionsResponse>> shippingOptionsResponse = shippingOptionsService
        .createShippingOptions(shippingOptionsPostRequest, List.of("ClickAndCollect"));
    StepVerifier.create(shippingOptionsResponse).thenConsumeWhile(shippingResponse -> {
      Assertions.assertEquals("ClickAndCollect", shippingResponse.get(0).getFulfillmentType());
      Assertions.assertEquals("Click And Collect", shippingResponse.get(0).getName());
      Assertions.assertEquals("Click And Collect", shippingResponse.get(0).getDescription());      
      Assertions.assertEquals("Pearl Street", shippingResponse.get(0).getLocations().get(0).getLocationName());
      Assertions.assertEquals("US", shippingResponse.get(0).getLocations().get(0).getAddress().getCountry());
      Assertions.assertEquals(47.666401, shippingResponse.get(0).getLocations().get(0).getGeoLocation().getLatitude());
      Assertions.assertEquals(-122.142483, shippingResponse.get(0).getLocations().get(0).getGeoLocation().getLongitude());     
      Assertions.assertEquals("17", shippingResponse.get(0).getLocations().get(0).getOpeningHours().getSunday().getEndHours());
      Assertions.assertEquals("30", shippingResponse.get(0).getLocations().get(0).getOpeningHours().getSunday().getEndMinutes());
      Assertions.assertEquals("08", shippingResponse.get(0).getLocations().get(0).getOpeningHours().getSunday().getStartHours());
      Assertions.assertEquals("20", shippingResponse.get(0).getLocations().get(0).getOpeningHours().getSunday().getStartMinutes());      
      Assertions.assertEquals("EG4958_550", shippingResponse.get(0).getShipments().get(0).getProductLines().get(0).getSku());
      Assertions.assertEquals("1", shippingResponse.get(0).getShipments().get(0).getProductLines().get(0).getId());
      Assertions.assertEquals(2, shippingResponse.get(0).getShipments().get(0).getProductLines().get(0).getQuantity());
      Assertions.assertEquals("inline", shippingResponse.get(0).getShipments().get(0).getProductLines().get(0).getLineType());     
      Assertions.assertEquals("inline", shippingResponse.get(0).getShipments().get(0).getProductLines().get(0).getLineType());
      return true;
    }).verifyComplete();
  }
   
  @Test
  void testGetShippingOptionsForHomeDeliveryDPEHDEmptyResponse() {
  ShippingOptionsPostRequest shippingOptionsPostRequest = buildShippingOptionsPostRequest();
  Mockito.when(siteIdReadRepository.findByName(Mockito.anyString())).thenReturn(Mono.just(prepareSiteId()));
  Mockito.when(shippingOptionsValidationService.validateCNCDPEShippingOptionsRequest(Mockito.any(), Mockito.any())).thenReturn(Mono.just(buildGeoCodingResponse()));
  Mockito.when(fulfillmentReadRepository.findBySiteIdAndFulfilmentType(Mockito.any(), Mockito.anyList())).thenReturn(Flux.just(prepareShippingOption()));
  Mockito.when(shippingOptionsValidationService.validateDPEHDRequest(Mockito.any())).thenReturn(Mono.error(new Exception()));
  Mockito.when(dpeHandler.createDPEHDEmptyResponse(Mockito.any(), Mockito.any())).thenReturn(Mono.just(buildShippingOptionsResponseForPostwithNull()));
  Mockito.when(dpeHandler.createDPE(Mockito.any(), Mockito.any(),Mockito.any(),Mockito.any())).thenReturn(Mono.empty());
  Mockito.when(dpeHandler.createDPEHDEmptyResponse(Mockito.any(), Mockito.any())).thenReturn(Mono.just(buildShippingOptionsResponseForPost()));
  Mockito.when(redisOperations.opsForValue()).thenReturn(reactiveValueOperations);
  Mockito.when(reactiveValueOperations.get(Mockito.any())).thenReturn(Mono.empty());
  Mockito.when(reactiveValueOperations.set(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(true));
  
  Mono<List<ShippingOptionsResponse>> shippingOptionsResponse = shippingOptionsService
  .createShippingOptions(shippingOptionsPostRequest, new ArrayList<>());
  StepVerifier.create(shippingOptionsResponse).thenConsumeWhile(shippingResponse -> {
  Assertions.assertEquals("HomeDelivery", shippingResponse.get(0).getFulfillmentType());
  Assertions.assertEquals("HomeDelivery", shippingResponse.get(0).getName());
  Assertions.assertEquals("HomeDelivery", shippingResponse.get(0).getDescription());
  Assertions.assertEquals("M20323_530", shippingResponse.get(0).getShipments().get(0).getProductLines().get(0).getSku());
  return true;
  }).verifyComplete();
  }

  @Test
  void testGetShippingOptionsWhenEmbedIsEmptyAndCNCFulfillmentTypeSupported() {
  ShippingOptionsPostRequest shippingOptionsPostRequest = buildShippingOptionsPostRequest();
  Mockito.when(siteIdReadRepository.findByName(Mockito.anyString())).thenReturn(Mono.just(prepareSiteId()));
  Mockito.when(shippingOptionsValidationService.validateCNCDPEShippingOptionsRequest(Mockito.any(), Mockito.any())).thenReturn(Mono.just(buildGeoCodingResponse()));
  Mockito.when(fulfillmentReadRepository.findBySiteIdAndFulfilmentType(Mockito.any(), Mockito.anyList())).thenReturn(Flux.just(prepareShippingOptionCNC()));
  Mockito.when(shippingOptionsValidationService.validateDPEHDRequest(Mockito.any())).thenReturn(Mono.just(buildShippingAddress()));
  Mockito.when(dpeHandler.createDPEHDEmptyResponse(Mockito.any(), Mockito.any())).thenReturn(Mono.just(buildShippingOptionsResponseForPostwithNull()));
  Mockito.when(cncdpeHandler.createCNCDPE(Mockito.any(), Mockito.any(),Mockito.any(), Mockito.any())).thenReturn(Mono.just(buildShippingOptionsResponseForCNCPost()));
  Mockito.when(redisOperations.opsForValue()).thenReturn(reactiveValueOperations);
  Mockito.when(reactiveValueOperations.get(Mockito.any())).thenReturn(Mono.empty());
  Mockito.when(reactiveValueOperations.set(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(true));
  
  Mono<List<ShippingOptionsResponse>> shippingOptionsResponse = shippingOptionsService
  .createShippingOptions(shippingOptionsPostRequest, new ArrayList<>());
  StepVerifier.create(shippingOptionsResponse).thenConsumeWhile(shippingResponse -> {
  Assertions.assertEquals("ClickAndCollect", shippingResponse.get(0).getFulfillmentType());
  Assertions.assertEquals("Click And Collect", shippingResponse.get(0).getName());
  Assertions.assertEquals("Click And Collect", shippingResponse.get(0).getDescription());
  Assertions.assertEquals("Pearl Street", shippingResponse.get(0).getLocations().get(0).getLocationName());
  Assertions.assertEquals("US", shippingResponse.get(0).getLocations().get(0).getAddress().getCountry());
  Assertions.assertEquals("Hudson Valley", shippingResponse.get(0).getLocations().get(0).getAddress().getCity());
  Assertions.assertEquals("10001", shippingResponse.get(0).getLocations().get(0).getAddress().getPostalCode());
  Assertions.assertEquals("Central New York", shippingResponse.get(0).getLocations().get(0).getAddress().getState());
  Assertions.assertEquals("EG4958_550", shippingResponse.get(0).getShipments().get(0).getProductLines().get(0).getSku());
  return true;
  }).verifyComplete();
  }

  @Test
  void testGetShippingOptionsWhenEmbedIsEmptyAndNotAnyFulfillmentTypeSupported() {
  ShippingOptionsPostRequest shippingOptionsPostRequest = buildShippingOptionsPostRequest();
  Mockito.when(siteIdReadRepository.findByName(Mockito.anyString())).thenReturn(Mono.just(prepareSiteId()));
  Mockito.when(shippingOptionsValidationService.validateCNCDPEShippingOptionsRequest(Mockito.any(), Mockito.any())).thenReturn(Mono.just(buildGeoCodingResponse()));
  Mockito.when(fulfillmentReadRepository.findBySiteIdAndFulfilmentType(Mockito.any(), Mockito.anyList())).thenReturn(Flux.just(prepareShippingOptionEmptyFulfillment()));
  Mockito.when(shippingOptionsValidationService.validateDPEHDRequest(Mockito.any())).thenReturn(Mono.just(buildShippingAddress()));
  Mockito.when(dpeHandler.createDPEHDEmptyResponse(Mockito.any(), Mockito.any())).thenReturn(Mono.just(buildShippingOptionsResponseForPostwithNull()));
  Mockito.when(redisOperations.opsForValue()).thenReturn(reactiveValueOperations);
  Mockito.when(reactiveValueOperations.get(Mockito.any())).thenReturn(Mono.empty());
  Mockito.when(reactiveValueOperations.set(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(true));
  
  Mono<List<ShippingOptionsResponse>> shippingOptionsResponse = shippingOptionsService
  .createShippingOptions(shippingOptionsPostRequest, new ArrayList<>());
  StepVerifier.create(shippingOptionsResponse).thenConsumeWhile(shippingResponse -> {
  Assertions.assertEquals(true,shippingResponse.isEmpty());
  return true;
  }).verifyComplete();
  }
  
  @Test
  void testGetShippingOptionsForPUDO() {
    List<String> embed = new ArrayList<>();
    embed.add("PUDO");
    ShippingOptionsPostRequest shippingOptionsPostRequest = buildShippingOptionsPostRequestForPUDO();
    Mockito.when(siteIdReadRepository.findByName(Mockito.anyString())).thenReturn(Mono.just(prepareSiteIdForPudo()));
    Mockito.when(fulfillmentReadRepository.findBySiteIdAndFulfilmentType(Mockito.any(), Mockito.anyList())).thenReturn(Flux.just(prepareShippingOptionForPUDO()));
    Mockito.when(pudodpeServiceHandler.createPUDODPE(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
        .thenReturn(Mono.just(buildShippingOptionsResponseForPostForPUDO()));
    Mockito.when(shippingOptionsValidationService.validatePUDODPERequest(Mockito.any(),Mockito.any()))
        .thenReturn(Mono.just(buildShippingAddress()));
    Mockito.when(redisOperations.opsForValue()).thenReturn(reactiveValueOperations);
    Mockito.when(reactiveValueOperations.get(Mockito.any())).thenReturn(Mono.empty());
    Mockito.when(reactiveValueOperations.set(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(true));
    Mono<List<ShippingOptionsResponse>> shippingOptionsResponse = shippingOptionsService
        .createShippingOptions(shippingOptionsPostRequest, embed);
    StepVerifier.create(shippingOptionsResponse).thenConsumeWhile(shippingResponse -> {
      Assertions.assertEquals("PUDO", shippingResponse.get(0).getFulfillmentType());
      Assertions.assertEquals("Abholung Abgabe", shippingResponse.get(0).getName());
      Assertions.assertEquals("Abholung Abgabe", shippingResponse.get(0).getDescription());
      return true;
    }).verifyComplete();
  }
  
  @Test
  void testGetShippingOptionsForPUDOWithPudoId() {
    List<String> embed = new ArrayList<>();
    embed.add("PUDO");
    ShippingOptionsPostRequest shippingOptionsPostRequest = buildShippingOptPostReqForPUDOAggregatorByPudoId();
    Mockito.when(siteIdReadRepository.findByName(Mockito.anyString())).thenReturn(Mono.just(prepareSiteIdForPudo()));
    Mockito.when(fulfillmentReadRepository.findBySiteIdAndFulfilmentType(Mockito.any(), Mockito.anyList())).thenReturn(Flux.just(prepareShippingOptionForPUDO()));
    Mockito.when(pudodpeServiceHandler.createPUDODPE(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
        .thenReturn(Mono.just(buildShippingOptionsResponseForPostForPUDO()));
    Mockito.when(shippingOptionsValidationService.validatePUDODPERequest(Mockito.any(),Mockito.any()))
    .thenReturn(Mono.just(buildShippingAddress()));
    Mockito.when(redisOperations.opsForValue()).thenReturn(reactiveValueOperations);
    Mockito.when(reactiveValueOperations.get(Mockito.any())).thenReturn(Mono.empty());
    Mockito.when(reactiveValueOperations.set(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(true));
    
    Mono<List<ShippingOptionsResponse>> shippingOptionsResponse = shippingOptionsService
        .createShippingOptions(shippingOptionsPostRequest, embed);
    StepVerifier.create(shippingOptionsResponse).thenConsumeWhile(shippingResponse -> {
      Assertions.assertEquals("PUDO", shippingResponse.get(0).getFulfillmentType());
      Assertions.assertEquals("Abholung Abgabe", shippingResponse.get(0).getName());
      Assertions.assertEquals("Abholung Abgabe", shippingResponse.get(0).getDescription());
      Assertions.assertEquals("1", shippingResponse.get(0).getShipments().get(0).getProductLines().get(0).getId());
      Assertions.assertEquals("M20323_530",
          shippingResponse.get(0).getShipments().get(0).getProductLines().get(0).getSku());
      Assertions.assertEquals(2, shippingResponse.get(0).getShipments().get(0).getProductLines().get(0).getQuantity());
      Assertions.assertEquals("inline",
          shippingResponse.get(0).getShipments().get(0).getProductLines().get(0).getLineType());
      return true;
    }).verifyComplete();
  }

  @Test
  void testGetShippingOptionsForPUDOWithLocation() {
    List<String> embed = new ArrayList<>();
    embed.add("PUDO");
    ShippingOptionsPostRequest shippingOptionsPostRequest = buildShippingOptionsPostRequestWhenLocationIsNotNull();
    Mockito.when(siteIdReadRepository.findByName(Mockito.anyString())).thenReturn(Mono.just(prepareSiteIdForPudo()));
    Mockito.when(fulfillmentReadRepository.findBySiteIdAndFulfilmentType(Mockito.any(), Mockito.anyList())).thenReturn(Flux.just(prepareShippingOptionForPUDO()));
    Mockito.when(pudodpeServiceHandler.createPUDODPE(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
        .thenReturn(Mono.just(buildShippingOptionsResponseForPostForPUDO()));
    Mockito.when(shippingOptionsValidationService.validatePUDODPERequest(Mockito.any(),Mockito.any()))
        .thenReturn(Mono.just(buildShippingAddress()));
    Mockito.when(redisOperations.opsForValue()).thenReturn(reactiveValueOperations);
    Mockito.when(reactiveValueOperations.get(Mockito.any())).thenReturn(Mono.empty());
    Mockito.when(reactiveValueOperations.set(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(true));
    
    Mono<List<ShippingOptionsResponse>> shippingOptionsResponse = shippingOptionsService
        .createShippingOptions(shippingOptionsPostRequest, embed);
    StepVerifier.create(shippingOptionsResponse).thenConsumeWhile(shippingResponse -> {
      Assertions.assertEquals("PUDO", shippingResponse.get(0).getFulfillmentType());
      Assertions.assertEquals("Abholung Abgabe", shippingResponse.get(0).getName());
      Assertions.assertEquals("Abholung Abgabe", shippingResponse.get(0).getDescription());
      return true;
    }).verifyComplete();
  }
  
  @Test
  void testGetShippingOptionsForPUDOFailure() {
    List<String> embed = new ArrayList<>();
    embed.add("PUDO");
    ShippingOptionsPostRequest shippingOptionsPostRequest = buildShippingOptionsPostRequestWhenLocationIsNotNull();
    Mockito.when(siteIdReadRepository.findByName(Mockito.anyString())).thenReturn(Mono.just(prepareSiteIdForPudo()));
    Mockito.when(fulfillmentReadRepository.findBySiteIdAndFulfilmentType(Mockito.any(), Mockito.anyList())).thenReturn(Flux.just(prepareShippingOptionForPUDO()));
    Mockito.when(pudodpeServiceHandler.createPUDODPE(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
        .thenReturn(Mono.just(buildShippingOptionsResponseForPostForPUDO()));
    Mockito.when(shippingOptionsValidationService.validatePUDODPERequest(Mockito.any(),Mockito.any()))
    .thenReturn(Mono.error(new Exception()));
    Mono<List<ShippingOptionsResponse>> shippingOptionsResponse = shippingOptionsService
        .createShippingOptions(shippingOptionsPostRequest, embed);
    StepVerifier.create(shippingOptionsResponse)
    .expectErrorMatches(throwable -> throwable instanceof Exception).verify();
  }
  
  @Test
  void testCachedSiteIdHomeDeliveryFlow() {
    List<String> embed = new ArrayList<>();
    embed.add("HomeDelivery");
    ShippingOptionsPostRequest shippingOptionsPostRequest = buildShippingOptionsPostRequest();
    Mockito.when(shippingOptionsValidationService.validateCNCDPEShippingOptionsRequest(Mockito.any(), Mockito.any()))
        .thenReturn(Mono.just(buildGeoCodingResponse()));
    Mockito.when(fulfillmentReadRepository.findBySiteIdAndFulfilmentType(Mockito.any(), Mockito.anyList())).thenReturn(Flux.just(prepareShippingOption()));
    Mockito.when(shippingOptionsValidationService.validateDPEHDRequest(Mockito.any())).thenReturn(Mono.just(buildShippingAddress()));
    Mockito.when(dpeHandler.createDPEHDEmptyResponse(Mockito.any(), Mockito.any()))
        .thenReturn(Mono.just(buildShippingOptionsResponseForPostwithNull()));
    Mockito.when(dpeHandler.createDPE(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
        .thenReturn(Mono.just(buildShippingOptionsResponseForPost()));
    Mockito.when(redisOperations.opsForValue()).thenReturn(reactiveValueOperations);
    Mockito.when(reactiveValueOperations.get("fulfillmentOptions-adidas-US")).thenReturn(Mono.empty());
    Mockito.when(reactiveValueOperations.get("siteId-adidas-US")).thenReturn(Mono.just(buildSiteIDJsonString()));
    Mockito.when(reactiveValueOperations.set(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(true));
    Mockito.when(jsonObjectMapper.jsonStringToObject(Mockito.any(), Mockito.any())).thenReturn(prepareSiteId());

    Mono<List<ShippingOptionsResponse>> shippingOptionsResponse = shippingOptionsService
        .createShippingOptions(shippingOptionsPostRequest, embed);
    StepVerifier.create(shippingOptionsResponse).thenConsumeWhile(shippingResponse -> {
      Assertions.assertEquals("HomeDelivery", shippingResponse.get(0).getFulfillmentType());
      Assertions.assertEquals("HomeDelivery", shippingResponse.get(0).getName());
      Assertions.assertEquals("HomeDelivery", shippingResponse.get(0).getDescription());
      Assertions.assertEquals("M20323_530", shippingResponse.get(0).getShipments().get(0).getProductLines().get(0).getSku());
      return true;
    }).verifyComplete();
  }

  @Test
  void testCachedFulfillmentOptionsHomeDeliveryFlow() {
    List<String> embed = new ArrayList<>();
    embed.add("HomeDelivery");
    ShippingOptionsPostRequest shippingOptionsPostRequest = buildShippingOptionsPostRequest();
    Mockito.when(shippingOptionsValidationService.validateCNCDPEShippingOptionsRequest(Mockito.any(), Mockito.any()))
        .thenReturn(Mono.just(buildGeoCodingResponse()));
    Mockito.when(siteIdReadRepository.findByName(Mockito.anyString())).thenReturn(Mono.just(prepareSiteId()));
    Mockito.when(shippingOptionsValidationService.validateDPEHDRequest(Mockito.any())).thenReturn(Mono.just(buildShippingAddress()));
    Mockito.when(dpeHandler.createDPEHDEmptyResponse(Mockito.any(), Mockito.any()))
        .thenReturn(Mono.just(buildShippingOptionsResponseForPostwithNull()));
    Mockito.when(dpeHandler.createDPE(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
        .thenReturn(Mono.just(buildShippingOptionsResponseForPost()));
    Mockito.when(redisOperations.opsForValue()).thenReturn(reactiveValueOperations);
    Mockito.when(reactiveValueOperations.get("fulfillmentOptions-adidas-US"))
        .thenReturn(Mono.just(buildFulfillmentOptionsJsonString()));
    Mockito.when(reactiveValueOperations.get("siteId-adidas-US")).thenReturn(Mono.empty());
    Mockito.when(reactiveValueOperations.set(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(true));
    Mockito.when(jsonObjectMapper.jsonStringToListOfObject(Mockito.any(), Mockito.any()))
        .thenReturn(List.of(prepareShippingOption()));

    Mono<List<ShippingOptionsResponse>> shippingOptionsResponse = shippingOptionsService
        .createShippingOptions(shippingOptionsPostRequest, embed);
    StepVerifier.create(shippingOptionsResponse).thenConsumeWhile(shippingResponse -> {
      Assertions.assertEquals("HomeDelivery", shippingResponse.get(0).getFulfillmentType());
      Assertions.assertEquals("HomeDelivery", shippingResponse.get(0).getName());
      Assertions.assertEquals("HomeDelivery", shippingResponse.get(0).getDescription());
      Assertions.assertEquals("M20323_530", shippingResponse.get(0).getShipments().get(0).getProductLines().get(0).getSku());
      return true;
    }).verifyComplete();
  }

  @Test
  void testGetShippingOptionsWhenEmbedIsEmptyAndPUDOFulfillmentTypeSupported() {
	    ShippingOptionsPostRequest shippingOptionsPostRequest = buildShippingOptionsPostRequestForPUDO();
	    Mockito.when(siteIdReadRepository.findByName(Mockito.anyString())).thenReturn(Mono.just(prepareSiteIdForPudo()));
	    Mockito.when(fulfillmentReadRepository.findBySiteIdAndFulfilmentType(Mockito.any(), Mockito.anyList())).thenReturn(Flux.just(prepareShippingOptionForPUDO()));
	    Mockito.when(pudodpeServiceHandler.createPUDODPE(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
	        .thenReturn(Mono.just(buildShippingOptionsResponseForPostForPUDO()));
	    Mockito.when(shippingOptionsValidationService.validatePUDODPERequest(Mockito.any(),Mockito.any()))
	        .thenReturn(Mono.just(buildShippingAddress()));
	    Mockito.when(redisOperations.opsForValue()).thenReturn(reactiveValueOperations);
	    Mockito.when(reactiveValueOperations.get(Mockito.any())).thenReturn(Mono.empty());
	    Mockito.when(reactiveValueOperations.set(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(true));
	    Mono<List<ShippingOptionsResponse>> shippingOptionsResponse = shippingOptionsService
	        .createShippingOptions(shippingOptionsPostRequest, new ArrayList<>());
	    StepVerifier.create(shippingOptionsResponse).thenConsumeWhile(shippingResponse -> {
	      Assertions.assertEquals("PUDO", shippingResponse.get(0).getFulfillmentType());
	      Assertions.assertEquals("Abholung Abgabe", shippingResponse.get(0).getName());
	      Assertions.assertEquals("Abholung Abgabe", shippingResponse.get(0).getDescription());
	      return true;
	    }).verifyComplete();
  }
  
  @Test
  void testGetShippingOptionsWhenHomeDeliveryInEmbedPassedAndOnlyCNCFulfillmentTypeSupported() {
    List<String> embed = new ArrayList<>();
    embed.add("HomeDelivery");
    ShippingOptionsPostRequest shippingOptionsPostRequest = buildShippingOptionsPostRequest();
    Mockito.when(siteIdReadRepository.findByName(Mockito.anyString())).thenReturn(Mono.just(prepareSiteId()));
    Mockito.when(shippingOptionsValidationService.validateCNCDPEShippingOptionsRequest(Mockito.any(), Mockito.any())).thenReturn(Mono.just(buildGeoCodingResponse()));
    Mockito.when(fulfillmentReadRepository.findBySiteIdAndFulfilmentType(Mockito.any(), Mockito.anyList())).thenReturn(Flux.just(prepareShippingOptionCNC()));
    Mockito.when(shippingOptionsValidationService.validateDPEHDRequest(Mockito.any())).thenReturn(Mono.just(buildShippingAddress()));
    Mockito.when(dpeHandler.createDPEHDEmptyResponse(Mockito.any(), Mockito.any())).thenReturn(Mono.just(buildShippingOptionsResponseForPostwithNull()));
    Mockito.when(dpeHandler.createDPE(Mockito.any(), Mockito.any(),Mockito.any(),Mockito.any())).thenReturn(Mono.just(buildShippingOptionsResponseForPost()));
    Mockito.when(redisOperations.opsForValue()).thenReturn(reactiveValueOperations);
    Mockito.when(reactiveValueOperations.get(Mockito.any())).thenReturn(Mono.empty());
    Mockito.when(reactiveValueOperations.set(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(true));
    Mono<List<ShippingOptionsResponse>> shippingOptionsResponse = shippingOptionsService
        .createShippingOptions(shippingOptionsPostRequest, embed);
    StepVerifier.create(shippingOptionsResponse)
    .expectErrorMatches(throwable -> throwable instanceof Exception).verify();
  }
  
  @Test
  void testGetShippingOptionsWhenHomeDeliveryInEmbedPassedAndNoSupportedFulfillmentType() {
    List<String> embed = new ArrayList<>();
    embed.add("HomeDelivery");
    ShippingOptionsPostRequest shippingOptionsPostRequest = buildShippingOptionsPostRequest();
    Mockito.when(siteIdReadRepository.findByName(Mockito.anyString())).thenReturn(Mono.just(prepareSiteId()));
    Mockito.when(shippingOptionsValidationService.validateCNCDPEShippingOptionsRequest(Mockito.any(), Mockito.any())).thenReturn(Mono.just(buildGeoCodingResponse()));
    Mockito.when(fulfillmentReadRepository.findBySiteIdAndFulfilmentType(Mockito.any(), Mockito.anyList())).thenReturn(Flux.empty());
    Mockito.when(shippingOptionsValidationService.validateDPEHDRequest(Mockito.any())).thenReturn(Mono.just(buildShippingAddress()));
    Mockito.when(dpeHandler.createDPEHDEmptyResponse(Mockito.any(), Mockito.any())).thenReturn(Mono.just(buildShippingOptionsResponseForPostwithNull()));
    Mockito.when(dpeHandler.createDPE(Mockito.any(), Mockito.any(),Mockito.any(),Mockito.any())).thenReturn(Mono.just(buildShippingOptionsResponseForPost()));
    Mockito.when(redisOperations.opsForValue()).thenReturn(reactiveValueOperations);
    Mockito.when(reactiveValueOperations.get(Mockito.any())).thenReturn(Mono.empty());
    Mockito.when(reactiveValueOperations.set(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(true));
    Mono<List<ShippingOptionsResponse>> shippingOptionsResponse = shippingOptionsService
        .createShippingOptions(shippingOptionsPostRequest, embed);
    StepVerifier.create(shippingOptionsResponse)
    .expectErrorMatches(throwable -> throwable instanceof Exception).verify();
  }
  
  @Test
  void testGetShippingOptionsForHomeDeliveryAndCNCSuccess() {
    List<String> embed = new ArrayList<>();
    embed.add("HomeDelivery");
    embed.add("ClickAndCollect");
    ShippingOptionsPostRequest shippingOptionsPostRequest = buildShippingOptionsPostRequest();
    Mockito.when(siteIdReadRepository.findByName(Mockito.anyString())).thenReturn(Mono.just(prepareSiteId()));
    Mockito.when(shippingOptionsValidationService.validateCNCDPEShippingOptionsRequest(Mockito.any(), Mockito.any())).thenReturn(Mono.just(buildGeoCodingResponse()));
    Mockito.when(fulfillmentReadRepository.findBySiteIdAndFulfilmentType(Mockito.any(), Mockito.anyList())).thenReturn(Flux.just(prepareShippingOption(), prepareShippingOptionForCNC()));
    Mockito.when(shippingOptionsValidationService.validateDPEHDRequest(Mockito.any())).thenReturn(Mono.just(buildShippingAddress()));
    Mockito.when(dpeHandler.createDPEHDEmptyResponse(Mockito.any(), Mockito.any())).thenReturn(Mono.just(buildShippingOptionsResponseForPostwithNull()));
    Mockito.when(dpeHandler.createDPE(Mockito.any(), Mockito.any(),Mockito.any(),Mockito.any())).thenReturn(Mono.just(buildShippingOptionsResponseForPost()));
    Mockito.when(redisOperations.opsForValue()).thenReturn(reactiveValueOperations);
    Mockito.when(reactiveValueOperations.get(Mockito.any())).thenReturn(Mono.empty());
    Mockito.when(reactiveValueOperations.set(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(true));
    Mockito.when(cncdpeHandler.createCNCDPE(Mockito.any(), Mockito.any(),Mockito.any(), Mockito.any())).thenReturn(Mono.just(buildShippingOptionsResponseForCNCPost()));
    
    Mono<List<ShippingOptionsResponse>> shippingOptionsResponse = shippingOptionsService
        .createShippingOptions(shippingOptionsPostRequest, embed);
    StepVerifier.create(shippingOptionsResponse).thenConsumeWhile(shippingResponse -> {
      Assertions.assertEquals(2, shippingResponse.size());
      return true;
    }).verifyComplete();
  }
  
  @Test
  void testGetShippingOptionsForHomeDeliveryAndPUDOSuccess() {
    List<String> embed = new ArrayList<>();
    embed.add("HomeDelivery");
    embed.add("PUDO");
    ShippingOptionsPostRequest shippingOptionsPostRequest = buildShippingOptionsPostRequest();
    Mockito.when(siteIdReadRepository.findByName(Mockito.anyString())).thenReturn(Mono.just(prepareSiteId()));
    Mockito.when(shippingOptionsValidationService.validateCNCDPEShippingOptionsRequest(Mockito.any(), Mockito.any())).thenReturn(Mono.just(buildGeoCodingResponse()));
    Mockito.when(fulfillmentReadRepository.findBySiteIdAndFulfilmentType(Mockito.any(), Mockito.anyList())).thenReturn(Flux.just(prepareShippingOption(), prepareShippingOptionForPUDO()));
    Mockito.when(shippingOptionsValidationService.validateDPEHDRequest(Mockito.any())).thenReturn(Mono.just(buildShippingAddress()));
    Mockito.when(dpeHandler.createDPEHDEmptyResponse(Mockito.any(), Mockito.any())).thenReturn(Mono.just(buildShippingOptionsResponseForPostwithNull()));
    Mockito.when(dpeHandler.createDPE(Mockito.any(), Mockito.any(),Mockito.any(),Mockito.any())).thenReturn(Mono.just(buildShippingOptionsResponseForPost()));
    Mockito.when(redisOperations.opsForValue()).thenReturn(reactiveValueOperations);
    Mockito.when(reactiveValueOperations.get(Mockito.any())).thenReturn(Mono.empty());
    Mockito.when(reactiveValueOperations.set(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(true));
    Mockito.when(pudodpeServiceHandler.createPUDODPE(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
    .thenReturn(Mono.just(buildShippingOptionsResponseForPostForPUDO()));
    Mockito.when(shippingOptionsValidationService.validatePUDODPERequest(Mockito.any(),Mockito.any()))
    .thenReturn(Mono.just(buildShippingAddress()));
    
    Mono<List<ShippingOptionsResponse>> shippingOptionsResponse = shippingOptionsService
        .createShippingOptions(shippingOptionsPostRequest, embed);
    StepVerifier.create(shippingOptionsResponse).thenConsumeWhile(shippingResponse -> {
      Assertions.assertEquals(2, shippingResponse.size());
      return true;
    }).verifyComplete();
  }
  
  @Test
  void testGetShippingOptionsForCNCAndPUDOSuccess() {
    List<String> embed = new ArrayList<>();
    embed.add("ClickAndCollect");
    embed.add("PUDO");
    ShippingOptionsPostRequest shippingOptionsPostRequest = buildShippingOptionsPostRequest();
    Mockito.when(siteIdReadRepository.findByName(Mockito.anyString())).thenReturn(Mono.just(prepareSiteId()));
    Mockito.when(shippingOptionsValidationService.validateCNCDPEShippingOptionsRequest(Mockito.any(), Mockito.any())).thenReturn(Mono.just(buildGeoCodingResponse()));
    Mockito.when(fulfillmentReadRepository.findBySiteIdAndFulfilmentType(Mockito.any(), Mockito.anyList())).thenReturn(Flux.just(prepareShippingOptionForCNC(), prepareShippingOptionForPUDO()));
    Mockito.when(cncdpeHandler.createCNCDPE(Mockito.any(), Mockito.any(),Mockito.any(), Mockito.any())).thenReturn(Mono.just(buildShippingOptionsResponseForCNCPost()));
    Mockito.when(redisOperations.opsForValue()).thenReturn(reactiveValueOperations);
    Mockito.when(reactiveValueOperations.get(Mockito.any())).thenReturn(Mono.empty());
    Mockito.when(reactiveValueOperations.set(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(true));
    Mockito.when(pudodpeServiceHandler.createPUDODPE(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
    .thenReturn(Mono.just(buildShippingOptionsResponseForPostForPUDO()));
    Mockito.when(shippingOptionsValidationService.validatePUDODPERequest(Mockito.any(),Mockito.any()))
    .thenReturn(Mono.just(buildShippingAddress()));
    
    Mono<List<ShippingOptionsResponse>> shippingOptionsResponse = shippingOptionsService
        .createShippingOptions(shippingOptionsPostRequest, embed);
    StepVerifier.create(shippingOptionsResponse).thenConsumeWhile(shippingResponse -> {
      Assertions.assertEquals(2, shippingResponse.size());
      return true;
    }).verifyComplete();
  }
  
  @Test
  void testGetShippingOptionsForHomeDeliveryCNCAndPUDOSuccess() {
    List<String> embed = new ArrayList<>();
    embed.add("HomeDelivery");
    embed.add("ClickAndCollect");
    embed.add("PUDO");
    ShippingOptionsPostRequest shippingOptionsPostRequest = buildShippingOptionsPostRequest();
    Mockito.when(siteIdReadRepository.findByName(Mockito.anyString())).thenReturn(Mono.just(prepareSiteId()));
    Mockito.when(shippingOptionsValidationService.validateCNCDPEShippingOptionsRequest(Mockito.any(), Mockito.any())).thenReturn(Mono.just(buildGeoCodingResponse()));
    Mockito.when(fulfillmentReadRepository.findBySiteIdAndFulfilmentType(Mockito.any(), Mockito.anyList())).thenReturn(Flux.just(prepareShippingOption(), 
        prepareShippingOptionForCNC(), prepareShippingOptionForPUDO()));
    Mockito.when(shippingOptionsValidationService.validateDPEHDRequest(Mockito.any())).thenReturn(Mono.just(buildShippingAddress()));
    Mockito.when(dpeHandler.createDPEHDEmptyResponse(Mockito.any(), Mockito.any())).thenReturn(Mono.just(buildShippingOptionsResponseForPostwithNull()));
    Mockito.when(dpeHandler.createDPE(Mockito.any(), Mockito.any(),Mockito.any(),Mockito.any())).thenReturn(Mono.just(buildShippingOptionsResponseForPost()));
    Mockito.when(redisOperations.opsForValue()).thenReturn(reactiveValueOperations);
    Mockito.when(reactiveValueOperations.get(Mockito.any())).thenReturn(Mono.empty());
    Mockito.when(reactiveValueOperations.set(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(true));
    Mockito.when(cncdpeHandler.createCNCDPE(Mockito.any(), Mockito.any(),Mockito.any(), Mockito.any())).thenReturn(Mono.just(buildShippingOptionsResponseForCNCPost()));
    Mockito.when(pudodpeServiceHandler.createPUDODPE(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
    .thenReturn(Mono.just(buildShippingOptionsResponseForPostForPUDO()));
    Mockito.when(shippingOptionsValidationService.validatePUDODPERequest(Mockito.any(),Mockito.any()))
    .thenReturn(Mono.just(buildShippingAddress()));
    
    Mono<List<ShippingOptionsResponse>> shippingOptionsResponse = shippingOptionsService
        .createShippingOptions(shippingOptionsPostRequest, embed);
    StepVerifier.create(shippingOptionsResponse).thenConsumeWhile(shippingResponse -> {
      Assertions.assertEquals(3, shippingResponse.size());
      return true;
    }).verifyComplete();
  }

  @Test
  void testGetShippingOptionsWhenEmbedIsEmptyAndAllFulfilmentSupported() {
    ShippingOptionsPostRequest shippingOptionsPostRequest = buildShippingOptionsPostRequest();
    Mockito.when(siteIdReadRepository.findByName(Mockito.anyString())).thenReturn(Mono.just(prepareSiteId()));
    Mockito.when(shippingOptionsValidationService.validateCNCDPEShippingOptionsRequest(Mockito.any(), Mockito.any())).thenReturn(Mono.just(buildGeoCodingResponse()));
    Mockito.when(fulfillmentReadRepository.findBySiteIdAndFulfilmentType(Mockito.any(), Mockito.anyList())).thenReturn(prepareShippingOptionList());
    Mockito.when(shippingOptionsValidationService.validateDPEHDRequest(Mockito.any())).thenReturn(Mono.just(buildShippingAddress()));
    Mockito.when(dpeHandler.createDPEHDEmptyResponse(Mockito.any(), Mockito.any())).thenReturn(Mono.just(buildShippingOptionsResponseForPostwithNull()));
    Mockito.when(dpeHandler.createDPE(Mockito.any(), Mockito.any(),Mockito.any(),Mockito.any())).thenReturn(Mono.just(buildShippingOptionsResponseForPost()));
    Mockito.when(cncdpeHandler.createCNCDPE(Mockito.any(), Mockito.any(),Mockito.any(), Mockito.any())).thenReturn(Mono.just(buildShippingOptionsResponseForCNCPost()));
    Mockito.when(redisOperations.opsForValue()).thenReturn(reactiveValueOperations);
    Mockito.when(reactiveValueOperations.get(Mockito.any())).thenReturn(Mono.empty());
    Mockito.when(reactiveValueOperations.set(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(true));
    Mockito.when(pudodpeServiceHandler.createPUDODPE(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
    .thenReturn(Mono.just(buildShippingOptionsResponseForPostForPUDO()));
    Mockito.when(shippingOptionsValidationService.validatePUDODPERequest(Mockito.any(), Mockito.any()))
        .thenReturn(Mono.just(buildShippingAddress()));
    Mockito.when(redisOperations.opsForValue()).thenReturn(reactiveValueOperations);
    Mockito.when(reactiveValueOperations.get(Mockito.any())).thenReturn(Mono.empty());
    Mockito.when(reactiveValueOperations.set(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(true));
    Mono<List<ShippingOptionsResponse>> shippingOptionsResponse = shippingOptionsService
        .createShippingOptions(shippingOptionsPostRequest, new ArrayList<>());
    StepVerifier.create(shippingOptionsResponse).thenConsumeWhile(shippingResponse -> {
      Assertions.assertEquals(3, shippingResponse.size());
      Assertions.assertEquals("ClickAndCollect", shippingResponse.get(0).getFulfillmentType());
      Assertions.assertEquals("PUDO", shippingResponse.get(1).getFulfillmentType());
      Assertions.assertEquals("HomeDelivery", shippingResponse.get(2).getFulfillmentType());
      return true;
    }).verifyComplete();
  }
  
  @Test
  void testGetShippingOptionsForPUDOOnlySiteId() {
    List<String> embed = new ArrayList<>();
    embed.add("PUDO");
    Mockito.when(siteIdReadRepository.findByName(Mockito.anyString())).thenReturn(Mono.just(prepareSiteIdForPudo()));
    Mockito.when(fulfillmentReadRepository.findBySiteIdAndFulfilmentType(Mockito.any(), Mockito.anyList()))
        .thenReturn(Flux.just(prepareShippingOptionForPUDO()));
    Mockito.when(shippingOptionsValidationService.validatePUDODPERequest(Mockito.any(), Mockito.any()))
        .thenReturn(Mono.just(buildShippingAddress()));
    Mockito.when(redisOperations.opsForValue()).thenReturn(reactiveValueOperations);
    Mockito.when(reactiveValueOperations.get(Mockito.any())).thenReturn(Mono.empty());
    Mockito.when(reactiveValueOperations.set(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(true));
    Mockito.when(pudoSiteIdDefaultResponseHandler.createPUDOSiteIdDefaultResponse(Mockito.any(), Mockito.any(), Mockito.any()))
        .thenReturn(Mono.just(buildShippingOptionsResponseForPostForPUDO()));
    Mockito.when(shippingOptionsValidationService.isSiteIdCall(Mockito.any())).thenReturn(true);
    ShippingOptionsPostRequest shippingOptionsPostRequest = buildShippingOptionsPostRequestOnlySiteIdUK();
    Mono<List<ShippingOptionsResponse>> shippingOptionsResponse = shippingOptionsService
        .createShippingOptions(shippingOptionsPostRequest, embed);
    StepVerifier.create(shippingOptionsResponse).thenConsumeWhile(shippingResponse -> {
      Assertions.assertEquals("PUDO", shippingResponse.get(0).getFulfillmentType());
      Assertions.assertEquals("Abholung Abgabe", shippingResponse.get(0).getName());
      Assertions.assertEquals("Abholung Abgabe", shippingResponse.get(0).getDescription());
      return true;
    }).verifyComplete();
  }
  
  @Test
  void testUSPSShippingMethodForPostBoxAddressLines() {
    List<String> embed = new ArrayList<>();
    embed.add("HomeDelivery");
    ShippingOptionsPostRequest shippingOptionsPostRequest = buildShippingOptionsPostRequestWithAddressLines();
    ShippingOptionsResponse shippingOptionsResponse = buildShippingOptionsResponseForUS();
    shippingOptionsResponse.setShipments(buildUSPSShipmentsForUS());
    
    Mockito.when(siteIdReadRepository.findByName(Mockito.anyString())).thenReturn(Mono.just(prepareSiteId()));
    Mockito.when(shippingOptionsValidationService.validateAddressLineAndState(Mockito.any())).thenReturn(true);
    Mockito.when(fulfillmentReadRepository.findBySiteIdAndFulfilmentType(Mockito.any(), Mockito.anyList()))
        .thenReturn(Flux.just(prepareShippingOption()));
    Mockito.when(redisOperations.opsForValue()).thenReturn(reactiveValueOperations);
    Mockito.when(reactiveValueOperations.get(Mockito.any())).thenReturn(Mono.empty());
    Mockito.when(reactiveValueOperations.set(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(true));
    Mockito.when(dpeDefaultResponseHandler.createUSPSDPEDefaultResponse(Mockito.any(), Mockito.any(), Mockito.any()))
        .thenReturn(Mono.just(shippingOptionsResponse));
    
    Mono<List<ShippingOptionsResponse>> shippingOptionsRes = shippingOptionsService
        .createShippingOptions(shippingOptionsPostRequest, embed);
    StepVerifier.create(shippingOptionsRes).thenConsumeWhile(shippingResponse -> {
      Assertions.assertEquals("HomeDelivery", shippingResponse.get(0).getFulfillmentType());
      String shippingMethod = shippingResponse.get(0).getShipments().toString();
      String[] shippingMethodArray = shippingMethod.split(",");
      Assertions.assertEquals("Standard USPS-Unregistered",
          shippingMethodArray[1].substring(shippingMethodArray[1].indexOf("=") + 1, shippingMethodArray[1].length()));
      Assertions.assertEquals("USPS", shippingMethodArray[10].substring(shippingMethodArray[10].indexOf("=") + 1,
          shippingMethodArray[10].length()));
      Assertions.assertEquals("USP000US0000000000", shippingMethodArray[11]
          .substring(shippingMethodArray[11].indexOf("=") + 1, shippingMethodArray[11].length()));
      Assertions.assertEquals("PP (Parcel Post)", shippingMethodArray[12]
          .substring(shippingMethodArray[12].indexOf("=") + 1, shippingMethodArray[12].length()));
      return true;
    }).verifyComplete();
  }

}
