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

import com.google.common.annotations.VisibleForTesting;
import com.google.enterprise.cloudsearch.sharepoint.SamlAuthenticationHandler.HttpPostClient;
import com.google.enterprise.cloudsearch.sharepoint.SamlAuthenticationHandler.HttpPostClientImpl;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * SamlHandshakeManager implementation for Live Authentication to request Live authentication token
 * and extract authentication cookie.
 */
class LiveAuthenticationHandshakeManager extends AdfsHandshakeManager {
  private static final Logger log
      = Logger.getLogger(LiveAuthenticationHandshakeManager.class.getName());
  private static final String LIVE_STS
      = "https://login.microsoftonline.com/extSTS.srf";
  private static final String LIVE_LOGIN_URL
      = "/_forms/default.aspx?wa=wsignin1.0";

  public static class Builder {
    private final String username;
    private final String password;
    private final String sharePointUrl;
    private String stsendpoint;
    private HttpPostClient httpClient;
    private String login;
    private String trustLocation;

    public Builder(String sharePointUrl, String username, String password) {
      this.sharePointUrl = sharePointUrl;
      this.username = username;
      this.password = password;
      this.httpClient = new HttpPostClientImpl();
      this.login = sharePointUrl + LIVE_LOGIN_URL;
      this.trustLocation = "";
      this.stsendpoint = LIVE_STS;
    }

    public Builder setLoginUrl(String login) {
      this.login = login;
      return this;
    }

    public Builder setStsendpoint(String stsendpoint) {
      this.stsendpoint = stsendpoint;
      return this;
    }

    @VisibleForTesting
    Builder setHttpClient(HttpPostClient httpClient) {
      this.httpClient = httpClient;
      return this;
    }

    public LiveAuthenticationHandshakeManager build() {
      return new LiveAuthenticationHandshakeManager(this);
    }
  }

  private LiveAuthenticationHandshakeManager(Builder builder) {
    super(
        new AdfsHandshakeManager.Builder(
                builder.sharePointUrl,
                builder.username,
                builder.password,
                builder.stsendpoint,
                builder.sharePointUrl)
            .setHttpClient(builder.httpClient)
            .setLoginUrl(builder.login)
            .setTrustLocation(builder.trustLocation));
  }

  @Override
  @VisibleForTesting
  String extractToken(String tokenResponse) throws IOException {
    if (tokenResponse == null) {
      throw new IOException("tokenResponse is null");
    }
    try {
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      dbf.setNamespaceAware(true);
      DocumentBuilder db = dbf.newDocumentBuilder();
      Document document
          = db.parse(new InputSource(new StringReader(tokenResponse)));
      NodeList nodes
          = document.getElementsByTagNameNS(
              "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-"
                  + "wssecurity-secext-1.0.xsd", "BinarySecurityToken");
      if (nodes == null || nodes.getLength() == 0) {
        log.log(Level.WARNING, "Live Authentication token not available"
            + " in response {0}", tokenResponse);
        throw new IOException(
            "Live Authentication token not available in response");
      }
      String token = nodes.item(0).getTextContent();
      log.log(Level.FINER, "Live Authentication Token {0}", token);
      return token;
    } catch (ParserConfigurationException ex) {
      throw new IOException(ex);
    } catch (SAXException ex) {
      throw new IOException(ex);
    } catch (DOMException ex) {
      throw new IOException(ex);
    }
  }

  @Override
  public String getAuthenticationCookie(String token) throws IOException {
    URL u = new URL(login);
    Map<String, String> requestProperties = new HashMap<String, String>();
    requestProperties.put("SOAPAction", stsendpoint);
    SamlAuthenticationHandler.PostResponseInfo postResponse
        = httpClient.issuePostRequest(u, requestProperties, token);
    return postResponse.getPostResponseHeaderField("Set-Cookie");
  }
}
