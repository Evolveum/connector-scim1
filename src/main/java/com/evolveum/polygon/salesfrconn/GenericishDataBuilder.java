package com.evolveum.polygon.salesfrconn;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.json.JSONArray;
import org.json.JSONObject;

public class GenericishDataBuilder {

	private static final Log LOGGER = Log.getLog(SalesfrcManager.class);
	
	public GenericishDataBuilder(){

	}

	public JSONObject setUserObject(Set<Attribute> attributes){

		JSONObject userObj = new JSONObject();

		for(Attribute at: attributes){

			String attributeName = at.getName();

			if(attributeName =="layeredAttrribute"){

				buildLayeredAtrribute(at, userObj);
			}
			else if(attributeName =="multiValueAttrribute"){

				buildMultiValueAtrribute(at,userObj);
			} else if (attributeName == null) {
				
				LOGGER.error("AttributeName has to be a not null value", attributeName);
			}else {
				
					userObj.put(attributeName, AttributeUtil.getSingleValue(at));
			}

		}

		return userObj;

	}


	private void buildLayeredAtrribute(Attribute at, JSONObject jsonObject){

		JSONArray jArray = new JSONArray();



		Map<String, Collection<Map<String, Object>>> keys = (Map<String, Collection<Map<String, Object>>>) AttributeUtil.getSingleValue(at);

		String attrName = null;

		for(String typeKey: keys.keySet()){
			attrName = typeKey;
			Collection<Map<String, Object>> typeLayer = keys.get(typeKey);	

			for(Map<String, Object> map: typeLayer){
				JSONObject arrayElement = new JSONObject();
				for(String s: map.keySet()){	
					arrayElement.put(s, map.get(s)); 
				}
				jArray.put(arrayElement);
				jsonObject.put(attrName, jArray);
			}


		}


	}

	private void buildMultiValueAtrribute(Attribute at, JSONObject object){
		JSONObject element = new JSONObject();
		
			Map<String,Map<String, Object>> multival= (Map<String, Map<String, Object>>) AttributeUtil.getSingleValue(at);
			
		for(String key: multival.keySet()){
			
			Map<String, Object> map= multival.get(key);
			
			for(String s : map.keySet()){
				
				element.put(s, map.get(s));
			}
			object.put(key, element);
			
		}
	}

}
