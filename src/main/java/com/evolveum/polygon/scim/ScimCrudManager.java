package com.evolveum.polygon.scim;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;

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

	private void logIntoService() {

		HttpClient httpclient = HttpClientBuilder.create().build();

		String loginURL = new StringBuilder(conf.getLoginURL()).append(conf.getService()).append("&client_id=")
				.append(conf.getClientID()).append("&client_secret=").append(conf.getClientSecret())
				.append("&username=").append(conf.getUserName()).append("&password=").append(conf.getPassword())
				.toString();

		loginInstance = new HttpPost(loginURL);
		HttpResponse response = null;

		LOGGER.info("The login URL value is: {0}", loginURL);

		try {
			// Execute the login POST request
			response = httpclient.execute(loginInstance);
		} catch (ClientProtocolException e) {

			LOGGER.error(
					"An protocol exception has ocoured while processing the http response to the login request. Possible mismatch in interpretation of the HTTP specification: {0}",
					e.getLocalizedMessage());
			LOGGER.warn(
					"An protocol exception has ocoured while processing the http response to the login request. Possible mismatch in interpretation of the HTTP specification: {0}",
					e);

			throw new ConnectionFailedException(e);

		} catch (IOException ioException) {

			LOGGER.error("An error ocoured while processing the queuery http response to the login request : {0}",
					ioException.getLocalizedMessage());
			LOGGER.warn("An error ocoured while processing the queuery http response to the login request : {0}",
					ioException);
			throw new ConnectorIOException(ioException);
		}

		// verify response is HTTP OK
		final int statusCode = response.getStatusLine().getStatusCode();
		if (statusCode != HttpStatus.SC_OK) {
			LOGGER.error("Error with authenticating : {0}", statusCode);
			try {
				LOGGER.error("Error cause: {0}", EntityUtils.toString(response.getEntity()));
			} catch (ParseException | IOException e) {

				LOGGER.error("An exception has ocoured while parsing the http response to the login request: {0}",
						e.getLocalizedMessage());
				LOGGER.warn("An exception has ocoured while parsing the http response to the login request: {0}", e);
				throw new ConnectorIOException(e);
			}
		}

		String getResult = null;
		try {
			getResult = EntityUtils.toString(response.getEntity());
		} catch (IOException ioException) {

			LOGGER.error("An exception has ocoured while parsing the http response to the login request: {0}",
					ioException.getLocalizedMessage());
			LOGGER.warn("An exception has ocoured while parsing the http response to the login request: {0}",
					ioException);
			throw new ConnectorIOException(ioException);
		}
		JSONObject jsonObject = null;
		String loginAccessToken = null;
		String loginInstanceUrl = null;
		try {
			jsonObject = (JSONObject) new JSONTokener(getResult).nextValue();
			loginAccessToken = jsonObject.getString("access_token");
			loginInstanceUrl = jsonObject.getString("instance_url");
		} catch (JSONException jsonException) {

			LOGGER.error(
					"An exception has ocoured while setting the variable \"jsonObject\". Ocourance while processing the http response to the login request: {0}",
					jsonException.getLocalizedMessage());
			LOGGER.warn(
					"An exception has ocoured while setting the variable \"jsonObject\". Ocourance while processing the http response to the login request: {0}",
					jsonException);
			throw new ConnectorException(jsonException);
		}
		scimBaseUri = new StringBuilder(loginInstanceUrl).append(conf.getEndpoint()).append(conf.getVersion())
				.toString();
		oauthHeader = new BasicHeader("Authorization", "OAuth " + loginAccessToken);
		LOGGER.info("Login Successful");
	}

	public void qeueryEntity(String q, String resourceEndPoint, ResultsHandler resultHandler) {
		logIntoService();
		HttpClient httpClient = HttpClientBuilder.create().build();

		String uri = new StringBuilder(scimBaseUri).append("/").append(resourceEndPoint).append(q).toString();
		LOGGER.info("qeury url: {0}", uri);
		HttpGet httpGet = new HttpGet(uri);
		httpGet.addHeader(oauthHeader);
		httpGet.addHeader(prettyPrintHeader);

		String responseString = null;

		try {
			HttpResponse response = httpClient.execute(httpGet);
			loginInstance.releaseConnection();
			LOGGER.info("Connection released");
			int statusCode = response.getStatusLine().getStatusCode();

			if (statusCode == 200) {

				responseString = EntityUtils.toString(response.getEntity());

				try {
					JSONObject jsonObject = new JSONObject(responseString);
					try {
					if (q != ""){
						ConnectorObjBuilder objBuilder = new ConnectorObjBuilder();
						resultHandler.handle(objBuilder.buildConnectorObject(jsonObject));

					} else{						
						for(int i=0 ; i<jsonObject.getJSONArray("Resources").length(); i++){
							ConnectorObjBuilder objBuilder = new ConnectorObjBuilder();
							resultHandler.handle(objBuilder.buildConnectorObject(jsonObject.getJSONArray("Resources").getJSONObject(i)));

						}
						
					}
					
					} catch (Exception e) {
						LOGGER.error("Builder error", e.getLocalizedMessage());
					throw new ConnectorException(e);
					}

					LOGGER.info("Json response: {0}", jsonObject.toString(1));

				} catch (JSONException jsonException) {
					if (q == null) {
						q = "the full resource representation";
					}
					LOGGER.error(
							"An exception has ocoured while setting the variable \"jsonObject\". Ocourance while processing the http response to the queuey request for: {1}, exception message: {0}",
							jsonException.getLocalizedMessage(), q);
					LOGGER.warn(
							"An exception has ocoured while setting the variable \"jsonObject\". Ocourance while processing the http response to the queuey request for: {1}, exception message: {0}",
							jsonException, q);
					throw new ConnectorException(jsonException);
				}

			} else {

				onNoSuccess(response, statusCode, responseString, uri);
			}

		} catch (IOException e) {

			if (q == null) {
				q = "the full resource representation";
			}

			LOGGER.error(
					"An error ocoured while processing the queuery http response. Ocourance while processing the http response to the queuey request for: {1}, exception message: {0}",
					e.getLocalizedMessage(), q);
			LOGGER.warn(
					"An error ocoured while processing the queuery http response. Ocourance while processing the http response to the queuey request for: {1}, exception message: {0}",
					e, q);
			throw new ConnectorIOException(e);
		}

		loginInstance.releaseConnection();
		LOGGER.info("Connection released");
	}

	public Uid createEntity(String resourceEndPoint, JSONObject jsonObject) {
		logIntoService();

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
				LOGGER.warn(
						"An protocol exception has ocoured while in the process of creating a new resource object. Possible mismatch in interpretation of the HTTP specification: {0}",
						e);
				throw new ConnectionFailedException(e);

			} catch (IOException e) {
				LOGGER.error(
						"An error has ocoured while processing the http response. Ocourance in the process of creating a new resource object: : {0}",
						e.getLocalizedMessage());
				LOGGER.warn(
						"An error has ocoured while processing the http response. Ocourance in the process of creating a new resource object: : {0}",
						e);

				throw new ConnectorIOException(e);
			}

		} catch (JSONException e) {

			LOGGER.error(
					"An exception has ocoured while processing an json object. Ocourance in the process of creating a new resource object: {0}",
					e.getLocalizedMessage());
			LOGGER.warn(
					"An exception has ocoured while processing an json object. Ocourance in the process of creating a new resource object: {0}",
					e);

			throw new ConnectorException(e);
		} catch (UnsupportedEncodingException e1) {
			LOGGER.error("Unsupported encoding: {0}. Ocourance in the process of creating a new resource object ",
					e1.getMessage());
			LOGGER.warn("Unsupported encoding: {0}. Ocourance in the process of creating a new resource object ", e1);

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
			LOGGER.warn("Unsupported encoding: {0}. Ocourance in the process of updating a resource object ", e);

			throw new ConnectorException(e);

		} catch (JSONException e) {

			LOGGER.error(
					"An exception has ocoured while processing an json object. Ocourance in the process of updating a resource object: {0}",
					e.getLocalizedMessage());
			LOGGER.warn(
					"An exception has ocoured while processing an json object. Ocourance in the process of updating a resource object: {0}",
					e);

			throw new ConnectorException(e);
		} catch (ClientProtocolException e) {
			LOGGER.error(
					"An protocol exception has ocoured while in the process of updating a resource object. Possible mismatch in the interpretation of the HTTP specification: {0}",
					e.getLocalizedMessage());
			LOGGER.warn(
					"An protocol exception has ocoured while in the process of updating a resource object. Possible mismatch in the interpretation of the HTTP specification: {0}",
					e);
			throw new ConnectionFailedException(e);
		} catch (IOException e) {

			LOGGER.error(
					"An error has ocoured while processing the http response. Ocourance in the process of updating a resource object: : {0}",
					e.getLocalizedMessage());
			LOGGER.warn(
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
				LOGGER.info("####Deletion of resource was succesfull####");
			}

			else if (statusCode == 404) {

				LOGGER.info("####Resource not found or resource was already deleted####");
			} else {
				onNoSuccess(response, statusCode, responseString, "deleting object");
			}

		} catch (ClientProtocolException e) {
			LOGGER.error(
					"An protocol exception has ocoured while in the process of deleting a resource object. Possible mismatch in the interpretation of the HTTP specification: {0}",
					e.getLocalizedMessage());
			LOGGER.warn(
					"An protocol exception has ocoured while in the process of deleting a resource object. Possible mismatch in the interpretation of the HTTP specification: {0}",
					e);
			throw new ConnectionFailedException(e);
		} catch (IOException e) {
			LOGGER.error(
					"An error has ocoured while processing the http response. Ocourance in the process of deleting a resource object: : {0}",
					e.getLocalizedMessage());
			LOGGER.warn(
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
		LOGGER.warn("Query for {1} was unsuccessful. Status code returned is {0}", statusCode, message);

		throw new ConnectorIOException("Query was unsuccessful");
	}

}
