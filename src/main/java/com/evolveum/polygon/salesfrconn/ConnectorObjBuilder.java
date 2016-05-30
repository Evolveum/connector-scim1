package com.evolveum.polygon.salesfrconn;

import java.util.ArrayList;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.json.JSONArray;
import org.json.JSONObject;

// Class containing the method needed for building connector objects from json objects

public class ConnectorObjBuilder {

	private static final Log LOGGER = Log.getLog(ScimCrudManager.class);
	
	public ConnectorObject buildConnectorObject(JSONObject jsonObject){
		
		LOGGER.info("Building the connector object from provided json");
		
		if(jsonObject == null){
			LOGGER.error("Empty json object was passed from data provider");
			throw new IllegalArgumentException("Empty json object was passed from data provider ");
		}
		
		ConnectorObjectBuilder cob = new ConnectorObjectBuilder();
		
		cob.setUid(jsonObject.getString("id"));
		cob.setName(jsonObject.getString("id"));
		
		for(String key: jsonObject.keySet()){
		
			Object attribute =jsonObject.get(key);
				if(attribute instanceof JSONArray){
					JSONArray jArray = (JSONArray) attribute;
					ArrayList<String> list= new ArrayList<String>();
					
						for(Object o: jArray){
							
						list.add(o.toString());
							
						}
					cob.addAttribute(key, list);
					
				} else if(attribute instanceof JSONObject){
					for(String s: ((JSONObject) attribute).keySet()){
						
						cob.addAttribute(s,((JSONObject) attribute).get(s));
					}
					
				}
		}
		LOGGER.error("Heeeey im here!!! and with {0} ",cob.build());
		return cob.build();
		
	}
}
