package com.test.slsfrc.salesfrcTest;

import java.security.DomainCombiner;

import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.spi.AbstractConfiguration;
import org.identityconnectors.framework.spi.ConfigurationProperty;
import org.identityconnectors.framework.spi.StatefulConfiguration;

public class SalesFrcConfiguration extends AbstractConfiguration implements StatefulConfiguration {

	private String domain = null;
	
	private String clientId;
	
	private GuardedString clientSecret = null;
	
	private GuardedString refreshToken = null;

	@ConfigurationProperty(order = 1, displayMessageKey = "domain.display",
            groupMessageKey = "basic.group", helpMessageKey = "domain.help", required = true,
            confidential = false)
	
	public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }
    
    @ConfigurationProperty(order = 2, displayMessageKey = "clientid.display",
            groupMessageKey = "basic.group", helpMessageKey = "clientid.help", required = true,
            confidential = false)
    
    public String getClientId(){
    	return clientId;
    	
    }
    
    public void setClientId(String clientId) {
		this.clientId = clientId;
	}
	
    @ConfigurationProperty(order = 3, displayMessageKey = "clientsecret.display",
            groupMessageKey = "basic.group", helpMessageKey = "clientsecret.help", required = true,
            confidential = true)
    
    public GuardedString getClientSecret() {
        return clientSecret;
    }
    
    public void setClientSecret(GuardedString clientSecret) {
        this.clientSecret = clientSecret;
    }
    
    @ConfigurationProperty(order = 4, displayMessageKey = "refreshtoken.display",
            groupMessageKey = "basic.group", helpMessageKey = "refreshtoken.help", required = true,
            confidential = true)
    public GuardedString getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(GuardedString refreshToken) {
        this.refreshToken = refreshToken;
    }
    
    
	
	@Override
	public void validate() {

		if (StringUtil.isBlank(domain)){
			throw new IllegalArgumentException("Domain cannot be null or empty");	
		}
		
		if (StringUtil.isBlank(clientId)){
			throw new IllegalArgumentException("Client Id cannot be null or empty");
		}
		
		if (null == clientSecret){
			throw new IllegalArgumentException("Client Secret cannot be null or empty.");
		}
		
		if (null == refreshToken) {
	            throw new IllegalArgumentException("Refresh Token cannot be null or empty.");
	        }
	}

	@Override
	public void release() {
		// TODO Auto-generated method stub
		
	}
	
	

}
