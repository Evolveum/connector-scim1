package com.evolveum.polygon.test.slsfrc;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import com.evolveum.polygon.scim.common.HttpPatch;

public class UserManager {

	private JSONObject userObj;
	private String scimBaseUri;
	private Header oauthHeader;
	private Header prettyPrintHeader;
	
	
	public UserManager(JSONObject userObj,String scimBaseUri, Header oath, Header prettyH){
		
		this.userObj = userObj;
		this.scimBaseUri = scimBaseUri;
		this.oauthHeader =oath;
		this.prettyPrintHeader = prettyH;
		
	}
	
	public void qeueryUsers(){
    	System.out.println("-----------------Query1------------------------");
    	
    	HttpClient httpClient = HttpClientBuilder.create().build();
    	
    	String userId= "00558000000VcXnAAK";
    	
    	String uri = scimBaseUri +"/Users/"+ userId ;
    	System.out.println("qeury url: " + uri);
    	HttpGet httpGet = new HttpGet(uri);
    	System.out.println("oauth2 header: " + oauthHeader);
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
    }
	
public void createUsers(){

    	HttpClient httpClient = HttpClientBuilder.create().build();
    	String uri = scimBaseUri +"/Users";
    	
    	try {
    		
			
			System.out.println(" New user jsonObj --->>>" + userObj);
			
			HttpPost httpPost = new HttpPost(uri);
			httpPost.addHeader(oauthHeader);
			httpPost.addHeader(prettyPrintHeader);
			
			
			StringEntity bodyContent = new StringEntity(userObj.toString(1));
			
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

public void updateUser(){
	
HttpClient httpClient = HttpClientBuilder.create().build();
   	
   	String userID = "00558000000VcXnAAK";
   	String uri = scimBaseUri +"/Users/" + userID;
   	
   	HttpPatch httpPatch = new HttpPatch(uri);
   	
   	httpPatch.addHeader(oauthHeader);
   	httpPatch.addHeader(prettyPrintHeader);
   	
   	String responseString ;
		try {
			StringEntity bodyContent = new StringEntity(userObj.toString(1));
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

public  void deleteUser(){
	HttpClient httpClient = HttpClientBuilder.create().build();
	
	String userID = "00558000000VbG0AAK";
	String uri = scimBaseUri +"/Users/" + userID;
	
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
