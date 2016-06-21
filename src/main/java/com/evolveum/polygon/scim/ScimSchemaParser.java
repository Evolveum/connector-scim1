package com.evolveum.polygon.scim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.identityconnectors.common.logging.Log;
import org.json.JSONArray;
import org.json.JSONObject;

public class ScimSchemaParser {
	
	private Map<String, Map<String, Object>> attributeMap;
	private Map<String, String> hlAttributeMap;
	
	private List<Map<String, Map<String, Object>>> attributeMapList = new ArrayList<Map<String, Map<String, Object>>>();
	private List<Map<String, String>> hlAttributeMapList = new ArrayList<Map<String, String>>();
	
	private static final Log LOGGER = Log.getLog(ScimSchemaParser.class);

	public void parseSchema(JSONObject schemaJson) {
		hlAttributeMap = new HashMap<String, String>();
		attributeMap = new HashMap<String, Map<String, Object>>();
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

		hlAttributeMapList.add(hlAttributeMap);
		attributeMapList.add(attributeMap);
	}

	private void parseAttribute(JSONObject attribute) {
		String attributeName = null;
		Boolean isComplex = false;
		Boolean isMultiValued = false;
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
				
				for (String nameKey : attribute.keySet()) {
					if (nameKey.intern() == "multiValued") {
						isMultiValued = (Boolean)attribute.get(nameKey);
						break;
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
						break;
					}
				}
					if (hasTypeValues){
						Map<String, Object> typeObject = new HashMap<String, Object>();
						typeObject = (Map<String, Object>) subAttributeMap.get("type");
						if (typeObject.containsKey("canonicalValues")|| typeObject.containsKey("referenceTypes")) {
							JSONArray referenceValues = new JSONArray();
							if (typeObject.containsKey("canonicalValues")){
								referenceValues = (JSONArray) typeObject.get("canonicalValues");
								}else {
									referenceValues = (JSONArray) typeObject.get("referenceTypes");
								}
							
							for (int j = 0; j < referenceValues.length(); j++) {
								JSONObject referenceValue = new JSONObject();
								referenceValue = ((JSONArray) referenceValues).getJSONObject(j);
								for (String k : subAttributeMap.keySet()) {
									if (k.intern() != "type") { // TODO some other complex attribute names may be used
										StringBuilder complexAttrName = new StringBuilder(attributeName);
										attributeMap.put(
												complexAttrName.append(".").append(referenceValue.get("value"))
												.append(".").append(k).toString(),
												(HashMap<String, Object>) subAttributeMap.get(k));
										isComplex=true;
										
									}
								}
							}
						}else {
							ArrayList<String> defaultReferenceTypeValues = new ArrayList<String>();
							defaultReferenceTypeValues.add("User");
							defaultReferenceTypeValues.add("Group");
							//defaultReferenceValues.add("external");
							//defaultReferenceValues.add("uri");
							for (String k : subAttributeMap.keySet()) {
								if (k.intern() != "type") {
									for(String defaultTypeReferenceValues: defaultReferenceTypeValues){
								StringBuilder complexAttrName = new StringBuilder(attributeName);
								complexAttrName.append(".").append(defaultTypeReferenceValues);
								attributeMap.put(
										complexAttrName.append(".").append(k).toString(),
										(HashMap<String, Object>) subAttributeMap.get(k));
								isComplex=true;
								}}
							
						}
							
							
						}

					}else{
						if (!isMultiValued){
						for (String k : subAttributeMap.keySet()) {
						StringBuilder complexAttrName = new StringBuilder(attributeName);
						attributeMap.put(
								complexAttrName.append(".").append(k).toString(),
								(HashMap<String, Object>) subAttributeMap.get(k));
						isComplex=true;
					}}else {
						for (String k : subAttributeMap.keySet()) {
							StringBuilder complexAttrName = new StringBuilder(attributeName);
							attributeMap.put(
									complexAttrName.append(".").append("default").append(".").append(k).toString(),
									(HashMap<String, Object>) subAttributeMap.get(k));
							isComplex=true;
					}
						
						
					}}
				
			} else {

				attributeObjects.put(key, attribute.get(key));
			}

		}
		if(!isComplex){
		attributeMap.put(attributeName, attributeObjects);
		}
	}

	public List<Map<String, Map<String, Object>>> getAttributeMapList() {
		return attributeMapList;

	}

	public List<Map<String, String>> gethlAttributeMapList() {
		return hlAttributeMapList;
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
