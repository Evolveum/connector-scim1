package com.evolveum.polygon.scim;


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
    
	@ConfigurationProperty(order = 1, displayMessageKey = "Username",
            groupMessageKey = "basic.group", helpMessageKey = "Please provie the administrator user name.", required = true,
            confidential = false)
	
	public String getUserName() {
        return USERNAME;
    }

    public void setUserName(String username) {
        this.USERNAME = username;
    }
    
    @ConfigurationProperty(order = 2, displayMessageKey = "Password",
            groupMessageKey = "basic.group", helpMessageKey = "Please provide the administrator password used to connect to the scim enabled service", required = true,
            confidential = true)
    
    public String getPassword(){
    	return PASSWORD;
    	
    }
    
    public void setPassword(String passwd) {
		this.PASSWORD = passwd;
	}
	
    @ConfigurationProperty(order = 3, displayMessageKey = "Clientsecret",
            groupMessageKey = "basic.group", helpMessageKey = "Please provide the client token generated by the service endpoint for application interconnection", required = true,
            confidential = false)
    
    public String getClientSecret() {
        return CLIENTSECRET;
    }
    
    public void setClientSecret(String clientSecret) {
        this.CLIENTSECRET = clientSecret;
    }
    
    @ConfigurationProperty(order = 4, displayMessageKey = "Client ID",
            groupMessageKey = "basic.group", helpMessageKey = "Please provide the client ID generated by the service endpoint", required = true,
            confidential = false)
    public String getClientID() {
        return CLIENTID;
    }

    public void setClientID(String clientID) {
        this.CLIENTID = clientID;
    }
 
    @ConfigurationProperty(order = 5, displayMessageKey = "Scim endpoint",
            groupMessageKey = "basic.group", helpMessageKey = "Please provide the scim endpoint of the service provider", required = true,
            confidential = false)
    
    public String getEndpoint() {
        return SCIM_ENDPOINT;
    }

    public void setEndpoint(String endpoint) {
        this.SCIM_ENDPOINT = endpoint;
    }
  
    @ConfigurationProperty(order = 6, displayMessageKey = "Scim version",
            groupMessageKey = "basic.group", helpMessageKey = "Please provide the scim version which is supported by the service provider eq. /v1", required = true,
            confidential = false)
    
    public String getVersion() {
        return SCIM_VERSION;
    }
    
    public void setVersion(String version) {
        this.SCIM_VERSION = version;
    }
    
    @ConfigurationProperty(order = 7, displayMessageKey = "Login url",
            groupMessageKey = "basic.group", helpMessageKey = "Please provide the ULR address used to log into the service", required = true,
            confidential = false)
    
    public String getLoginURL() {
        return LOGINURL;
    }
    
    public void setLoginURL(String loginURL) {
        this.LOGINURL = loginURL;
    }
    
    @ConfigurationProperty(order = 8, displayMessageKey = "Service Grant",
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
			
	}
	
}