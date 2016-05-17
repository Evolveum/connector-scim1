package com.evolveum.polygon.salesfrconn;
 
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.filefilter.NotFileFilter;
import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.Name;
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

import com.evolveum.polygon.test.slsfrc.JsonDataProvider;
 
public class Main {

	public static final Uid TEST_UID = new Uid("00558000000VcXnAAK");
	public static final ArrayList<ConnectorObject> result = new ArrayList<>();
	
    public static void main(String[] args) {
    	
    	
    	
    	ObjectClass userC = ObjectClass.ACCOUNT;
    	ObjectClass groupC = ObjectClass.GROUP;
    	
    	EqualsFilter aeq = (EqualsFilter)FilterBuilder.equalTo(TEST_UID);
    	
    /*TODO set for emails*/	EqualsFilter eq = (EqualsFilter)FilterBuilder.equalTo(AttributeBuilder.build("userName","johnsnow@winterfell.com"));
    	
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
    	
    	//// test 
    	
    	
    	
    	Set<Attribute> attrs = new HashSet<Attribute>();
        attrs.add(AttributeBuilder.build("userName", "stefan@stefansplace.com"));
        
        Map<String, String> names = CollectionUtil.newCaseInsensitiveMap();
        
        Map<String, String> foo = CollectionUtil.newCaseInsensitiveMap();
        
       Map<String, Map<String, Object>> phoneNumbers = CollectionUtil.newCaseInsensitiveMap();
       
       Map<String, Object> type = CollectionUtil.newCaseInsensitiveMap();
       
       Map<String, Map<String, Object>> emails = CollectionUtil.newCaseInsensitiveMap();
       
       Map<String, Object> emailAtr = CollectionUtil.newCaseInsensitiveMap();
       
       foo.put("organization", "00D58000000YfgfEAC");
       
       names.put("formatted", "Ms. Barbara J Jensen III");
       names.put("familyName", "Jensen");
       names.put("givenName", "Barbara");
       //names.put("middleName", "Jane");
       //names.put("honorificPrefix", "Ms.");
       //names.put("honorificSuffix", "III");
       
       emails.put("work",emailAtr);
       emailAtr.put("type", "work");
       emailAtr.put("value", "bjensen@example.com");
       emailAtr.put("primary", true);
        
       phoneNumbers.put("home", type);
        
        type.put("display", "Custom: Support Profile");
        type.put("value", "00e58000000qvhqAAA");
        type.put("primary", true);
        
        attrs.add(AttributeBuilder.build("nickName", "Babs"));
        attrs.add(AttributeBuilder.build("entitlements", phoneNumbers));
        attrs.add(AttributeBuilder.build("name", names));
        attrs.add(AttributeBuilder.build("emails", emails));
        attrs.add(AttributeBuilder.build("urn:scim:schemas:extension:enterprise:1.0", foo));
        
       /* for(Attribute at: attrs){
        	
        	if(at.getName() == "Telephone"){
        		Map<String, Map<String, String>> m = (Map<String, Map<String, String>>) (AttributeUtil.getSingleValue(at));
        		
        		for(String key: m.keySet()){
        			 System.out.println(m.get(key));
        			 Map<String, String> ma = m.get(key);
        			 
        			 for(String keey: ma.keySet()){
        				 
        				 System.out.println(ma.get(keey));
        			 }
        		}
        	}else{
        	
        System.out.println(AttributeUtil.getSingleValue(at));
        }}
        */
        ///test
        
    	/// METODA KTORU HLADAS!!!!! VVVVVV
       conn.create(userC, attrs, null);
    	conn.executeQuery(userC, aeq, handler, null);
    	
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

