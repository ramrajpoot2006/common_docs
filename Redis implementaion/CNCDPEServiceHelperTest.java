package com.adidas.next.shippingapi.handler;

import java.util.HashSet;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.adidas.next.shippingapi.TestHelper;
import com.adidas.next.shippingapi.handler.cnc.CNCDPEServiceHelper;
import com.adidas.next.shippingapi.repository.ShippingReadRepository;
import com.adidas.next.shippingapi.util.JsonObjectMapper;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(SpringExtension.class)
public class CNCDPEServiceHelperTest extends TestHelper {

  @Mock
  ReactiveRedisOperations<String, String> redisOperations;
  
  @Mock
  ReactiveValueOperations<String, String> reactiveValueOperations;
  
  @Mock
  JsonObjectMapper jsonObjectMapper ;
  
  @Mock
  ShippingReadRepository shippingReadRepository;
  
  @Mock
  ShippingMethodsCNCRulesHandler shippingMethodsCNCRulesHandler;
  
  @InjectMocks
  CNCDPEServiceHelper cncDPEServiceHelper;

  @Test
  void testGetShippingMethodDB() {
    Mockito.when(shippingReadRepository.findBySiteIdAndFulfillmentType(Mockito.any(),Mockito.any())).thenReturn(buildShippingMethodsForCNC());
    Mockito.when(shippingMethodsCNCRulesHandler.getRules(Mockito.any())).thenReturn(Mono.just(buildShippingMethodsRulesDataList()));
    Mockito.when(shippingMethodsCNCRulesHandler.getCNCFilteredShippingMethods(Mockito.any(),Mockito.any(), Mockito.any())).thenReturn(Mono.just(buildDependentAndIndependetRules()));
    Mockito.when(redisOperations.opsForValue()).thenReturn(reactiveValueOperations);
    Mockito.when(reactiveValueOperations.get(Mockito.any())).thenReturn(Mono.empty());
    Mockito.when(reactiveValueOperations.set(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(true));
    StepVerifier.create(
        cncDPEServiceHelper.getShippingMethods("ClickAndCollect", prepareShippingOptionsPostRequestForCNCUS(), prepareSiteId()))
        .thenConsumeWhile(shippingMethodsResponse -> {
          Assertions.assertEquals(1, shippingMethodsResponse.getSiteId());
          Assertions.assertEquals(UUID.fromString("760565d9-3e78-4b27-bf6e-b64912e3c531"), shippingMethodsResponse.getShippingMethodId());
          return true;
        }).verifyComplete();
  }
  
  @Test
  void testGetShippingMethodCache() {
    Mockito.when(redisOperations.opsForValue()).thenReturn(reactiveValueOperations);
    Mockito.when(reactiveValueOperations.get(Mockito.any())).thenReturn(Mono.just(buildShippingMethodsJsonString()));
    Mockito.when(jsonObjectMapper.jsonStringToObject(Mockito.any(), Mockito.any())).thenReturn(buildShippingMethod());
    Mockito.when(shippingReadRepository.findBySiteIdAndFulfillmentType(Mockito.any(),Mockito.any())).thenReturn(buildShippingMethodsForCNC());
    Mockito.when(shippingMethodsCNCRulesHandler.getRules(Mockito.any())).thenReturn(Mono.just(buildShippingMethodsRulesDataList()));
    Mockito.when(shippingMethodsCNCRulesHandler.getCNCFilteredShippingMethods(Mockito.any(),Mockito.any(), Mockito.any())).thenReturn(Mono.just(buildDependentAndIndependetRules()));
    StepVerifier.create(
        cncDPEServiceHelper.getShippingMethods("ClickAndCollect", prepareShippingOptionsPostRequestForCNCUS(), prepareSiteId()))
        .thenConsumeWhile(shippingMethodsResponse -> {
          Assertions.assertEquals(1, shippingMethodsResponse.getSiteId());
          Assertions.assertEquals(UUID.fromString("f5ffb268-5ae6-423e-8e88-58f4af0354c1"), shippingMethodsResponse.getShippingMethodId());
          return true;
        }).verifyComplete();
  }
  
  @Test
  void getShipmentMethodRuleData() {
    Mockito.when(shippingReadRepository.findBySiteIdAndFulfillmentType(Mockito.any(),Mockito.any())).thenReturn(buildShippingMethodsForCNC());
    Mockito.when(shippingMethodsCNCRulesHandler.getRules(Mockito.any())).thenReturn(Mono.just(buildShippingMethodsRulesDataList()));
    Mockito.when(shippingMethodsCNCRulesHandler.getCNCFilteredShippingMethods(Mockito.any(),Mockito.any(), Mockito.any())).thenReturn(Mono.just(buildDependentAndIndependetRules()));
    StepVerifier.create(
        cncDPEServiceHelper.getShipmentMethodRuleData(buildShippingMethodsListForCNC(), prepareShippingOptionsPostRequestForCNCUS(), new HashSet<Integer>()))
        .thenConsumeWhile(shipmentMethodsRulesData -> {
          Assertions.assertTrue(shipmentMethodsRulesData.getShipmentMethodsRulesData().get(0).getInclusionRuleTypes().contains("Postal_Code"));
          Assertions.assertTrue(shipmentMethodsRulesData.getShipmentMethodsRulesData().get(0).getInclusionData().contains("99972"));
          return true;
        }).verifyComplete();
  }

}
