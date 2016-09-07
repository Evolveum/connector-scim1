package com.evolveum.polygon.scim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.identityconnectors.common.logging.Log;
import org.json.JSONArray;
import org.json.JSONObject;

public class ParserSchemaScim {

	private static final String SUBATTRIBUTES = "subAttributes";
	private static final String MULTIVALUED = "multiValued";
	private static final String TYPE = "type";
	private static final String CANONICALVALUES = "canonicalValues";
	private static final String REFERENCETYPES = "referenceTypes";
	private static final String DEFAULT = "default";

	private Map<String, Map<String, Object>> attributeMap;
	private Map<String, String> hlAttributeMap;

	private List<Map<String, Map<String, Object>>> attributeMapList = new ArrayList<Map<String, Map<String, Object>>>();
	private List<Map<String, String>> hlAttributeMapList = new ArrayList<Map<String, String>>();

	private static final Log LOGGER = Log.getLog(ParserSchemaScim.class);

	public void parseSchema(JSONObject schemaJson, String providerName) {
		hlAttributeMap = new HashMap<String, String>();
		attributeMap = new HashMap<String, Map<String, Object>>();
		for (String attributeName : schemaJson.keySet()) {
			Object hlAttribute = schemaJson.get(attributeName);
			if (hlAttribute instanceof JSONArray) {

				for (int i = 0; i < ((JSONArray) hlAttribute).length(); i++) {
					JSONObject attribute = new JSONObject();
					attribute = ((JSONArray) hlAttribute).getJSONObject(i);

					attributeMap = parseAttribute(attribute, attributeMap, providerName);
				}

			} else {
				hlAttributeMap.put(attributeName, hlAttribute.toString());
			}
		}
		hlAttributeMapList.add(hlAttributeMap);
		attributeMapList.add(attributeMap);
	}

	public Map<String, Map<String, Object>> parseAttribute(JSONObject attribute,
			Map<String, Map<String, Object>> attributeMap, String providerName) {

		String attributeName = null;
		Boolean isComplex = false;
		Boolean isMultiValued = false;
		Map<String, Object> attributeObjects = new HashMap<String, Object>();
		Map<String, Object> subAttributeMap = new HashMap<String, Object>();

		if (attribute.has(SUBATTRIBUTES)) {
			boolean hasTypeValues = false;
			JSONArray subAttributes = new JSONArray();
			subAttributes = (JSONArray) attribute.get(SUBATTRIBUTES);
			if (attributeName == null) {
				for (String subAttributeNameKeys : attribute.keySet()) {
					if ("name".equals(subAttributeNameKeys)) {
						attributeName = attribute.get(subAttributeNameKeys).toString();
						break;
					}
				}
			}

			for (String nameKey : attribute.keySet()) {
				if (MULTIVALUED.equals(nameKey)) {
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
				if (TYPE.equals(typeKey)) {
					hasTypeValues = true;
					break;
				}
			}

			if (hasTypeValues) {
				Map<String, Object> typeObject = new HashMap<String, Object>();
				typeObject = (HashMap<String, Object>) subAttributeMap.get(TYPE);
				if (typeObject.containsKey(CANONICALVALUES) || typeObject.containsKey(REFERENCETYPES)) {
					JSONArray referenceValues = new JSONArray();
					if (typeObject.containsKey(CANONICALVALUES)) {
						referenceValues = (JSONArray) typeObject.get(CANONICALVALUES);
					} else {
						referenceValues = (JSONArray) typeObject.get(REFERENCETYPES);
					}

					StrategyFetcher fetcher = new StrategyFetcher();
					HandlingStrategy strategy = fetcher.fetchStrategy(providerName);

					for (int position = 0; position < referenceValues.length(); position++) {

						Map<String, Object> processedParameters = strategy.translateReferenceValues(attributeMap,
								referenceValues, subAttributeMap, position, attributeName);

						for (String parameterName : processedParameters.keySet()) {
							if ("isComplex".equals(parameterName)) {

								isComplex = (Boolean) processedParameters.get(parameterName);

							} else {
								attributeMap = (Map<String, Map<String, Object>>) processedParameters
										.get(parameterName);
							}

						}

					}
				} else {
					List<String> defaultReferenceTypeValues = new ArrayList<String>();
					defaultReferenceTypeValues.add("User");
					defaultReferenceTypeValues.add("Group");

					defaultReferenceTypeValues.add("external");
					defaultReferenceTypeValues.add("uri");

					for (String subAttributeKeyNames : subAttributeMap.keySet()) {
						if (!TYPE.equals(subAttributeKeyNames)) {
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

						Map<String, Object> subattributeKeyMap = (HashMap<String, Object>) subAttributeMap
								.get(subAttributeKeyNames);

						for (String attributePropertie : subattributeKeyMap.keySet()) {

							if (MULTIVALUED.equals(attributePropertie)) {
								subattributeKeyMap.put(MULTIVALUED, true);
							}
						}
						attributeMap.put(complexAttrName.append(".").append(DEFAULT).append(".")
								.append(subAttributeKeyNames).toString(), subattributeKeyMap);
						isComplex = true;
					}
				}
			}

		} else {

			for (String attributeNameKeys : attribute.keySet()) {

				if ("name".equals(attributeNameKeys)) {
					attributeName = attribute.get(attributeNameKeys).toString();

				} else {
					attributeObjects.put(attributeNameKeys, attribute.get(attributeNameKeys));
				}

			}
		}
		if (!isComplex) {
			attributeMap.put(attributeName, attributeObjects);
		}
		return attributeMap;

	}

	public List<Map<String, Map<String, Object>>> getAttributeMapList(String providerName) {
		StrategyFetcher fetch = new StrategyFetcher();
		HandlingStrategy strategy = fetch.fetchStrategy(providerName);

		return strategy.getAttributeMapList(attributeMapList);

	}

	public List<Map<String, String>> gethlAttributeMapList() {
		return hlAttributeMapList;
	}

	public Map<String, Object> parseSubAttribute(JSONObject subAttribute, Map<String, Object> subAttributeMap) {
		Map<String, Object> attributeObjects = new HashMap<String, Object>();
		String subAttributeName = null;
		for (String subAttrName : subAttribute.keySet()) {

			if ("name".equals(subAttrName)) {
				subAttributeName = subAttribute.get(subAttrName).toString();
			} else {
				attributeObjects.put(subAttrName, subAttribute.get(subAttrName));
			}
		}
		LOGGER.info("The sub attribute which is being processed: {0}", subAttributeName);
		subAttributeMap.put(subAttributeName, attributeObjects);

		return subAttributeMap;
	}

}
