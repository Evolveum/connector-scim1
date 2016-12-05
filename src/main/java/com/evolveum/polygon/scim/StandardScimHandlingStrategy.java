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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.util.EntityUtils;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.AlreadyExistsException;
import org.identityconnectors.framework.common.exceptions.ConnectionFailedException;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.ConnectorIOException;
import org.identityconnectors.framework.common.exceptions.InvalidCredentialException;
import org.identityconnectors.framework.common.exceptions.OperationTimeoutException;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfoBuilder;
import org.identityconnectors.framework.common.objects.OperationalAttributeInfos;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.SearchResult;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.AttributeFilter;
import org.identityconnectors.framework.common.objects.filter.ContainsAllValuesFilter;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.spi.SearchResultsHandler;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.evolveum.polygon.scim.common.HttpPatch;

/**
 * @author Macik
 * 
 *         An implementation of all strategy methods used for processing of
 *         data.
 * 
 */

public class StandardScimHandlingStrategy implements HandlingStrategy {

	private static final Log LOGGER = Log.getLog(StandardScimHandlingStrategy.class);
	private static final String CANONICALVALUES = "canonicalValues";
	private static final String REFERENCETYPES = "referenceTypes";
	private static final String RESOURCES = "Resources";
	private static final String STARTINDEX = "startIndex";
	private static final String TOTALRESULTS = "totalResults";
	private static final String ITEMSPERPAGE = "itemsPerPage";
	private static final String FORBIDENSEPPARATOR = ":";
	private static final String SEPPARATOR = "-";
	private static final char QUERYCHAR = '?';
	private static final char QUERYDELIMITER = '&';

	@Override
	public Uid create(String resourceEndPoint, ObjectTranslator objectTranslator, Set<Attribute> attributes,
			Set<Attribute> injectedAttributeSet, ScimConnectorConfiguration conf) {

		ServiceAccessManager accessManager = new ServiceAccessManager(conf);

		Header authHeader = accessManager.getAuthHeader();
		String scimBaseUri = accessManager.getBaseUri();

		if (authHeader == null || scimBaseUri.isEmpty()) {

			throw new ConnectorException("The data needed for authorization of request to the provider was not found.");
		}

		injectedAttributeSet = attributeInjection(injectedAttributeSet, accessManager.getLoginJson());

		JSONObject jsonObject = new JSONObject();

		jsonObject = objectTranslator.translateSetToJson(attributes, injectedAttributeSet, resourceEndPoint);

		HttpClient httpClient = initHttpClient(conf);

		String uri = new StringBuilder(scimBaseUri).append(SLASH).append(resourceEndPoint).append(SLASH).toString();

		LOGGER.info("Query url: {0}", uri);

		try {
			// LOGGER.info("Json object to be send: {0}",
			// jsonObject.toString(1));

			HttpPost httpPost = buildHttpPost(uri, authHeader, jsonObject);
			String responseString = null;
			try (CloseableHttpResponse response = (CloseableHttpResponse) httpClient.execute(httpPost)) {

				HttpEntity entity = response.getEntity();

				if (entity != null) {
					responseString = EntityUtils.toString(entity);
				} else {
					responseString = "";
				}

				int statusCode = response.getStatusLine().getStatusCode();
				LOGGER.info("Status code: {0}", statusCode);

				if (statusCode == 201) {
					// LOGGER.info("Creation of resource was successful");

					if (!responseString.isEmpty()) {
						JSONObject json = new JSONObject(responseString);

						Uid uid = new Uid(json.getString(ID));

						// LOGGER.info("Json response: {0}", json.toString(1));
						return uid;
					} else {
						return null;
					}
				} else {
					handleInvalidStatus(" while resource creation, please check if credentials are valid. ",
							responseString, "creating a new object", statusCode);
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

				if ((e instanceof SocketTimeoutException || e instanceof NoRouteToHostException)) {

					throw new OperationTimeoutException(
							"The connection timed out. Occurrence in the process of creating a new resource object", e);
				} else {

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
		} catch (UnsupportedEncodingException e) {
			LOGGER.error("Unsupported encoding: {0}. Occurrence in the process of creating a new resource object ",
					e.getLocalizedMessage());
			LOGGER.info("Unsupported encoding: {0}. Occurrence in the process of creating a new resource object ", e);

			throw new ConnectorException(
					"Unsupported encoding, Occurrence in the process of creating a new resource object ", e);
		}
		return null;
	}

	@Override
	public void query(Filter query, StringBuilder queryUriSnippet, String resourceEndPoint,
			ResultsHandler resultHandler, ScimConnectorConfiguration conf) {

		LOGGER.info("Processing query");

		Boolean isCAVGroupQuery = false; // is the query a ContainsAllValues
											// filter query for the group
											// endpoint?
		Boolean valueIsUid = false;
		ServiceAccessManager accessManager = new ServiceAccessManager(conf);

		Header authHeader = accessManager.getAuthHeader();
		String scimBaseUri = accessManager.getBaseUri();

		if (authHeader == null || scimBaseUri.isEmpty()) {

			throw new ConnectorException("The data needed for authorization of request to the provider was not found.");
		}

		String q;

		String[] baseUrlParts = scimBaseUri.split("\\.");
		String providerName = baseUrlParts[1];

		if (query != null) {

			if (query instanceof EqualsFilter) {
				Attribute filterAttr = ((EqualsFilter) query).getAttribute();

				if (filterAttr instanceof Uid) {

					valueIsUid = true;

					isCAVGroupQuery = checkFilter(query, resourceEndPoint);

					if (!isCAVGroupQuery) {
						q = ((Uid) filterAttr).getUidValue();
					} else {
						q = ((Uid) filterAttr).getUidValue();
						resourceEndPoint = "Users";
					}
				} else {

					isCAVGroupQuery = checkFilter(query, resourceEndPoint);

					if (!isCAVGroupQuery) {
						LOGGER.info("Attribute not instance of UID");
						q = qIsFilter(query, queryUriSnippet, providerName, resourceEndPoint);
					} else {
						q = (String) filterAttr.getValue().get(0);
						resourceEndPoint = "Users";
					}

				}

			} else {

				isCAVGroupQuery = checkFilter(query, resourceEndPoint);

				if (!isCAVGroupQuery) {
					q = qIsFilter(query, queryUriSnippet, providerName, resourceEndPoint);
				} else {

					Attribute filterAttr = ((AttributeFilter) query).getAttribute();
					q = (String) filterAttr.getValue().get(0);
					resourceEndPoint = "Users";
				}
			}

		} else {
			LOGGER.info("No filter was defined, query will return all the resource values");
			q = queryUriSnippet.toString();

		}
		HttpClient httpClient = initHttpClient(conf);

		String uri = new StringBuilder(scimBaseUri).append(SLASH).append(resourceEndPoint).append(SLASH).append(q)
				.toString();
		LOGGER.info("Qeury url: {0}", uri);
		HttpGet httpGet = buildHttpGet(uri, authHeader);
		String responseString = null;
		try (CloseableHttpResponse response = (CloseableHttpResponse) httpClient.execute(httpGet)) {
			int statusCode = response.getStatusLine().getStatusCode();
			HttpEntity entity = response.getEntity();

			if (entity != null) {
				responseString = EntityUtils.toString(entity);
			} else {
				responseString = "";
			}
			LOGGER.info("Status code: {0}", statusCode);
			if (statusCode == 200) {

				if (!responseString.isEmpty()) {
					try {
						JSONObject jsonObject = new JSONObject(responseString);

						// LOGGER.info("Json object returned from service
						// provider: {0}", jsonObject.toString(1));
						try {

							if (valueIsUid) {

								ConnectorObject connectorObject = buildConnectorObject(jsonObject, resourceEndPoint);
								resultHandler.handle(connectorObject);

							} else {

								if (isCAVGroupQuery) {

									handleCAVGroupQuery(jsonObject, GROUPS, resultHandler, scimBaseUri, authHeader,
											conf);

								} else if (jsonObject.has(RESOURCES)) {
									int amountOfResources = jsonObject.getJSONArray(RESOURCES).length();
									int totalResults = 0;
									int startIndex = 0;
									int itemsPerPage = 0;

									if (jsonObject.has(STARTINDEX) && jsonObject.has(TOTALRESULTS)
											&& jsonObject.has(ITEMSPERPAGE)) {
										totalResults = (int) jsonObject.get(TOTALRESULTS);
										startIndex = (int) jsonObject.get(STARTINDEX);
										itemsPerPage = (int) jsonObject.get(ITEMSPERPAGE);
									}

									for (int i = 0; i < amountOfResources; i++) {
										JSONObject minResourceJson = new JSONObject();
										minResourceJson = jsonObject.getJSONArray(RESOURCES).getJSONObject(i);
										if (minResourceJson.has(ID) && minResourceJson.getString(ID) != null) {

											if (minResourceJson.has(USERNAME)) {

												ConnectorObject connectorObject = buildConnectorObject(minResourceJson,
														resourceEndPoint);

												resultHandler.handle(connectorObject);
											} else if (!USERS.equals(resourceEndPoint)) {

												if (minResourceJson.has(DISPLAYNAME)) {
													ConnectorObject connectorObject = buildConnectorObject(
															minResourceJson, resourceEndPoint);
													resultHandler.handle(connectorObject);
												}
											} else if (minResourceJson.has(META)) {

												String resourceUri = minResourceJson.getJSONObject(META)
														.getString("location").toString();

												HttpGet httpGetR = buildHttpGet(resourceUri, authHeader);
												try (CloseableHttpResponse resourceResponse = (CloseableHttpResponse) httpClient
														.execute(httpGetR)) {

													statusCode = resourceResponse.getStatusLine().getStatusCode();
													responseString = EntityUtils.toString(resourceResponse.getEntity());
													if (statusCode == 200) {

														JSONObject fullResourcejson = new JSONObject(responseString);

														// LOGGER.info(
														// "The {0}. resource
														// jsonobject which was
														// returned by the
														// service
														// provider: {1}",
														// i + 1,
														// fullResourcejson);

														ConnectorObject connectorObject = buildConnectorObject(
																fullResourcejson, resourceEndPoint);

														resultHandler.handle(connectorObject);

													} else {

														ErrorHandler.onNoSuccess(responseString, statusCode,
																resourceUri);

													}
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

										// LOGGER.info("The number of remaining
										// results: {0}", remainingResult);
										SearchResult searchResult = new SearchResult(DEFAULT, remainingResult,
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
							throw new ConnectorException("Builder error. Error while building connId object.", e);
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

					LOGGER.warn("Service provider response is empty, responce returned on query: {0}", q);
				}
			} else if (statusCode == 401) {

				handleInvalidStatus("while querying for resources. ", responseString, "retrieving an object",
						statusCode);

			} else if (valueIsUid) {
				
				LOGGER.info("Abouth to throw an exception, the resource: {0} was not found.", q);
				
				ErrorHandler.onNoSuccess(responseString, statusCode, uri);

				StringBuilder errorBuilder = new StringBuilder("The resource with the uid: ").append(q)
						.append(" was not found.");

				throw new UnknownUidException(errorBuilder.toString());
			} else if (statusCode == 404) {

			} else {
				ErrorHandler.onNoSuccess(responseString, statusCode, uri);
			}

		} catch (IOException e) {

			if (q == null) {
				q = "the full resource representation";
			}

			StringBuilder errorBuilder = new StringBuilder(
					"An error occurred while processing the query http response for ");
			errorBuilder.append(q);
			if ((e instanceof SocketTimeoutException || e instanceof NoRouteToHostException)) {

				errorBuilder.insert(0, "The connection timed out while closing the http connection. ");

				throw new OperationTimeoutException(errorBuilder.toString(), e);
			} else {

				LOGGER.error(
						"An error occurred while processing the queuery http response. Occurrence while processing the http response to the queuey request for: {1}, exception message: {0}",
						e.getLocalizedMessage(), q);
				LOGGER.info(
						"An error occurred while processing the queuery http response. Occurrence while processing the http response to the queuey request for: {1}, exception message: {0}",
						e, q);
				throw new ConnectorIOException(errorBuilder.toString(), e);
			}
		}
	}

	@Override
	public Uid update(Uid uid, String resourceEndPoint, ObjectTranslator objectTranslator, Set<Attribute> attributes,
			ScimConnectorConfiguration conf) {
		ServiceAccessManager accessManager = new ServiceAccessManager(conf);

		Header authHeader = accessManager.getAuthHeader();
		String scimBaseUri = accessManager.getBaseUri();

		if (authHeader == null || scimBaseUri.isEmpty()) {

			throw new ConnectorException("The data needed for authorization of request to the provider was not found.");
		}

		HttpClient httpClient = initHttpClient(conf);

		String uri = new StringBuilder(scimBaseUri).append(SLASH).append(resourceEndPoint).append(SLASH)
				.append(uid.getUidValue()).toString();
		LOGGER.info("The uri for the update request: {0}", uri);

		String responseString = null;
		try {
			JSONObject jsonObject = objectTranslator.translateSetToJson(attributes, null, resourceEndPoint);
			HttpPatch httpPatch = buildHttpPatch(uri, authHeader, jsonObject);

			try (CloseableHttpResponse response = (CloseableHttpResponse) httpClient.execute(httpPatch)) {

				int statusCode = response.getStatusLine().getStatusCode();

				HttpEntity entity = response.getEntity();

				if (entity != null) {
					responseString = EntityUtils.toString(entity);
				} else {
					responseString = "";
				}
				if (statusCode == 200 || statusCode == 201) {
					LOGGER.info("Update of resource was succesfull");

					if (!responseString.isEmpty()) {
						JSONObject json = new JSONObject(responseString);
						// LOGGER.ok("Json response: {0}", json.toString());
						Uid id = new Uid(json.getString(ID));
						return id;

					} else {
						LOGGER.warn("Service provider response is empty, no response after the update procedure");
					}
				} else if (statusCode == 204) {

					LOGGER.warn("Status code {0}. Response body left intentionally empty", statusCode);

					return uid;
				} else if (statusCode == 404) {

					ErrorHandler.onNoSuccess(responseString, statusCode, uri);

					StringBuilder errorBuilder = new StringBuilder("The resource with the uid: ").append(uid)
							.append(" was not found.");

					throw new UnknownUidException(errorBuilder.toString());

				} else if (statusCode == 500 && GROUPS.equals(resourceEndPoint)) {

					Uid id = groupUpdateProcedure(statusCode, jsonObject, uri, authHeader, conf);

					if (id != null) {

						return id;
					} else {
						ErrorHandler.onNoSuccess(responseString, statusCode, "updating object");
					}
				} else {
					handleInvalidStatus("while updating resource. ", responseString, "updating object", statusCode);
				}
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

			StringBuilder errorBuilder = new StringBuilder(
					"An error has occurred while processing the http response. Occurrence in the process of updating a resource object wit the Uid: ");

			errorBuilder.append(uid.toString());

			if ((e instanceof SocketTimeoutException || e instanceof NoRouteToHostException)) {
				errorBuilder.insert(0, "The connection timed out. ");

				throw new OperationTimeoutException(errorBuilder.toString(), e);
			} else {

				LOGGER.error(
						"An error has occurred while processing the http response. Occurrence in the process of updating a resource object: {0}",
						e.getLocalizedMessage());
				LOGGER.info(
						"An error has occurred while processing the http response. Occurrence in the process of updating a resource object: {0}",
						e);

				throw new ConnectorIOException(errorBuilder.toString(), e);
			}
		}
		return null;

	}

	@Override
	public void delete(Uid uid, String resourceEndPoint, ScimConnectorConfiguration conf) {

		ServiceAccessManager accessManager = new ServiceAccessManager(conf);

		Header authHeader = accessManager.getAuthHeader();
		String scimBaseUri = accessManager.getBaseUri();

		if (authHeader == null || scimBaseUri.isEmpty()) {

			throw new ConnectorException("The data needed for authorization of request to the provider was not found.");
		}

		HttpClient httpClient = initHttpClient(conf);

		String uri = new StringBuilder(scimBaseUri).append(SLASH).append(resourceEndPoint).append(SLASH)
				.append(uid.getUidValue()).toString();

		LOGGER.info("The uri for the delete request: {0}", uri);
		HttpDelete httpDelete = buildHttpDelete(uri, authHeader);

		try (CloseableHttpResponse response = (CloseableHttpResponse) httpClient.execute(httpDelete)) {
			int statusCode = response.getStatusLine().getStatusCode();

			if (statusCode == 204 || statusCode == 200) {
				LOGGER.info("Deletion of resource was succesfull");
			} else {

				String responseString;
				HttpEntity entity = response.getEntity();

				if (entity != null) {
					responseString = EntityUtils.toString(entity);
				} else {
					responseString = "";
				}

					handleInvalidStatus("while deleting resource. ", responseString, "deleting object", statusCode);
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

			StringBuilder errorBuilder = new StringBuilder(
					"An error has occurred while processing the http response. Occurrence in the process of deleting a resource object with the Uid:  ");

			errorBuilder.append(uid.toString());

			if ((e instanceof SocketTimeoutException || e instanceof NoRouteToHostException)) {

				errorBuilder.insert(0, "Connection timed out. ");

				throw new OperationTimeoutException(errorBuilder.toString(), e);
			} else {

				LOGGER.error(
						"An error has occurred while processing the http response. Occurrence in the process of deleting a resource object: : {0}",
						e.getLocalizedMessage());
				LOGGER.info(
						"An error has occurred while processing the http response. Occurrence in the process of deleting a resource object: : {0}",
						e);

				throw new ConnectorIOException(errorBuilder.toString(), e);
			}
		}
	}

	@Override
	public ParserSchemaScim querySchemas(String providerName, String resourceEndPoint,
			ScimConnectorConfiguration conf) {

		ServiceAccessManager accessManager = new ServiceAccessManager(conf);

		Header authHeader = accessManager.getAuthHeader();
		String scimBaseUri = accessManager.getBaseUri();

		if (authHeader == null || scimBaseUri.isEmpty()) {

			throw new ConnectorException("The data needed for authorization of request to the provider was not found.");
		}

		HttpClient httpClient = initHttpClient(conf);

		String uri = new StringBuilder(scimBaseUri).append(SLASH).append(resourceEndPoint).toString();
		LOGGER.info("Qeury url: {0}", uri);
		HttpGet httpGet = buildHttpGet(uri, authHeader);
		try (CloseableHttpResponse response = (CloseableHttpResponse) httpClient.execute(httpGet)) {
			HttpEntity entity = response.getEntity();
			String responseString;
			if (entity != null) {
				responseString = EntityUtils.toString(entity);
			} else {
				responseString = "";
			}

			int statusCode = response.getStatusLine().getStatusCode();
			LOGGER.info("Schema query status code: {0} ", statusCode);
			if (statusCode == 200) {

				if (!responseString.isEmpty()) {

					LOGGER.warn("The returned response string for the \"schemas/\" endpoint");

					JSONObject jsonObject = new JSONObject(responseString);

					ParserSchemaScim schemaParser = processSchemaResponse(jsonObject);
					return schemaParser;

				} else {

					LOGGER.warn("Response string for the \"schemas/\" endpoint returned empty ");

					String resources[] = { USERS, GROUPS };
					JSONObject responseObject = new JSONObject();
					JSONArray responseArray = new JSONArray();
					for (String resourceName : resources) {
						uri = new StringBuilder(scimBaseUri).append(SLASH).append(resourceEndPoint).append(resourceName)
								.toString();
						LOGGER.info("Additional query url: {0}", uri);

						httpGet = buildHttpGet(uri, authHeader);

						try (CloseableHttpResponse secondaryResponse = (CloseableHttpResponse) httpClient
								.execute(httpGet)) {

							statusCode = secondaryResponse.getStatusLine().getStatusCode();
							responseString = EntityUtils.toString(secondaryResponse.getEntity());

							if (statusCode == 200) {
								JSONObject jsonObject = new JSONObject(responseString);
								jsonObject = injectMissingSchemaAttributes(resourceName, jsonObject);

								responseArray.put(jsonObject);
							} else {

								LOGGER.warn(
										"No definition for provided shcemas was found, the connector will switch to default core schema configuration!");
								return null;
							}
						}
						responseObject.put(RESOURCES, responseArray);

					}
					if (responseObject == JSONObject.NULL) {

						return null;

					} else {

						return processSchemaResponse(responseObject);
					}
				}

			} else {

				handleInvalidStatus("while querying for schema. ", responseString, "schema", statusCode);
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

			StringBuilder errorBuilder = new StringBuilder(
					"An error has occurred while processing the http response. Occurrence in the process of querying the provider Schemas resource object");

			if ((e instanceof SocketTimeoutException || e instanceof NoRouteToHostException)) {

				errorBuilder.insert(0, "The connection timed out. ");

				throw new OperationTimeoutException(errorBuilder.toString(), e);
			} else {

				LOGGER.error(
						"An error has occurred while processing the http response. Occurrence in the process of querying the provider Schemas resource object: {0}",
						e.getLocalizedMessage());
				LOGGER.info(
						"An error has occurred while processing the http response. Occurrence in the process of querying the provider Schemas resource object: {0}",
						e);

				throw new ConnectorIOException(errorBuilder.toString(), e);
			}
		}
		return null;
	}

	@Override
	public JSONObject injectMissingSchemaAttributes(String resourceName, JSONObject jsonObject) {
		return jsonObject;
	}

	@Override
	public ParserSchemaScim processSchemaResponse(JSONObject responseObject) {

		// LOGGER.info("The resources json representation: {0}",
		// responseObject.toString(1));
		ParserSchemaScim scimParser = new ParserSchemaScim();
		for (int i = 0; i < responseObject.getJSONArray(RESOURCES).length(); i++) {
			JSONObject minResourceJson = new JSONObject();
			minResourceJson = responseObject.getJSONArray(RESOURCES).getJSONObject(i);

			if (minResourceJson.has("endpoint")) {
				scimParser.parseSchema(minResourceJson, this);

			} else {
				LOGGER.error("No endpoint identifier present in fetched object: {0}", minResourceJson);

				throw new ConnectorException(
						"No endpoint identifier present in fetched object while processing queuery result");
			}
		}
		return scimParser;

	}

	@Override
	public Map<String, Map<String, Object>> parseSchemaAttribute(JSONObject attribute,
			Map<String, Map<String, Object>> attributeMap, ParserSchemaScim parser) {

		String attributeName = null;
		Boolean isComplex = false;
		Boolean isMultiValued = false;
		Boolean hasSubAttributes = false;
		String nameFromDictionary = "";
		Map<String, Object> attributeObjects = new HashMap<String, Object>();
		Map<String, Object> subAttributeMap = new HashMap<String, Object>();

		List<String> dictionary = populateDictionary(WorkaroundFlags.PARSERFLAG);
		List<String> excludedAttributes = defineExcludedAttributes();
		for (int position = 0; position < dictionary.size(); position++) {
			nameFromDictionary = dictionary.get(position);

			if (attribute.has(nameFromDictionary)) {

				hasSubAttributes = true;
				break;
			}

		}
		if (hasSubAttributes) {

			boolean hasTypeValues = false;
			JSONArray subAttributes = new JSONArray();
			subAttributes = (JSONArray) attribute.get(nameFromDictionary);
			if (attributeName == null) {
				for (String subAttributeNameKeys : attribute.keySet()) {
					if (NAME.equals(subAttributeNameKeys)) {
						attributeName = attribute.get(subAttributeNameKeys).toString();

						if (attributeName.contains(FORBIDENSEPPARATOR)) {
							attributeName = attributeName.replace(FORBIDENSEPPARATOR, SEPPARATOR);
						}

						break;
					}
				}
			}

			if (!excludedAttributes.contains(attributeName)) {

				for (String nameKey : attribute.keySet()) {
					if (MULTIVALUED.equals(nameKey)) {
						isMultiValued = (Boolean) attribute.get(nameKey);
						break;
					}
				}

				for (int i = 0; i < subAttributes.length(); i++) {
					JSONObject subAttribute = new JSONObject();
					subAttribute = subAttributes.getJSONObject(i);
					subAttributeMap = parser.parseSubAttribute(subAttribute, subAttributeMap);
				}
				for (String typeKey : subAttributeMap.keySet()) {
					if (TYPE.equals(typeKey)) {
						hasTypeValues = true;
						break;
					}
				}

				if (hasTypeValues) {
					Map<String, Object> typeObject = new HashMap<String, Object>();
					typeObject = (HashMap<String, Object>) subAttributeMap.get(TYPE);
					if (typeObject.containsKey(CANONICALVALUES) || typeObject.containsKey(REFERENCETYPES)) {
						JSONArray referenceValues = new JSONArray();
						if (typeObject.containsKey(CANONICALVALUES)) {
							referenceValues = (JSONArray) typeObject.get(CANONICALVALUES);
						} else {
							referenceValues = (JSONArray) typeObject.get(REFERENCETYPES);
						}

						for (int position = 0; position < referenceValues.length(); position++) {

							Map<String, Object> processedParameters = translateReferenceValues(attributeMap,
									referenceValues, subAttributeMap, position, attributeName);

							for (String parameterName : processedParameters.keySet()) {
								if (ISCOMPLEX.equals(parameterName)) {

									isComplex = (Boolean) processedParameters.get(parameterName);

								} else {
									attributeMap = (Map<String, Map<String, Object>>) processedParameters
											.get(parameterName);
								}

							}

						}
					} else {
						// default set of canonical values.

						List<String> defaultReferenceTypeValues = new ArrayList<String>();
						defaultReferenceTypeValues.add("User");
						defaultReferenceTypeValues.add("Group");

						defaultReferenceTypeValues.add("external");
						defaultReferenceTypeValues.add(URI);

						for (String subAttributeKeyNames : subAttributeMap.keySet()) {
							if (!TYPE.equals(subAttributeKeyNames)) {
								for (String defaultTypeReferenceValues : defaultReferenceTypeValues) {
									StringBuilder complexAttrName = new StringBuilder(attributeName);
									complexAttrName.append(DOT).append(defaultTypeReferenceValues);
									attributeMap.put(
											complexAttrName.append(DOT).append(subAttributeKeyNames).toString(),
											(HashMap<String, Object>) subAttributeMap.get(subAttributeKeyNames));
									isComplex = true;
								}
							}
						}
					}
				} else {

					if (!isMultiValued) {
						for (String subAttributeKeyNames : subAttributeMap.keySet()) {
							StringBuilder complexAttrName = new StringBuilder(attributeName);
							attributeMap.put(complexAttrName.append(DOT).append(subAttributeKeyNames).toString(),
									(HashMap<String, Object>) subAttributeMap.get(subAttributeKeyNames));
							isComplex = true;
						}
					} else {
						for (String subAttributeKeyNames : subAttributeMap.keySet()) {
							StringBuilder complexAttrName = new StringBuilder(attributeName);

							Map<String, Object> subattributeKeyMap = (HashMap<String, Object>) subAttributeMap
									.get(subAttributeKeyNames);

							for (String attributeProperty : subattributeKeyMap.keySet()) {

								if (MULTIVALUED.equals(attributeProperty)) {
									subattributeKeyMap.put(MULTIVALUED, true);
								}
							}
							attributeMap.put(complexAttrName.append(DOT).append(DEFAULT).append(DOT)
									.append(subAttributeKeyNames).toString(), subattributeKeyMap);
							isComplex = true;
						}
					}
				}
			}
		} else {

			for (String attributeNameKeys : attribute.keySet()) {
				if (!excludedAttributes.contains(attributeName)) {
					if (NAME.equals(attributeNameKeys)) {
						attributeName = attribute.get(attributeNameKeys).toString();
						if (attributeName.contains(FORBIDENSEPPARATOR)) {
							attributeName = attributeName.replace(FORBIDENSEPPARATOR, SEPPARATOR);
						}
					} else {
						attributeObjects.put(attributeNameKeys, attribute.get(attributeNameKeys));
					}
				} else {
					if (!attributeObjects.isEmpty()) {

						attributeObjects.clear();
					}
				}
			}
		}
		if (!isComplex) {
			if (!attributeObjects.isEmpty()) {
				attributeMap.put(attributeName, attributeObjects);
			}
		}
		return attributeMap;
	}

	@Override
	public List<Map<String, Map<String, Object>>> getAttributeMapList(
			List<Map<String, Map<String, Object>>> attributeMapList) {
		return attributeMapList;
	}

	@Override
	public Map<String, Object> translateReferenceValues(Map<String, Map<String, Object>> attributeMap,
			JSONArray referenceValues, Map<String, Object> subAttributeMap, int position, String attributeName) {

		Boolean isComplex = null;
		Map<String, Object> processedParameters = new HashMap<String, Object>();

		String sringReferenceValue = (String) referenceValues.get(position);

		for (String subAttributeKeyNames : subAttributeMap.keySet()) {
			if (!TYPE.equals(subAttributeKeyNames)) {

				StringBuilder complexAttrName = new StringBuilder(attributeName);
				attributeMap.put(complexAttrName.append(DOT).append(sringReferenceValue).append(DOT)
						.append(subAttributeKeyNames).toString(),
						(HashMap<String, Object>) subAttributeMap.get(subAttributeKeyNames));
				isComplex = true;

			}
		}
		if (isComplex != null) {
			processedParameters.put(ISCOMPLEX, isComplex);
		}
		processedParameters.put("attributeMap", attributeMap);

		return processedParameters;
	}

	@Override
	public List<String> defineExcludedAttributes() {

		List<String> excludedList = new ArrayList<String>();

		return excludedList;
	}

	@Override
	public Set<Attribute> addAttributesToInject(Set<Attribute> injectetAttributeSet) {
		return injectetAttributeSet;
	}

	@Override
	public Uid groupUpdateProcedure(Integer statusCode, JSONObject jsonObject, String uri, Header authHeader,
			ScimConnectorConfiguration conf) {
		return null;
	}

	@Override
	public ConnectorObject buildConnectorObject(JSONObject resourceJsonObject, String resourceEndPoint)
			throws ConnectorException {

		List<String> excludedAttributes = new ArrayList<String>();

		LOGGER.info("Building the connector object from provided json");

		if (resourceJsonObject == null) {
			LOGGER.error(
					"Empty json object was passed from data provider. Error ocourance while building connector object");
			throw new ConnectorException(
					"Empty json object was passed from data provider. Error ocourance while building connector object");
		}

		ConnectorObjectBuilder cob = new ConnectorObjectBuilder();
		cob.setUid(resourceJsonObject.getString(ID));
		excludedAttributes.add(ID);
		if (USERS.equals(resourceEndPoint)) {
			cob.setName(resourceJsonObject.getString(USERNAME));
			excludedAttributes.add(USERNAME);
		} else if (GROUPS.equals(resourceEndPoint)) {

			cob.setName(resourceJsonObject.getString(DISPLAYNAME));
			excludedAttributes.add(DISPLAYNAME);
			cob.setObjectClass(ObjectClass.GROUP);
		} else {
			cob.setName(resourceJsonObject.getString(DISPLAYNAME));
			excludedAttributes.add(DISPLAYNAME);
			ObjectClass objectClass = new ObjectClass(resourceEndPoint);
			cob.setObjectClass(objectClass);

		}
		for (String key : resourceJsonObject.keySet()) {
			Object attribute = resourceJsonObject.get(key);

			excludedAttributes = excludeFromAssembly(excludedAttributes);

			if (excludedAttributes.contains(key)) {
				LOGGER.warn("The attribute \"{0}\" was omitted from the connId object build.", key);
			} else

			if (attribute instanceof JSONArray) {

				JSONArray attributeArray = (JSONArray) attribute;

				Map<String, Collection<Object>> multivaluedAttributeMap = new HashMap<String, Collection<Object>>();
				Collection<Object> attributeValues = new ArrayList<Object>();

				for (Object singleAttribute : attributeArray) {
					StringBuilder objectNameBilder = new StringBuilder(key);
					String objectKeyName = "";
					if (singleAttribute instanceof JSONObject) {
						for (String singleSubAttribute : ((JSONObject) singleAttribute).keySet()) {
							if (TYPE.equals(singleSubAttribute)) {
								objectKeyName = objectNameBilder.append(DOT)
										.append(((JSONObject) singleAttribute).get(singleSubAttribute)).toString();
								objectNameBilder.delete(0, objectNameBilder.length());
								break;
							}
						}

						for (String singleSubAttribute : ((JSONObject) singleAttribute).keySet()) {
							Object sAttributeValue;
							if (((JSONObject) singleAttribute).isNull(singleSubAttribute)) {
								sAttributeValue = null;
							} else {

								sAttributeValue = ((JSONObject) singleAttribute).get(singleSubAttribute);
							}

							if (TYPE.equals(singleSubAttribute)) {
							} else {

								if (!"".equals(objectKeyName)) {
									objectNameBilder = objectNameBilder.append(objectKeyName).append(DOT)
											.append(singleSubAttribute);
								} else {
									objectKeyName = objectNameBilder.append(DOT).append(DEFAULT).toString();
									objectNameBilder = objectNameBilder.append(DOT).append(singleSubAttribute);
								}

								if (attributeValues.isEmpty()) {

									attributeValues.add(sAttributeValue);
									multivaluedAttributeMap.put(objectNameBilder.toString(), attributeValues);
								} else {
									if (multivaluedAttributeMap.containsKey(objectNameBilder.toString())) {
										attributeValues = multivaluedAttributeMap.get(objectNameBilder.toString());
										attributeValues.add(sAttributeValue);
									} else {
										Collection<Object> newAttributeValues = new ArrayList<Object>();
										newAttributeValues.add(sAttributeValue);
										multivaluedAttributeMap.put(objectNameBilder.toString(), newAttributeValues);
									}

								}
								objectNameBilder.delete(0, objectNameBilder.length());

							}
						}
					} else {
						objectKeyName = objectNameBilder.append(DOT).append(singleAttribute.toString()).toString();
						cob.addAttribute(objectKeyName, singleAttribute);
					}
				}

				if (!multivaluedAttributeMap.isEmpty()) {
					for (String attributeName : multivaluedAttributeMap.keySet()) {
						cob.addAttribute(attributeName, multivaluedAttributeMap.get(attributeName));
					}

				}

			} else if (attribute instanceof JSONObject) {
				for (String s : ((JSONObject) attribute).keySet()) {
					Object attributeValue;
					if (key.contains(FORBIDENSEPPARATOR)) {
						key = key.replace(FORBIDENSEPPARATOR, SEPPARATOR);
					}

					if (((JSONObject) attribute).isNull(s)) {

						attributeValue = null;

					} else {

						attributeValue = ((JSONObject) attribute).get(s);

					}

					StringBuilder objectNameBilder = new StringBuilder(key);
					cob.addAttribute(objectNameBilder.append(DOT).append(s).toString(), attributeValue);
				}

			} else {

				if (ACTIVE.equals(key)) {
					cob.addAttribute(OperationalAttributes.ENABLE_NAME, resourceJsonObject.get(key));
				} else {

					if (!resourceJsonObject.isNull(key)) {

						cob.addAttribute(key, resourceJsonObject.get(key));
					} else {
						Object value = null;
						cob.addAttribute(key, value);

					}
				}
			}
		}
		ConnectorObject finalConnectorObject = cob.build();
		// LOGGER.info("The connector object returned from the processed json:
		// {0}", finalConnectorObject);
		return finalConnectorObject;

	}

	@Override
	public List<String> excludeFromAssembly(List<String> excludedAttributes) {

		excludedAttributes.add(META);
		excludedAttributes.add("schemas");

		return excludedAttributes;
	}

	@Override
	public Set<Attribute> attributeInjection(Set<Attribute> injectedAttributeSet, JSONObject loginJson) {
		return injectedAttributeSet;
	}

	@Override
	public StringBuilder processContainsAllValuesFilter(String p, ContainsAllValuesFilter filter,
			FilterHandler handler) {
		StringBuilder preprocessedFilter = null;
		preprocessedFilter = handler.processArrayQ(filter, p);
		return preprocessedFilter;
	}

	@Override
	public ObjectClassInfoBuilder schemaBuilder(String attributeName, Map<String, Map<String, Object>> attributeMap,
			ObjectClassInfoBuilder builder, SchemaObjectBuilderGeneric schemaBuilder) {

		AttributeInfoBuilder infoBuilder = new AttributeInfoBuilder(attributeName);
		Boolean containsDictionaryValue = false;
		Map<String, Object> caseHandlingMap = new HashMap<String, Object>();

		List<String> dictionary = populateDictionary(WorkaroundFlags.BUILDERFLAG);

		if (dictionary.contains(attributeName)) {
			containsDictionaryValue = true;
		}

		if (!containsDictionaryValue) {
			dictionary.clear();
			Map<String, Object> schemaSubPropertysMap = new HashMap<String, Object>();
			schemaSubPropertysMap = attributeMap.get(attributeName);

			for (String subPropertyName : schemaSubPropertysMap.keySet()) {
				containsDictionaryValue = false;
				dictionary = populateDictionary(WorkaroundFlags.PARSERFLAG);
				if (dictionary.contains(subPropertyName)) {
					containsDictionaryValue = true;
				}

				if (containsDictionaryValue) {
					// TODO check positive cases
					infoBuilder = new AttributeInfoBuilder(attributeName);
					JSONArray jsonArray = new JSONArray();

					jsonArray = ((JSONArray) schemaSubPropertysMap.get(subPropertyName));
					for (int i = 0; i < jsonArray.length(); i++) {
						JSONObject attribute = new JSONObject();
						attribute = jsonArray.getJSONObject(i);
					}
					break;
				} else {

					if ("type".equals(subPropertyName)) {

						if ("string".equals(schemaSubPropertysMap.get(subPropertyName).toString())) {

							caseHandlingMap.put("type", "string");

						} else if ("boolean".equals(schemaSubPropertysMap.get(subPropertyName).toString())) {

							caseHandlingMap.put("type", "bool");
							infoBuilder.setType(Boolean.class);
						}
					} else if ("caseExact".equals(subPropertyName)) {

						caseHandlingMap.put("caseExact", (Boolean) schemaSubPropertysMap.get(subPropertyName));
					}

					infoBuilder = schemaBuilder.subPropertiesChecker(infoBuilder, schemaSubPropertysMap,
							subPropertyName);
					infoBuilder = schemaObjectParametersInjection(infoBuilder, attributeName);

				}

			}
			if (!caseHandlingMap.isEmpty()) {
				if (caseHandlingMap.containsKey("type")) {
					if ("string".equals(caseHandlingMap.get("type"))) {
						infoBuilder.setType(String.class);
						if (caseHandlingMap.containsKey("caseExact")) {
							if (!(Boolean) caseHandlingMap.get("caseExact")) {
								infoBuilder.setSubtype(AttributeInfo.Subtypes.STRING_CASE_IGNORE);
							}
						}
					} else if ("boolean".equals(caseHandlingMap.get("type"))) {
						infoBuilder.setType(Boolean.class);
					}

				}

			}

			builder.addAttributeInfo(infoBuilder.build());
		} else {
			builder = schemaObjectInjection(builder, attributeName, infoBuilder);
		}
		return builder;
	}

	@Override
	public ObjectClassInfoBuilder schemaObjectInjection(ObjectClassInfoBuilder builder, String attributeName,
			AttributeInfoBuilder infoBuilder) {

		builder.addAttributeInfo(OperationalAttributeInfos.ENABLE);
		builder.addAttributeInfo(OperationalAttributeInfos.PASSWORD);

		return builder;
	}

	@Override
	public AttributeInfoBuilder schemaObjectParametersInjection(AttributeInfoBuilder infoBuilder,
			String attributeName) {
		return infoBuilder;
	}

	@Override
	public List<String> populateDictionary(WorkaroundFlags flag) {

		List<String> dictionary = new ArrayList<String>();

		if (WorkaroundFlags.PARSERFLAG.getValue().equals(flag.getValue())) {
			dictionary.add(SUBATTRIBUTES);
		} else if (WorkaroundFlags.BUILDERFLAG.getValue().equals(flag.getValue())) {

			dictionary.add(ACTIVE);
		} else {

			LOGGER.warn("No such flag defined: {0}", flag);
		}
		return dictionary;

	}

	@Override
	public Boolean checkFilter(Filter filter, String endpointName) {
		LOGGER.info("Check filter standard");
		return false;
	}

	/**
	 * Called when the query is evaluated as an filter not containing an uid
	 * type attribute.
	 * 
	 * @param endPoint
	 *            The name of the endpoint which should be queried (e.g.
	 *            "Users").
	 * @param query
	 *            The provided filter query.
	 * @param resultHandler
	 *            The provided result handler used to handle the query result.
	 * @param queryUriSnippet
	 *            A part of the query uri which will build a larger query.
	 */

	private String qIsFilter(Filter query, StringBuilder queryUriSnippet, String providerName,
			String resourceEndPoint) {

		char prefixChar;
		String attributeName = "";
		StringBuilder filterSnippet = new StringBuilder();
		if (queryUriSnippet.toString().isEmpty()) {
			prefixChar = QUERYCHAR;

		} else {

			prefixChar = QUERYDELIMITER;
		}

		if (query instanceof AttributeFilter) {

			attributeName = ((AttributeFilter) query).getName();

			if ("__NAME__".equals(attributeName)) {

				if (USERS.equals(resourceEndPoint)) {
					attributeName = "userName";
				} else {

					attributeName = "displayName";
				}

			} else {
				attributeName = "";

			}

		}

		if (!attributeName.isEmpty()) {

			filterSnippet = query.accept(new FilterHandler(), attributeName);

		} else {

			filterSnippet = query.accept(new FilterHandler(), providerName);

		}
		queryUriSnippet.append(prefixChar).append("filter=").append(filterSnippet.toString());

		return queryUriSnippet.toString();
	}

	@Override
	public void handleCAVGroupQuery(JSONObject jsonObject, String resourceEndPoint, ResultsHandler handler,
			String scimBaseUri, Header authHeader, ScimConnectorConfiguration conf)
			throws ClientProtocolException, IOException {

		ConnectorObject connectorObject = buildConnectorObject(jsonObject, resourceEndPoint);

		handler.handle(connectorObject);

	}

	protected HttpPost buildHttpPost(String uri, Header authHeader, JSONObject jsonBody)
			throws UnsupportedEncodingException, JSONException {

		HttpPost httpPost = new HttpPost(uri);
		httpPost.addHeader(authHeader);
		httpPost.addHeader(PRETTYPRINTHEADER);

		HttpEntity entity = new ByteArrayEntity(jsonBody.toString().getBytes("UTF-8"));
		// LOGGER.info("The update JSON object wich is being sent: {0}",
		// jsonBody);
		httpPost.setEntity(entity);
		httpPost.setHeader("Content-Type", CONTENTTYPE);

		// StringEntity bodyContent = new StringEntity(jsonBody.toString(1));

		// bodyContent.setContentType(CONTENTTYPE);
		// httpPost.setEntity(bodyContent);

		return httpPost;
	}

	protected HttpGet buildHttpGet(String uri, Header authHeader) {

		HttpGet httpGet = new HttpGet(uri);
		httpGet.addHeader(authHeader);
		httpGet.addHeader(PRETTYPRINTHEADER);

		return httpGet;
	}

	protected HttpPatch buildHttpPatch(String uri, Header authHeader, JSONObject jsonBody)
			throws UnsupportedEncodingException, JSONException {

		HttpPatch httpPatch = new HttpPatch(uri);

		httpPatch.addHeader(authHeader);
		httpPatch.addHeader(PRETTYPRINTHEADER);
		HttpEntity entity = new ByteArrayEntity(jsonBody.toString().getBytes("UTF-8"));
		// LOGGER.info("The update JSON object wich is being sent: {0}",
		// jsonBody);
		httpPatch.setEntity(entity);
		// StringEntity bodyContent = new StringEntity(jsonBody.toString(1));

		// bodyContent.setContentType(CONTENTTYPE);
		// httpPatch.setEntity(bodyContent);

		httpPatch.setHeader("Content-Type", CONTENTTYPE);

		return httpPatch;
	}

	protected HttpDelete buildHttpDelete(String uri, Header authHeader) {

		HttpDelete httpDelete = new HttpDelete(uri);
		httpDelete.addHeader(authHeader);
		httpDelete.addHeader(PRETTYPRINTHEADER);
		return httpDelete;
	}

	public void handleInvalidStatus(String errorPitch, String responseString, String situation, int statusCode)
			throws ParseException, IOException {

		String error = ErrorHandler.onNoSuccess(responseString, statusCode, situation);
		StringBuilder errorString = new StringBuilder(errorPitch).append(error);
		switch (statusCode) {
		case 400:
			handleBadRequest(error);
			break;
		case 401:
			errorString.insert(0, "Unauthorized ");
			throw new InvalidCredentialException(errorString.toString());
		case 404:
			LOGGER.warn("Resource not found or resource was already deleted");
			break;
		case 409:
			errorString.insert(0, "Conflict ");
			throw new AlreadyExistsException(errorString.toString());
		case 500:
			errorString.insert(0, "Provider server error ");
			throw new ConnectorException(errorString.toString());
		default:
			LOGGER.warn(error);
			break;
		}
	}

	public void handleBadRequest(String error) {

		throw new ConnectorException(error);
	}

	protected HttpClient initHttpClient(ScimConnectorConfiguration conf) {
		HttpClientBuilder httpClientBulder = HttpClientBuilder.create();

		if (StringUtil.isNotEmpty(conf.getProxyUrl())) {
			HttpHost proxy = new HttpHost(conf.getProxyUrl(), conf.getProxyPortNumber());
			DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);
			httpClientBulder.setRoutePlanner(routePlanner);
		}

		HttpClient httpClient = httpClientBulder.build();

		return httpClient;
	}

}
