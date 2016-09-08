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
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
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
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.SearchResult;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.spi.SearchResultsHandler;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.evolveum.polygon.scim.common.HttpPatch;

/**
 * Holds the CRUD+L methods and other methods needed for interaction with the
 * service provider.
 */
public class CrudManagerScim {

	private Header prettyPrintHeader = new BasicHeader("X-PrettyPrint", "1");

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

		LOGGER.info("Login Successful");

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
			ScimConnectorConfiguration conf) {
		Header authHeader = null;
		String scimBaseUri = "";
		Map<String, Object> autoriazationData = logIntoService(conf);
		HttpPost loginInstance = null;

		for (String data : autoriazationData.keySet()) {
			if ("authHeader".equals(data)) {

				authHeader = (Header) autoriazationData.get(data);

			} else if ("uri".equals(data)) {

				scimBaseUri = (String) autoriazationData.get(data);
			} else if ("loginInstance".equals(data)) {

				loginInstance = (HttpPost) autoriazationData.get(data);
			}
		}

		if (authHeader == null || scimBaseUri.isEmpty()) {

			throw new ConnectorException("The data needed for authorization of request to the provider was not found.");
		}

		HttpClient httpClient = HttpClientBuilder.create().build();
		String q;
		if (query instanceof Uid) {

			q = ((Uid) query).getUidValue();
		} else {

			q = (String) query;
		}

		String uri = new StringBuilder(scimBaseUri).append("/").append(resourceEndPoint).append("/").append(q)
				.toString();
		LOGGER.info("Qeury url: {0}", uri);
		HttpGet httpGet = new HttpGet(uri);
		httpGet.addHeader(authHeader);
		httpGet.addHeader(prettyPrintHeader);

		String responseString = null;
		HttpResponse response;
		try {
			long providerStartTime = System.currentTimeMillis();
			response = httpClient.execute(httpGet);
			long providerEndTime = System.currentTimeMillis();
			long providerDuration = (providerEndTime - providerStartTime);

			LOGGER.info(
					"The amouth of time it took to get the response to the query from the provider : {0} milliseconds ",
					providerDuration);

			providerDuration = 0;

			int statusCode = response.getStatusLine().getStatusCode();
			LOGGER.info("Status code: {0}", statusCode);
			if (statusCode == 200) {

				responseString = EntityUtils.toString(response.getEntity());
				if (!responseString.isEmpty()) {
					try {
						JSONObject jsonObject = new JSONObject(responseString);

						LOGGER.info("Json object returned from service provider: {0}", jsonObject.toString(1));
						try {
							if (query instanceof Uid) {

								BuilderConnectorObject builder = new BuilderConnectorObject();

								ConnectorObject connectorObject = builder.buildConnectorObject(jsonObject,
										resourceEndPoint, scimBaseUri);
								resultHandler.handle(connectorObject);

							} else {
								if (jsonObject.has("Resources")) {
									int amountOfResources = jsonObject.getJSONArray("Resources").length();
									int totalResults = 0;
									int startIndex = 0;
									int itemsPerPage = 0;

									if (jsonObject.has("startIndex") && jsonObject.has("totalResults")
											&& jsonObject.has("itemsPerPage")) {
										totalResults = (int) jsonObject.get("totalResults");
										startIndex = (int) jsonObject.get("startIndex");
										itemsPerPage = (int) jsonObject.get("itemsPerPage");
									}

									for (int i = 0; i < amountOfResources; i++) {
										JSONObject minResourceJson = new JSONObject();
										minResourceJson = jsonObject.getJSONArray("Resources").getJSONObject(i);
										if (minResourceJson.has("id") && minResourceJson.getString("id") != null) {

											if (minResourceJson.has("meta")) {

												String resourceUri = minResourceJson.getJSONObject("meta")
														.getString("location").toString();
												HttpGet httpGetR = new HttpGet(resourceUri);
												httpGetR.addHeader(authHeader);
												httpGetR.addHeader(prettyPrintHeader);

												providerStartTime = System.currentTimeMillis();
												HttpResponse resourceResponse = httpClient.execute(httpGetR);
												providerEndTime = System.currentTimeMillis();
												providerDuration = (providerEndTime - providerStartTime);

												LOGGER.info(
														"The amouth of time it took to get the response to the query from the provider : {0} milliseconds ",
														providerDuration);

												statusCode = resourceResponse.getStatusLine().getStatusCode();

												if (statusCode == 200) {
													responseString = EntityUtils.toString(resourceResponse.getEntity());
													JSONObject fullResourcejson = new JSONObject(responseString);

													LOGGER.info(
															"The {0}. resource json object which was returned by the service provider: {1}",
															i + 1, fullResourcejson);

													BuilderConnectorObject builder = new BuilderConnectorObject();
													ConnectorObject connectorObject = builder.buildConnectorObject(
															fullResourcejson, resourceEndPoint, scimBaseUri);

													resultHandler.handle(connectorObject);

												} else {

													onNoSuccess(resourceResponse, resourceUri);
												}

											}
										} else {
											LOGGER.error("No uid present in fetched object: {0}", minResourceJson);

											throw new ConnectorException(
													"No uid present in fetchet object while processing queuery result");

										}
									}
									if (resultHandler instanceof SearchResultsHandler) {
										Boolean allResultsReturned = false;
										int remainingResult = totalResults - (startIndex - 1) - itemsPerPage;

										if (remainingResult <= 0) {
											remainingResult = 0;
											allResultsReturned = true;

										}

										LOGGER.info("The number of remaining results: {0}", remainingResult);
										SearchResult searchResult = new SearchResult("default", remainingResult,
												allResultsReturned);
										((SearchResultsHandler) resultHandler).handleResult(searchResult);
									}

								} else {

									LOGGER.error("Resource object not present in provider response to the query");

									throw new ConnectorException(
											"No uid present in fetchet object while processing queuery result");

								}
							}

						} catch (Exception e) {
							LOGGER.error(
									"Builder error. Error while building connId object. The exception message: {0}",
									e.getLocalizedMessage());
							LOGGER.info("Builder error. Error while building connId object. The excetion message: {0}",
									e);
							throw new ConnectorException(e);
						}

					} catch (JSONException jsonException) {
						if (q == null) {
							q = "the full resource representation";
						}
						LOGGER.error(
								"An exception has occurred while setting the variable \"jsonObject\". Occurrence while processing the http response to the queuey request for: {1}, exception message: {0}",
								jsonException.getLocalizedMessage(), q);
						LOGGER.info(
								"An exception has occurred while setting the variable \"jsonObject\". Occurrence while processing the http response to the queuey request for: {1}, exception message: {0}",
								jsonException, q);
						throw new ConnectorException(
								"An exception has occurred while setting the variable \"jsonObject\".", jsonException);
					}

				} else {

					LOGGER.warn("Service provider response is empty, responce returned on queuery: {0}", query);
				}
			} else {
				onNoSuccess(response, uri);
			}

		} catch (IOException e) {

			if (q == null) {
				q = "the full resource representation";
			}

			LOGGER.error(
					"An error occurred while processing the queuery http response. Occurrence while processing the http response to the queuey request for: {1}, exception message: {0}",
					e.getLocalizedMessage(), q);
			LOGGER.info(
					"An error occurred while processing the queuery http response. Occurrence while processing the http response to the queuey request for: {1}, exception message: {0}",
					e, q);
			throw new ConnectorIOException("An error occurred while processing the queuery http response.", e);
		} finally {
			logOut(loginInstance);
		}
	}

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
			ScimConnectorConfiguration conf) {
		logIntoService(conf);

		Header authHeader = null;
		String scimBaseUri = "";
		Map<String, Object> autoriazationData = logIntoService(conf);

		HttpPost loginInstance = null;

		for (String data : autoriazationData.keySet()) {
			if ("authHeader".equals(data)) {

				authHeader = (Header) autoriazationData.get(data);

			} else if ("uri".equals(data)) {

				scimBaseUri = (String) autoriazationData.get(data);
			} else if ("loginInstance".equals(data)) {

				loginInstance = (HttpPost) autoriazationData.get(data);
			}
		}

		if (authHeader == null || scimBaseUri.isEmpty()) {

			throw new ConnectorException("The data needed for authorization of request to the provider was not found.");
		}

		HttpClient httpClient = HttpClientBuilder.create().build();
		String uri = new StringBuilder(scimBaseUri).append("/").append(resourceEndPoint).toString();
		LOGGER.info("Qeury url: {0}", uri);
		HttpGet httpGet = new HttpGet(uri);
		httpGet.addHeader(authHeader);
		httpGet.addHeader(prettyPrintHeader);

		HttpResponse response;
		try {

			long providerStartTime = System.currentTimeMillis();
			response = httpClient.execute(httpGet);
			long providerEndTime = System.currentTimeMillis();

			long providerDuration = (providerEndTime - providerStartTime);

			LOGGER.info(
					"The amouth of time it took to get the response to the query from the provider : {0} milliseconds ",
					providerDuration);
			providerDuration = 0;

			int statusCode = response.getStatusLine().getStatusCode();
			LOGGER.info("Schema query status code: {0} ", statusCode);
			if (statusCode == 200) {

				String responseString = EntityUtils.toString(response.getEntity());
				if (!responseString.isEmpty()) {

					LOGGER.warn("The returned response string for the \"schemas/\" endpoint");

					JSONObject jsonObject = new JSONObject(responseString);

					long startTime = System.currentTimeMillis();
					ParserSchemaScim schemaParser = processSchemaResponse(jsonObject, providerName);
					long endTime = System.currentTimeMillis();

					long time = (endTime - startTime);

					LOGGER.error("The process filter method Time: {0} milliseconds", time);
					return schemaParser;

				} else {

					LOGGER.warn("Response string for the \"schemas/\" endpoint returned empty ");

					String resources[] = { "Users", "Groups" };
					JSONObject responseObject = new JSONObject();
					JSONArray responseArray = new JSONArray();
					for (String resourceName : resources) {
						uri = new StringBuilder(scimBaseUri).append("/").append(resourceEndPoint).append(resourceName)
								.toString();
						LOGGER.info("Additional query url: {0}", uri);

						httpGet = new HttpGet(uri);
						httpGet.addHeader(authHeader);
						httpGet.addHeader(prettyPrintHeader);

						providerStartTime = System.currentTimeMillis();
						response = httpClient.execute(httpGet);
						providerEndTime = System.currentTimeMillis();

						providerDuration = (providerEndTime - providerStartTime);
						statusCode = response.getStatusLine().getStatusCode();

						if (statusCode == 200) {

							responseString = EntityUtils.toString(response.getEntity());
							JSONObject jsonObject = new JSONObject(responseString);

							StrategyFetcher fetcher = new StrategyFetcher();
							HandlingStrategy strategy = fetcher.fetchStrategy(providerName);

							strategy.injectMissingSchemaAttributes(resourceName, jsonObject);

							responseArray.put(jsonObject);
						} else {

							LOGGER.warn(
									"No definition for provided shcemas was found, the connector will switch to default core schema configuration!");
							return null;
						}
					}
					responseObject.put("Resources", responseArray);
					return processSchemaResponse(responseObject, providerName);

				}

			} else {
				LOGGER.warn("Query for {1} was unsuccessful. Status code returned is {0}", statusCode,
						resourceEndPoint);
				LOGGER.warn(
						"No definition for provided schemas was found, the connector will switch to default core schema configuration!");
				return null;
			}
		} catch (ClientProtocolException e) {
			LOGGER.error(
					"An protocol exception has occurred while in the process of querying the provider Schemas resource object. Possible mismatch in interpretation of the HTTP specification: {0}",
					e.getLocalizedMessage());
			LOGGER.info(
					"An protocol exception has occurred while in the process of querying the provider Schemas resource object. Possible mismatch in interpretation of the HTTP specification: {0}",
					e);
			throw new ConnectorException(
					"An protocol exception has occurred while in the process of querying the provider Schemas resource object. Possible mismatch in interpretation of the HTTP specification",
					e);
		} catch (IOException e) {
			LOGGER.error(
					"An error has occurred while processing the http response. Occurrence in the process of querying the provider Schemas resource object: {0}",
					e.getLocalizedMessage());
			LOGGER.info(
					"An error has occurred while processing the http response. Occurrence in the process of querying the provider Schemas resource object: {0}",
					e);

			throw new ConnectorIOException(
					"An error has occurred while processing the http response. Occurrence in the process of querying the provider Schemas resource object",
					e);
		} finally {
			logOut(loginInstance);
		}

	}

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
			Set<Attribute> injectedAttributeSet, ScimConnectorConfiguration conf) {

		Header authHeader = null;
		String scimBaseUri = "";
		Map<String, Object> autoriazationData = logIntoService(conf);

		HttpPost loginInstance = null;

		for (String data : autoriazationData.keySet()) {
			if ("authHeader".equals(data)) {

				authHeader = (Header) autoriazationData.get(data);

			} else if ("uri".equals(data)) {

				scimBaseUri = (String) autoriazationData.get(data);
			} else if ("loginInstance".equals(data)) {

				loginInstance = (HttpPost) autoriazationData.get(data);
			}
		}

		if (authHeader == null || scimBaseUri.isEmpty()) {

			throw new ConnectorException("The data needed for authorization of request to the provider was not found.");
		}

		StrategyFetcher fetcher = new StrategyFetcher();

		HandlingStrategy strategy = fetcher.fetchStrategy(scimBaseUri);

		injectedAttributeSet = strategy.attributeInjection(injectedAttributeSet, autoriazationData);

		JSONObject jsonObject = new JSONObject();

		jsonObject = objectTranslator.translateSetToJson(attributes, injectedAttributeSet);

		HttpClient httpClient = HttpClientBuilder.create().build();
		String uri = new StringBuilder(scimBaseUri).append("/").append(resourceEndPoint).append("/").toString();

		LOGGER.info("Qeury url: {0}", uri);

		try {
			LOGGER.info("Json object to be send: {0}", jsonObject.toString(1));

			HttpPost httpPost = new HttpPost(uri);
			httpPost.addHeader(authHeader);
			httpPost.addHeader(prettyPrintHeader);

			StringEntity bodyContent = new StringEntity(jsonObject.toString(1));

			bodyContent.setContentType("application/json");
			httpPost.setEntity(bodyContent);
			String responseString = null;
			try {

				long providerStartTime = System.currentTimeMillis();
				HttpResponse response = httpClient.execute(httpPost);
				long providerEndTime = System.currentTimeMillis();
				long providerDuration = (providerEndTime - providerStartTime);

				LOGGER.info(
						"The amouth of time it took to get the response to the query from the provider : {0} milliseconds ",
						providerDuration);
				providerDuration = 0;
				int statusCode = response.getStatusLine().getStatusCode();
				LOGGER.info("Status code: {0}", statusCode);
				if (statusCode == 201) {
					LOGGER.info("Creation of resource was succesfull");

					responseString = EntityUtils.toString(response.getEntity());
					JSONObject json = new JSONObject(responseString);

					Uid uid = new Uid(json.getString("id"));

					LOGGER.info("Json response: {0}", json.toString(1));
					return uid;
				} else {

					onNoSuccess(response, "creating a new object");
				}

			} catch (ClientProtocolException e) {
				LOGGER.error(
						"An protocol exception has occurred while in the process of creating a new resource object. Possible mismatch in interpretation of the HTTP specification: {0}",
						e.getLocalizedMessage());
				LOGGER.info(
						"An protocol exception has occurred while in the process of creating a new resource object. Possible mismatch in interpretation of the HTTP specification: {0}",
						e);
				throw new ConnectionFailedException(
						"An protocol exception has occurred while in the process of creating a new resource object. Possible mismatch in interpretation of the HTTP specification: {0}",
						e);

			} catch (IOException e) {
				LOGGER.error(
						"An error has occurred while processing the http response. Occurrence in the process of creating a new resource object: {0}",
						e.getLocalizedMessage());
				LOGGER.info(
						"An error has occurred while processing the http response. Occurrence in the process of creating a new resource object: {0}",
						e);

				throw new ConnectorIOException(
						"An error has occurred while processing the http response. Occurrence in the process of creating a new resource object",
						e);
			}

		} catch (JSONException e) {

			LOGGER.error(
					"An exception has occurred while processing an json object. Occurrence in the process of creating a new resource object: {0}",
					e.getLocalizedMessage());
			LOGGER.info(
					"An exception has occurred while processing an json object. Occurrence in the process of creating a new resource object: {0}",
					e);

			throw new ConnectorException(
					"An exception has occurred while processing an json object. Occurrence in the process of creating a new resource objec",
					e);
		} catch (UnsupportedEncodingException e1) {
			LOGGER.error("Unsupported encoding: {0}. Occurrence in the process of creating a new resource object ",
					e1.getLocalizedMessage());
			LOGGER.info("Unsupported encoding: {0}. Occurrence in the process of creating a new resource object ", e1);

			throw new ConnectorException(
					"Unsupported encoding, Occurrence in the process of creating a new resource object ", e1);
		} finally {
			logOut(loginInstance);
		}
		throw new UnknownUidException("No uid returned in the process of resource creation");
	}

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
	public Uid update(Uid uid, String resourceEndPoint, JSONObject jsonObject, ScimConnectorConfiguration conf) {
		Header authHeader = null;
		String scimBaseUri = "";
		Map<String, Object> autoriazationData = logIntoService(conf);

		HttpPost loginInstance = null;

		for (String data : autoriazationData.keySet()) {
			if ("authHeader".equals(data)) {

				authHeader = (Header) autoriazationData.get(data);

			} else if ("uri".equals(data)) {

				scimBaseUri = (String) autoriazationData.get(data);
			} else if ("loginInstance".equals(data)) {

				loginInstance = (HttpPost) autoriazationData.get(data);
			}
		}

		if (authHeader == null || scimBaseUri.isEmpty()) {

			throw new ConnectorException("The data needed for authorization of request to the provider was not found.");
		}

		logIntoService(conf);

		HttpClient httpClient = HttpClientBuilder.create().build();

		String uri = new StringBuilder(scimBaseUri).append("/").append(resourceEndPoint).append("/")
				.append(uid.getUidValue()).toString();
		LOGGER.info("The uri for the update request: {0}", uri);
		HttpPatch httpPatch = new HttpPatch(uri);

		httpPatch.addHeader(authHeader);
		httpPatch.addHeader(prettyPrintHeader);

		String responseString = null;
		try {
			StringEntity bodyContent = new StringEntity(jsonObject.toString(1));
			LOGGER.info("The update JSON object wich is being sent: {0}", jsonObject);
			bodyContent.setContentType("application/json");
			httpPatch.setEntity(bodyContent);

			long providerStartTime = System.currentTimeMillis();
			HttpResponse response = httpClient.execute(httpPatch);
			long providerEndTime = System.currentTimeMillis();
			long providerDuration = (providerEndTime - providerStartTime);

			LOGGER.info(
					"The amouth of time it took to get the response to the query from the provider : {0} milliseconds ",
					providerDuration);
			providerDuration = 0;
			int statusCode = response.getStatusLine().getStatusCode();

			if (statusCode == 200 || statusCode == 201) {
				LOGGER.info("Update of resource was succesfull");

				responseString = EntityUtils.toString(response.getEntity());
				if (!responseString.isEmpty()) {
					JSONObject json = new JSONObject(responseString);
					LOGGER.ok("Json response: {0}", json.toString());
					Uid id = new Uid(json.getString("id"));
					return id;

				} else {
					LOGGER.warn("Service provider response is empty, no response after the update procedure");
				}
			} else if (statusCode == 204) {

				LOGGER.warn("Status code {0}. Response body left intentionally empty, operation may not be successful",
						statusCode);

				return uid;
			} else if (statusCode == 500 && "Groups".equals(resourceEndPoint)) {

				StrategyFetcher fetcher = new StrategyFetcher();

				HandlingStrategy strategy = fetcher.fetchStrategy(scimBaseUri);

				Uid id = strategy.groupUpdateProcedure(response, jsonObject, uri, authHeader, this);

				if (id != null) {

					return id;
				} else {
					onNoSuccess(response, "updating object");
				}
			} else {
				onNoSuccess(response, "updating object");
			}

		} catch (UnsupportedEncodingException e) {

			LOGGER.error("Unsupported encoding: {0}. Occurrence in the process of updating a resource object ",
					e.getMessage());
			LOGGER.info("Unsupported encoding: {0}. Occurrence in the process of updating a resource object ", e);

			throw new ConnectorException(
					"Unsupported encoding, Occurrence in the process of updating a resource object ", e);

		} catch (JSONException e) {

			LOGGER.error(
					"An exception has occurred while processing a json object. Occurrence in the process of updating a resource object: {0}",
					e.getLocalizedMessage());
			LOGGER.info(
					"An exception has occurred while processing a json object. Occurrence in the process of updating a resource object: {0}",
					e);

			throw new ConnectorException(
					"An exception has occurred while processing a json object,Occurrence in the process of updating a resource object",
					e);
		} catch (ClientProtocolException e) {
			LOGGER.error(
					"An protocol exception has occurred while in the process of updating a resource object. Possible mismatch in the interpretation of the HTTP specification: {0}",
					e.getLocalizedMessage());
			LOGGER.info(
					"An protocol exception has occurred while in the process of updating a resource object. Possible mismatch in the interpretation of the HTTP specification: {0}",
					e);
			throw new ConnectionFailedException(
					"An protocol exception has occurred while in the process of updating a resource object, Possible mismatch in the interpretation of the HTTP specification.",
					e);
		} catch (IOException e) {

			LOGGER.error(
					"An error has occurred while processing the http response. Occurrence in the process of updating a resource object: {0}",
					e.getLocalizedMessage());
			LOGGER.info(
					"An error has occurred while processing the http response. Occurrence in the process of creating a resource object: {0}",
					e);

			throw new ConnectorIOException(
					"An error has occurred while processing the http response. Occurrence in the process of creating a resource object",
					e);

		} finally {
			logOut(loginInstance);
		}
		throw new UnknownUidException("No uid returned in the process of resource update");

	}

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
	public void delete(Uid uid, String resourceEndPoint, ScimConnectorConfiguration conf) {

		Header authHeader = null;
		String scimBaseUri = "";
		Map<String, Object> autoriazationData = logIntoService(conf);

		HttpPost loginInstance = null;

		for (String data : autoriazationData.keySet()) {
			if ("authHeader".equals(data)) {

				authHeader = (Header) autoriazationData.get(data);

			} else if ("uri".equals(data)) {

				scimBaseUri = (String) autoriazationData.get(data);
			} else if ("loginInstance".equals(data)) {

				loginInstance = (HttpPost) autoriazationData.get(data);
			}
		}

		if (authHeader == null || scimBaseUri.isEmpty()) {

			throw new ConnectorException("The data needed for authorization of request to the provider was not found.");
		}

		HttpClient httpClient = HttpClientBuilder.create().build();

		String uri = new StringBuilder(scimBaseUri).append("/").append(resourceEndPoint).append("/")
				.append(uid.getUidValue()).toString();

		LOGGER.info("The uri for the delete request: {0}", uri);
		HttpDelete httpDelete = new HttpDelete(uri);
		httpDelete.addHeader(authHeader);
		httpDelete.addHeader(prettyPrintHeader);

		try {
			long providerStartTime = System.currentTimeMillis();
			HttpResponse response = httpClient.execute(httpDelete);
			long providerEndTime = System.currentTimeMillis();
			long providerDuration = (providerEndTime - providerStartTime);

			LOGGER.info(
					"The amouth of time it took to get the response to the query from the provider : {0} milliseconds ",
					providerDuration);
			providerDuration = 0;

			int statusCode = response.getStatusLine().getStatusCode();

			if (statusCode == 204 || statusCode == 200) {
				LOGGER.info("Deletion of resource was succesfull");
			}

			else if (statusCode == 404) {

				LOGGER.info("Resource not found or resource was already deleted");
			} else {
				onNoSuccess(response, "deleting object");
			}

		} catch (ClientProtocolException e) {
			LOGGER.error(
					"An protocol exception has occurred while in the process of deleting a resource object. Possible mismatch in the interpretation of the HTTP specification: {0}",
					e.getLocalizedMessage());
			LOGGER.info(
					"An protocol exception has occurred while in the process of deleting a resource object. Possible mismatch in the interpretation of the HTTP specification: {0}",
					e);
			throw new ConnectionFailedException(
					"An protocol exception has occurred while in the process of deleting a resource object. Possible mismatch in the interpretation of the HTTP specification.",
					e);
		} catch (IOException e) {
			LOGGER.error(
					"An error has occurred while processing the http response. Occurrence in the process of deleting a resource object: : {0}",
					e.getLocalizedMessage());
			LOGGER.info(
					"An error has occurred while processing the http response. Occurrence in the process of deleting a resource object: : {0}",
					e);

			throw new ConnectorIOException(
					"An error has occurred while processing the http response. Occurrence in the process of deleting a resource object.",
					e);
		} finally {
			logOut(loginInstance);
		}
	}

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

	public ParserSchemaScim processSchemaResponse(JSONObject responseObject, String providerName) {

		LOGGER.info("The resources json representation: {0}", responseObject.toString(1));
		ParserSchemaScim scimParser = new ParserSchemaScim();
		for (int i = 0; i < responseObject.getJSONArray("Resources").length(); i++) {
			JSONObject minResourceJson = new JSONObject();
			minResourceJson = responseObject.getJSONArray("Resources").getJSONObject(i);

			if (minResourceJson.has("endpoint")) {
				scimParser.parseSchema(minResourceJson, providerName);

			} else {
				LOGGER.error("No uid present in fetched object: {0}", minResourceJson);

				throw new ConnectorException("No uid present in fetchet object while processing queuery result");
			}
		}
		return scimParser;

	}

	public void queryMembershipData(Uid uid, String resourceEndPoint, ResultsHandler resultHandler,
			String membershipResourceEndpoin, ScimConnectorConfiguration conf) {

		Header authHeader = null;
		String scimBaseUri = "";
		Map<String, Object> autoriazationData = logIntoService(conf);

		HttpPost loginInstance = null;

		for (String data : autoriazationData.keySet()) {
			if ("authHeader".equals(data)) {

				authHeader = (Header) autoriazationData.get(data);

			} else if ("uri".equals(data)) {

				scimBaseUri = (String) autoriazationData.get(data);
			} else if ("loginInstance".equals(data)) {

				loginInstance = (HttpPost) autoriazationData.get(data);
			}
		}

		if (authHeader == null || scimBaseUri.isEmpty()) {

			throw new ConnectorException("The data needed for authorization of request to the provider was not found.");
		}

		logIntoService(conf);
		HttpClient httpClient = HttpClientBuilder.create().build();
		String q;
		q = ((Uid) uid).getUidValue();

		String uri = new StringBuilder(scimBaseUri).append("/").append(resourceEndPoint).append("/").append(q)
				.toString();
		LOGGER.info("Qeury url: {0}", uri);
		HttpGet httpGet = new HttpGet(uri);
		httpGet.addHeader(authHeader);
		httpGet.addHeader(prettyPrintHeader);

		String responseString = null;
		HttpResponse response;
		try {
			long providerStartTime = System.currentTimeMillis();
			response = httpClient.execute(httpGet);
			long providerEndTime = System.currentTimeMillis();
			long providerDuration = (providerEndTime - providerStartTime);

			LOGGER.info(
					"The amouth of time it took to get the response to the query from the provider : {0} milliseconds ",
					providerDuration);

			providerDuration = 0;

			int statusCode = response.getStatusLine().getStatusCode();
			LOGGER.info("Status code: {0}", statusCode);
			if (statusCode == 200) {

				responseString = EntityUtils.toString(response.getEntity());
				if (!responseString.isEmpty()) {
					try {
						JSONObject jsonObject = new JSONObject(responseString);

						LOGGER.info("Json object returned from service provider: {0}", jsonObject.toString(1));
						try {

							if (jsonObject.has("groups")) {
								int amountOfResources = jsonObject.getJSONArray("groups").length();

								for (int i = 0; i < amountOfResources; i++) {
									JSONObject minResourceJson = new JSONObject();
									minResourceJson = jsonObject.getJSONArray("groups").getJSONObject(i);
									if (minResourceJson.has("value")) {

										String groupUid = minResourceJson.getString("value");
										if (groupUid != null && !groupUid.isEmpty()) {

											StringBuilder groupUri = new StringBuilder(scimBaseUri).append("/")
													.append(membershipResourceEndpoin).append("/").append(groupUid);

											LOGGER.info("The uri to which we are sending the queri {0}", groupUri);

											HttpGet httpGetR = new HttpGet(groupUri.toString());
											httpGetR.addHeader(authHeader);
											httpGetR.addHeader(prettyPrintHeader);

											HttpResponse resourceResponse = httpClient.execute(httpGetR);

											if (statusCode == 200) {
												responseString = EntityUtils.toString(resourceResponse.getEntity());
												JSONObject fullResourcejson = new JSONObject(responseString);

												LOGGER.info(
														"The {0}. resource json object which was returned by the service provider: {1}",
														i + 1, fullResourcejson.toString(1));

												BuilderConnectorObject build = new BuilderConnectorObject();
												ConnectorObject connectorObject = build.buildConnectorObject(
														fullResourcejson, membershipResourceEndpoin, scimBaseUri);
												resultHandler.handle(connectorObject);

											} else {

												onNoSuccess(resourceResponse, groupUri.toString());
											}

										}
									} else {
										LOGGER.error("No uid present in fetched object: {0}", minResourceJson);

										throw new ConnectorException(
												"No uid present in fetchet object while processing queuery result");

									}
								}
							} else {

								LOGGER.error("Resource object not present in provider response to the query");

								throw new ConnectorException(
										"No uid present in fetchet object while processing queuery result");

							}

						} catch (Exception e) {
							LOGGER.error(
									"Builder error. Error while building connId object. The exception message: {0}",
									e.getLocalizedMessage());
							LOGGER.info("Builder error. Error while building connId object. The excetion message: {0}",
									e);
							throw new ConnectorException(e);
						}

					} catch (JSONException jsonException) {
						if (q == null) {
							q = "the full resource representation";
						}
						LOGGER.error(
								"An exception has occurred while setting the variable \"jsonObject\". Occurrence while processing the http response to the queuey request for: {1}, exception message: {0}",
								jsonException.getLocalizedMessage(), q);
						LOGGER.info(
								"An exception has occurred while setting the variable \"jsonObject\". Occurrence while processing the http response to the queuey request for: {1}, exception message: {0}",
								jsonException, q);
						throw new ConnectorException(
								"An exception has occurred while setting the variable \"jsonObject\".", jsonException);
					}

				} else {

					LOGGER.warn("Service provider response is empty, responce returned on queuery: {0}", uri);
				}
			} else {
				onNoSuccess(response, uri);
			}

		} catch (IOException e) {

			if (q == null) {
				q = "the full resource representation";
			}

			LOGGER.error(
					"An error occurred while processing the queuery http response. Occurrence while processing the http response to the queuey request for: {1}, exception message: {0}",
					e.getLocalizedMessage(), q);
			LOGGER.info(
					"An error occurred while processing the queuery http response. Occurrence while processing the http response to the queuey request for: {1}, exception message: {0}",
					e, q);
			throw new ConnectorIOException("An error occurred while processing the queuery http response.", e);
		} finally {
			logOut(loginInstance);
		}

	}

	public static void logOut(HttpPost loginInstance) {
		loginInstance.releaseConnection();
		LOGGER.info("The connection was released");
	}
}
