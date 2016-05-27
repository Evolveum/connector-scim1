package com.evolveum.polygon.salesfrconn;


import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.spi.AbstractConfiguration;
import org.identityconnectors.framework.spi.ConfigurationProperty;
import org.identityconnectors.framework.spi.StatefulConfiguration;

public class ScimConnectorConfiguration extends AbstractConfiguration implements StatefulConfiguration {

	private static String SCIM_ENDPOINT = "/services/scim";
    private static String SCIM_VERSION = "/v1";
    
    private  String USERNAME = "matus.macik@gmail.com"; // TODO the user will have to provide this information/ better to change into non uri authentication
    private  String PASSWORD= "Iujm31hnPTlaN8tHPMgn3nn3lDS1fVZI";// TODO the user will have to provide this information
    private  String LOGINURL= "https://login.salesforce.com";
    private  String SERVICEGRANT = "/services/oauth2/token?grant_type=password";
    private  String CLIENTID= "3MVG98_Psg5cppyZ.wx3xXhdg46KDzaNSwpQFRqKfsBdDnyHrNSTodpJ5il8ZAdSB4eIjlF3RagOYYXWz8vTB";// TODO the user will have to provide this information
    private  String CLIENTSECRET= "8826126769332672628";

    private static final Log LOGGER = Log.getLog(ScimConnectorConfiguration.class);
    
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
            confidential = false)
    
    public String getPassword(){
    	return PASSWORD;
    	
    }
    
    public void setPassword(String passwd) {
		this.PASSWORD = passwd;
	}
	
    @ConfigurationProperty(order = 3, displayMessageKey = "CLIENTSECRET.display",
            groupMessageKey = "basic.group", helpMessageKey = "CLIENTSECRET.help", required = true,
            confidential = false)
    
    public String getClientSecret() {
        return CLIENTSECRET;
    }
    
    public void setClientSecret(String clientSecret) {
        this.CLIENTSECRET = clientSecret;
    }
    
    @ConfigurationProperty(order = 4, displayMessageKey = "CLIENTID.display",
            groupMessageKey = "basic.group", helpMessageKey = "CLIENTID.help", required = true,
            confidential = false)
    public String getClientID() {
        return CLIENTID;
    }

    public void setClientID(String clientID) {
        this.CLIENTID = clientID;
    }
 
    @ConfigurationProperty(order = 5, displayMessageKey = "SCIM_ENDPOINT.display",
            groupMessageKey = "basic.group", helpMessageKey = "SCIM_ENDPOINT.help", required = true,
            confidential = false)
    
    public String getEndpoint() {
        return SCIM_ENDPOINT;
    }

    public void setEndpoint(String endpoint) {
        this.SCIM_ENDPOINT = endpoint;
    }
  
    @ConfigurationProperty(order = 6, displayMessageKey = "SCIM_VERSION.display",
            groupMessageKey = "basic.group", helpMessageKey = "SCIM_VERSION.help", required = true,
            confidential = false)
    
    public String getVersion() {
        return SCIM_VERSION;
    }
    
    public void setVersion(String version) {
        this.SCIM_VERSION = version;
    }
    
    
    @ConfigurationProperty(order = 7, displayMessageKey = "LOGINURL.display",
            groupMessageKey = "basic.group", helpMessageKey = "LOGINURL.help", required = true,
            confidential = false)
    
    public String getLoginURL() {
        return LOGINURL;
    }
    
    public void setLoginURL(String loginURL) {
        this.LOGINURL = loginURL;
    }
    
    @ConfigurationProperty(order = 8, displayMessageKey = "SERVICEGRANT.display",
            groupMessageKey = "basic.group", helpMessageKey = "SERVICEGRANT.help", required = true,
            confidential = false)
    
    public String getService() {
        return SERVICEGRANT;
    }
    
    public void setService(String service) {
        this.SERVICEGRANT = service;
    }
	
	@Override
	public void validate() {

		if (StringUtil.isBlank(USERNAME)){
			throw new IllegalArgumentException("Username cannot be null or empty");	
		}
		
		if (StringUtil.isBlank(PASSWORD)){
			throw new IllegalArgumentException("Password cannot be null or empty");
		}
		
		if ( StringUtil.isBlank(CLIENTSECRET)){
			throw new IllegalArgumentException("Client Secret cannot be null or empty.");
		}
		
		if  (StringUtil.isBlank(USERNAME)){
	            throw new IllegalArgumentException("Client id cannot be null or empty.");
	        }
		LOGGER.info("Configuration valid");
	}

	@Override
	public void release() {
		// TODO Auto-generated method stub	
	}
	
}
