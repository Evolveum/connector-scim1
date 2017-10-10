/*
 * Copyright (c) 2016 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
 * @author Macik
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
	private static final String OMMITATEDRIBUTE = "schema"; //TODO add to appropriate handling strategy
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
		LOGGER.info("Bellow is a logged message");
		LOGGER.info("The schema json which is about to be processed {0}", schemaJson);
		for (String attributeName : schemaJson.keySet()) {
			LOGGER.info("Processed attribute {0}", attributeName);
			Object hlAttribute = schemaJson.get(attributeName);
			if (!(OMMITATEDRIBUTE.equals(attributeName)) && hlAttribute instanceof JSONArray) {

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
		//LOGGER.info("The sub attribute which is being processed: {0}", subAttributeName);
		subAttributeMap.put(subAttributeName, attributeObjects);

		return subAttributeMap;
	}

}
