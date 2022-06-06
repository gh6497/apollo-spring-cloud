package io.gh6497.apollo.spring.cloud;

// Created on 2022-04-07

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.context.scope.refresh.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Len
 */
@Configuration(proxyBeanMethods = false)
public class ApolloCloudAutoConfiguration {

  @Bean
  @ConditionalOnProperty(prefix = "apollo.bootstrap.auto-refresh", name = "enabled", havingValue = "true", matchIfMissing = true)
  public ApolloAutoRefresher apolloCloudConfigService(RefreshScope refreshScope) {
    return new ApolloAutoRefresher(refreshScope);
  }
}
