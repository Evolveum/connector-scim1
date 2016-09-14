package com.evolveum.polygon.scim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.identityconnectors.common.logging.Log;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * 
 * @author Matus
 *
 *         Contains a set of methods used for translation of json schema
 *         representation objects. The schema representation objects are
 *         translated into maps an lists of maps representing the specific
 *         attributes, sub attributes and parameters for the service endpoints.
 */
public class ParserSchemaScim {

	private Map<String, Map<String, Object>> attributeMap;
	private Map<String, String> hlAttributeMap;

	private List<Map<String, Map<String, Object>>> attributeMapList = new ArrayList<Map<String, Map<String, Object>>>();
	private List<Map<String, String>> hlAttributeMapList = new ArrayList<Map<String, String>>();

	private static final Log LOGGER = Log.getLog(ParserSchemaScim.class);

	public void parseSchema(JSONObject schemaJson, HandlingStrategy strategy) {
		hlAttributeMap = new HashMap<String, String>();
		attributeMap = new HashMap<String, Map<String, Object>>();
		for (String attributeName : schemaJson.keySet()) {
			Object hlAttribute = schemaJson.get(attributeName);
			if (hlAttribute instanceof JSONArray) {

				for (int position = 0; position < ((JSONArray) hlAttribute).length(); position++) {
					JSONObject attribute = new JSONObject();
					attribute = ((JSONArray) hlAttribute).getJSONObject(position);

					attributeMap = strategy.parseSchemaAttribute(attribute, attributeMap, this);
				}

			} else {
				hlAttributeMap.put(attributeName, hlAttribute.toString());
			}
		}
		hlAttributeMapList.add(hlAttributeMap);
		attributeMapList.add(attributeMap);
	}

	public List<Map<String, Map<String, Object>>> getAttributeMapList(HandlingStrategy strategy) {
		return strategy.getAttributeMapList(attributeMapList);

	}

	public List<Map<String, String>> getHlAttributeMapList() {
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
