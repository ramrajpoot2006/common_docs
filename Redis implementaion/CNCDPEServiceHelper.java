package com.adidas.next.shippingapi.handler.cnc;

import java.time.Duration;
import java.util.Arrays;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.stereotype.Component;

import com.adidas.next.shippingapi.entity.ShippingMethods;
import com.adidas.next.shippingapi.entity.SiteId;
import com.adidas.next.shippingapi.handler.ShippingMethodsCNCRulesHandler;
import com.adidas.next.shippingapi.repository.ShippingReadRepository;
import com.adidas.next.shippingapi.resources.request.ShippingOptionsPostRequest;
import com.adidas.next.shippingapi.resources.response.rule.DependentAndIndependetRules;
import com.adidas.next.shippingapi.util.JsonObjectMapper;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class CNCDPEServiceHelper {
  
  @Value("${cache-expiry-seconds}")
  private long cacheExpiryTime;
  
  private final ReactiveRedisOperations<String, String> redisOperations;
  private final ShippingReadRepository shippingReadRepository;
  private final JsonObjectMapper jsonObjectMapper;
  private final ShippingMethodsCNCRulesHandler shippingMethodsCNCRulesHandler;
  
  public CNCDPEServiceHelper(ReactiveRedisOperations<String, String> redisOperations,
      ShippingReadRepository shippingReadRepository, JsonObjectMapper jsonObjectMapper,
      ShippingMethodsCNCRulesHandler shippingMethodsCNCRulesHandler) {
    this.redisOperations = redisOperations;
    this.shippingReadRepository = shippingReadRepository;
    this.jsonObjectMapper = jsonObjectMapper;
    this.shippingMethodsCNCRulesHandler = shippingMethodsCNCRulesHandler;
  }

  public Mono<ShippingMethods> getShippingMethods(String fulfillmentType, ShippingOptionsPostRequest shippingOptionsRequest, SiteId siteResponse) {
    String siteId = shippingOptionsRequest.getSiteId();
    var cacheKey = String.join("-", "shippingMethods", siteId, fulfillmentType);
    return redisOperations.opsForValue().get(cacheKey).map(jsonString -> {
      log.info("Get cached shippingMethods value for siteId : {} and fulfillmentType : {}", siteId, fulfillmentType);
      return jsonObjectMapper.jsonStringToObject(ShippingMethods.class, jsonString);
    }).switchIfEmpty(Mono.defer(() -> shippingReadRepository.findBySiteIdAndFulfillmentType(siteResponse.getId(), fulfillmentType)
            .flatMap(shippingMethods -> redisOperations.opsForValue().set(cacheKey, jsonObjectMapper.toJsonString(shippingMethods), Duration.ofSeconds(cacheExpiryTime))
                .then(Mono.just(shippingMethods)))));
  }
  
  public Mono<DependentAndIndependetRules> getShipmentMethodRuleData(ShippingMethods shipMethod,ShippingOptionsPostRequest shippingOptionsRequest, Set<Integer> excludedShippingId) {
   return shippingMethodsCNCRulesHandler.getRules(Arrays.asList(shipMethod.getShippingMethodId()))
  .flatMap(rulesData -> shippingMethodsCNCRulesHandler.getCNCFilteredShippingMethods(rulesData,shippingOptionsRequest,excludedShippingId));
  }
  
}
