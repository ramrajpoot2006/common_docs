package com.adidas.next.shippingapi.service;

import java.time.Duration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisOperations;

import com.adidas.next.shippingapi.entity.FulfillmentOption;
import com.adidas.next.shippingapi.entity.SiteId;
import com.adidas.next.shippingapi.enums.FulfillmentType;
import com.adidas.next.shippingapi.handler.hd.DPEDefaultResponseHandler;
import com.adidas.next.shippingapi.repository.FulfillmentReadRepository;
import com.adidas.next.shippingapi.repository.SiteIdReadRepository;
import com.adidas.next.shippingapi.resources.request.ShippingOptionsPostRequest;
import com.adidas.next.shippingapi.resources.response.ShippingOptionsResponse;
import com.adidas.next.shippingapi.util.JsonObjectMapper;

import lombok.extern.slf4j.Slf4j;

import reactor.core.publisher.Mono;

@Slf4j
public class ShippingOptionsBaseService {
  
  private static final String HOMEDELIVERY = FulfillmentType.HOMEDELIVERY.getValue();

  @Value("${cache-expiry-seconds}")
  private long cacheExpiryTime;

  @Autowired
  private ReactiveRedisOperations<String, String> redisOperations;

  @Autowired
  private JsonObjectMapper jsonObjectMapper;

  @Autowired
  private DPEDefaultResponseHandler dpeDefaultResponseHandler;

  private final FulfillmentReadRepository fulfillmentReadRepository;
  private final SiteIdReadRepository siteIdReadRepository;

  public ShippingOptionsBaseService(FulfillmentReadRepository fulfillmentReadRepository,
      SiteIdReadRepository siteIdReadRepository) {
    this.fulfillmentReadRepository = fulfillmentReadRepository;
    this.siteIdReadRepository = siteIdReadRepository;
  }

  public Mono<Map<String, FulfillmentOption>> getfulfillmentOptions(SiteId siteId, List<String> fulfillmentTypes) {
    var cacheKey = String.join("-", "fulfillmentOptions", siteId.getName());
    return redisOperations.opsForValue().get(cacheKey).map(jsonString -> {
      log.info("Get cached fulfillmentOptions value for siteId : {}", siteId.getName());
      return jsonObjectMapper.jsonStringToListOfObject(jsonString, FulfillmentOption.class);
    }).switchIfEmpty(Mono.defer(() -> fulfillmentReadRepository.findBySiteIdAndFulfilmentType(siteId.getId(), 
        fulfillmentTypes).collectList()
        .flatMap(fulfillmentOptions -> redisOperations.opsForValue()
                    .set(cacheKey, jsonObjectMapper.toJsonString(fulfillmentOptions),
                        Duration.ofSeconds(cacheExpiryTime))
                    .then(Mono.just(fulfillmentOptions)))))
      .flatMap(this::prepareFulfillmentOptions);
  }

  public Mono<SiteId> getSiteId(String siteId) {
    var cacheKey = String.join("-", "siteId", siteId);
    return redisOperations.opsForValue().get(cacheKey).map(jsonString -> {
      log.info("Get cached value for siteId : {}", siteId);
      return jsonObjectMapper.jsonStringToObject(SiteId.class, jsonString);
    }).switchIfEmpty(getSiteIdByName(siteId, cacheKey));
  }
  
  public Mono<ShippingOptionsResponse> createUSPSDPEResponse(ShippingOptionsPostRequest shippingOptionsRequest,
      SiteId siteIdResponse, Map<String, FulfillmentOption> fulfillmentOptionMap) {
    return dpeDefaultResponseHandler.createUSPSDPEDefaultResponse(shippingOptionsRequest, siteIdResponse,
        fulfillmentOptionMap.get(HOMEDELIVERY));
  }

  private Mono<Map<String, FulfillmentOption>> prepareFulfillmentOptions(List<FulfillmentOption> fulfillmentOptions) {
    Map<String, FulfillmentOption> fulfillmentOptionsMap = new HashMap<>();
    fulfillmentOptions.forEach(shippingOption -> fulfillmentOptionsMap.put(shippingOption.getFulfillmentType(), shippingOption));
    return Mono.just(fulfillmentOptionsMap);
  }

  private Mono<SiteId> getSiteIdByName(String siteId, String cacheKey) {
    return Mono.defer(() -> siteIdReadRepository.findByName(siteId)
        .flatMap(siteIdResponse -> redisOperations.opsForValue()
            .set(cacheKey, jsonObjectMapper.toJsonString(siteIdResponse), Duration.ofSeconds(cacheExpiryTime))
            .then(Mono.just(siteIdResponse))));
  }
}
