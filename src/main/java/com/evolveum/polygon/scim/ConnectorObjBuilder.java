package com.evolveum.polygon.scim;

import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Map;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.json.JSONArray;
import org.json.JSONObject;

// Class containing the method needed for building connector objects from json objects

public class ConnectorObjBuilder {

	private static final Log LOGGER = Log.getLog(ScimCrudManager.class);
	private static Map<String, String> objectNameDictionary = CollectionUtil.newCaseInsensitiveMap();
	
	static{
		
	objectNameDictionary.put("userName", "userName");
	//
	objectNameDictionary.put("id", "id");
	//
	objectNameDictionary.put("name.formatted", "formatted");
	objectNameDictionary.put("name.familyName", "familyName");
	objectNameDictionary.put("name.givenName", "givenName");
	objectNameDictionary.put("name.middleName", "middleName");
	objectNameDictionary.put("name.honorificPrefix", "honorificPrefix");
	objectNameDictionary.put("name.honorificSuffix", "honorificSuffix");

	objectNameDictionary.put("displayName", "displayName");
	objectNameDictionary.put("nickName", "nickName");
	
	}
	
	public ConnectorObject buildConnectorObject(JSONObject jsonObject) throws ConnectException{
		
		LOGGER.info("Building the connector object from provided json");
		
		if(jsonObject == null){
			LOGGER.error("Empty json object was passed from data provider. Error ocourance while building connector object");
			throw new ConnectException("Empty json object was passed from data provider. Error ocourance while building connector object");
		}
		
		ConnectorObjectBuilder cob = new ConnectorObjectBuilder();
		if (jsonObject.has("id")){
		cob.setUid(jsonObject.getString("id"));
		cob.setName(jsonObject.getString("id"));
		}
		
		for(String key: jsonObject.keySet()){
			Object attribute =jsonObject.get(key);
				if(attribute instanceof JSONArray){
					/*
					JSONArray jArray = (JSONArray) attribute;
					ArrayList<String> list= new ArrayList<String>();
					
						for(Object o: jArray){
						list.add(o.toString());
							
						}
					cob.addAttribute(key, list);
					
					*/
				} else if(attribute instanceof JSONObject){/*
					for(String s: ((JSONObject) attribute).keySet()){
						
						cob.addAttribute(s,((JSONObject) attribute).get(s));
					}
					*/
				} else { 
					for(String akey :objectNameDictionary.keySet()){
						if (objectNameDictionary.get(akey).intern()== key.intern()){
							cob.addAttribute(key.intern(), jsonObject.get(key));
						}
						
					}
					
				}
		}
		System.out.println(cob.build());
		return cob.build();
		
	}
}


/*
 * builder.addAttributeInfo(Name.INFO);

		builder.addAttributeInfo(AttributeInfoBuilder.define("userName").setRequired(true).build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("name.formatted").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("name.familyName").setRequired(true).build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("name.givenName").setRequired(true).build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("name.middleName").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("name.honorificPrefix").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("name.honorificSuffix").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("displayName").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("nickName").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("profileUrl").build());
 * 
 * */
 