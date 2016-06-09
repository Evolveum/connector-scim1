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
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.json.JSONArray;
import org.json.JSONObject;

// Class containing methods needed for building connector objects from json objects

public class ConnectorObjBuilder {
	
	// TODO there will be overhead because we need to get each name from the list of resources 
	// if we list all resources and that can be done only by searching for individual UIDs eg. 
	// ScimCrudManager class caller method of this class

	private static final Log LOGGER = Log.getLog(ScimCrudManager.class);
	private static Map<String, String> acountObjectNameDictionary = CollectionUtil.newCaseInsensitiveMap();
	private static Map<String, String> groupObjectNameDictionary = CollectionUtil.newCaseInsensitiveMap();
	static{
		
	//objectNameDictionary.put("userName", "userName");
	//
	acountObjectNameDictionary.put("id", "id");
	//
	
	acountObjectNameDictionary.put("name", "name");
	acountObjectNameDictionary.put("formatted", "name.formatted");
	acountObjectNameDictionary.put("familyName", "name.familyName");
	acountObjectNameDictionary.put("givenName", "name.givenName");
	acountObjectNameDictionary.put("middleName", "name.middleName");
	acountObjectNameDictionary.put("honorificPrefix", "name.honorificPrefix");
	acountObjectNameDictionary.put("honorificSuffix", "name.honorificSuffix");
	
	//acountObjectNameDictionary.put("emails", "emails");
	
	acountObjectNameDictionary.put("displayName", "displayName");
	acountObjectNameDictionary.put("nickName", "nickName");
	
	acountObjectNameDictionary.put("userType", "userType");
	acountObjectNameDictionary.put("userName", "userName");
	acountObjectNameDictionary.put("locale", "locale");
	acountObjectNameDictionary.put("preferredLanguage", "preferredLanguage");
	
	//Group dictionary definition 
	
	groupObjectNameDictionary.put("displayName", "displayName");
	//groupObjectNameDictionary.put("members", "members");
	
	groupObjectNameDictionary.put("display", "members..display");
	groupObjectNameDictionary.put("value", "members..value");
	
	}
	public ConnectorObject buildConnectorObject(JSONObject jsonObject) throws ConnectException{
		
		LOGGER.info("Building the connector object from provided json");
		
		if(jsonObject == null){
			LOGGER.error("Empty json object was passed from data provider. Error ocourance while building connector object");
			throw new ConnectException("Empty json object was passed from data provider. Error ocourance while building connector object");
		}
		
		ConnectorObjectBuilder cob = new ConnectorObjectBuilder();
		Map<String, String> objectNameDictionary = null;

		cob.setUid(jsonObject.getString("id"));
		
		if(!jsonObject.has("userName")){
			cob.setName(jsonObject.getString("displayName"));
			cob.setObjectClass(ObjectClass.GROUP);
			objectNameDictionary =groupObjectNameDictionary;
		}else{
		cob.setName(jsonObject.getString("userName"));
		objectNameDictionary =acountObjectNameDictionary;
		}
		for(String key: jsonObject.keySet()){
			Object attribute =jsonObject.get(key);
			
			if(objectNameDictionary.containsKey(key.intern())){
				if(attribute instanceof JSONArray){
					
					JSONArray jArray = (JSONArray) attribute;
					ArrayList<String> list= new ArrayList<String>();
					
						for(Object o: jArray){
						list.add(o.toString());
							
						}
					cob.addAttribute(key, list);
					
					
				} else if(attribute instanceof JSONObject){
					for(String s: ((JSONObject) attribute).keySet()){
						if(objectNameDictionary.containsKey(s.intern())){
						cob.addAttribute(objectNameDictionary.get(s),((JSONObject) attribute).get(s));
						
						}
					}
					
				} else { 
					
						cob.addAttribute(objectNameDictionary.get(key), jsonObject.get(key));
						
				}
		}}
		
	 LOGGER.error("Connector object {0}", cob.build());
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
 