package com.evolveum.polygon.scim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Contains methods needed for building "ConnectorObject" objects from json
 * objects which represent the queried resource.
 */
public class ConnectorObjBuilder {

	private static final Log LOGGER = Log.getLog(CrudManagerScim.class);

	/**
	 * Builds the connector object from the provided json object.
	 * 
	 * @param resourceJsonObject
	 *            The json object which is returned from the resource provider.
	 * @param resourceEndPoint
	 *            Name of the resource endpoint which will be used for the
	 *            correct object class assigment.
	 * @return The connector object which was build.
	 * @throws ConnectorException
	 *             In case of no data present in provided json object.
	 */

	public ConnectorObject buildConnectorObject(JSONObject resourceJsonObject, String resourceEndPoint)
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
			;
			cob.setObjectClass(objectClass);

		}
		for (String key : resourceJsonObject.keySet()) {
			Object attribute = resourceJsonObject.get(key);
			// Salesforce workaround
			if ("meta".equals(key.intern()) || "alias".equals(key.intern()) || "schemas".equals(key.intern())) {

				LOGGER.warn(
						"Processing trought salesforce \"schema inconsistencies\" workaround. Because of the \"{0}\" resoure attribute.",
						key.intern());
				// some inconsistencies found in meta attribute in
				// the schema definition present.
				// In the Schemas/ resource and the actual attributes in an
				// resource representation (salesForce) (meta.location)

			} else

				if (attribute instanceof JSONArray) {
					

					JSONArray jArray = (JSONArray) attribute;

					Map<String, Collection<Object>> multivaluedAttributeMap = new HashMap<String, Collection<Object>>();
					Collection<Object> attributeValues = new ArrayList<Object>();

					for (Object o : jArray) {
						StringBuilder objectNameBilder = new StringBuilder(key.intern());
						String objectKeyName = "";
						if (o instanceof JSONObject) {
							for (String s : ((JSONObject) o).keySet()) {

								if ("type".equals(s.intern())) {
									objectKeyName = objectNameBilder.append(".").append(((JSONObject) o).get(s)).toString();
									objectNameBilder.delete(0, objectNameBilder.length());
									break;
								}
							}

							for (String s : ((JSONObject) o).keySet()) {

								if ("type".equals(s.intern())) {
								} else {

									if (!"".equals(objectKeyName)) {
										objectNameBilder = objectNameBilder.append(objectKeyName).append(".")
												.append(s.intern());
									} else {
										objectKeyName = objectNameBilder.append(".").append("default").toString();
										objectNameBilder = objectNameBilder.append(".").append(s.intern());
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

						StringBuilder objectNameBilder = new StringBuilder(key.intern());
						cob.addAttribute(objectNameBilder.append(".").append(s).toString(),
								((JSONObject) attribute).get(s));
						
					}

				} else {

					if ("active".equals(key)) {
						cob.addAttribute("__ENABLE__", resourceJsonObject.get(key));
					} else {
						
					if (!resourceJsonObject.get(key).equals(null)){

						cob.addAttribute(key.intern(), resourceJsonObject.get(key));
					}else{
						cob.addAttribute(key.intern(), "");
						
					}
					}
				}
		}
		ConnectorObject finalConnectorObject = cob.build();
		LOGGER.info("The connector object returned for the processed json: {0}", finalConnectorObject);
		return finalConnectorObject;

	}
}