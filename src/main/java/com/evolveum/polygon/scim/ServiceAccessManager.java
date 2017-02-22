/*
 * Copyright (c) 2016 Evolveum
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
package com.evolveum.polygon.scim;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.ConnectionFailedException;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.ConnectorIOException;
import org.identityconnectors.framework.common.exceptions.OperationTimeoutException;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.evolveum.polygon.common.GuardedStringAccessor;

/**
 * 
 * @author Macik
 *
 *         Holds the basic methods for initiation and termination of the
 *         communication with the service provider.
 *
 */
public class ServiceAccessManager {
	
	private String baseUri;
	private JSONObject loginJson;
	private Header aHeader;
	private static final Log LOGGER = Log.getLog(ServiceAccessManager.class);

	public ServiceAccessManager(ScimConnectorConfiguration configuration) {

		logIntoService(configuration);
	}
	
	/**
	 * Used for login to the service. The data needed for this operation is
	 * provided by the configuration.
	 * 
	 * @param configuration
	 *            The instance of "ScimConnectorConfiguration" which holds all
	 *            the provided configuration data.
	 * 
	 * @return a Map object carrying meta information about the login session.
	 */
	public void logIntoService(ScimConnectorConfiguration configuration) {

		HttpPost loginInstance = new HttpPost();

		Header authHeader = null;
		String loginAccessToken = null;
		String loginInstanceUrl = null;
		JSONObject jsonObject = null;
		String proxyUrl = configuration.getProxyUrl();
		LOGGER.ok("proxyUrl: {0}", proxyUrl);
	//	LOGGER.ok("Configuration: {0}", configuration);
		if (!"token".equalsIgnoreCase(configuration.getAuthentication())) {

			HttpClient httpClient;

			if (proxyUrl != null && !proxyUrl.isEmpty()) {

				HttpHost proxy = new HttpHost(proxyUrl, configuration.getProxyPortNumber());

				DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);

				httpClient = HttpClientBuilder.create().setRoutePlanner(routePlanner).build();

				LOGGER.ok("Proxy enabled: {0}:{1}", proxyUrl, configuration.getProxyPortNumber());
			} else {

				httpClient = HttpClientBuilder.create().build();

			}
			String loginURL = new StringBuilder(configuration.getLoginURL()).append(configuration.getService())
					.toString();

			GuardedString guardedPassword = configuration.getPassword();
			GuardedStringAccessor accessor = new GuardedStringAccessor();
			guardedPassword.access(accessor);

			String contentUri = new StringBuilder("&client_id=").append(configuration.getClientID())
					.append("&client_secret=").append(configuration.getClientSecret()).append("&username=")
					.append(configuration.getUserName()).append("&password=").append(accessor.getClearString())
					.toString();

			loginInstance = new HttpPost(loginURL);
			CloseableHttpResponse response = null;

			StringEntity bodyContent;
			String getResult = null;
			Integer statusCode = null;
			try {
				bodyContent = new StringEntity(contentUri);
				bodyContent.setContentType("application/x-www-form-urlencoded");
				loginInstance.setEntity(bodyContent);

				response = (CloseableHttpResponse) httpClient.execute(loginInstance);

				getResult = EntityUtils.toString(response.getEntity());

				statusCode = response.getStatusLine().getStatusCode();

				if (statusCode == 200) {

					LOGGER.info("Login Successful");
					
				}else{
					
					String[] loginUrlParts;
					String providerName="";
					
					if (configuration.getLoginURL() != null && !configuration.getLoginURL().isEmpty()) {

						loginUrlParts = configuration.getLoginURL().split("\\."); // e.g.
						// https://login.salesforce.com

					} else {

						loginUrlParts = configuration.getBaseUrl().split("\\."); // e.g.
					}
					// https://login.salesforce.com
					if (loginUrlParts.length >= 2) {
						providerName = loginUrlParts[1];
					}
					
					if (!providerName.isEmpty()){
						
						StrategyFetcher fetcher = new StrategyFetcher();
						HandlingStrategy strategy = fetcher.fetchStrategy(providerName);
						strategy.handleInvalidStatus(" while loging into service", getResult, "loging into service", statusCode);
					}
				
				}

				jsonObject = (JSONObject) new JSONTokener(getResult).nextValue();

				loginAccessToken = jsonObject.getString("access_token");
				loginInstanceUrl = jsonObject.getString("instance_url");

			} catch (UnsupportedEncodingException e) {
				LOGGER.error("Unsupported encoding: {0}. Occurrence in the process of login into the service",
						e.getLocalizedMessage());
				LOGGER.info("Unsupported encoding: {0}. Occurrence in the process of login into the service", e);

				throw new ConnectorException(
						"Unsupported encoding. Occurrence in the process of login into the service", e);
			}

			catch (ClientProtocolException e) {

				LOGGER.error(
						"An protocol exception has occurred while processing the http response to the login request. Possible mismatch in interpretation of the HTTP specification: {0}",
						e.getLocalizedMessage());
				LOGGER.info(
						"An protocol exception has occurred while processing the http response to the login request. Possible mismatch in interpretation of the HTTP specification: {0}",
						e);

				throw new ConnectionFailedException(
						"An protocol exception has occurred while processing the http response to the login request. Possible mismatch in interpretation of the HTTP specification",
						e);

			} catch (IOException ioException) {

				StringBuilder errorBuilder = new StringBuilder(
						"An error occurred while processing the query http response to the login request. ");

				if ((ioException instanceof SocketTimeoutException || ioException instanceof NoRouteToHostException)) {

					errorBuilder.insert(0, "The connection timed out. ");

					throw new OperationTimeoutException(errorBuilder.toString(), ioException);
				} else {

					LOGGER.error(
							"An error occurred while processing the query http response to the login request : {0}",
							ioException.getLocalizedMessage());
					LOGGER.info("An error occurred while processing the query http response to the login request : {0}",
							ioException);
					throw new ConnectorIOException(errorBuilder.toString(), ioException);
				}
			} catch (JSONException jsonException) {

				LOGGER.error(
						"An exception has occurred while setting the \"jsonObject\". Occurrence while processing the http response to the login request: {0}",
						jsonException.getLocalizedMessage());
				LOGGER.info(
						"An exception has occurred while setting the \"jsonObject\". Occurrence while processing the http response to the login request: {0}",
						jsonException);
				throw new ConnectorException("An exception has occurred while setting the \"jsonObject\".",
						jsonException);
			} finally {
				try {
					response.close();
				} catch (IOException e) {

					if ((e instanceof SocketTimeoutException || e instanceof NoRouteToHostException)) {

						throw new OperationTimeoutException(
								"The connection timed out while closing the http connection. Occurrence in the process of logging into the service",
								e);
					} else {

						LOGGER.error(
								"An error has occurred while processing the http response and closing the http connection. Occurrence in the process of logging into the service: {0}",
								e.getLocalizedMessage());

						throw new ConnectorIOException(
								"An error has occurred while processing the http response and closing the http connection. Occurrence in the process of logging into the service",
								e);
					}

				}

			}
			authHeader = new BasicHeader("Authorization", "OAuth " + loginAccessToken);
		} else {
			loginInstanceUrl = configuration.getBaseUrl();

			GuardedString guardedToken = configuration.getToken();

			GuardedStringAccessor accessor = new GuardedStringAccessor();
			guardedToken.access(accessor);

			loginAccessToken = accessor.getClearString();

			authHeader = new BasicHeader("Authorization", "Bearer " + loginAccessToken);
		}
		String scimBaseUri = new StringBuilder(loginInstanceUrl).append(configuration.getEndpoint())
				.append(configuration.getVersion()).toString();

		this.baseUri=scimBaseUri;
		this.aHeader = authHeader;
		if (jsonObject != null) {
			this.loginJson = jsonObject;
		}

	}
	
	public String getBaseUri(){
		return baseUri;
	}
	public Header getAuthHeader(){
		return aHeader;
	}
	public JSONObject getLoginJson(){
		return loginJson;
	}

}
