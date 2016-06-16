package com.evolveum.polygon.scim;

import java.util.HashMap;
import java.util.Map;

import org.identityconnectors.common.logging.Log;
import org.json.JSONArray;
import org.json.JSONObject;

public class ScimSchemaParser {
	Map<String, Map<String, Object>> attributeMap = new HashMap<String, Map<String, Object>>();
	Map<String, String> hlAttributeMap = new HashMap<String, String>();

	private static final Log LOGGER = Log.getLog(ScimSchemaParser.class);

	public ScimSchemaParser(JSONObject schemaJson) {
		parseSchema(schemaJson);
	}

	private void parseSchema(JSONObject schemaJson) {
		for (String key : schemaJson.keySet()) {

			// Iterating trough higher layer attributes
			Object hlAttribute = schemaJson.get(key);
			if (hlAttribute instanceof JSONArray) {
				for (int i = 0; i < ((JSONArray) hlAttribute).length(); i++) {
					JSONObject attribute = new JSONObject();
					attribute = ((JSONArray) hlAttribute).getJSONObject(i);
					parseAttribute(attribute);
				}

			} else {
				hlAttributeMap.put(key, hlAttribute.toString());
			}

		}

		LOGGER.error("The hash map: {0}", attributeMap);
	}



	private void parseAttribute(JSONObject attribute) {
		String attributeName = null;
		Boolean isComplex = false;
		Map<String, Object> attributeObjects = new HashMap<String, Object>();
		Map<String, Object> subAttributeMap = new HashMap<String, Object>();
		for (String key : attribute.keySet()) {
			if (key.intern() == "name") {
				attributeName = attribute.get(key).toString();
			} else if (key.intern() == "subAttributes") {
				boolean hasTypeValues = false;
				JSONArray subAttribtues = new JSONArray();
				subAttribtues = (JSONArray) attribute.get(key);
				if (attributeName == null) {
					for (String nameKey : attribute.keySet()) {
						if (nameKey.intern() == "name") {
							attributeName = attribute.get(nameKey).toString();
							break;
						}
					}
				}
				for (int i = 0; i < subAttribtues.length(); i++) {
					JSONObject subAttribute = new JSONObject();
					subAttribute = subAttribtues.getJSONObject(i);
					subAttributeMap = parseSubAttribute(subAttribute, subAttributeMap);
				}
				for (String typeKey : subAttributeMap.keySet()) {
					if (typeKey.intern() == "type") {
						hasTypeValues = true;
					}
				}
					if (hasTypeValues){
						Map<String, Object> typeObject = new HashMap<String, Object>();
						typeObject = (Map<String, Object>) subAttributeMap.get("type");
						if (typeObject.containsKey("canonicalValues")) {
							JSONArray cannonicalValues = new JSONArray();
							cannonicalValues = (JSONArray) typeObject.get("canonicalValues");
							for (int j = 0; j < cannonicalValues.length(); j++) {
								JSONObject cannonicalValue = new JSONObject();
								cannonicalValue = ((JSONArray) cannonicalValues).getJSONObject(j);
								for (String k : subAttributeMap.keySet()) {
									if (k.intern() != "type") {
										StringBuilder complexAttrName = new StringBuilder(attributeName);
										attributeMap.put(
												complexAttrName.append(".").append(cannonicalValue.get("value"))
												.append(".").append(k).toString(),
												(HashMap<String, Object>) subAttributeMap.get(k));
										isComplex=true;
										
									}
								}
							}
						}

					}else{
						for (String k : subAttributeMap.keySet()) {
						StringBuilder complexAttrName = new StringBuilder(attributeName);
						attributeMap.put(
								complexAttrName.append(".").append(k).toString(),
								(HashMap<String, Object>) subAttributeMap.get(k));
						isComplex=true;
					}}
				
			} else {

				attributeObjects.put(key, attribute.get(key));
			}

		}
		if(!isComplex){
		attributeMap.put(attributeName, attributeObjects);
		}
	}

	public Map<String, Map<String, Object>> geAttributeMap() {
		return attributeMap;

	}

	public Map<String, String> gethlAttributeMap() {
		return hlAttributeMap;
	}

	private Map<String, Object> parseSubAttribute(JSONObject subAttribute, Map<String, Object> subAttributeMap) {
		HashMap<String, Object> attributeObjects = new HashMap<String, Object>();
		String subAttributeName = null;
		for (String key : subAttribute.keySet()) {
			if (key.intern() == "name") {
				subAttributeName = subAttribute.get(key).toString();
			} else {

				attributeObjects.put(key, subAttribute.get(key));

			}
		}
		subAttributeMap.put(subAttributeName, attributeObjects);

		return subAttributeMap;
	}

}
