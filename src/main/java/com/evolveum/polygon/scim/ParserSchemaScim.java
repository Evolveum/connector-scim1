package com.evolveum.polygon.scim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.identityconnectors.common.logging.Log;
import org.json.JSONArray;
import org.json.JSONObject;

public class ParserSchemaScim {

	private Map<String, Map<String, Object>> attributeMap;
	private Map<String, String> hlAttributeMap;

	private List<Map<String, Map<String, Object>>> attributeMapList = new ArrayList<Map<String, Map<String, Object>>>();
	private List<Map<String, String>> hlAttributeMapList = new ArrayList<Map<String, String>>();

	private static final Log LOGGER = Log.getLog(ParserSchemaScim.class);

	private String providerName;

	public ParserSchemaScim(String providerName) {
		this.providerName = providerName;

	}

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

		// slack attribute "subAttributes" invalid naming workaround
		// ("subattributes")
		if (attribute.has("subAttributes") || attribute.has("subattributes")) {
			boolean hasTypeValues = false;
			JSONArray subAttributes = new JSONArray();
			if (attribute.has("subAttributes")) {
				subAttributes = (JSONArray) attribute.get("subAttributes");
			} else {

				subAttributes = (JSONArray) attribute.get("subattributes");
			}
			if (attributeName == null) {
				for (String subAttributeNameKeys : attribute.keySet()) {
					if ("name".equals(subAttributeNameKeys.intern())) {
						attributeName = attribute.get(subAttributeNameKeys).toString();
						break;
					}
				}
			}

			for (String nameKey : attribute.keySet()) {
				if ("multiValued".equals(nameKey.intern())) {
					isMultiValued = (Boolean) attribute.get(nameKey);
					break;
				}

			}

			for (int i = 0; i < subAttributes.length(); i++) {
				JSONObject subAttribute = new JSONObject();
				subAttribute = subAttributes.getJSONObject(i);
				subAttributeMap = parseSubAttribute(subAttribute, subAttributeMap);
			}
			for (String typeKey : subAttributeMap.keySet()) {
				if ("type".equals(typeKey.intern())) {
					hasTypeValues = true;
					break;
				}
			}

			if (hasTypeValues) {
				Map<String, Object> typeObject = new HashMap<String, Object>();
				typeObject = (Map<String, Object>) subAttributeMap.get("type");
				if (typeObject.containsKey("canonicalValues") || typeObject.containsKey("referenceTypes")) {
					JSONArray referenceValues = new JSONArray();
					if (typeObject.containsKey("canonicalValues")) {
						referenceValues = (JSONArray) typeObject.get("canonicalValues");
					} else {
						referenceValues = (JSONArray) typeObject.get("referenceTypes");
					}

					for (int j = 0; j < referenceValues.length(); j++) {
						JSONObject referenceValue = new JSONObject();

						/*
						 * Salesforce scim schema inconsistencies workaround
						 * (canonicalValues,referenceTypes) defined as array of
						 * json objects -> should be defined as array of string
						 * values
						 */
						if ("salesforce".equals(providerName)) {
							LOGGER.warn(
									"Processing trought Salesforce scim schema inconsistencies workaround (canonicalValues,referenceTypes) ");
							referenceValue = ((JSONArray) referenceValues).getJSONObject(j);
							for (String subAttributeKeyNames : subAttributeMap.keySet()) {
								if (!"type".equals(subAttributeKeyNames.intern())) { // TODO
									// some
									// other
									// complex
									// attribute
									// names
									// may
									// be
									// used
									StringBuilder complexAttrName = new StringBuilder(attributeName);
									attributeMap.put(
											complexAttrName.append(".").append(referenceValue.get("value")).append(".")
													.append(subAttributeKeyNames).toString(),
											(HashMap<String, Object>) subAttributeMap.get(subAttributeKeyNames));
									isComplex = true;

								}
							}
						} else {

							String sringReferenceValue = (String) referenceValues.get(j);
							for (String subAttributeKeyNames : subAttributeMap.keySet()) {
								if (!"type".equals(subAttributeKeyNames.intern())) { // TODO
									// some
									// other
									// complex
									// attribute
									// names
									// may
									// be
									// used
									StringBuilder complexAttrName = new StringBuilder(attributeName);
									attributeMap.put(
											complexAttrName.append(".").append(sringReferenceValue).append(".")
													.append(subAttributeKeyNames).toString(),
											(HashMap<String, Object>) subAttributeMap.get(subAttributeKeyNames));
									isComplex = true;

								}
							}

						}
					}
				} else {
					ArrayList<String> defaultReferenceTypeValues = new ArrayList<String>();
					defaultReferenceTypeValues.add("User");
					defaultReferenceTypeValues.add("Group");

					defaultReferenceTypeValues.add("external");
					defaultReferenceTypeValues.add("uri");

					for (String subAttributeKeyNames : subAttributeMap.keySet()) {
						if (!"type".equals(subAttributeKeyNames.intern())) {
							for (String defaultTypeReferenceValues : defaultReferenceTypeValues) {
								StringBuilder complexAttrName = new StringBuilder(attributeName);
								complexAttrName.append(".").append(defaultTypeReferenceValues);
								attributeMap.put(complexAttrName.append(".").append(subAttributeKeyNames).toString(),
										(HashMap<String, Object>) subAttributeMap.get(subAttributeKeyNames));
								isComplex = true;
							}
						}

					}

				}

			} else {

				// For Salesforce workaround purposes
				if ("salesforce".equals(providerName) && "roles".equals(attributeName)) {

					LOGGER.warn(
							"Processing trought salesforce \"schema inconsistencies\" workaround. Because of the \"{0}\" resoure attribute.",
							attributeName);

					isMultiValued = true;
				}

				if (!isMultiValued) {
					for (String subAttributeKeyNames : subAttributeMap.keySet()) {
						StringBuilder complexAttrName = new StringBuilder(attributeName);
						attributeMap.put(complexAttrName.append(".").append(subAttributeKeyNames).toString(),
								(HashMap<String, Object>) subAttributeMap.get(subAttributeKeyNames));
						isComplex = true;
					}
				} else {
					for (String subAttributeKeyNames : subAttributeMap.keySet()) {
						StringBuilder complexAttrName = new StringBuilder(attributeName);

						HashMap<String, Object> subattributeKeyMap = (HashMap<String, Object>) subAttributeMap
								.get(subAttributeKeyNames);

						for (String attributePropertie : subattributeKeyMap.keySet()) {

							if ("multiValued".equals(attributePropertie)) {
								subattributeKeyMap.put("multiValued", true);
							}
						}

						attributeMap.put(complexAttrName.append(".").append("default").append(".")
								.append(subAttributeKeyNames).toString(), subattributeKeyMap);
						isComplex = true;
					}

				}
			}

		} else {

			for (String attributeNameKeys : attribute.keySet()) {

				if ("name".equals(attributeNameKeys.intern())) {
					attributeName = attribute.get(attributeNameKeys).toString();

				} else {
					attributeObjects.put(attributeNameKeys, attribute.get(attributeNameKeys));
				}

			}
		}
		if (!isComplex) {
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
		for (String subAttributeKeyNames : subAttribute.keySet()) {
			if ("name".equals(subAttributeKeyNames.intern())) {
				subAttributeName = subAttribute.get(subAttributeKeyNames).toString();
			} else {
				attributeObjects.put(subAttributeKeyNames, subAttribute.get(subAttributeKeyNames));
			}
		}
		subAttributeMap.put(subAttributeName, attributeObjects);

		return subAttributeMap;
	}

}
