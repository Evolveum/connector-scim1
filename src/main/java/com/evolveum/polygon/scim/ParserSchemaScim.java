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

	/**
	 * Iterates trough the provided json for attribute. If an attribute is an
	 * instance of "JSONObject" then it's marked as a "higher layer" attribute
	 * (e.g."endpoint": "/Users") else if the attribute is an instance of
	 * "JSONArray" the attribute is processed further as a list of sub
	 * attributes (e.g. "attributes": [{...}]).
	 * 
	 * @param schemaJson
	 *            The provided "JSONObject" acquired from the service provider.
	 * @param strategy
	 *            The Handling strategy instance which should be used for
	 *            processing.
	 */

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

	/**
	 * Depending on the "handling strategy" the method returns a list of map
	 * representations of attributes and their sub attributes.
	 * 
	 * @param strategy
	 *            The handling strategy which should be used for processing.
	 * @return a list of map representations of attributes and their sub
	 *         attributes.
	 */

	public List<Map<String, Map<String, Object>>> getAttributeMapList(HandlingStrategy strategy) {
		return strategy.getAttributeMapList(attributeMapList);
	}

	/**
	 * A getter method which returns the list of map representations of higher
	 * layer attributes (e.g."endpoint": "/Users") .
	 * 
	 * @return the list of map representations of higher layer attributes
	 */

	public List<Map<String, String>> getHlAttributeMapList() {
		return hlAttributeMapList;
	}

	/**
	 * Iterates trough a "JSONObject" which represents a sub attribute and
	 * processes its parameters.
	 * 
	 * @param subAttribute
	 *            The "JSONObject" representing the sub attribute.
	 * 
	 * @param subAttributeMap
	 *            A map representation of all the processed sub attributes of
	 *            the attribute to which the sub attribute belongs.
	 * @return The provided "subAttributeMap" extended with the processed sub
	 *         attribute.
	 */

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
