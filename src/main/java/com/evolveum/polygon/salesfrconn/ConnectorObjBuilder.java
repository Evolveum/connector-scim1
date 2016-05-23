package com.evolveum.polygon.salesfrconn;
import java.awt.List;
import java.util.ArrayList;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.json.JSONArray;
import org.json.JSONObject;

public class ConnectorObjBuilder {

	private static final Log LOGGER = Log.getLog(SalesfrcManager.class);
	
	public ConnectorObject buildConnectorObject(JSONObject jsonObject){
		
		if(jsonObject == null){
			LOGGER.error("Empty json object");
			throw new IllegalArgumentException("Empty json object");
		}
		
		ConnectorObjectBuilder cob = new ConnectorObjectBuilder();
		
		cob.setUid(jsonObject.getString("id"));
		cob.setName(jsonObject.getString("id"));
		
		for(String a: jsonObject.keySet()){
		
			Object attribute =jsonObject.get(a);
				if(attribute instanceof JSONArray){
					JSONArray jArray = (JSONArray) attribute;
					ArrayList<String> list= new ArrayList<String>();
					
						for(Object o: jArray){
							
						list.add(o.toString());
							
						}
					cob.addAttribute(a, list);
					
				} else if(attribute instanceof JSONObject){
					for(String s: ((JSONObject) attribute).keySet()){
						
						cob.addAttribute(s,((JSONObject) attribute).get(s));
						
					}
					
					
					
				}
		}
		
		System.out.println(cob.build().toString());
		return cob.build();
		
	}
}
