package com.evolveum.polygon.scim;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.HashSet;
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
import org.identityconnectors.framework.common.objects.AttributeBuilder;
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
 * Holds the CRUD+L methods needed for interaction with the service provider.
 */
public class CrudManagerScim {

	// TODO check all cases when logout is called

	private ScimConnectorConfiguration conf;
	private boolean tokenAuthentication = false;

	private Header prettyPrintHeader = new BasicHeader("X-PrettyPrint", "1");

	long providerStartTime;
	long providerEndTime;
	long providerDuration;

	long operationStartTime;
	long operationEndTime;
	long operationDuration;

	HttpPost loginInstance;

	private static final Log LOGGER = Log.getLog(CrudManagerScim.class);

	/**
	 * Constructor which populates a variable with the provided configuration.
	 * 
	 * @param conf
	 *            The provided configuration.
	 */
	public CrudManagerScim(ScimConnectorConfiguration conf) {
		this.conf = (ScimConnectorConfiguration) conf;
	}

	/**
	 * Used for loging to the service. The data needed for this operation is
	 * provided by the configuration.
	 * 
	 * @return a json object carrying meta information about the login session.
	 * @throws ConnectorException
	 * @throws ConnectionFailedException
	 * @throws ConnectorIOException
	 */
	public HashMap<String, Object> logIntoService() {

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
				providerStartTime = System.currentTimeMillis();
				response = httpclient.execute(loginInstance);
				providerEndTime = System.currentTimeMillis();
				providerDuration = (providerEndTime - providerStartTime);
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
			tokenAuthentication = true;
			loginInstanceUrl = conf.getBaseUrl();
			loginAccessToken = conf.getToken();

			authHeader = new BasicHeader("Authorization", "Bearer " + loginAccessToken);
		}
		String scimBaseUri = new StringBuilder(loginInstanceUrl).append(conf.getEndpoint()).append(conf.getVersion())
				.toString();

		LOGGER.info("Login Successful");

		/// TODO login to return authorization data
		HashMap<String, Object> autoriazationData = new HashMap<String, Object>();
		autoriazationData.put("uri", scimBaseUri);
		autoriazationData.put("authHeader", authHeader);

		if (jsonObject != null) {
			autoriazationData.put("json", jsonObject);
		}

		///
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
	public void qeueryEntity(Object query, String resourceEndPoint, ResultsHandler resultHandler) {

		Header authHeader = null;
		String scimBaseUri = "";
		HashMap<String, Object> autoriazationData = logIntoService();

		for (String data : autoriazationData.keySet()) {
			if ("authHeader".equals(data)) {
				authHeader = (Header) autoriazationData.get(data);

			} else if ("uri".equals(data)) {
				scimBaseUri = (String) autoriazationData.get(data);
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
			providerStartTime = System.currentTimeMillis();
			response = httpClient.execute(httpGet);
			providerEndTime = System.currentTimeMillis();
			providerDuration = (providerEndTime - providerStartTime);

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
								ConnectorObjBuilder objBuilder = new ConnectorObjBuilder();
								resultHandler.handle(objBuilder.buildConnectorObject(jsonObject, resourceEndPoint));

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

													ConnectorObjBuilder objBuilder = new ConnectorObjBuilder();

													long startTime = System.currentTimeMillis();
													ConnectorObject conOb = objBuilder
															.buildConnectorObject(fullResourcejson, resourceEndPoint);
													long endTime = System.currentTimeMillis();

													long time = (endTime - startTime);

													LOGGER.error(
															"The connector object builder method Time: {0} milliseconds",
															time);

													resultHandler.handle(conOb);

												} else {

													onNoSuccess(resourceResponse, statusCode, resourceUri);
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
				onNoSuccess(response, statusCode, uri);
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
			logOut();
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
	public ParserSchemaScim qeueryEntity(String providerName, String resourceEndPoint) {
		logIntoService();

		Header authHeader = null;
		String scimBaseUri = "";
		HashMap<String, Object> autoriazationData = logIntoService();

		for (String data : autoriazationData.keySet()) {
			if ("authHeader".equals(data)) {
				authHeader = (Header) autoriazationData.get(data);

			} else if ("uri".equals(data)) {
				scimBaseUri = (String) autoriazationData.get(data);
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

			providerStartTime = System.currentTimeMillis();
			response = httpClient.execute(httpGet);
			providerEndTime = System.currentTimeMillis();

			providerDuration = (providerEndTime - providerStartTime);

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
					ParserSchemaScim schemaParser = processResponse(jsonObject, providerName);
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

							if ("slack".equals(providerName)) {
								missingSchemaAttributesWorkaround(resourceName, jsonObject);
							}

							responseArray.put(jsonObject);
						} else {

							LOGGER.warn(
									"No definition for provided shcemas was found, the connector will switch to default core schema configuration!");
							return null;
						}
					}
					responseObject.put("Resources", responseArray);
					return processResponse(responseObject, providerName);

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
			logOut();
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

	public Uid createEntity(String resourceEndPoint, ObjectTranslator objectTranslator, Set<Attribute> attributes,
			HashSet<Attribute> injectedAttributeSet) {

		String orgID = null;
		JSONObject loginObject = null;
		Header authHeader = null;
		String scimBaseUri = "";
		HashMap<String, Object> autoriazationData = logIntoService();

		for (String data : autoriazationData.keySet()) {
			if ("authHeader".equals(data)) {
				authHeader = (Header) autoriazationData.get(data);
			} else if ("uri".equals(data)) {
				scimBaseUri = (String) autoriazationData.get(data);
			} else if ("json".equals(data)) {
				loginObject = (JSONObject) autoriazationData.get(data);
			}
		}

		if (authHeader == null || scimBaseUri.isEmpty()) {

			throw new ConnectorException("The data needed for authorization of request to the provider was not found.");
		}

		if (loginObject != null) {
			if (loginObject.has("id")) {
				orgID = loginObject.getString("id");
				String idParts[] = orgID.split("\\/");
				orgID = idParts[4];
			}
		} else {

			LOGGER.info("No json object returned after login");
		}
		// injection of organization ID into the set of attributes
		if (orgID != null) {
			LOGGER.info("The organization ID is: {0}", orgID);

			// TODO schema version might change
			injectedAttributeSet
					.add(AttributeBuilder.build("schema.type", "urn:scim:schemas:extension:enterprise:1.0"));

			injectedAttributeSet.add(AttributeBuilder.build("schema.organization", orgID));
		} else {
			LOGGER.warn("No organization ID specified in instance URL");
		}

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

				providerStartTime = System.currentTimeMillis();
				HttpResponse response = httpClient.execute(httpPost);
				providerEndTime = System.currentTimeMillis();
				providerDuration = (providerEndTime - providerStartTime);

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

					onNoSuccess(response, statusCode, "creating a new object");
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
			logOut();
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
	public Uid updateEntity(Uid uid, String resourceEndPoint, JSONObject jsonObject) {
		Header authHeader = null;
		String scimBaseUri = "";
		HashMap<String, Object> autoriazationData = logIntoService();

		for (String data : autoriazationData.keySet()) {
			if ("authHeader".equals(data)) {
				authHeader = (Header) autoriazationData.get(data);
			} else if ("uri".equals(data)) {
				scimBaseUri = (String) autoriazationData.get(data);
			}
		}

		if (authHeader == null || scimBaseUri.isEmpty()) {

			throw new ConnectorException("The data needed for authorization of request to the provider was not found.");
		}

		logIntoService();

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

			providerStartTime = System.currentTimeMillis();
			HttpResponse response = httpClient.execute(httpPatch);
			providerEndTime = System.currentTimeMillis();
			providerDuration = (providerEndTime - providerStartTime);

			LOGGER.info(
					"The amouth of time it took to get the response to the query from the provider : {0} milliseconds ",
					providerDuration);
			providerDuration = 0;
			int statusCode = response.getStatusLine().getStatusCode();

			if (statusCode == 200 || statusCode == 201) {
				LOGGER.info("Update of resource was succesfull");

				responseString = EntityUtils.toString(response.getEntity());

				JSONObject json = new JSONObject(responseString);
				Uid id = new Uid(json.getString("id"));
				LOGGER.ok("Json response: {0}", json.toString());
				return id;

			} else if (statusCode == 204) {

				LOGGER.warn("Status code {0}. Response body left intentionally empty, operation may not be successful",
						statusCode);

				return uid;
			} else if (statusCode == 500 && "Groups".equals(resourceEndPoint)) {
				HandlingStrategy strategy;
				// For Salesforce group/members workaround purposes
				String[] uriParts = scimBaseUri.split("\\."); // e.g.
				// https://eu6.salesforce.com/services/scim/v1

				if (uriParts.length >= 2) {

					if ("salesforce".equals(uriParts[1])) {
						strategy = new SalesforceHandlingStrategy();

					} else if ("slack".equals(uriParts[1])) {

						strategy = new SlackHandlingStrategy();

					} else {

						strategy = new StandardScimHandlingStrategy();
					}
					Uid id = strategy.specialGroupUpdateProcedure(response, jsonObject, uri, authHeader);

					if (id != null) {

						return id;
					} else {
						onNoSuccess(response, statusCode, "updating object");
					}
				}

			}

			else {
				onNoSuccess(response, statusCode, "updating object");
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
			logOut();
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
	public void deleteEntity(Uid uid, String resourceEndPoint) {

		Header authHeader = null;
		String scimBaseUri = "";
		HashMap<String, Object> autoriazationData = logIntoService();

		for (String data : autoriazationData.keySet()) {
			if ("authHeader".equals(data)) {
				authHeader = (Header) autoriazationData.get(data);
			} else if ("uri".equals(data)) {
				scimBaseUri = (String) autoriazationData.get(data);
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
			providerStartTime = System.currentTimeMillis();
			HttpResponse response = httpClient.execute(httpDelete);
			providerEndTime = System.currentTimeMillis();
			providerDuration = (providerEndTime - providerStartTime);

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
				onNoSuccess(response, statusCode, "deleting object");
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
			logOut();
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
	private void onNoSuccess(HttpResponse response, int statusCode, String message) throws ParseException, IOException {

		StringBuilder exceptionStringBuilder = null;

		if (response.getEntity() != null) {
			String responseString = EntityUtils.toString(response.getEntity());

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
					.append(" was unsuccessful. Status code returned: ").append(statusCode);
		}

		String exceptionString = exceptionStringBuilder.toString();

		if (message == null) {
			message = "the full resource representation";
		}
		LOGGER.error(exceptionString);

		LOGGER.info("An error has occured. Http status: \"{0}\"", statusCode);
		LOGGER.info(exceptionString);

		throw new ConnectorIOException(exceptionString);
	}

	public StringBuilder buildErrorMessage(JSONObject responseObject, String message, int statusCode) {

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

	public ParserSchemaScim processResponse(JSONObject responseObject, String providerName) {

		LOGGER.info("The resources json representation: {0}", responseObject.toString(1));
		ParserSchemaScim scimParser = new ParserSchemaScim(providerName);
		for (int i = 0; i < responseObject.getJSONArray("Resources").length(); i++) {
			JSONObject minResourceJson = new JSONObject();
			minResourceJson = responseObject.getJSONArray("Resources").getJSONObject(i);

			// TODO test if id check needed
			// if (minResourceJson.has("id") && minResourceJson.getString("id")
			// != null) {
			if (minResourceJson.has("endpoint")) {
				scimParser.parseSchema(minResourceJson);

			} else {
				LOGGER.error("No uid present in fetched object: {0}", minResourceJson);

				throw new ConnectorException("No uid present in fetchet object while processing queuery result");
			}

			// }

		}
		return scimParser;

	}

	private void missingSchemaAttributesWorkaround(String resourceName, JSONObject jsonObject) {

		// For workaround purposes. Slack missing attributes workaround.
		if ("Users".equals(resourceName)) {

			HashMap<String, String> missingAttirbutes = new HashMap<String, String>();
			missingAttirbutes.put("userName", "userName");
			missingAttirbutes.put("nickName", "nickName");
			missingAttirbutes.put("title", "title");
			missingAttirbutes.put("schemas", "schemas");

			missingAttirbutes.put("profileUrl", "profileUrl");
			missingAttirbutes.put("displayName", "displayName");
			missingAttirbutes.put("timezone", "timezone");
			missingAttirbutes.put("externalId", "externalId");
			missingAttirbutes.put("active", "active");
			missingAttirbutes.put("photos", "photos");

			if (jsonObject.has("attributes")) {

				JSONArray attributesArray = new JSONArray();

				attributesArray = jsonObject.getJSONArray("attributes");
				for (int indexValue = 0; indexValue < attributesArray.length(); indexValue++) {
					JSONObject iteratedObjects = attributesArray.getJSONObject(indexValue);

					if (iteratedObjects.has("name")) {

						if ("userName".equals(iteratedObjects.get("name"))) {

							missingAttirbutes.remove("userName");
						} else if ("nickName".equals(iteratedObjects.get("name"))) {

							missingAttirbutes.remove("nickName");
						} else if ("title".equals(iteratedObjects.get("name"))) {

							missingAttirbutes.remove("title");
						} else if ("schemas".equals(iteratedObjects.get("name"))) {

							missingAttirbutes.remove("schemas");
						} else if ("profileUrl".equals(iteratedObjects.get("name"))) {

							missingAttirbutes.remove("profileUrl");
						} else if ("displayName".equals(iteratedObjects.get("name"))) {

							missingAttirbutes.remove("displayName");
						} else if ("timezone".equals(iteratedObjects.get("name"))) {

							missingAttirbutes.remove("timezone");
						} else if ("externalId".equals(iteratedObjects.get("name"))) {

							missingAttirbutes.remove("externalId");
						} else if ("active".equals(iteratedObjects.get("name"))) {

							missingAttirbutes.remove("active");
						} else if ("photos".equals(iteratedObjects.get("name"))) {

							missingAttirbutes.remove("photos");
						}
					}
				}

				for (String missingAttributeNames : missingAttirbutes.keySet()) {

					if ("userName".equals(missingAttributeNames)) {
						JSONObject userName = new JSONObject();

						userName.put("schema", "urn:scim:schemas:core:1.0");
						userName.put("name", "userName");
						userName.put("readOnly", false);
						userName.put("type", "string");
						userName.put("required", true);
						userName.put("caseExact", false);

						attributesArray.put(userName);

					} else if ("active".equals(missingAttributeNames)) {
						JSONObject active = new JSONObject();

						active.put("schema", "urn:scim:schemas:core:1.0");
						active.put("name", "active");
						active.put("readOnly", false);
						active.put("type", "boolean");
						active.put("required", false);
						active.put("caseExact", false);

						attributesArray.put(active);

					} else if ("externalId".equals(missingAttributeNames)) {
						JSONObject externalId = new JSONObject();

						externalId.put("schema", "urn:scim:schemas:core:1.0");
						externalId.put("name", "externalId");
						externalId.put("readOnly", false);
						externalId.put("type", "string");
						externalId.put("required", false);
						externalId.put("caseExact", true);

						attributesArray.put(externalId);

					} else if ("timezone".equals(missingAttributeNames)) {
						JSONObject timezone = new JSONObject();

						timezone.put("schema", "urn:scim:schemas:core:1.0");
						timezone.put("name", "timezone");
						timezone.put("readOnly", false);
						timezone.put("type", "string");
						timezone.put("required", false);
						timezone.put("caseExact", false);

						attributesArray.put(timezone);

					} else if ("displayName".equals(missingAttributeNames)) {
						JSONObject displayName = new JSONObject();

						displayName.put("schema", "urn:scim:schemas:core:1.0");
						displayName.put("name", "displayName");
						displayName.put("readOnly", false);
						displayName.put("type", "string");
						displayName.put("required", false);
						displayName.put("caseExact", false);

						attributesArray.put(displayName);

					} else if ("profileUrl".equals(missingAttributeNames)) {
						JSONObject profileUrl = new JSONObject();

						profileUrl.put("schema", "urn:scim:schemas:core:1.0");
						profileUrl.put("name", "profileUrl");
						profileUrl.put("readOnly", false);
						profileUrl.put("type", "string");
						profileUrl.put("required", false);
						profileUrl.put("caseExact", false);

						attributesArray.put(profileUrl);

					} else if ("nickName".equals(missingAttributeNames)) {
						JSONObject nickName = new JSONObject();

						nickName.put("schema", "urn:scim:schemas:core:1.0");
						nickName.put("name", "nickName");
						nickName.put("readOnly", false);
						nickName.put("type", "string");
						nickName.put("required", true);
						nickName.put("caseExact", false);

						attributesArray.put(nickName);

					} else if ("title".equals(missingAttributeNames)) {
						JSONObject title = new JSONObject();

						title.put("schema", "urn:scim:schemas:core:1.0");
						title.put("name", "title");
						title.put("readOnly", false);
						title.put("type", "string");
						title.put("required", false);
						title.put("caseExact", false);

						attributesArray.put(title);
					} else if ("schemas".equals(missingAttributeNames)) {
						JSONObject schemas = new JSONObject();
						JSONArray subattributeArray = new JSONArray();

						JSONObject valueBlank = new JSONObject();

						schemas.put("schema", "urn:scim:schemas:core:1.0");
						schemas.put("name", "schemas");
						schemas.put("readOnly", false);
						schemas.put("type", "complex");
						schemas.put("multiValued", true);
						schemas.put("required", false);
						schemas.put("caseExact", false);

						valueBlank.put("name", "blank");
						valueBlank.put("readOnly", true);
						valueBlank.put("required", false);
						valueBlank.put("multiValued", false);

						subattributeArray.put(valueBlank);

						schemas.put("subAttributes", subattributeArray);

						attributesArray.put(schemas);
					}

				}

				jsonObject.put("attributes", attributesArray);
			}

		}

	}

	public void queryMembershipData(Uid uid, String resourceEndPoint, ResultsHandler resultHandler,
			String membershipResourceEndpoin) {

		Header authHeader = null;
		String scimBaseUri = "";
		HashMap<String, Object> autoriazationData = logIntoService();

		for (String data : autoriazationData.keySet()) {
			if ("authHeader".equals(data)) {
				authHeader = (Header) autoriazationData.get(data);
			} else if ("uri".equals(data)) {
				scimBaseUri = (String) autoriazationData.get(data);
			}
		}

		if (authHeader == null || scimBaseUri.isEmpty()) {

			throw new ConnectorException("The data needed for authorization of request to the provider was not found.");
		}

		logIntoService();
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
			providerStartTime = System.currentTimeMillis();
			response = httpClient.execute(httpGet);
			providerEndTime = System.currentTimeMillis();
			providerDuration = (providerEndTime - providerStartTime);

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

												ConnectorObjBuilder objBuilder = new ConnectorObjBuilder();

												long startTime = System.currentTimeMillis();
												ConnectorObject conOb = objBuilder.buildConnectorObject(
														fullResourcejson, membershipResourceEndpoin);
												long endTime = System.currentTimeMillis();

												long time = (endTime - startTime);

												LOGGER.error(
														"The connector object builder method Time: {0} milliseconds",
														time);

												resultHandler.handle(conOb);

											} else {

												onNoSuccess(resourceResponse, statusCode, groupUri.toString());
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
				onNoSuccess(response, statusCode, uri);
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
			logOut();
		}

	}

	public void logOut() {
		if (!tokenAuthentication) {
			loginInstance.releaseConnection();
			LOGGER.info("The connecion was released");
		}

	}

	public Uid specialUpdateProcedure(HttpResponse response, HttpClient httpClient, String uri) {

		Header authHeader = null;
		String scimBaseUri = "";
		HashMap<String, Object> autoriazationData = logIntoService();

		for (String data : autoriazationData.keySet()) {
			if ("authHeader".equals(data)) {
				authHeader = (Header) autoriazationData.get(data);
			} else if ("uri".equals(data)) {
				scimBaseUri = (String) autoriazationData.get(data);
			}
		}

		if (authHeader == null || scimBaseUri.isEmpty()) {

			throw new ConnectorException("The data needed for authorization of request to the provider was not found.");
		}

		Uid id = null;
		Integer statusCode = response.getStatusLine().getStatusCode();

		JSONObject jsonObject = new JSONObject();

		// Salesforce group/members workaround
		String[] uriParts = scimBaseUri.split("\\."); // e.g.
		// https://eu6.salesforce.com/services/scim/v1

		if (uriParts.length >= 2) {

			if ("salesforce".equals(uriParts[1])) {
				LOGGER.warn(
						"Status code from first update query: {0}. Processing trought Salesforce \"group/member update\" workaround. ",
						statusCode);
				HttpGet httpGet = new HttpGet(uri);
				httpGet.addHeader(authHeader);
				httpGet.addHeader(prettyPrintHeader);

				providerStartTime = System.currentTimeMillis();
				try {
					response = httpClient.execute(httpGet);

					providerEndTime = System.currentTimeMillis();
					providerDuration = (providerEndTime - providerStartTime);

					LOGGER.info(
							"The amouth of time it took to get the response to the query from the provider : {0} milliseconds ",
							providerDuration);

					providerDuration = 0;

					statusCode = response.getStatusLine().getStatusCode();
					LOGGER.info("status code: {0}", statusCode);
					if (statusCode == 200) {

						String responseString = EntityUtils.toString(response.getEntity());
						if (!responseString.isEmpty()) {

							JSONObject json = new JSONObject(responseString);
							LOGGER.info("Json object returned from service provider: {0}", json);
							for (String attributeName : jsonObject.keySet()) {

								json.put(attributeName, jsonObject.get(attributeName));

							}
							StringEntity bodyContent = new StringEntity(jsonObject.toString(1));
							HttpPatch httpPatch = new HttpPatch(uri);

							bodyContent = new StringEntity(json.toString(1));
							bodyContent.setContentType("application/json");
							httpPatch.setEntity(bodyContent);

							providerStartTime = System.currentTimeMillis();
							response = httpClient.execute(httpPatch);

							providerEndTime = System.currentTimeMillis();
							providerDuration = (providerEndTime - providerStartTime);

							LOGGER.info(
									"The amouth of time it took to get the response to the query from the provider : {0} milliseconds ",
									providerDuration);
							providerDuration = 0;

							statusCode = response.getStatusLine().getStatusCode();
							LOGGER.info("status code: {0}", statusCode);
							if (statusCode == 200 || statusCode == 201) {
								LOGGER.info("Update of resource was succesfull");
								responseString = EntityUtils.toString(response.getEntity());
								json = new JSONObject(responseString);
								id = new Uid(json.getString("id"));
								LOGGER.ok("Json response: {0}", json.toString(1));
								return id;
							} else {

								onNoSuccess(response, statusCode, "updating object");
							}

						}
					}

				} catch (ClientProtocolException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		}
		return id;

	}
}
