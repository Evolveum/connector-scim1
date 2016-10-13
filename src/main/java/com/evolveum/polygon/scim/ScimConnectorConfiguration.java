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

import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.spi.AbstractConfiguration;
import org.identityconnectors.framework.spi.ConfigurationProperty;
import org.identityconnectors.framework.spi.StatefulConfiguration;

/**
 * @author Macik
 * 
 *         Connector configuration class. It contains all the needed methods for
 *         processing the connector configuration.
 */
public class ScimConnectorConfiguration extends AbstractConfiguration implements StatefulConfiguration {

	private String authentication;
	private String scim_endpoint;
	private String scim_version;
	private String username;
	private String password;
	private String loginUrl;
	private String baseUrl;
	private String grant;
	private String clientId;
	private String token;
	private String clientSecret;

	private String proxy;
	private Integer proxy_port_number;

	private static final Log LOGGER = Log.getLog(ScimConnectorConfiguration.class);

	/**
	 * Getter method for the "authentication" attribute.
	 * 
	 * @return the authentication string.
	 */

	@ConfigurationProperty(order = 1, displayMessageKey = "authentication.display", helpMessageKey = "authentication.help", required = true, confidential = false)
	public String getAuthentication() {
		return authentication;
	}

	/**
	 * Setter method for the "authentication" attribute.
	 * 
	 * @param authentication
	 *            the authentication string value.
	 */
	public void setAuthentication(String authentication) {
		this.authentication = authentication;
	}

	/**
	 * Getter method for the "token" attribute.
	 * 
	 * @return the token string value.
	 */

	@ConfigurationProperty(order = 2, displayMessageKey = "token.display", helpMessageKey = "token.help", required = false, confidential = true)
	public String getToken() {
		return token;
	}

	/**
	 * Setter method for the "token" attribute.
	 * 
	 * @param token
	 *            the token string value.
	 */
	public void setToken(String token) {
		this.token = token;
	}

	/**
	 * Getter method for the "username" attribute.
	 * 
	 * @return the username string value.
	 */

	@ConfigurationProperty(order = 3, displayMessageKey = "username.display", helpMessageKey = "username.help", required = false, confidential = false)

	public String getUserName() {
		return username;
	}

	/**
	 * Setter method for the "username" attribute.
	 * 
	 * @param username
	 *            the user name string value.
	 */
	public void setUserName(String username) {
		this.username = username;
	}

	/**
	 * Getter method for the "password" attribute.
	 * 
	 * @return the password.
	 */
	@ConfigurationProperty(order = 4, displayMessageKey = "password.display", helpMessageKey = "password.help", required = false, confidential = true)

	public String getPassword() {
		return password;

	}

	/**
	 * Setter method for the "password" attribute.
	 * 
	 * @param passwd
	 *            the password string value.
	 */
	public void setPassword(String passwd) {
		this.password = passwd;
	}

	/**
	 * Getter method for the "clientSecret" attribute.
	 * 
	 * @return the client secret.
	 */
	@ConfigurationProperty(order = 5, displayMessageKey = "clientSecret.display", helpMessageKey = "clientSecret.help", required = false, confidential = true)

	public String getClientSecret() {
		return clientSecret;
	}

	/**
	 * Setter method for the "clientSecret" attribute.
	 * 
	 * @param clientSecret
	 *            the client secret string value.
	 */
	public void setClientSecret(String clientSecret) {
		this.clientSecret = clientSecret;
	}

	/**
	 * Getter method for the "clientId" attribute.
	 * 
	 * @return the client id.
	 */
	@ConfigurationProperty(order = 6, displayMessageKey = "clientId.display", helpMessageKey = "clientId.help", required = false, confidential = false)
	public String getClientID() {
		return clientId;
	}

	/**
	 * Setter method for the "clientId" attribute.
	 * 
	 * @param clientID
	 *            the client id string value.
	 */
	public void setClientID(String clientID) {
		this.clientId = clientID;
	}

	/**
	 * Getter method for the "scim_endpoint" attribute.
	 * 
	 * @return the scim endpoint.
	 */
	@ConfigurationProperty(order = 7, displayMessageKey = "scim_endpoint.display", helpMessageKey = "scim_endpoint.help", required = true, confidential = false)

	public String getEndpoint() {
		return scim_endpoint;
	}

	/**
	 * Setter method for the "scim_endpoint" attribute.
	 * 
	 * @param endpoint
	 *            the scim endpoint string value.
	 */
	public void setEndpoint(String endpoint) {
		this.scim_endpoint = endpoint;
	}

	/**
	 * Getter method for the "scim_version" attribute.
	 * 
	 * @return the scim version.
	 */
	@ConfigurationProperty(order = 8, displayMessageKey = "scim_version.display", helpMessageKey = "scim_version.help", required = true, confidential = false)

	public String getVersion() {
		return scim_version;
	}

	/**
	 * Setter method for the "scim_version" attribute.
	 * 
	 * @param version
	 *            the scim version string value.
	 */
	public void setVersion(String version) {
		this.scim_version = version;
	}

	/**
	 * Getter method for the "loginUrl" attribute.
	 * 
	 * @return the login url.
	 */
	@ConfigurationProperty(order = 9, displayMessageKey = "loginUrl.display", helpMessageKey = "loginUrl.help", required = false, confidential = false)

	public String getLoginURL() {
		return loginUrl;
	}

	/**
	 * Setter method for the "loginUrl" attribute.
	 * 
	 * @param loginURL
	 *            the login url string value.
	 */
	public void setLoginURL(String loginURL) {
		this.loginUrl = loginURL;
	}

	/**
	 * Getter method for the "grant" attribute.
	 * 
	 * @return the service grant.
	 */
	@ConfigurationProperty(order = 10, displayMessageKey = "grant.display", helpMessageKey = "grant.help", required = false, confidential = false)

	public String getService() {
		return grant;
	}

	/**
	 * Setter method for the "grant" attribute.
	 * 
	 * @param service
	 *            the grant type string value.
	 */
	public void setService(String service) {
		this.grant = service;
	}

	/**
	 * Getter method for the "baseUrl" attribute.
	 * 
	 * @return the baseUrl string value.
	 */

	@ConfigurationProperty(order = 11, displayMessageKey = "baseUrl.display", helpMessageKey = "baseUrl.help", required = false, confidential = false)
	public String getBaseUrl() {
		return baseUrl;
	}

	/**
	 * Setter method for the "baseUrl" attribute.
	 * 
	 * @param baseUrl
	 *            the base url string value.
	 */
	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	/**
	 * Getter method for the "proxy" attribute.
	 * 
	 * @return the proxy url string value.
	 */

	@ConfigurationProperty(order = 12, displayMessageKey = "proxy.display", helpMessageKey = "proxy.help", required = false, confidential = false)
	public String getProxyUrl() {
		return proxy;
	}

	/**
	 * Setter method for the "proxy" attribute.
	 * 
	 * @param proxy
	 *            the proxy url string value.
	 */
	public void setProxyUrl(String proxy) {
		this.proxy = proxy;
	}

	/**
	 * Getter method for the "proxy_port_number" attribute.
	 * 
	 * @return the proxy_port_number integer value.
	 */

	@ConfigurationProperty(order = 13, displayMessageKey = "proxy_port_number.display", helpMessageKey = "proxy_port_number.help", required = false, confidential = false)
	public Integer getProxyPortNumber() {
		return proxy_port_number;
	}

	/**
	 * Setter method for the "proxy_port_number" attribute.
	 * 
	 * @param proxyPortNumber
	 *            the proxy number integer value.
	 */
	public void setProxyPortNumber(Integer proxyPortNumber) {
		this.proxy_port_number = proxyPortNumber;
	}

	@Override
	public void validate() {

		LOGGER.info("Processing trough configuration validation procedure.");

		if (StringUtil.isBlank(authentication)) {
			throw new IllegalArgumentException("Authentication cannot be empty.");
		}

		if (!"token".equalsIgnoreCase(authentication)) {

			if (StringUtil.isBlank(username)) {
				throw new IllegalArgumentException("Username cannot be empty.");
			}

			if (StringUtil.isBlank(password)) {
				throw new IllegalArgumentException("Password cannot be empty");
			}

			if (StringUtil.isBlank(clientSecret)) {
				throw new IllegalArgumentException("Client Secret cannot be empty.");
			}

			if (StringUtil.isBlank(loginUrl)) {
				throw new IllegalArgumentException("Login url cannot be empty.");
			}
			if (StringUtil.isBlank(grant)) {
				throw new IllegalArgumentException("Grant type cannot be empty.");
			}
			if (StringUtil.isBlank(clientId)) {
				throw new IllegalArgumentException("Client id cannot be empty.");
			}

		} else {

			if (StringUtil.isBlank(token)) {
				throw new IllegalArgumentException("Token cannot be empty.");
			}
			if (StringUtil.isBlank(baseUrl)) {
				throw new IllegalArgumentException("Base URL cannot be empty.");
			}

		}

		if (StringUtil.isBlank(scim_endpoint)) {
			throw new IllegalArgumentException("Scim endpoint cannot be empty.");
		}
		if (StringUtil.isBlank(scim_version)) {
			throw new IllegalArgumentException("Scim version cannot be empty.");
		}
		LOGGER.info("Configuration valid");
	}

	@Override
	public void release() {
		LOGGER.info("The release of configuration resources is being performed");

		this.loginUrl = null;
		this.scim_version = null;
		this.scim_endpoint = null;
		this.clientId = null;
		this.clientSecret = null;
		this.password = null;
		this.username = null;
		this.grant = null;
		this.authentication = null;
		this.proxy = null;
		this.proxy_port_number = null;
		this.token = null;
		this.baseUrl = null;
	}

}
