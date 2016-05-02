package com.test.slsfrc.salesfrcTest;

import java.security.DomainCombiner;

import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.spi.AbstractConfiguration;
import org.identityconnectors.framework.spi.StatefulConfiguration;

public class SalesFrcConfiguration extends AbstractConfiguration implements StatefulConfiguration {

	private String domain = null;
	
	private String clientId;
	
	private GuardedString clientSecret = null;
	
	private GuardedString refreshToken = null;

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
