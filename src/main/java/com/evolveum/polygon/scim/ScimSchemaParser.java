package com.evolveum.polygon.scim;

import java.util.HashMap;
import java.util.Map;

import org.identityconnectors.common.logging.Log;
import org.json.JSONArray;
import org.json.JSONObject;

public class ScimSchemaParser {
	 Map<String, Map<String, Object>> attributeMap = new HashMap<String,Map<String, Object>>();
	 Map<String,String> hlAttributeMap = new HashMap<String,String>();
	
	 private static final Log LOGGER = Log.getLog(ScimSchemaParser.class);
	 
	 public ScimSchemaParser(JSONObject schemaJson){
		 parseSchema(schemaJson);
	 }
	 
private void parseSchema(JSONObject schemaJson){
	for(String key: schemaJson.keySet()){
		
		// Iterating trough higher layer attributes
		Object hlAttribute = schemaJson.get(key);
			if(hlAttribute instanceof JSONArray){
			for(int i=0; i<((JSONArray)hlAttribute).length();i++){
				JSONObject attribute = new JSONObject();
				attribute =((JSONArray) hlAttribute).getJSONObject(i);
				parseAttribute(attribute);
			}
			
		}else {
			hlAttributeMap.put(key,hlAttribute.toString());
		}
		
	}
	
	//TODO delete println
	//System.out.println(hlAttributeMap);
	//System.out.println(attributeMap);
	LOGGER.error("The hash map: {0}", attributeMap);
}
//
// TODO name.familyname & email.type.value ... 
//
private void parseAttribute(JSONObject attribute){
	String attributeName = null;
	Map<String, Object> attributeObjects = new HashMap<String, Object>();
	for(String key: attribute.keySet()){
		if (key.intern() == "name"){
			attributeName = attribute.get(key).toString();
		}else if(key.intern() == "subAttributes"){
			JSONArray subAttribtues = new JSONArray();
			subAttribtues = (JSONArray)attribute.get(key);
			for(int i = 0; i< subAttribtues.length(); i++){
				JSONObject subAttribute = new JSONObject();
				subAttribute = subAttribtues.getJSONObject(i);
				this.parseAttribute(subAttribute);
			}
		}else{
			attributeObjects.put(key, attribute.get(key));
		}
		
	}
	attributeMap.put(attributeName, attributeObjects);
}

public Map<String, Map<String, Object>> geAttributeMap() {
	return attributeMap;
	
}
	
public Map<String, String> gethlAttributeMap() {
	return hlAttributeMap;
}

}
