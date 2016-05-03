package com.test.slsfrc.salesfrcTest;

import java.security.DomainCombiner;

import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.spi.AbstractConfiguration;
import org.identityconnectors.framework.spi.ConfigurationProperty;
import org.identityconnectors.framework.spi.StatefulConfiguration;

public class SalesFrcConfiguration extends AbstractConfiguration implements StatefulConfiguration {

	private static String SCIM_ENDPOINT = "/services/scim";
    private static String SCIM_VERSION = "/v1";
    private static Header prettyPrintHeader = new BasicHeader("X-PrettyPrint", "1");
    private static String scimBaseUri;
    private static Header oauthHeader;
    
    
    private  String USERNAME;
    private  GuardedString PASSWORD;
    private  String LOGINURL= "https://login.salesforce.com";
    private  String SERVICEGRANT = "/services/oauth2/token?grant_type=password";
    private  GuardedString CLIENTID;
    private  GuardedString CLIENTSECRET;

	@ConfigurationProperty(order = 1, displayMessageKey = "USERNAME.display",
            groupMessageKey = "basic.group", helpMessageKey = "USERNAME.help", required = true,
            confidential = false)
	
	public String getUserName() {
        return USERNAME;
    }

    public void setUserName(String username) {
        this.USERNAME = username;
    }
    
    @ConfigurationProperty(order = 2, displayMessageKey = "PASSWORD.display",
            groupMessageKey = "basic.group", helpMessageKey = "PASSWORD.help", required = true,
            confidential = true)
    
    public GuardedString getPassword(){
    	return PASSWORD;
    	
    }
    
    public void setPassword(GuardedString passwd) {
		this.PASSWORD = passwd;
	}
	
    @ConfigurationProperty(order = 3, displayMessageKey = "CLIENTSECRET.display",
            groupMessageKey = "basic.group", helpMessageKey = "CLIENTSECRET.help", required = true,
            confidential = true)
    
    public GuardedString getClientSecret() {
        return CLIENTSECRET;
    }
    
    public void setClientSecret(GuardedString clientSecret) {
        this.CLIENTSECRET = clientSecret;
    }
    
    @ConfigurationProperty(order = 4, displayMessageKey = "refreshtoken.display",
            groupMessageKey = "basic.group", helpMessageKey = "refreshtoken.help", required = true,
            confidential = true)
    public GuardedString getClientID() {
        return CLIENTID;
    }

    public void setgetClientID(GuardedString clientID) {
        this.CLIENTID = clientID;
    }
    
    
	
	@Override
	public void validate() {

		if (StringUtil.isBlank(USERNAME)){
			throw new IllegalArgumentException("Username cannot be null or empty");	
		}
		
		if (PASSWORD == null){
			throw new IllegalArgumentException("Password cannot be null or empty");
		}
		
		if ( CLIENTSECRET == null){
			throw new IllegalArgumentException("Client Secret cannot be null or empty.");
		}
		
		if  (CLIENTID== null) {
	            throw new IllegalArgumentException("Client id cannot be null or empty.");
	        }
	}

	@Override
	public void release() {
		// TODO Auto-generated method stub
		
	}
	
	

}
