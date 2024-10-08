package com.adidas.next.shippingapi.controller;

import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.adidas.next.shippingapi.PactHelper;
import com.adidas.next.shippingapi.service.ShippingOptionsService;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import au.com.dius.pact.provider.junitsupport.loader.PactBrokerAuth;
import au.com.dius.pact.provider.spring.junit5.MockMvcTestTarget;
import lombok.val;
import reactor.core.publisher.Mono;

@Provider("shipping-service-api")
@PactBroker(host = "adidascdc.pactflow.io",
        authentication = @PactBrokerAuth(token = "4qX5J4fMVinEsUpYLFbJPA"))
@SuppressWarnings({"deprecation", "unused"})
@ExtendWith(SpringExtension.class)
public class ShippingControllerVerifyPactTest extends PactHelper {
  @Mock
  ShippingOptionsService shippingOptionsService;

  @InjectMocks
  ShippingOptionsController shippingOptionsController;

  @TestTemplate
  @ExtendWith(PactVerificationInvocationContextProvider.class)
  void pactVerificationTestTemplate(PactVerificationContext context) {
    ReflectionTestUtils.setField(shippingOptionsService,"cacheExpiryTime",7200);
    context.verifyInteraction();
  }

  @BeforeEach
  void before(PactVerificationContext context) {
    val target = new MockMvcTestTarget();
    target.setMockMvc(MockMvcBuilders.standaloneSetup(shippingOptionsController).setCustomArgumentResolvers().build());
    context.setTarget(target);
  }

  @State("get home delivery shipping options")
  void getHomeDeliveryShippingOptions() {
    Mockito.when(shippingOptionsService.createShippingOptions(Mockito.any(),Mockito.any()))
        .thenReturn(Mono.just(buildShippingOptionsResponseForHomeDelivery()));
  }

  @State("get cnc shipping options")
  void getCNCShippingOptions() {
    Mockito.when(shippingOptionsService.createShippingOptions(Mockito.any(),Mockito.any()))
            .thenReturn(Mono.just(buildShippingOptionsResponseForCNC()));
  }

  @State("get all available shipping options")
  void getAllShippingOptions() {
    Mockito.when(shippingOptionsService.createShippingOptions(Mockito.any(),Mockito.any()))
            .thenReturn(Mono.just(buildShippingOptionsResponseForAll()));
  }
}
