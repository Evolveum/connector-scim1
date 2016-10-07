package com.evolveum.polygon.scim;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.ConnectionFailedException;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.ConnectorIOException;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * 
 * @author Matus
 *
 *         Holds the basic methods for initiation and termination of the
 *         communication with the service provider.
 *
 */
public class ServiceAccessManager {

	private static final Log LOGGER = Log.getLog(ServiceAccessManager.class);

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
	public static Map<String, Object> logIntoService(ScimConnectorConfiguration configuration) {

		HttpPost loginInstance = new HttpPost();

		Header authHeader = null;
		String loginAccessToken = null;
		String loginInstanceUrl = null;
		JSONObject jsonObject = null;
		String proxyUrl = configuration.getProxyUrl();

		if (!"token".equals(configuration.getAuthentication())) {

			HttpClient httpClient;

			if (proxyUrl != null && !proxyUrl.isEmpty()) {

				HttpHost proxy = new HttpHost(proxyUrl, configuration.getProxyPortNumber());

				DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);

				httpClient = HttpClientBuilder.create().setRoutePlanner(routePlanner).build();

			} else {

				httpClient = HttpClientBuilder.create().build();

			}
			String loginURL = new StringBuilder(configuration.getLoginURL()).append(configuration.getService())
					.toString();
			String contentUri = new StringBuilder("&client_id=").append(configuration.getClientID())
					.append("&client_secret=").append(configuration.getClientSecret()).append("&username=")
					.append(configuration.getUserName()).append("&password=").append(configuration.getPassword())
					.toString();

			loginInstance = new HttpPost(loginURL);
			HttpResponse response = null;

			StringEntity bodyContent;
			try {
				bodyContent = new StringEntity(contentUri);
				bodyContent.setContentType("application/x-www-form-urlencoded");
				loginInstance.setEntity(bodyContent);

			} catch (UnsupportedEncodingException e) {
				LOGGER.error("Unsupported encoding: {0}. Occurrence in the process of login into the service",
						e.getLocalizedMessage());
				LOGGER.info("Unsupported encoding: {0}. Occurrence in the process of login into the service", e);

				throw new ConnectorException(
						"Unsupported encoding. Occurrence in the process of login into the service", e);
			}

			try {

				response = httpClient.execute(loginInstance);

			} catch (ClientProtocolException e) {

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

				LOGGER.error("An error occurred while processing the query http response to the login request : {0}",
						ioException.getLocalizedMessage());
				LOGGER.info("An error occurred while processing the query http response to the login request : {0}",
						ioException);
				throw new ConnectorIOException(
						"An error occurred while processing the query http response to the login request", ioException);
			}

			final int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != HttpStatus.SC_OK) {
				LOGGER.error("Error with authenticating : {0}", statusCode);
				try {
					LOGGER.error("Error cause: {0}", EntityUtils.toString(response.getEntity()));
				} catch (ParseException | IOException e) {

					LOGGER.error("An exception has occurred while parsing the http response to the login request: {0}",
							e.getLocalizedMessage());
					LOGGER.info("An exception has occurred while parsing the http response to the login request: {0}",
							e);
					throw new ConnectorIOException(
							"An exception has occurred while parsing the http response to the login request.", e);
				}
			} else {
				LOGGER.info("Login Successful");
			}

			String getResult = null;
			try {
				getResult = EntityUtils.toString(response.getEntity());
			} catch (IOException ioException) {

				LOGGER.error("An exception has occurred while parsing the http response to the login request: {0}",
						ioException.getLocalizedMessage());
				LOGGER.info("An exception has occurred while parsing the http response to the login request: {0}",
						ioException);
				throw new ConnectorIOException(
						"An exception has occurred while parsing the http response to the login request", ioException);
			}

			try {

				jsonObject = (JSONObject) new JSONTokener(getResult).nextValue();

				loginAccessToken = jsonObject.getString("access_token");
				loginInstanceUrl = jsonObject.getString("instance_url");
			} catch (JSONException jsonException) {

				LOGGER.error(
						"An exception has occurred while setting the \"jsonObject\". Occurrence while processing the http response to the login request: {0}",
						jsonException.getLocalizedMessage());
				LOGGER.info(
						"An exception has occurred while setting the \"jsonObject\". Occurrence while processing the http response to the login request: {0}",
						jsonException);
				throw new ConnectorException("An exception has occurred while setting the \"jsonObject\".",
						jsonException);
			}
			authHeader = new BasicHeader("Authorization", "OAuth " + loginAccessToken);
		} else {
			loginInstanceUrl = configuration.getBaseUrl();
			loginAccessToken = configuration.getToken();

			authHeader = new BasicHeader("Authorization", "Bearer " + loginAccessToken);
		}
		String scimBaseUri = new StringBuilder(loginInstanceUrl).append(configuration.getEndpoint())
				.append(configuration.getVersion()).toString();

		Map<String, Object> autoriazationData = new HashMap<String, Object>();
		autoriazationData.put("uri", scimBaseUri);
		autoriazationData.put("authHeader", authHeader);
		autoriazationData.put("loginInstance", loginInstance);

		if (jsonObject != null) {
			autoriazationData.put("json", jsonObject);
		}

		return autoriazationData;
	}

	/**
	 * Method used to log out of the service.
	 * 
	 * @param loginInstance
	 *            Data representing the login instance of a communication
	 *            session.
	 * 
	 */
	public static void logOut(HttpPost loginInstance) {
		loginInstance.releaseConnection();
		LOGGER.info("The connection was released");
	}

}
