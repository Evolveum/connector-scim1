package com.test.slsfrc.salesfrcTest;

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
import org.apache.http.impl.conn.LoggingSessionOutputBuffer;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.identityconnectors.common.logging.Log;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class SalesfrcEntityManager {
	
	private String scimBaseUri;
	private Header oauthHeader;
	private Header prettyPrintHeader = new BasicHeader("X-PrettyPrint", "1");
	private SalesFrcConfiguration conf;
	
	
	HttpPost loginInstance;
	
	private static final Log logging = Log.getLog(SalesfrcEntityManager.class);
	
	public SalesfrcEntityManager(SalesFrcConfiguration conf){
		this.conf=(SalesFrcConfiguration)conf;
	}

				
		private void logIntoService(){
			
			HttpClient httpclient = HttpClientBuilder.create().build();
			
			String loginURL = conf.getLoginURL() +
                    conf.getService()+
                    "&client_id=" + conf.getClientID() +
                    "&client_secret=" + conf.getClientSecret() +
                    "&username=" + conf.getUserName()+
                    "&password=" + conf.getPassword();
			
				loginInstance = new HttpPost(loginURL);
		        HttpResponse response = null;
		        
		        logging.info("The login URL value is: {0}", loginURL);
		        
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
		            System.out.println("Error authenticating to Force.com: "+statusCode);
		            try {
						System.out.println("*Error cause ->>> " + EntityUtils.toString(response.getEntity()));
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
		        System.out.println(response.getStatusLine());
		        System.out.println("####Successful login####");
		        System.out.println("  instance URL: "+loginInstanceUrl);
		        System.out.println("  access token/session ID: "+loginAccessToken);
		}
		
		
		public void qeueryEntity(String q ,String resourceEndPoint){
			logIntoService();
	    	System.out.println("-----------------Query1------------------------");
	    	
	    	HttpClient httpClient = HttpClientBuilder.create().build();
	    	
	    	String uri = scimBaseUri +"/"+ resourceEndPoint+"/" + q ;
	    	logging.info("qeury url: {0}", uri);
	    	HttpGet httpGet = new HttpGet(uri);
	    	logging.info("oauth2 header: {0}", oauthHeader);
	    	httpGet.addHeader(oauthHeader);
	    	httpGet.addHeader(prettyPrintHeader);
	    	
	    	try {
				HttpResponse response = httpClient.execute(httpGet);
				
				int statusCode = response.getStatusLine().getStatusCode();
				
				if(statusCode == 200){
					
					String responseString = EntityUtils.toString(response.getEntity());
					
					try {
						JSONObject json = new JSONObject(responseString);
						System.out.println("Json from Q ->> \n" + json.toString(1) + "\n");
						
						//System.out.println("Json from Q ->> \n" + responseString + "\n");
					} catch (JSONException e) {
						
						e.printStackTrace();
					}

				} else {
					
					 System.out.println("Query was unsuccessful. Status code returned is " + statusCode);
		                System.out.println("####An error has occured. Http status: " + response.getStatusLine().getStatusCode());
		                System.exit(-1);
				}
				
			} catch (IOException e) {
				
				e.printStackTrace();
			}
	    	loginInstance.releaseConnection();
	    	logging.info("Connection released");
	    }
		
	public void createEntity(String resourceEndPoint, JSONObject jsonObject){

	    	HttpClient httpClient = HttpClientBuilder.create().build();
	    	String uri = scimBaseUri +"/"+ resourceEndPoint;
	    	
	    	try {
	    		
				
				System.out.println(" New user jsonObj --->>>" + jsonObject);
				
				HttpPost httpPost = new HttpPost(uri);
				httpPost.addHeader(oauthHeader);
				httpPost.addHeader(prettyPrintHeader);
				
				
				StringEntity bodyContent = new StringEntity(jsonObject.toString(1));
				
				 bodyContent.setContentType("application/json");
				 httpPost.setEntity(bodyContent);
				 String responseString ;
				 try {
					HttpResponse response = httpClient.execute(httpPost);
					
					int statusCode = response.getStatusLine().getStatusCode();
					
					if(statusCode==201){
						System.out.println("####Creation of resource was succesfull####");
						
						responseString = EntityUtils.toString(response.getEntity());
						JSONObject json = new JSONObject(responseString);
						System.out.println("Json response ->> \n" + json.toString(1) + "\n");
						
					}
					
					else{
						responseString = EntityUtils.toString(response.getEntity());
						System.out.println("Query was unsuccessful. Status code returned is " + statusCode);
		                System.out.println("####An error has occured. Http status: " + responseString);
		                System.exit(-1);
						
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
	    }

	public void updateEntity(String q ,String resourceEndPoint, JSONObject jsonObject){
		
	HttpClient httpClient = HttpClientBuilder.create().build();

	   	String uri = scimBaseUri +"/" +resourceEndPoint +"/" + q;
	   	
	   	HttpPatch httpPatch = new HttpPatch(uri);
	   	
	   	httpPatch.addHeader(oauthHeader);
	   	httpPatch.addHeader(prettyPrintHeader);
	   	
	   	String responseString ;
			try {
				StringEntity bodyContent = new StringEntity(jsonObject.toString(1));
				 bodyContent.setContentType("application/json");
				 httpPatch.setEntity(bodyContent);

		    	HttpResponse response = httpClient.execute(httpPatch);
		    	
		    	int statusCode = response.getStatusLine().getStatusCode();
				
				if(statusCode==200||statusCode==201 ){
					System.out.println("####Update of resource was succesfull####");
					
					responseString = EntityUtils.toString(response.getEntity());
					JSONObject json = new JSONObject(responseString);
					System.out.println("Json response ->> \n" + json.toString(1) + "\n");
					
				}
				
				else{
					responseString = EntityUtils.toString(response.getEntity());
					System.out.println("Query was unsuccessful. Status code returned is " + statusCode);
	               System.out.println("####An error has occured. Http status: " + responseString);
	               System.exit(-1);
					
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
			
			
	   	
	   }

	public  void deleteEntity(String q ,String resourceEndPoint){
		HttpClient httpClient = HttpClientBuilder.create().build();

		String uri = scimBaseUri +"/"+resourceEndPoint+"/" + q;
		
		HttpDelete httpDelete = new HttpDelete(uri);
		httpDelete.addHeader(oauthHeader);
		httpDelete.addHeader(prettyPrintHeader);
		
		 String responseString ;
		
		try {
			HttpResponse response = httpClient.execute(httpDelete);
			
			int statusCode = response.getStatusLine().getStatusCode();
			
			if(statusCode==204 || statusCode==200){ /// malo by vracat 204 ale vrati 200 
				System.out.println("####Deletion of resource was succesfull####");
				
			}
			
			else if (statusCode == 404){
				
				System.out.println("####Resource not found or resource was already deleted####");
			}else{
				responseString = EntityUtils.toString(response.getEntity());
				System.out.println("Query was unsuccessful. Status code returned is " + statusCode);
	            System.out.println("####An error has occured. Http status: " + responseString);
	            System.exit(-1);
				
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
