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
import org.identityconnectors.framework.common.objects.filter.AndFilter;
import org.identityconnectors.framework.common.objects.filter.ContainsFilter;
import org.identityconnectors.framework.common.objects.filter.EndsWithFilter;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.common.objects.filter.FilterBuilder;
import org.identityconnectors.framework.common.objects.filter.NotFilter;
import org.identityconnectors.framework.common.objects.filter.OrFilter;
import org.identityconnectors.framework.common.objects.filter.StartsWithFilter;
 
public class Main {

	public static final Uid TEST_UID = new Uid("00558000000VcXnAAK");
	public static final ArrayList<ConnectorObject> result = new ArrayList<>();
	
    public static void main(String[] args) {
    	
    	
    	
    	ObjectClass userC = ObjectClass.ACCOUNT;
    	ObjectClass groupC = ObjectClass.GROUP;
    	
    	EqualsFilter eq = (EqualsFilter)FilterBuilder.equalTo(AttributeBuilder.build("displayName","TesttestGroup2"));
    	
    	ContainsFilter con = (ContainsFilter)FilterBuilder.contains(AttributeBuilder.build("userName","john"));
    	
    	ContainsFilter ct = (ContainsFilter)FilterBuilder.contains(AttributeBuilder.build("userName","aeoinoaiedoiaedionasinad"));
    	
    
    	
    	OrFilter orf = (OrFilter) FilterBuilder.or(eq, ct);
    	
    	AndFilter andf = (AndFilter) FilterBuilder.and(con, orf);
    	
    	NotFilter not= (NotFilter)FilterBuilder.not(eq);
    
    	
    	//System.out.println(eq.);
    	
    	Attribute attribute = ((EqualsFilter) eq).getAttribute();
    	
    	String s = AttributeUtil.getStringValue(attribute);
    				
    	if(attribute instanceof Uid){
    		
    		
    		System.out.println("yeah = "+ eq.getName());
    		System.out.println("yeah = "+ s);
    		
    	}else {System.out.println("not Yeah");}
    	
    	SalesFrcConfiguration conf= new SalesFrcConfiguration();
    	SalesfrcConnector conn = new SalesfrcConnector();
    	conn.init(conf);
    	
    	/// METODA KTORU HLADAS!!!!! VVVVVV
    	conn.executeQuery(groupC, eq, handler, null);
    	/////
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

