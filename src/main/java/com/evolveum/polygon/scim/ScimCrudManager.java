package com.evolveum.polygon.scim;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.identityconnectors.framework.common.objects.SearchResult;
import org.apache.http.Header;
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
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.ConnectionFailedException;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.ConnectorIOException;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.ObjectClassInfoBuilder;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.spi.SearchResultsHandler;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.evolveum.polygon.scim.common.HttpPatch;

public class ScimCrudManager {

	private String scimBaseUri;
	private Header oauthHeader;
	private Header prettyPrintHeader = new BasicHeader("X-PrettyPrint", "1");
	private ScimConnectorConfiguration conf;
	
	long providerStartTime;
	long providerEndTime;
	long providerDuration;
	
	long operationStartTime;
	long operationEndTime;
	long operationDuration;
	
	
	HttpPost loginInstance;

	private static final Log LOGGER = Log.getLog(ScimCrudManager.class);

	public ScimCrudManager(ScimConnectorConfiguration conf) {
		this.conf = (ScimConnectorConfiguration) conf;
	}

	private String logIntoService() {

		String orgID = null;
		HttpClient httpclient = HttpClientBuilder.create().build();

		String loginURL = new StringBuilder(conf.getLoginURL()).append(conf.getService()).toString();
		String uri = new StringBuilder("&client_id=").append(conf.getClientID()).append("&client_secret=")
				.append(conf.getClientSecret()).append("&username=").append(conf.getUserName()).append("&password=")
				.append(conf.getPassword()).toString();

		loginInstance = new HttpPost(loginURL);
		HttpResponse response = null;

		StringEntity bodyContent;
		try {
			bodyContent = new StringEntity(uri);
			bodyContent.setContentType("application/x-www-form-urlencoded");
			loginInstance.setEntity(bodyContent);

		} catch (UnsupportedEncodingException e1) {
			LOGGER.error("Unsupported encoding: {0}. Ocourance in the process of login into the service",
					e1.getLocalizedMessage());
			LOGGER.info("Unsupported encoding: {0}. Ocourance in the process of login into the service", e1);

			throw new ConnectorException("Unsupported encoding. Ocourance in the process of login into the service",
					e1);
		}

		try {
			 providerStartTime = System.nanoTime();
			 response = httpclient.execute(loginInstance);
			 providerEndTime = System.nanoTime();
			 providerDuration = (providerEndTime - providerStartTime)/1000000;
			 LOGGER.info("The amouth of time it took to get the response to the login query from the provider : {0} milliseconds", providerDuration);
			 providerDuration = 0;
			 
		} catch (ClientProtocolException e) {

			LOGGER.error(
					"An protocol exception has ocoured while processing the http response to the login request. Possible mismatch in interpretation of the HTTP specification: {0}",
					e.getLocalizedMessage());
			LOGGER.info(
					"An protocol exception has ocoured while processing the http response to the login request. Possible mismatch in interpretation of the HTTP specification: {0}",
					e);

			throw new ConnectionFailedException(
					"An protocol exception has ocoured while processing the http response to the login request. Possible mismatch in interpretation of the HTTP specification",
					e);

		} catch (IOException ioException) {

			LOGGER.error("An error ocoured while processing the queuery http response to the login request : {0}",
					ioException.getLocalizedMessage());
			LOGGER.info("An error ocoured while processing the queuery http response to the login request : {0}",
					ioException);
			throw new ConnectorIOException(
					"An error ocoured while processing the queuery http response to the login request", ioException);
		}

		final int statusCode = response.getStatusLine().getStatusCode();
		if (statusCode != HttpStatus.SC_OK) {
			LOGGER.error("Error with authenticating : {0}", statusCode);
			try {
				LOGGER.error("Error cause: {0}", EntityUtils.toString(response.getEntity()));
			} catch (ParseException | IOException e) {

				LOGGER.error("An exception has ocoured while parsing the http response to the login request: {0}",
						e.getLocalizedMessage());
				LOGGER.info("An exception has ocoured while parsing the http response to the login request: {0}", e);
				throw new ConnectorIOException(
						"An exception has ocoured while parsing the http response to the login request.", e);
			}
		}

		String getResult = null;
		try {
			getResult = EntityUtils.toString(response.getEntity());
		} catch (IOException ioException) {

			LOGGER.error("An exception has ocoured while parsing the http response to the login request: {0}",
					ioException.getLocalizedMessage());
			LOGGER.info("An exception has ocoured while parsing the http response to the login request: {0}",
					ioException);
			throw new ConnectorIOException(
					"An exception has ocoured while parsing the http response to the login request", ioException);
		}
		JSONObject jsonObject = null;
		String loginAccessToken = null;
		String loginInstanceUrl = null;
		try {

			jsonObject = (JSONObject) new JSONTokener(getResult).nextValue();
			if (jsonObject.has("id")) {
				orgID = jsonObject.getString("id");
				String idParts[] = orgID.split("\\/");
				orgID = idParts[4];
			}
			loginAccessToken = jsonObject.getString("access_token");
			loginInstanceUrl = jsonObject.getString("instance_url");
		} catch (JSONException jsonException) {

			LOGGER.error(
					"An exception has ocoured while setting the \"jsonObject\". Ocourance while processing the http response to the login request: {0}",
					jsonException.getLocalizedMessage());
			LOGGER.info(
					"An exception has ocoured while setting the \"jsonObject\". Ocourance while processing the http response to the login request: {0}",
					jsonException);
			throw new ConnectorException("An exception has ocoured while setting the \"jsonObject\".", jsonException);
		}
		scimBaseUri = new StringBuilder(loginInstanceUrl).append(conf.getEndpoint()).append(conf.getVersion())
				.toString();
		oauthHeader = new BasicHeader("Authorization", "OAuth " + loginAccessToken);
		LOGGER.info("Login Successful");

		return orgID;
	}

	public void qeueryEntity(Object queuery, String resourceEndPoint, ResultsHandler resultHandler) {
		logIntoService();
		HttpClient httpClient = HttpClientBuilder.create().build();
		String q;
		if (queuery instanceof Uid) {

			q = ((Uid) queuery).getUidValue();
		} else {

			q = (String) queuery;
		}

		String uri = new StringBuilder(scimBaseUri).append("/").append(resourceEndPoint).append("/").append(q)
				.toString();
		LOGGER.info("qeury url: {0}", uri);
		HttpGet httpGet = new HttpGet(uri);
		httpGet.addHeader(oauthHeader);
		httpGet.addHeader(prettyPrintHeader);

		String responseString = null;
		HttpResponse response;
		try {
			providerStartTime = System.nanoTime();
			response = httpClient.execute(httpGet);
			providerEndTime= System.nanoTime();
			providerDuration =(providerEndTime - providerStartTime)/1000000;
			
			LOGGER.info("The amouth of time it took to get the response to the query from the provider : {0} milliseconds ", providerDuration);
			
			providerDuration = 0;
			
			int statusCode = response.getStatusLine().getStatusCode();
			LOGGER.info("status code: {0}", statusCode);
			if (statusCode == 200) {

				responseString = EntityUtils.toString(response.getEntity());
				if (!responseString.isEmpty()) {
					try {
						JSONObject jsonObject = new JSONObject(responseString);

						LOGGER.info("Json object returned from service provider: {0}", jsonObject);
						try {
							if (queuery instanceof Uid) {
								loginInstance.releaseConnection();
								ConnectorObjBuilder objBuilder = new ConnectorObjBuilder();
								resultHandler.handle(objBuilder.buildConnectorObject(jsonObject, resourceEndPoint));

							} else { 
								if (jsonObject.has("Resources")){
									int amountOfResources = jsonObject.getJSONArray("Resources").length();
									int totalResults = 0;
									int startIndex = 0;
									int itemsPerPage = 0;
									
									if (jsonObject.has("startIndex") && jsonObject.has("totalResults")&& jsonObject.has("itemsPerPage")){
										totalResults = (int)jsonObject.get("totalResults");
										startIndex = (int)jsonObject.get("startIndex");
										itemsPerPage = (int)jsonObject.get("itemsPerPage");
									}
									
								for (int i = 0; i < amountOfResources; i++) {
									JSONObject minResourceJson = new JSONObject();
									minResourceJson = jsonObject.getJSONArray("Resources").getJSONObject(i);
									if (minResourceJson.has("id") && minResourceJson.getString("id") != null) {

										if (minResourceJson.has("meta")) {

											String resourceUri = minResourceJson.getJSONObject("meta")
													.getString("location").toString();
											HttpGet httpGetR = new HttpGet(resourceUri);
											httpGetR.addHeader(oauthHeader);
											httpGetR.addHeader(prettyPrintHeader);

											providerStartTime = System.nanoTime();
											HttpResponse resourceResponse = httpClient.execute(httpGetR);
											providerEndTime= System.nanoTime();
											providerDuration =(providerEndTime - providerStartTime )/1000000;
											
											LOGGER.info("The amouth of time it took to get the response to the query from the provider : {0} milliseconds ", providerDuration);
											
											
											statusCode = resourceResponse.getStatusLine().getStatusCode();

											if (statusCode == 200) {
												responseString = EntityUtils.toString(resourceResponse.getEntity());
												JSONObject fullResourcejson = new JSONObject(responseString);

												LOGGER.info(
														"The {0}. resource json object which was returned by the service provider: {1}",
														i + 1, fullResourcejson);

												ConnectorObjBuilder objBuilder = new ConnectorObjBuilder();

												ConnectorObject conOb = objBuilder
														.buildConnectorObject(fullResourcejson, resourceEndPoint);
												
												resultHandler.handle(conOb);
												
												
											} else {
												loginInstance.releaseConnection();
												LOGGER.info("Connection released");
												onNoSuccess(resourceResponse, statusCode, responseString, resourceUri);
											}

										}
									} else {
										LOGGER.error("No uid present in fetched object: {0}", minResourceJson);

										throw new ConnectorException(
												"No uid present in fetchet object while processing queuery result");

									}
								}
								if (resultHandler instanceof SearchResultsHandler){
										Boolean allResultsReturned = false;
										int remainingResult = totalResults - (startIndex-1+itemsPerPage);
										
										if (remainingResult == 0){
											allResultsReturned = true;
											
										}
									
										LOGGER.info("The number of remaining results: {0}", remainingResult);
										SearchResult searchResult = new SearchResult("default", remainingResult, allResultsReturned);
										((SearchResultsHandler)resultHandler).handleResult(searchResult);
									}
								
								
								}else {
									
									LOGGER.error("Resource object not present in provider response to the query");

									throw new ConnectorException(
											"No uid present in fetchet object while processing queuery result");
									
								}
								loginInstance.releaseConnection();
							}

						} catch (Exception e) {
							LOGGER.error("Builder error. Error while building connId object. The excetion message: {0}",
									e.getLocalizedMessage());
							LOGGER.info("Builder error. Error while building connId object. The excetion message: {0}",
									e);
							throw new ConnectorException(e);
						} finally {
							loginInstance.releaseConnection();
							LOGGER.info("Connection released");
						}

						LOGGER.info("Json response: {0}", jsonObject.toString(1));

					} catch (JSONException jsonException) {
						if (q == null) {
							q = "the full resource representation";
						}
						LOGGER.error(
								"An exception has ocoured while setting the variable \"jsonObject\". Ocourance while processing the http response to the queuey request for: {1}, exception message: {0}",
								jsonException.getLocalizedMessage(), q);
						LOGGER.info(
								"An exception has ocoured while setting the variable \"jsonObject\". Ocourance while processing the http response to the queuey request for: {1}, exception message: {0}",
								jsonException, q);
						throw new ConnectorException(
								"An exception has ocoured while setting the variable \"jsonObject\".", jsonException);
					} finally {
						loginInstance.releaseConnection();
						LOGGER.info("Connection released");
					}

				} else {
					loginInstance.releaseConnection();
					LOGGER.error("Service provider response is empty, responce returned on queuery: {0}", queuery);
					throw new ConnectorException(
							"No resources returned for the selected criteria. Please change list or search criteria.");

				}
			} else {
				loginInstance.releaseConnection();
				LOGGER.info("Connection released");
				onNoSuccess(response, statusCode, responseString, uri);
			}

		} catch (IOException e) {

			if (q == null) {
				q = "the full resource representation";
			}

			LOGGER.error(
					"An error ocoured while processing the queuery http response. Ocourance while processing the http response to the queuey request for: {1}, exception message: {0}",
					e.getLocalizedMessage(), q);
			LOGGER.info(
					"An error ocoured while processing the queuery http response. Ocourance while processing the http response to the queuey request for: {1}, exception message: {0}",
					e, q);
			throw new ConnectorIOException("An error ocoured while processing the queuery http response.", e);
		} finally {
			loginInstance.releaseConnection();
			LOGGER.info("Connection released");
		}

		loginInstance.releaseConnection();
		LOGGER.info("Connection released");
	}

	public ScimSchemaParser qeueryEntity(Object queuery, String resourceEndPoint) {
		logIntoService();
		HttpClient httpClient = HttpClientBuilder.create().build();
		String q;
		q = (String) queuery;

		String uri = new StringBuilder(scimBaseUri).append("/").append(resourceEndPoint).append(q).toString();
		LOGGER.info("qeury url: {0}", uri);
		HttpGet httpGet = new HttpGet(uri);
		httpGet.addHeader(oauthHeader);
		httpGet.addHeader(prettyPrintHeader);

		String responseString = null;

		HttpResponse response;
		try {
			
			providerStartTime = System.nanoTime();
			response = httpClient.execute(httpGet);
			providerEndTime= System.nanoTime();
			
			providerDuration =(providerEndTime - providerStartTime)/1000000;
			
			LOGGER.info("The amouth of time it took to get the response to the query from the provider : {0} milliseconds ", providerDuration);
			providerDuration = 0;
			
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode == 200) {

				responseString = EntityUtils.toString(response.getEntity());
				if (!responseString.isEmpty()) {

					JSONObject jsonObject = new JSONObject(responseString);
					LOGGER.info("Json object returned from service provider: {0}", jsonObject);
					ScimSchemaParser scimParser = new ScimSchemaParser();
					for (int i = 0; i < jsonObject.getJSONArray("Resources").length(); i++) {
						JSONObject minResourceJson = new JSONObject();
						minResourceJson = jsonObject.getJSONArray("Resources").getJSONObject(i);
						if (minResourceJson.has("id") && minResourceJson.getString("id") != null) {
							if (minResourceJson.has("endpoint")) {
								scimParser.parseSchema(minResourceJson);

							} else {
								LOGGER.error("No uid present in fetched object: {0}", minResourceJson);

								throw new ConnectorException(
										"No uid present in fetchet object while processing queuery result");
							}

						}

					}
					return scimParser;

				} else {
					LOGGER.warn(
							"No definition for provided shcemas was found, the connector will switch to default core schema configuration!");
				}

			} else {
				LOGGER.warn("Query for {1} was unsuccessful. Status code returned is {0}", statusCode,
						resourceEndPoint);
				LOGGER.warn(
						"No definition for provided shcemas was found, the connector will switch to default core schema configuration!");
				return null;
			}
		} catch (ClientProtocolException e) {
			LOGGER.error(
					"An protocol exception has ocoured while in the process of querying the provider Schemas resource object. Possible mismatch in interpretation of the HTTP specification: {0}",
					e.getLocalizedMessage());
			LOGGER.info(
					"An protocol exception has ocoured while in the process of querying the provider Schemas resource object. Possible mismatch in interpretation of the HTTP specification: {0}",
					e);
			throw new ConnectorException(
					"An protocol exception has ocoured while in the process of querying the provider Schemas resource object. Possible mismatch in interpretation of the HTTP specification",
					e);
		} catch (IOException e) {
			LOGGER.error(
					"An error has ocoured while processing the http response. Ocourance in the process of querying the provider Schemas resource object: {0}",
					e.getLocalizedMessage());
			LOGGER.info(
					"An error has ocoured while processing the http response. Ocourance in the process of querying the provider Schemas resource object: {0}",
					e);

			throw new ConnectorIOException(
					"An error has ocoured while processing the http response. Ocourance in the process of querying the provider Schemas resource object",
					e);
		}
		return null;
	}

	public Uid createEntity(String resourceEndPoint, ObjectTranslator objectTranslator, Set<Attribute> attributes,
			Object attributeMap) {
		Set<Attribute> orgIdAttributeset = new HashSet<Attribute>();
		String oID = logIntoService();

		// injection of organization ID into the set of attributes
		if (oID != null) {
			LOGGER.info("The organization ID is: {0}", oID);

			//TODO schema version might change
			orgIdAttributeset.add(AttributeBuilder.build("schema.type", "urn:scim:schemas:extension:enterprise:1.0")); 
																														
			orgIdAttributeset.add(AttributeBuilder.build("schema.organization", oID));
		} else {
			orgIdAttributeset = null;
			LOGGER.warn("No organization ID specified in instance URL");
		}

		JSONObject jsonObject = new JSONObject();

		if (attributeMap != null && attributeMap instanceof HashMap<?, ?>) {
			jsonObject = objectTranslator.translateSetToJson(attributes, orgIdAttributeset,
					(Map<String, Map<String, Object>>) attributeMap);
		} else {
			jsonObject = objectTranslator.translateSetToJson(attributes, orgIdAttributeset);
		}
		HttpClient httpClient = HttpClientBuilder.create().build();
		String uri = new StringBuilder(scimBaseUri).append("/").append(resourceEndPoint).append("/").toString();

		try {
			LOGGER.info("New json object: {0}", jsonObject);

			HttpPost httpPost = new HttpPost(uri);
			httpPost.addHeader(oauthHeader);
			httpPost.addHeader(prettyPrintHeader);

			StringEntity bodyContent = new StringEntity(jsonObject.toString(1));

			bodyContent.setContentType("application/json");
			httpPost.setEntity(bodyContent);
			String responseString = null;
			try {
				
				providerStartTime = System.nanoTime();
				HttpResponse response = httpClient.execute(httpPost);
				providerEndTime= System.nanoTime();
				providerDuration =(providerEndTime - providerStartTime)/1000000;
				
				LOGGER.info("The amouth of time it took to get the response to the query from the provider : {0} milliseconds ", providerDuration);
				providerDuration = 0;
				
				
				loginInstance.releaseConnection();
				LOGGER.info("Connection released");

				int statusCode = response.getStatusLine().getStatusCode();

				if (statusCode == 201) {
					LOGGER.info("Creation of resource was succesfull");

					responseString = EntityUtils.toString(response.getEntity());
					JSONObject json = new JSONObject(responseString);

					Uid uid = new Uid(json.getString("id"));

					LOGGER.info("Json response: {0}", json.toString(1));
					return uid;
				}

				else {
					onNoSuccess(response, statusCode, responseString, "creating new object");
				}

			} catch (ClientProtocolException e) {
				LOGGER.error(
						"An protocol exception has ocoured while in the process of creating a new resource object. Possible mismatch in interpretation of the HTTP specification: {0}",
						e.getLocalizedMessage());
				LOGGER.info(
						"An protocol exception has ocoured while in the process of creating a new resource object. Possible mismatch in interpretation of the HTTP specification: {0}",
						e);
				throw new ConnectionFailedException(
						"An protocol exception has ocoured while in the process of creating a new resource object. Possible mismatch in interpretation of the HTTP specification: {0}",
						e);

			} catch (IOException e) {
				LOGGER.error(
						"An error has ocoured while processing the http response. Ocourance in the process of creating a new resource object: {0}",
						e.getLocalizedMessage());
				LOGGER.info(
						"An error has ocoured while processing the http response. Ocourance in the process of creating a new resource object: {0}",
						e);

				throw new ConnectorIOException(
						"An error has ocoured while processing the http response. Ocourance in the process of creating a new resource object",
						e);
			}

		} catch (JSONException e) {

			LOGGER.error(
					"An exception has ocoured while processing an json object. Ocourance in the process of creating a new resource object: {0}",
					e.getLocalizedMessage());
			LOGGER.info(
					"An exception has ocoured while processing an json object. Ocourance in the process of creating a new resource object: {0}",
					e);

			throw new ConnectorException(
					"An exception has ocoured while processing an json object. Ocourance in the process of creating a new resource objec",
					e);
		} catch (UnsupportedEncodingException e1) {
			LOGGER.error("Unsupported encoding: {0}. Ocourance in the process of creating a new resource object ",
					e1.getLocalizedMessage());
			LOGGER.info("Unsupported encoding: {0}. Ocourance in the process of creating a new resource object ", e1);

			throw new ConnectorException(
					"Unsupported encoding, ocourance in the process of creating a new resource object ", e1);
		}
		loginInstance.releaseConnection();
		throw new UnknownUidException("No uid returned in the process of resource creation");
	}

	public Uid updateEntity(Uid uid, String resourceEndPoint, JSONObject jsonObject) {

		logIntoService();

		HttpClient httpClient = HttpClientBuilder.create().build();
		
		String uri = new StringBuilder(scimBaseUri).append("/").append(resourceEndPoint).append("/")
				.append(uid.getUidValue()).toString();
LOGGER.info("The uri for the update request: {0}", uri);
		HttpPatch httpPatch = new HttpPatch(uri);

		httpPatch.addHeader(oauthHeader);
		httpPatch.addHeader(prettyPrintHeader);

		String responseString = null;
		try {
			StringEntity bodyContent = new StringEntity(jsonObject.toString(1));
			LOGGER.info("The update JSON object wich is beaing send: {0}", jsonObject);
			bodyContent.setContentType("application/json");
			httpPatch.setEntity(bodyContent);

			providerStartTime = System.nanoTime();
			HttpResponse response = httpClient.execute(httpPatch);
			providerEndTime= System.nanoTime();
			providerDuration =(providerEndTime - providerStartTime)/1000000;
			
			LOGGER.info("The amouth of time it took to get the response to the query from the provider : {0} milliseconds ", providerDuration);
			providerDuration = 0;
			
			loginInstance.releaseConnection();
			int statusCode = response.getStatusLine().getStatusCode();

			if (statusCode == 200 || statusCode == 201) {
				LOGGER.info("Update of resource was succesfull");

				responseString = EntityUtils.toString(response.getEntity());
				JSONObject json = new JSONObject(responseString);
				Uid id = new Uid(json.getString("id"));
				LOGGER.ok("Json response: ", json.toString(1));
				return id;
			}

			else {
				onNoSuccess(response, statusCode, responseString, "updating object");
			}

		} catch (UnsupportedEncodingException e) {

			LOGGER.error("Unsupported encoding: {0}. Ocourance in the process of updating a resource object ",
					e.getMessage());
			LOGGER.info("Unsupported encoding: {0}. Ocourance in the process of updating a resource object ", e);

			throw new ConnectorException(
					"Unsupported encoding, ocourance in the process of updating a resource object ", e);

		} catch (JSONException e) {

			LOGGER.error(
					"An exception has ocoured while processing an json object. Ocourance in the process of updating a resource object: {0}",
					e.getLocalizedMessage());
			LOGGER.info(
					"An exception has ocoured while processing an json object. Ocourance in the process of updating a resource object: {0}",
					e);

			throw new ConnectorException(
					"An exception has ocoured while processing an json object,ocourance in the process of updating a resource object",
					e);
		} catch (ClientProtocolException e) {
			LOGGER.error(
					"An protocol exception has ocoured while in the process of updating a resource object. Possible mismatch in the interpretation of the HTTP specification: {0}",
					e.getLocalizedMessage());
			LOGGER.info(
					"An protocol exception has ocoured while in the process of updating a resource object. Possible mismatch in the interpretation of the HTTP specification: {0}",
					e);
			throw new ConnectionFailedException(
					"An protocol exception has ocoured while in the process of updating a resource object, Possible mismatch in the interpretation of the HTTP specification.",
					e);
		} catch (IOException e) {

			LOGGER.error(
					"An error has ocoured while processing the http response. Ocourance in the process of updating a resource object: {0}",
					e.getLocalizedMessage());
			LOGGER.info(
					"An error has ocoured while processing the http response. Ocourance in the process of creating a resource object: {0}",
					e);

			throw new ConnectorIOException(
					"An error has ocoured while processing the http response. Ocourance in the process of creating a resource object",
					e);

		}
		loginInstance.releaseConnection();
		throw new UnknownUidException("No uid returned in the process of resource update");

	}

	public void deleteEntity(Uid uid, String resourceEndPoint) {

		logIntoService();

		HttpClient httpClient = HttpClientBuilder.create().build();

		String uri = new StringBuilder(scimBaseUri).append("/").append(resourceEndPoint).append("/")
				.append(uid.getUidValue()).toString();

		LOGGER.info("The uri for the delete request: {0}", uri);
		HttpDelete httpDelete = new HttpDelete(uri);
		httpDelete.addHeader(oauthHeader);
		httpDelete.addHeader(prettyPrintHeader);

		String responseString = null;

		try {
			providerStartTime = System.nanoTime();
			HttpResponse response = httpClient.execute(httpDelete);
			providerEndTime= System.nanoTime();
			providerDuration =(providerEndTime - providerStartTime)/1000000;
			
			LOGGER.info("The amouth of time it took to get the response to the query from the provider : {0} milliseconds ", providerDuration);
			providerDuration = 0;
			
			loginInstance.releaseConnection();

			int statusCode = response.getStatusLine().getStatusCode();

			if (statusCode == 204 || statusCode == 200) {
				LOGGER.info("Deletion of resource was succesfull");
			}

			else if (statusCode == 404) {

				LOGGER.info("Resource not found or resource was already deleted");
			} else {
				onNoSuccess(response, statusCode, responseString, "deleting object");
			}

		} catch (ClientProtocolException e) {
			LOGGER.error(
					"An protocol exception has ocoured while in the process of deleting a resource object. Possible mismatch in the interpretation of the HTTP specification: {0}",
					e.getLocalizedMessage());
			LOGGER.info(
					"An protocol exception has ocoured while in the process of deleting a resource object. Possible mismatch in the interpretation of the HTTP specification: {0}",
					e);
			throw new ConnectionFailedException(
					"An protocol exception has ocoured while in the process of deleting a resource object. Possible mismatch in the interpretation of the HTTP specification.",
					e);
		} catch (IOException e) {
			LOGGER.error(
					"An error has ocoured while processing the http response. Ocourance in the process of deleting a resource object: : {0}",
					e.getLocalizedMessage());
			LOGGER.info(
					"An error has ocoured while processing the http response. Ocourance in the process of deleting a resource object: : {0}",
					e);

			throw new ConnectorIOException(
					"An error has ocoured while processing the http response. Ocourance in the process of deleting a resource object.",
					e);
		}
	}

	private void onNoSuccess(HttpResponse response, int statusCode, String responseString, String message)
			throws ParseException, IOException {
		responseString = EntityUtils.toString(response.getEntity());

		if (message == null) {
			message = "the full resource representation";
		}
		LOGGER.error("Query for {1} was unsuccessful. Status code returned is {0}", statusCode, message);
		LOGGER.info("An error has occured. Http status: {0}", responseString);
		LOGGER.info("Query for {1} was unsuccessful. Status code returned is {0}", statusCode, message);

		throw new ConnectorIOException("Query was unsuccessful");
	}

}
