package com.evolveum.polygon.scim;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

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
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.evolveum.polygon.scim.common.HttpPatch;

public class ScimCrudManager {

	private String scimBaseUri;
	private Header oauthHeader;
	private Header prettyPrintHeader = new BasicHeader("X-PrettyPrint", "1");
	private ScimConnectorConfiguration conf;

	HttpPost loginInstance;

	private static final Log LOGGER = Log.getLog(ScimCrudManager.class);

	public ScimCrudManager(ScimConnectorConfiguration conf) {
		this.conf = (ScimConnectorConfiguration) conf;
	}

	private String logIntoService() { // TODO check if good Idea
		String orgID= null;
		HttpClient httpclient = HttpClientBuilder.create().build();

		// Building the login url TODO Replace to login method
		String loginURL = new StringBuilder(conf.getLoginURL()).append(conf.getService()).append("&client_id=")
				.append(conf.getClientID()).append("&client_secret=").append(conf.getClientSecret())
				.append("&username=").append(conf.getUserName()).append("&password=").append(conf.getPassword())
				.toString();

		loginInstance = new HttpPost(loginURL);
		HttpResponse response = null;

		//LOGGER.info("The login URL value is: {0}", loginURL);

		try {
	
			response = httpclient.execute(loginInstance);
		} catch (ClientProtocolException e) {

			LOGGER.error(
					"An protocol exception has ocoured while processing the http response to the login request. Possible mismatch in interpretation of the HTTP specification: {0}",
					e.getLocalizedMessage());
			LOGGER.info(
					"An protocol exception has ocoured while processing the http response to the login request. Possible mismatch in interpretation of the HTTP specification: {0}",
					e);

			throw new ConnectionFailedException(e);

		} catch (IOException ioException) {

			LOGGER.error("An error ocoured while processing the queuery http response to the login request : {0}",
					ioException.getLocalizedMessage());
			LOGGER.info("An error ocoured while processing the queuery http response to the login request : {0}",
					ioException);
			throw new ConnectorIOException(ioException);
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
				throw new ConnectorIOException(e);
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
			throw new ConnectorIOException(ioException);
		}
		JSONObject jsonObject = null;
		String loginAccessToken = null;
		String loginInstanceUrl = null;
		try {
			
			jsonObject = (JSONObject) new JSONTokener(getResult).nextValue();
			if (jsonObject.has("id")){
			orgID= jsonObject.getString("id");
			String idParts [] =orgID.split("\\/");
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
			throw new ConnectorException(jsonException);
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
		if(queuery instanceof Uid){
			
			q = ((Uid)queuery).getUidValue();
		}else{
			
			q = (String)queuery;
		}
		
		String uri = new StringBuilder(scimBaseUri).append("/").append(resourceEndPoint).append(q).toString();
		LOGGER.info("qeury url: {0}", uri);
		HttpGet httpGet = new HttpGet(uri);
		httpGet.addHeader(oauthHeader);
		httpGet.addHeader(prettyPrintHeader);

		String responseString = null;

		try {
			HttpResponse response = httpClient.execute(httpGet);
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode == 200) {

				responseString = EntityUtils.toString(response.getEntity());
				if(!responseString.isEmpty()){
				try {
					JSONObject jsonObject = new JSONObject(responseString);
					
					LOGGER.info("Json object returned from service provider: {0}",jsonObject);
					try {
					if (queuery instanceof Uid){
						loginInstance.releaseConnection();
						ConnectorObjBuilder objBuilder = new ConnectorObjBuilder();
						resultHandler.handle(objBuilder.buildConnectorObject(jsonObject));

					} else{						
						for(int i=0 ; i<jsonObject.getJSONArray("Resources").length(); i++){
							JSONObject minResourceJson = new JSONObject();
							minResourceJson = jsonObject.getJSONArray("Resources").getJSONObject(i);
							if(minResourceJson.has("id") && minResourceJson.getString("id")!=null){
								
								if(minResourceJson.has("meta")){
								
								String resourceUri= minResourceJson.getJSONObject("meta").getString("location").toString();
								HttpGet httpGetR= new HttpGet(resourceUri);
								httpGetR.addHeader(oauthHeader);
								httpGetR.addHeader(prettyPrintHeader);
								
								HttpResponse resourceResponse = httpClient.execute(httpGetR);
								
								statusCode = resourceResponse.getStatusLine().getStatusCode();

								if (statusCode == 200) {
									responseString = EntityUtils.toString(resourceResponse.getEntity());
									JSONObject fullResourcejson = new JSONObject(responseString);
									
									LOGGER.info("The {0}. resource json object which was returned by the service provider: {1}",i+1,fullResourcejson);
									
									ConnectorObjBuilder objBuilder = new ConnectorObjBuilder();
									
									ConnectorObject conOb = objBuilder.buildConnectorObject(fullResourcejson);
									
									resultHandler.handle(conOb);
									
									
									
								}else{
									loginInstance.releaseConnection();
									LOGGER.info("Connection released");
									onNoSuccess(resourceResponse, statusCode, responseString, resourceUri);
								}
								//TODO check if good condition for schema identification 
							}
								} else {
								LOGGER.error("No uid present in fetched object: {0}", minResourceJson);
								
								throw new ConnectorException("No uid present in fetchet object while processing queuery result");
							}
						}
						loginInstance.releaseConnection();
					}
					
					} catch (Exception e) {
						LOGGER.error("Builder error. Error while building connId object. The excetion message: {0}", e.getLocalizedMessage());
						LOGGER.info("Builder error. Error while building connId object. The excetion message: {0}", e);
					throw new ConnectorException(e);
					}
					// TODO uncomment json response info
					// LOGGER.info("Json response: {0}", jsonObject.toString(1));

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
					throw new ConnectorException(jsonException);
				}

			}else{
			 loginInstance.releaseConnection();
			LOGGER.error("Service provider response is empty, responce returned on queuery: {0}", queuery);
			throw new ConnectorException("Service provider response is empty, exception ocoured while fetching response from service provider endpoint.");
				
			}} else {
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
			throw new ConnectorIOException(e);
		}

		loginInstance.releaseConnection();
		LOGGER.info("Connection released");
	}
	public ScimSchemaParser qeueryEntity(Object queuery, String resourceEndPoint){
		
		//TODO delete this println
		
		System.out.println("query of schemas");
		logIntoService();
		HttpClient httpClient = HttpClientBuilder.create().build();
		String q;
		q = (String)queuery;
		
		String uri = new StringBuilder(scimBaseUri).append("/").append(resourceEndPoint).append(q).toString();
		LOGGER.info("qeury url: {0}", uri);
		HttpGet httpGet = new HttpGet(uri);
		httpGet.addHeader(oauthHeader);
		httpGet.addHeader(prettyPrintHeader);
		
		String responseString = null;
		
		HttpResponse response;
		try {
			response = httpClient.execute(httpGet);
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode == 200) {
				
				responseString = EntityUtils.toString(response.getEntity());
				if(!responseString.isEmpty()){
					
					JSONObject jsonObject = new JSONObject(responseString);
					LOGGER.info("Json object returned from service provider: {0}",jsonObject);
					ScimSchemaParser scimParser = new ScimSchemaParser();
					for(int i=0 ; i<jsonObject.getJSONArray("Resources").length(); i++){
						JSONObject minResourceJson = new JSONObject();
						minResourceJson = jsonObject.getJSONArray("Resources").getJSONObject(i);
						if(minResourceJson.has("id") && minResourceJson.getString("id")!=null){
							if (minResourceJson.has("endpoint")) {
							scimParser.parseSchema(minResourceJson);
								
							}else {
								LOGGER.error("No uid present in fetched object: {0}", minResourceJson);
								
								throw new ConnectorException("No uid present in fetchet object while processing queuery result");
							}
							
						}
					
					}
					return scimParser;
					
				}
				
			}
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		//TODO add exception
		return null;
	}

	public Uid createEntity(String resourceEndPoint, ObjectTranslator objectTranslator, Set<Attribute> attributes) {
		Set<Attribute> attr = new HashSet<Attribute>();
		String oID = logIntoService();
		
		// injection of organization ID into the set of attributes
		if (oID !=null){
			LOGGER.info("The organization ID is: {0}", oID);
			
			attr.add(AttributeBuilder.build("schema.type", "urn:scim:schemas:extension:enterprise:1.0")); // TODO schema may change 
			attr.add(AttributeBuilder.build("schema.organization", oID));
			
			LOGGER.info("The finnal attribute set: {0}", attr);
		}else {
			
			LOGGER.info("No organization ID specified in instance URL");
		}
		
		JSONObject jsonObject = new JSONObject();
		
		jsonObject = objectTranslator.translateSetToJson(attributes, attr);
		
		HttpClient httpClient = HttpClientBuilder.create().build();
		String uri = new StringBuilder(scimBaseUri).append("/").append(resourceEndPoint).toString();

		try {
			LOGGER.info("New user jsonObj: {0}", jsonObject);

			HttpPost httpPost = new HttpPost(uri);
			httpPost.addHeader(oauthHeader);
			httpPost.addHeader(prettyPrintHeader);

			StringEntity bodyContent = new StringEntity(jsonObject.toString(1));

			bodyContent.setContentType("application/json");
			httpPost.setEntity(bodyContent);
			String responseString = null;
			try {
				HttpResponse response = httpClient.execute(httpPost);

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
				throw new ConnectionFailedException(e);

			} catch (IOException e) {
				LOGGER.error(
						"An error has ocoured while processing the http response. Ocourance in the process of creating a new resource object: : {0}",
						e.getLocalizedMessage());
				LOGGER.info(
						"An error has ocoured while processing the http response. Ocourance in the process of creating a new resource object: : {0}",
						e);

				throw new ConnectorIOException(e);
			}

		} catch (JSONException e) {

			LOGGER.error(
					"An exception has ocoured while processing an json object. Ocourance in the process of creating a new resource object: {0}",
					e.getLocalizedMessage());
			LOGGER.info(
					"An exception has ocoured while processing an json object. Ocourance in the process of creating a new resource object: {0}",
					e);

			throw new ConnectorException(e);
		} catch (UnsupportedEncodingException e1) {
			LOGGER.error("Unsupported encoding: {0}. Ocourance in the process of creating a new resource object ",
					e1.getLocalizedMessage());
			LOGGER.info("Unsupported encoding: {0}. Ocourance in the process of creating a new resource object ", e1);

			throw new ConnectorException(e1);
		}
		loginInstance.releaseConnection();
		throw new UnknownUidException("No uid returned in the process of resource creation");
	}

	public Uid updateEntity(Uid uid, String resourceEndPoint, JSONObject jsonObject) {

		logIntoService();

		HttpClient httpClient = HttpClientBuilder.create().build();
		String uri = new StringBuilder(scimBaseUri).append("/").append(resourceEndPoint).append("/")
				.append(uid.getUidValue()).toString();

		HttpPatch httpPatch = new HttpPatch(uri);

		httpPatch.addHeader(oauthHeader);
		httpPatch.addHeader(prettyPrintHeader);

		String responseString = null;
		try {
			StringEntity bodyContent = new StringEntity(jsonObject.toString(1));
			bodyContent.setContentType("application/json");
			httpPatch.setEntity(bodyContent);

			HttpResponse response = httpClient.execute(httpPatch);
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

			throw new ConnectorException(e);

		} catch (JSONException e) {

			LOGGER.error(
					"An exception has ocoured while processing an json object. Ocourance in the process of updating a resource object: {0}",
					e.getLocalizedMessage());
			LOGGER.info(
					"An exception has ocoured while processing an json object. Ocourance in the process of updating a resource object: {0}",
					e);

			throw new ConnectorException(e);
		} catch (ClientProtocolException e) {
			LOGGER.error(
					"An protocol exception has ocoured while in the process of updating a resource object. Possible mismatch in the interpretation of the HTTP specification: {0}",
					e.getLocalizedMessage());
			LOGGER.info(
					"An protocol exception has ocoured while in the process of updating a resource object. Possible mismatch in the interpretation of the HTTP specification: {0}",
					e);
			throw new ConnectionFailedException(e);
		} catch (IOException e) {

			LOGGER.error(
					"An error has ocoured while processing the http response. Ocourance in the process of updating a resource object: : {0}",
					e.getLocalizedMessage());
			LOGGER.info(
					"An error has ocoured while processing the http response. Ocourance in the process of creating a resource object: : {0}",
					e);

			throw new ConnectorIOException(e);

		}
		loginInstance.releaseConnection();
		throw new UnknownUidException("No uid returned in the process of resource update");

	}

	public void deleteEntity(Uid uid, String resourceEndPoint) {

		logIntoService();

		HttpClient httpClient = HttpClientBuilder.create().build();

		String uri = new StringBuilder(scimBaseUri).append("/").append(resourceEndPoint).append("/")
				.append(uid.getUidValue()).toString();

		HttpDelete httpDelete = new HttpDelete(uri);
		httpDelete.addHeader(oauthHeader);
		httpDelete.addHeader(prettyPrintHeader);

		String responseString = null;

		try {
			HttpResponse response = httpClient.execute(httpDelete);
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
			throw new ConnectionFailedException(e);
		} catch (IOException e) {
			LOGGER.error(
					"An error has ocoured while processing the http response. Ocourance in the process of deleting a resource object: : {0}",
					e.getLocalizedMessage());
			LOGGER.info(
					"An error has ocoured while processing the http response. Ocourance in the process of deleting a resource object: : {0}",
					e);

			throw new ConnectorIOException(e);
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
