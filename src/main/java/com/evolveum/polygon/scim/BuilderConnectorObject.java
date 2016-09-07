package com.evolveum.polygon.scim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.json.JSONArray;
import org.json.JSONObject;

public class BuilderConnectorObject {
	
	private static final String TYPE = "type";
	private static final String DEFAULT = "default";

	private static final Log LOGGER = Log.getLog(BuilderConnectorObject.class);
	
	public ConnectorObject buildConnectorObject(JSONObject resourceJsonObject, String resourceEndPoint, String providerName)
			throws ConnectorException {

	LOGGER.info("Building the connector object from provided json");

	if (resourceJsonObject == null) {
		LOGGER.error(
				"Empty json object was passed from data provider. Error ocourance while building connector object");
		throw new ConnectorException(
				"Empty json object was passed from data provider. Error ocourance while building connector object");
	}

	ConnectorObjectBuilder cob = new ConnectorObjectBuilder();
	cob.setUid(resourceJsonObject.getString("id"));

	if ("Users".equals(resourceEndPoint)) {
		cob.setName(resourceJsonObject.getString("userName"));
	} else if ("Groups".equals(resourceEndPoint)) {

		cob.setName(resourceJsonObject.getString("displayName"));
		cob.setObjectClass(ObjectClass.GROUP);
	} else {
		cob.setName(resourceJsonObject.getString("displayName"));
		ObjectClass objectClass = new ObjectClass(resourceEndPoint);
		cob.setObjectClass(objectClass);

	}
	for (String key : resourceJsonObject.keySet()) {
		Object attribute = resourceJsonObject.get(key);
		List<String> excludedAttributes= new ArrayList<String>();

		StrategyFetcher fetcher = new StrategyFetcher();
		HandlingStrategy strategy = fetcher.fetchStrategy(providerName);
		
		excludedAttributes = strategy.excludeFromAssembly(excludedAttributes);

		if (excludedAttributes.contains(key)) {
			LOGGER.warn("The attribute \"{0}\" was omitted from the connId object build.", key);
		} else

		if (attribute instanceof JSONArray) {

			JSONArray jArray = (JSONArray) attribute;

			Map<String, Collection<Object>> multivaluedAttributeMap = new HashMap<String, Collection<Object>>();
			Collection<Object> attributeValues = new ArrayList<Object>();

			for (Object o : jArray) {
				StringBuilder objectNameBilder = new StringBuilder(key);
				String objectKeyName = "";
				if (o instanceof JSONObject) {
					for (String s : ((JSONObject) o).keySet()) {
						if (TYPE.equals(s)) {
							objectKeyName = objectNameBilder.append(".").append(((JSONObject) o).get(s)).toString();
							objectNameBilder.delete(0, objectNameBilder.length());
							break;
						}
					}

					for (String s : ((JSONObject) o).keySet()) {

						if (TYPE.equals(s)) {
						} else {

							if (!"".equals(objectKeyName)) {
								objectNameBilder = objectNameBilder.append(objectKeyName).append(".").append(s);
							} else {
								objectKeyName = objectNameBilder.append(".").append(DEFAULT).toString();
								objectNameBilder = objectNameBilder.append(".").append(s);
							}

							if (attributeValues.isEmpty()) {
								attributeValues.add(((JSONObject) o).get(s));
								multivaluedAttributeMap.put(objectNameBilder.toString(), attributeValues);
							} else {
								if (multivaluedAttributeMap.containsKey(objectNameBilder.toString())) {
									attributeValues = multivaluedAttributeMap.get(objectNameBilder.toString());
									attributeValues.add(((JSONObject) o).get(s));
								} else {
									Collection<Object> newAttributeValues = new ArrayList<Object>();
									newAttributeValues.add(((JSONObject) o).get(s));
									multivaluedAttributeMap.put(objectNameBilder.toString(), newAttributeValues);
								}

							}
							objectNameBilder.delete(0, objectNameBilder.length());

						}
					}

					//

				} else {
					objectKeyName = objectNameBilder.append(".").append(o.toString()).toString();
					cob.addAttribute(objectKeyName, o);
				}
			}

			if (!multivaluedAttributeMap.isEmpty()) {
				for (String attributeName : multivaluedAttributeMap.keySet()) {
					cob.addAttribute(attributeName, multivaluedAttributeMap.get(attributeName));
				}

			}

		} else if (attribute instanceof JSONObject) {
			for (String s : ((JSONObject) attribute).keySet()) {

				StringBuilder objectNameBilder = new StringBuilder(key);
				cob.addAttribute(objectNameBilder.append(".").append(s).toString(),
						((JSONObject) attribute).get(s));

			}

		} else {

			if ("active".equals(key)) {
				cob.addAttribute("__ENABLE__", resourceJsonObject.get(key));
			} else {

				if (!resourceJsonObject.get(key).equals(null)) {

					cob.addAttribute(key, resourceJsonObject.get(key));
				} else {
					cob.addAttribute(key, "");

				}
			}
		}
	}
	ConnectorObject finalConnectorObject = cob.build();
	LOGGER.info("The connector object returned for the processed json: {0}", finalConnectorObject);
	return finalConnectorObject;
	
}
	}
