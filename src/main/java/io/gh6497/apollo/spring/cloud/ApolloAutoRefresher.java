package io.gh6497.apollo.spring.cloud;

// Created on 2022-03-31

import com.ctrip.framework.apollo.ConfigChangeListener;
import com.ctrip.framework.apollo.ConfigService;
import com.ctrip.framework.apollo.core.ConfigConsts;
import com.ctrip.framework.apollo.model.ConfigChangeEvent;
import com.ctrip.framework.apollo.spring.config.PropertySourcesConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.cloud.context.scope.refresh.RefreshScope;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Len
 */
public class ApolloAutoRefresher implements ConfigChangeListener, ApplicationListener<ApplicationReadyEvent>, ApplicationContextAware, EnvironmentAware {

  public static final String APOLLO_CONFIG_PROTOCOL = "apollo://";

  protected final Log logger = LogFactory.getLog(getClass());

  @Nullable
  ApplicationContext applicationContext;

  @Nullable
  Environment environment;

  RefreshScope refreshScope;

  boolean init = false;

  public ApolloAutoRefresher(RefreshScope refreshScope) {
    this.refreshScope = refreshScope;
  }

  @Override
  public void onChange(ConfigChangeEvent changeEvent) {

    logger.info("refresh namespace:[" + changeEvent.getNamespace() + "],keys:" + changeEvent.changedKeys());

    if (applicationContext == null) {
      return;
    }
    refreshScope.refreshAll();
    applicationContext.publishEvent(new EnvironmentChangeEvent(applicationContext, changeEvent.changedKeys()));
  }

  public Set<String> getNamespaces() {
    if (environment == null) {
      return Collections.emptySet();
    }
    String version = SpringBootVersion.getVersion();
    Set<String> namespaces = new HashSet<String>();
    if (version.compareTo("2.4") >= 0) {
      String imports = environment.getProperty("spring.config.import");
      Set<String> strings = StringUtils.commaDelimitedListToSet(imports);
      for (String string : strings) {
        if (string.startsWith(APOLLO_CONFIG_PROTOCOL)) {
          namespaces.add(string.substring(string.indexOf(APOLLO_CONFIG_PROTOCOL) + APOLLO_CONFIG_PROTOCOL.length()).trim());
        }
      }
      if (!namespaces.isEmpty()) {
        return namespaces;
      }
    }
    String apolloNamespacesString = environment.getProperty(PropertySourcesConstants.APOLLO_BOOTSTRAP_NAMESPACES, ConfigConsts.NAMESPACE_APPLICATION);
    namespaces.addAll(StringUtils.commaDelimitedListToSet(apolloNamespacesString));

    return namespaces;
  }

  private synchronized void addWatch() {
    for (String namespace : getNamespaces()) {
      ConfigService.getConfig(namespace).addChangeListener(this);
    }
    init = true;
  }

  @Override
  public void onApplicationEvent(ApplicationReadyEvent event) {
    addWatch();
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }

  @Override
  public void setEnvironment(Environment environment) {
    this.environment = environment;
  }
}
