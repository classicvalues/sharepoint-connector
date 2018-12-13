/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.enterprise.cloudsearch.sharepoint;

import static org.junit.Assert.assertEquals;

import com.google.common.collect.ImmutableMap;
import com.google.enterprise.cloudsearch.sdk.InvalidConfigurationException;
import com.google.enterprise.cloudsearch.sdk.config.Configuration.ResetConfigRule;
import com.google.enterprise.cloudsearch.sdk.config.Configuration.SetupConfigRule;
import com.google.enterprise.cloudsearch.sdk.identity.IdentitySourceConfiguration;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/** Unit tests for {@link SharePointConfiguration} */
public class SharePointConfigurationTest {
  @Rule public ExpectedException thrown = ExpectedException.none();
  @Rule public ResetConfigRule resetConfig = new ResetConfigRule();
  @Rule public SetupConfigRule setupConfig = SetupConfigRule.uninitialized();

  @Test
  public void testFromConfigurationConfigNotInitialized() {
    thrown.expect(IllegalStateException.class);
    SharePointConfiguration.fromConfiguration();
  }

  @Test
  public void testFromConfigurationConfigNoSharePointUrl() {
    setupConfig.initConfig(new Properties());
    thrown.expect(InvalidConfigurationException.class);
    thrown.expectMessage("sharepoint.server");
    SharePointConfiguration.fromConfiguration();
  }

  @Test
  public void testFromConfigurationInavlidSharepointUrl() {
    Properties properties = new Properties();
    properties.put("sharepoint.server", "something");
    setupConfig.initConfig(properties);
    thrown.expect(InvalidConfigurationException.class);
    thrown.expectMessage("Invalid SharePoint URL");
    SharePointConfiguration.fromConfiguration();
  }

  @Test
  public void testFromConfigurationWithDefaults() throws Exception {
    setupConfig.initConfig(getBaseConfiguration());
    SharePointConfiguration configuration = SharePointConfiguration.fromConfiguration();
    assertEquals(true, configuration.isPerformBrowserLeniency());
    assertEquals(false, configuration.isPerformXmlValidation());
    SharePointUrl expectedUrl =
        new SharePointUrl.Builder("http://localhost").setPerformBrowserLeniency(true).build();
    assertEquals(expectedUrl, configuration.getSharePointUrl());
    assertEquals(false, configuration.isSiteCollectionUrl());
  }

  @Test
  public void testFromConfigurationMatchesDefaultBuilder() throws Exception {
    setupConfig.initConfig(getBaseConfiguration());
    SharePointConfiguration fromConfiguration = SharePointConfiguration.fromConfiguration();
    SharePointConfiguration fromBuilder =
        new SharePointConfiguration.Builder(
                new SharePointUrl.Builder("http://localhost")
                    .setPerformBrowserLeniency(true)
                    .build())
            .setPassword("password")
            .setUserName("username")
            .setReferenceIdentitySourceConfiguration(ImmutableMap.of())
            .build();
    assertEquals(fromConfiguration, fromBuilder);
    assertEquals(fromConfiguration.toString(), fromBuilder.toString());
    assertEquals(fromConfiguration.hashCode(), fromBuilder.hashCode());
  }

  @Test
  public void testFromConfigurationWithSiteCollectionOnly() throws Exception {
    Properties baseConfiguration = getBaseConfiguration();
    baseConfiguration.put("sharepoint.siteCollectionOnly", "true");
    setupConfig.initConfig(baseConfiguration);
    SharePointConfiguration configuration = SharePointConfiguration.fromConfiguration();
    assertEquals(true, configuration.isPerformBrowserLeniency());
    assertEquals(false, configuration.isPerformXmlValidation());
    SharePointUrl expectedUrl =
        new SharePointUrl.Builder("http://localhost").setPerformBrowserLeniency(true).build();
    assertEquals(expectedUrl, configuration.getSharePointUrl());
    assertEquals(true, configuration.isSiteCollectionUrl());
  }

  @Test
  public void testFromConfigurationWithSiteCollectionOnlyByUrl() throws Exception {
    Properties baseConfiguration = getBaseConfiguration();
    baseConfiguration.replace("sharepoint.server", "http://localhost/sites/collection");
    setupConfig.initConfig(baseConfiguration);
    SharePointConfiguration configuration = SharePointConfiguration.fromConfiguration();
    assertEquals(true, configuration.isPerformBrowserLeniency());
    assertEquals(false, configuration.isPerformXmlValidation());
    SharePointUrl expectedUrl =
        new SharePointUrl.Builder("http://localhost/sites/collection")
            .setPerformBrowserLeniency(true)
            .build();
    assertEquals(expectedUrl, configuration.getSharePointUrl());
    assertEquals(true, configuration.isSiteCollectionUrl());
  }

  @Test
  public void testNegativeSocketTimeoutSecs() throws Exception {
    Properties baseConfiguration = getBaseConfiguration();
    baseConfiguration.put("sharepoint.webservices.socketTimeoutSecs", "-50");
    setupConfig.initConfig(baseConfiguration);
    thrown.expect(InvalidConfigurationException.class);
    thrown.expectMessage("Invalid SharePoint Configuration");
    SharePointConfiguration.fromConfiguration();
  }

  @Test
  public void testNegativeReadTimeoutSecs() throws Exception {
    Properties baseConfiguration = getBaseConfiguration();
    baseConfiguration.put("sharepoint.webservices.readTimeOutSecs", "-50");
    setupConfig.initConfig(baseConfiguration);
    thrown.expect(InvalidConfigurationException.class);
    thrown.expectMessage("Invalid SharePoint Configuration");
    SharePointConfiguration.fromConfiguration();
  }

  @Test
  public void testFromConfigurationWithNonDefaults() throws Exception {
    Properties baseConfiguration = getBaseConfiguration();
    baseConfiguration.put("sharepoint.userAgent", "agent");
    baseConfiguration.put("sharepoint.webservices.socketTimeoutSecs", "50");
    baseConfiguration.put("sharepoint.webservices.readTimeOutSecs", "120");
    baseConfiguration.put("api.referenceIdentitySources", "GDC-PSL");
    baseConfiguration.put("api.referenceIdentitySource.GDC-PSL.id", "idSourceGdcPsl");
    setupConfig.initConfig(baseConfiguration);
    SharePointConfiguration configuration = SharePointConfiguration.fromConfiguration();
    assertEquals(true, configuration.isPerformBrowserLeniency());
    assertEquals(false, configuration.isPerformXmlValidation());
    SharePointUrl expectedUrl =
        new SharePointUrl.Builder("http://localhost").setPerformBrowserLeniency(true).build();
    assertEquals(expectedUrl, configuration.getSharePointUrl());
    assertEquals("agent", configuration.getSharePointUserAgent());
    assertEquals(
        TimeUnit.MILLISECONDS.convert(50, TimeUnit.SECONDS),
        configuration.getWebservicesSocketTimeoutMills());
    assertEquals(
        TimeUnit.MILLISECONDS.convert(120, TimeUnit.SECONDS),
        configuration.getWebservicesReadTimeoutMills());
    assertEquals(
        ImmutableMap.of(
            "GDC-PSL", new IdentitySourceConfiguration.Builder("idSourceGdcPsl").build()),
        configuration.getReferenceIdentitySourceConfiguration());
  }

  private Properties getBaseConfiguration() {
    Properties properties = new Properties();
    properties.put("sharepoint.server", "http://localhost");
    properties.put("sharepoint.username", "username");
    properties.put("sharepoint.password", "password");
    return properties;
  }
}
