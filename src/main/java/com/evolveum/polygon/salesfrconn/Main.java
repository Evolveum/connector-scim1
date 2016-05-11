package com.evolveum.polygon.salesfrconn;
 
import java.util.ArrayList;

import org.apache.commons.io.filefilter.NotFileFilter;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.ContainsFilter;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.common.objects.filter.FilterBuilder;
import org.identityconnectors.framework.common.objects.filter.NotFilter;
import org.identityconnectors.framework.common.objects.filter.StartsWithFilter;
 
public class Main {

	public static final Uid TEST_UID = new Uid("00558000000VcXnAAK");
	public static final ArrayList<ConnectorObject> result = new ArrayList<>();
	
    public static void main(String[] args) {
    	
    	
    	
    	ObjectClass userC = ObjectClass.ACCOUNT;
    	
    	
    	StartsWithFilter eq = (StartsWithFilter)FilterBuilder.startsWith(AttributeBuilder.build("userName","john"));
    	
    	ContainsFilter con = (ContainsFilter)FilterBuilder.contains(AttributeBuilder.build("userName","john"));
    	
    	NotFilter not= (NotFilter)FilterBuilder.not(eq);
    	
    	//System.out.println(eq.);
    	
    	Attribute attribute = ((StartsWithFilter) eq).getAttribute();
    	
    	String s = AttributeUtil.getStringValue(attribute);
    				
    	if(attribute instanceof Uid){
    		
    		
    		System.out.println("yeah = "+ eq.getName());
    		System.out.println("yeah = "+ s);
    		
    	}else {System.out.println("not Yeah");}
    	
    	SalesFrcConfiguration conf= new SalesFrcConfiguration();
    	SalesfrcConnector conn = new SalesfrcConnector();
    	conn.init(conf);
    	conn.executeQuery(userC, con, handler, null);
    	
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

