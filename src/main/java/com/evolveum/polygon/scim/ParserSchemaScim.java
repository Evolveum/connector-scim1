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

				HandlingStrategy strategy;
				if ("salesforce".equals(providerName)) {
					strategy = new SalesforceHandlingStrategy();

				} else if ("slack".equals(providerName)) {

					strategy = new SlackHandlingStrategy();

				} else {

					strategy = new StandardScimHandlingStrategy();
				}

				for (int i = 0; i < ((JSONArray) hlAttribute).length(); i++) {
					JSONObject attribute = new JSONObject();
					attribute = ((JSONArray) hlAttribute).getJSONObject(i);

					attributeMap = strategy.parseAttribute(attribute, attributeMap, this);
				}

			} else {
				hlAttributeMap.put(key, hlAttribute.toString());
			}
		}
		hlAttributeMapList.add(hlAttributeMap);
		attributeMapList.add(attributeMap);
	}

	public List<Map<String, Map<String, Object>>> getAttributeMapList() {
		HandlingStrategy strategy;

		if ("salesforce".equals(providerName)) {
			strategy = new SalesforceHandlingStrategy();
		} else if ("slack".equals(providerName)) {
			strategy = new SlackHandlingStrategy();
		} else {
			strategy = new StandardScimHandlingStrategy();
		}

		return strategy.getAttributeMapList(attributeMapList);

	}

	public List<Map<String, String>> gethlAttributeMapList() {
		return hlAttributeMapList;
	}

	public Map<String, Object> parseSubAttribute(JSONObject subAttribute, Map<String, Object> subAttributeMap) {
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
