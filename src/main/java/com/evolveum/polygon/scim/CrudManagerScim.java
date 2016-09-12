package com.evolveum.polygon.scim;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Uid;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * Holds the CRUD+L methods and other methods needed for interaction with the
 * service provider.
 */
public class CrudManagerScim {

	private static final Log LOGGER = Log.getLog(CrudManagerScim.class);

	/**
	 * Used for loging to the service. The data needed for this operation is
	 * provided by the configuration.
	 * 
	 * @return a json object carrying meta information about the login session.
	 * @throws ConnectorException
	 * @throws ConnectionFailedException
	 * @throws ConnectorIOException
	 */
	public static Map<String, Object> logIntoService(ScimConnectorConfiguration conf) {

		HttpPost loginInstance = new HttpPost();

		Header authHeader = null;
		String loginAccessToken = null;
		String loginInstanceUrl = null;
		JSONObject jsonObject = null;
		String proxyUrl = conf.getProxyUrl();

		if (!"token".equals(conf.getAuthentication())) {

			HttpClient httpclient;

			if (proxyUrl != null && !proxyUrl.isEmpty()) {

				HttpHost proxy = new HttpHost(proxyUrl, conf.getProxyPortNumber());

				DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);

				httpclient = HttpClientBuilder.create().setRoutePlanner(routePlanner).build();

			} else {

				httpclient = HttpClientBuilder.create().build();

			}
			String loginURL = new StringBuilder(conf.getLoginURL()).append(conf.getService()).toString();
			String contentUri = new StringBuilder("&client_id=").append(conf.getClientID()).append("&client_secret=")
					.append(conf.getClientSecret()).append("&username=").append(conf.getUserName()).append("&password=")
					.append(conf.getPassword()).toString();

			loginInstance = new HttpPost(loginURL);
			HttpResponse response = null;

			StringEntity bodyContent;
			try {
				bodyContent = new StringEntity(contentUri);
				bodyContent.setContentType("application/x-www-form-urlencoded");
				loginInstance.setEntity(bodyContent);

			} catch (UnsupportedEncodingException e1) {
				LOGGER.error("Unsupported encoding: {0}. Occurrence in the process of login into the service",
						e1.getLocalizedMessage());
				LOGGER.info("Unsupported encoding: {0}. Occurrence in the process of login into the service", e1);

				throw new ConnectorException(
						"Unsupported encoding. Occurrence in the process of login into the service", e1);
			}

			try {
				long providerStartTime = System.currentTimeMillis();
				response = httpclient.execute(loginInstance);
				long providerEndTime = System.currentTimeMillis();
				long providerDuration = (providerEndTime - providerStartTime);
				LOGGER.info(
						"The amouth of time it took to get the response to the login query from the provider : {0} milliseconds",
						providerDuration);
				providerDuration = 0;

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

				LOGGER.error("An error occurred while processing the queuery http response to the login request : {0}",
						ioException.getLocalizedMessage());
				LOGGER.info("An error occurred while processing the queuery http response to the login request : {0}",
						ioException);
				throw new ConnectorIOException(
						"An error occurred while processing the queuery http response to the login request",
						ioException);
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
			}else{
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
			loginInstanceUrl = conf.getBaseUrl();
			loginAccessToken = conf.getToken();

			authHeader = new BasicHeader("Authorization", "Bearer " + loginAccessToken);
		}
		String scimBaseUri = new StringBuilder(loginInstanceUrl).append(conf.getEndpoint()).append(conf.getVersion())
				.toString();

		

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
	 * Sends queries to the service provider endpoints to retrieve the queried
	 * information and processes responses which are handed over to the provided
	 * result handler.
	 * 
	 * @param query
	 *            The query object which can be a string or an Uid type object.
	 * @param resourceEndPoint
	 *            The resource endpoint name.
	 * @param resultHandler
	 *            The provided result handler which handles results.
	 *
	 * @throws ConnectorException
	 * @throws ConnectorIOException
	 */
	public void qeuery(Object query, String resourceEndPoint, ResultsHandler resultHandler,
			ScimConnectorConfiguration conf) {}

	/**
	 * Sends queries to the service provider endpoints to retrieve the queried
	 * information and processes responses which are handed over to the provided
	 * result handler. This method send queries only to the endpoints containing
	 * the schema information of the services resources.
	 * 
	 * @param providerName
	 *            The name of the provider.
	 * @param resourceEndPoint
	 *            The resource endpoint name.
	 *
	 * @throws ConnectorException
	 * @throws ConnectorIOException
	 *
	 * @return an instance of "ScimSchemaParser" containing the schema
	 *         information of all endpoint.
	 */
	public ParserSchemaScim qeuerySchemas(String providerName, String resourceEndPoint,
			ScimConnectorConfiguration conf) {return null;}

	/**
	 * Sends queries for object creation to the service providers endpoints.
	 * After successful object creation the service provider returns the uid of
	 * the created object.
	 * 
	 * @param objectTranslator
	 *            An instance of object translator containing methods for the
	 *            creation of an json object out of an provided set of
	 *            attributes.
	 * @param resourceEndPoint
	 *            The resource endpoint name.
	 * @param attributes
	 *            The provided attributes set containing information for object
	 *            creation.
	 * @throws ConnectorException
	 * @throws ConnectorIOException
	 * @throws ConnectionFailedException
	 * @throws UnknownUidException
	 *
	 * @return the uid of the created object.
	 */

	public Uid create(String resourceEndPoint, ObjectTranslator objectTranslator, Set<Attribute> attributes,
			Set<Attribute> injectedAttributeSet, ScimConnectorConfiguration conf) {return null;}

	/**
	 * Sends queries for object update to the service providers endpoints. After
	 * successful object update the service provider returns the uid of the
	 * updated object.
	 * 
	 * @param uid
	 *            The uid of the resource object which should be updated.
	 * @param resourceEndPoint
	 *            The name of the resource endpoint.
	 * @param jsonObject
	 *            The json object which carries the information which should be
	 *            updated.
	 * 
	 * @throws ConnectorException
	 * @throws UnknownUidException
	 * @throws ConnectorIOException
	 * @throws ConnectionFailedException
	 *
	 * @return the uid of the created object.
	 */
	public Uid update(Uid uid, String resourceEndPoint, JSONObject jsonObject, ScimConnectorConfiguration conf) {return null;}

	/**
	 * Sends queries for object deletion to the service providers endpoints.
	 * 
	 * @param uid
	 *            The uid of the resource object which should be deleted.
	 * @param resourceEndPoint
	 *            The name of the resource endpoint.
	 * 
	 * @throws ConnectorIOException
	 * @throws ConnectionFailedException
	 *
	 */
	public void delete(Uid uid, String resourceEndPoint, ScimConnectorConfiguration conf) {}

	/**
	 * Error handling method called in case of an exception situation. The
	 * method returns log messages and throws exceptions.
	 * 
	 * @param response
	 *            The http response from the service provider.
	 * @param statusCode
	 *            The status code returned from the service provider.
	 * @param message
	 *            The generated message tailored for the thrown exception.
	 * 
	 * @throws ParseException
	 * @throws IOException
	 * @throws ConnectorIOException
	 */
	public static void onNoSuccess(HttpResponse response, String message) throws ParseException, IOException {

		Integer statusCode = null;
		StringBuilder exceptionStringBuilder = null;

		if (response.getEntity() != null) {
			String responseString = EntityUtils.toString(response.getEntity());
			statusCode = response.getStatusLine().getStatusCode();
			LOGGER.error("Full Error response from provider: {0}", responseString);

			JSONObject responseObject = new JSONObject(responseString);

			if (responseObject.has("Errors")) {
				Object returnedObject = new Object();

				returnedObject = responseObject.get("Errors");

				if (returnedObject instanceof JSONObject) {

					responseObject = (JSONObject) returnedObject;

					exceptionStringBuilder = buildErrorMessage(responseObject, message, statusCode);
				} else if (returnedObject instanceof JSONArray) {

					for (Object messageObject : (JSONArray) returnedObject) {
						exceptionStringBuilder = buildErrorMessage((JSONObject) messageObject, message, statusCode);

					}

				}

			} else {
				exceptionStringBuilder = new StringBuilder("Query for ").append(message)
						.append(" was unsuccessful. Status code returned: ").append(statusCode);
			}

		} else {
			exceptionStringBuilder = new StringBuilder("Query for ").append(message)
					.append(" was unsuccessful. No response object was returned");
		}

		String exceptionString = exceptionStringBuilder.toString();

		if (message == null) {
			message = "the full resource representation";
		}
		LOGGER.error(exceptionString);
		if (statusCode != null) {
			LOGGER.info("An error has occured. Http status: \"{0}\"", statusCode);
		}
		LOGGER.info(exceptionString);

		throw new ConnectorIOException(exceptionString);
	}

	public static StringBuilder buildErrorMessage(JSONObject responseObject, String message, int statusCode) {

		String responseString = new String();

		StringBuilder exceptionStringBuilder = new StringBuilder();

		if (responseObject.has("description")) {
			responseString = responseObject.getString("description");
			exceptionStringBuilder = new StringBuilder("Query for ").append(message)
					.append(" was unsuccessful. Status code returned: ").append("\"").append(statusCode).append("\"")
					.append(". Error response from provider: ").append("\"").append(responseString).append("\"");

		} else {
			responseString = ". No description was provided from the provider";
			exceptionStringBuilder = new StringBuilder("Query for ").append(message)
					.append(" was unsuccessful. Status code returned: ").append("\"").append(statusCode).append("\"")
					.append(responseString);
		}

		return exceptionStringBuilder;
	}

	/**
	 * Method used to log out of the service.
	 */

	public ParserSchemaScim processSchemaResponse(JSONObject responseObject, String providerName) {return null;}

	public void queryMembershipData(Uid uid, String resourceEndPoint, ResultsHandler resultHandler,
			String membershipResourceEndpoin, ScimConnectorConfiguration conf) {}

	public static void logOut(HttpPost loginInstance) {
		loginInstance.releaseConnection();
		LOGGER.info("The connection was released");
	}
}
