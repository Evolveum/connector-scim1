package com.evolveum.polygon.salesfrconn;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

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
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.Uid;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.evolveum.polygon.salesfrconn.helpermethods.HttpPatch;

public class SalesfrcManager {
	
	private String scimBaseUri;
	private Header oauthHeader;
	private Header prettyPrintHeader = new BasicHeader("X-PrettyPrint", "1");
	private SalesFrcConfiguration conf;
	
	
	HttpPost loginInstance;
	
	private static final Log LOGGER = Log.getLog(SalesfrcManager.class);
	
	public SalesfrcManager(SalesFrcConfiguration conf){
		this.conf=(SalesFrcConfiguration)conf;
	}

				
		private void logIntoService(){
			
			HttpClient httpclient = HttpClientBuilder.create().build();
			
			String loginURL = 
					new StringBuilder(conf.getLoginURL()).append(conf.getService()).append("&client_id=").append(conf.getClientID()).append("&client_secret=").append(conf.getClientSecret()).append("&username=").append(conf.getUserName()).append("&password=").append(conf.getPassword()).toString();
			
				loginInstance = new HttpPost(loginURL);
		        HttpResponse response = null;
		        
		        LOGGER.info("The login URL value is: {0}", loginURL);
		        
		        try {
		            // Execute the login POST request
		            response = httpclient.execute(loginInstance);
		        } catch (ClientProtocolException cpException) {
		            cpException.printStackTrace();
		        } catch (IOException ioException) {
		            ioException.printStackTrace();
		        }
		 
		        // verify response is HTTP OK
		        final int statusCode = response.getStatusLine().getStatusCode();
		        if (statusCode != HttpStatus.SC_OK) {
		           LOGGER.error("Error authenticating to Force.com: {0}",statusCode);
		            try {
						LOGGER.info("*Error cause: {0}", EntityUtils.toString(response.getEntity()));
					} catch (ParseException | IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
		            return;
		        }
		 
		        String getResult = null;
		        try {
		            getResult = EntityUtils.toString(response.getEntity());
		        } catch (IOException ioException) {
		            ioException.printStackTrace();
		        }
		        JSONObject jsonObject = null;
		        String loginAccessToken = null;
		        String loginInstanceUrl = null;
		        try {
		            jsonObject = (JSONObject) new JSONTokener(getResult).nextValue();
		            loginAccessToken = jsonObject.getString("access_token");
		            loginInstanceUrl = jsonObject.getString("instance_url");
		        } catch (JSONException jsonException) {
		            jsonException.printStackTrace();
		        }
		        scimBaseUri= loginInstanceUrl +conf.getEndpoint()+conf.getVersion();
		        oauthHeader = new BasicHeader("Authorization", "OAuth " + loginAccessToken) ;
		        LOGGER.info("####Successful login####");
		}
		
		public void qeueryEntity(String q ,String resourceEndPoint){
			logIntoService();
	    	HttpClient httpClient = HttpClientBuilder.create().build();
	    	
	    	
	    	// This is how a filter q should look like: "?filter=(userName%20eq%20%22johnsnow@winterfell.com%22)"
	    	// usefull: http://www.w3schools.com/tags/ref_urlencode.asp
	    	
	    	
	    	
	    	String uri = 
	    			new StringBuilder(scimBaseUri).append("/").append(resourceEndPoint).append(q).toString();
	    	LOGGER.info("qeury url: {0}", uri);
	    	HttpGet httpGet = new HttpGet(uri);
	    	LOGGER.info("oauth2 header: {0}", oauthHeader);
	    	httpGet.addHeader(oauthHeader);
	    	httpGet.addHeader(prettyPrintHeader);
	    	
	    	String responseString = null;
	    	
	    	try {
				HttpResponse response = httpClient.execute(httpGet);
				loginInstance.releaseConnection();
				LOGGER.info("Connection released");
				int statusCode = response.getStatusLine().getStatusCode();
				
				if(statusCode == 200){
					
						responseString = EntityUtils.toString(response.getEntity());
					
					try {
						JSONObject json = new JSONObject(responseString);
						
						//for id call -->> json.getJSONArray("Resources").getJSONObject(0).getString("id") // check if "Resources" is a good endpoint 
						
						//Uid uid =new Uid(json.getString("id"));
						
						
						ConnectorObjBuilder objBuilder = new ConnectorObjBuilder();
						objBuilder.buildConnectorObject(json);
						
						LOGGER.info("Json response: {0}", json.toString(1));
						LOGGER.info("Connection released");

					} catch (JSONException e) {
						
						e.printStackTrace();
					}

				} else {
					
					onNoSuccess(response,statusCode,responseString);
				}
				
			} catch (IOException e) {
				
				e.printStackTrace();
			}
	    	loginInstance.releaseConnection();
	    	LOGGER.info("Connection released");
	    }
		
	public Uid createEntity(String resourceEndPoint, JSONObject jsonObject){
		logIntoService();
		
	    	HttpClient httpClient = HttpClientBuilder.create().build();
	    	
	    	System.out.println(scimBaseUri);
	    	String uri = new StringBuilder(scimBaseUri).append("/").append(resourceEndPoint).toString();   	
	    	
	    	try {
				LOGGER.info("New user jsonObj: {0}", jsonObject);
				
				HttpPost httpPost = new HttpPost(uri);
				httpPost.addHeader(oauthHeader);
				httpPost.addHeader(prettyPrintHeader);
				
				
				StringEntity bodyContent = new StringEntity(jsonObject.toString(1));
				
				 bodyContent.setContentType("application/json");
				 httpPost.setEntity(bodyContent);
				 String responseString = null ;
				 try {
					HttpResponse response = httpClient.execute(httpPost);
					
					loginInstance.releaseConnection();
			    	LOGGER.info("Connection released");
					
					int statusCode = response.getStatusLine().getStatusCode();
					
					if(statusCode==201){
						LOGGER.info("####Creation of resource was succesfull####");
						
						responseString = EntityUtils.toString(response.getEntity());
						JSONObject json = new JSONObject(responseString);
						///json.get("id");
						
						Uid uid =new Uid(json.getString("id"));
						
						System.out.println(json.getString("id"));
						
						LOGGER.info("Json response: {0}", json.toString(1));
						return uid;
					}
					
					else{
						onNoSuccess(response, statusCode, responseString);	
					}
					
				} catch (ClientProtocolException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				 

			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (UnsupportedEncodingException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
	    	loginInstance.releaseConnection();
			return null;
	    }

	public Uid updateEntity(Uid uid ,String resourceEndPoint, JSONObject jsonObject){
		
		logIntoService();
		
	HttpClient httpClient = HttpClientBuilder.create().build();
	System.out.println(uid.getUidValue());
	System.out.println(uid.getUidValue());
	String uri =
			new StringBuilder(scimBaseUri).append("/").append(resourceEndPoint).append("/").append(uid.getUidValue()).toString();
	   	
	   	HttpPatch httpPatch = new HttpPatch(uri);
	   	
	   	httpPatch.addHeader(oauthHeader);
	   	httpPatch.addHeader(prettyPrintHeader);
	   	
	   	String responseString = null ;
			try {
				StringEntity bodyContent = new StringEntity(jsonObject.toString(1));
				 bodyContent.setContentType("application/json");
				 httpPatch.setEntity(bodyContent);

		    	HttpResponse response = httpClient.execute(httpPatch);
		    	loginInstance.releaseConnection();
		    	int statusCode = response.getStatusLine().getStatusCode();
				
				if(statusCode==200||statusCode==201 ){
					LOGGER.info("####Update of resource was succesfull####");
				;
					responseString = EntityUtils.toString(response.getEntity());
					JSONObject json = new JSONObject(responseString);
					Uid id =new Uid(json.getString("id"));
					LOGGER.ok("Json response: ", json.toString(1));
					return id;
				}
				
				else{
					onNoSuccess(response,statusCode,responseString);
					throw new IOException("Query was unsuccessful");
				}
				
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			loginInstance.releaseConnection();
			return null;
	   	
	   }

	public  void deleteEntity(Uid uid ,String resourceEndPoint){
		
		logIntoService();
		
		HttpClient httpClient = HttpClientBuilder.create().build();
		
		System.out.println(uid.getUidValue());

		String uri = 
				new StringBuilder(scimBaseUri).append("/").append(resourceEndPoint).append("/").append(uid.getUidValue()).toString();
		
		HttpDelete httpDelete = new HttpDelete(uri);
		httpDelete.addHeader(oauthHeader);
		httpDelete.addHeader(prettyPrintHeader);
		
		 String responseString = null ;
		
		try {
			HttpResponse response = httpClient.execute(httpDelete);
			loginInstance.releaseConnection();
			
			int statusCode = response.getStatusLine().getStatusCode();
			
			if(statusCode==204 || statusCode==200){ 
				LOGGER.info("####Deletion of resource was succesfull####");
			}
			
			else if (statusCode == 404){
				
				LOGGER.info("####Resource not found or resource was already deleted####");
			}else{
				onNoSuccess(response, statusCode, responseString);
			}
			
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void onNoSuccess(HttpResponse response, int statusCode, String responseString) throws ParseException, IOException {
		responseString = EntityUtils.toString(response.getEntity());
		LOGGER.error("Query was unsuccessful. Status code returned is {0}", statusCode);
        LOGGER.info("####An error has occured. Http status: {0}", responseString);
        throw new IOException("Query was unsuccessful");
	}
	
}
