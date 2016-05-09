package com.evolveum.polygon.salesfrconn;
 
import java.awt.List;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.acl.Group;
import java.util.ArrayList;

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
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.common.objects.filter.FilterBuilder;
import org.apache.http.client.ClientProtocolException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.json.JSONArray;
import org.json.JSONException;
 
public class Main {

	public static final Uid TEST_UID = new Uid("00558000000VcXnAAK");
	public static final ArrayList<ConnectorObject> result = new ArrayList<>();
	
    public static void main(String[] args) {
    	
    	
    	
    	ObjectClass userC = ObjectClass.ACCOUNT;
    	
    	
    	Filter eq = FilterBuilder.equalTo(TEST_UID);
    	
    	//System.out.println(eq.);
    	
    	Attribute attribute = ((EqualsFilter) eq).getAttribute();
    	
    	String s = AttributeUtil.getStringValue(attribute);
    	
    	if(attribute instanceof Uid){
    		
    		System.out.println("yeah = "+ s);
    		
    	}else {System.out.println("not Yeah");}
    	
    	SalesFrcConfiguration conf= new SalesFrcConfiguration();
    	SalesfrcConnector conn = new SalesfrcConnector();
    	conn.init(conf);
    	conn.executeQuery(userC, eq, handler, null);
    	
    	for(int i=0;i<result.size();i++){
    	    System.out.println(result.get(i));
    	} 
    	
    }
    
    static ResultsHandler handler= new ResultsHandler() {
		
		@Override
		public boolean handle(ConnectorObject connectorObject) {
			result.add(connectorObject);
			return true;
		}
	};
    
    }

