package com.test.slsfrc.salesfrcTest;
 
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.acl.Group;

import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.apache.http.util.EntityUtils;
import org.apache.http.client.ClientProtocolException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.json.JSONArray;
import org.json.JSONException;
 
public class Main {
 
    static final String USERNAME     = "matus.macik@gmail.com";
    static final String PASSWORD     = "Iujm31hnPTlaN8tHPMgn3nn3lDS1fVZI";
    static final String LOGINURL     = "https://login.salesforce.com";
    static final String GRANTSERVICE = "/services/oauth2/token?grant_type=password";
    static final String CLIENTID     = "3MVG98_Psg5cppyZ.wx3xXhdg46KDzaNSwpQFRqKfsBdDnyHrNSTodpJ5il8ZAdSB4eIjlF3RagOYYXWz8vTB";
    static final String CLIENTSECRET = "8826126769332672628";
    
    
    private static String SCIM_ENDPOINT = "/services/scim";
    private static String SCIM_VERSION = "/v1";
    private static String scimBaseUri;
    private static Header oauthHeader;
    private static Header prettyPrintHeader = new BasicHeader("X-PrettyPrint", "1");
 
    public static void main(String[] args) {
 
        HttpClient httpclient = HttpClientBuilder.create().build();
 
        // Assemble the login request URL
        String loginURL = LOGINURL +
                          GRANTSERVICE +
                          "&client_id=" + CLIENTID +
                          "&client_secret=" + CLIENTSECRET +
                          "&username=" + USERNAME +
                          "&password=" + PASSWORD;
 System.out.println(loginURL);
        // Login requests must be POSTs
        HttpPost httpPost = new HttpPost(loginURL);
        HttpResponse response = null;
 
        try {
            // Execute the login POST request
            response = httpclient.execute(httpPost);
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
        scimBaseUri= loginInstanceUrl +SCIM_ENDPOINT+SCIM_VERSION;
        oauthHeader = new BasicHeader("Authorization", "OAuth " + loginAccessToken) ;
        System.out.println(response.getStatusLine());
        System.out.println("####Successful login####");
        System.out.println("  instance URL: "+loginInstanceUrl);
        System.out.println("  access token/session ID: "+loginAccessToken);
        
        
        JsonDataProvider jsonData = new JsonDataProvider();
       /*
        //######################  USER CRUD METHODS ###################
        UserManager user = new UserManager(jsonData.setUserObject(), scimBaseUri, oauthHeader, prettyPrintHeader);
        
        // user.createUsers();
       // user.deleteUser();
        //user.updateUser();
       // user.qeueryUsers("");
      
        //######################  GROUP CRUD METHODS ###################
         GroupManager group = new GroupManager(jsonData.setGroupObject(), scimBaseUri, oauthHeader, prettyPrintHeader);
         
         	//group.qeueryGroup();
         	//group.deleteGroup();
         	//group.createGroup();
         	//group.updateGroup();
         
       //######################  Entitlements CRUD METHODS ###################
         EntitlementsManager entitlement = new EntitlementsManager(jsonData.setEntitlementObject(), scimBaseUri, oauthHeader, prettyPrintHeader);
         
        // entitlement.qeueryEntitlement(); // Running
         //entitlement.createEntitlement(); //501 Not Implemented
         //entitlement.deleteEntitlement(); // 501 Not Implemented
          //entitlement.updateEntitlement();
         */
        
        
      //######################  More Gereric solution for CRUD METHODS ###################
         SalesfrcEntityManager entity = new SalesfrcEntityManager(jsonData.setUserObject(), scimBaseUri, oauthHeader, prettyPrintHeader);
         entity.qeueryEntity("00558000000VcXnAAK", "Users");
         	// release connection
        
        httpPost.releaseConnection();
    }
    }

