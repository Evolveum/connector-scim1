package com.evolveum.polygon.scim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.InvalidAttributeValueException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.ObjectClassInfoBuilder;
import org.json.JSONArray;
import org.json.JSONObject;

public class GenericDataBuilder implements ObjectTranslator {

	private static final Log LOGGER = Log.getLog(UserDataBuilder.class);

	public JSONObject translateSetToJson(Set<Attribute> imsAttributes, Set<Attribute> injectedAttributes,
			Map<String, Map<String, Object>> attributeMap) {

		LOGGER.info("Building account JsonObject");

		JSONObject completeJsonObj = new JSONObject();

		Set<Attribute> multiValueAttribute = new HashSet<Attribute>(); // e.g.
																		// name.givenName
		Set<Attribute> multiLayerAttribute = new HashSet<Attribute>(); // e.g.
																		// emails.work.value

		if (injectedAttributes != null) {
			for (Attribute at : injectedAttributes) {
				multiValueAttribute.add(at);
			}

		} else {

			LOGGER.warn("No organization ID found in provider response");
		}

		for (Attribute attribute : imsAttributes) {

			String attributeName = attribute.getName();

			if (attributeMap.containsKey(attributeName)) {
				if (attributeName.contains(".")) {

					String[] keyParts = attributeName.split("\\."); // e.g.
																	// emails.work.value
					if (keyParts.length == 2) {

						multiValueAttribute.add(attribute);
					} else {
						multiLayerAttribute.add(attribute);
					}

				} else {
					
					
						completeJsonObj.put(attributeName, AttributeUtil.getSingleValue(attribute));
				}

			} else if("__ENABLE__".equals(attributeName)){
				completeJsonObj.put("active", AttributeUtil.getSingleValue(attribute));
			} else {
				LOGGER.warn("Attribute name not defined in dictionary {0}", attributeName);
			}
		}

		if (multiValueAttribute != null) {
			buildMultivalueAttribute(multiValueAttribute, completeJsonObj);
		}

		if (multiLayerAttribute != null) {
			buildLayeredAtrribute(multiLayerAttribute, completeJsonObj);
		}
		return completeJsonObj;

	}

	private JSONObject buildLayeredAtrribute(Set<Attribute> multiLayerAttribute, JSONObject json) {

		String mainAttributeName = "";
		ArrayList<String> checkedNames = new ArrayList<String>();
		for (Attribute i : multiLayerAttribute) {

			String attributeName = i.getName();
			String[] attributeNameParts = attributeName.split("\\."); // e.q.
																		// email.work.value

			if (checkedNames.contains(attributeNameParts[0])) {

			} else {
				Set<Attribute> subAttributeLayerSet = new HashSet<Attribute>();
				mainAttributeName = attributeNameParts[0].intern();
				checkedNames.add(mainAttributeName);
				for (Attribute j : multiLayerAttribute) {

					String secondLoopAttributeName = j.getName();
					String[] secondLoopAttributeNameParts = secondLoopAttributeName.split("\\."); // e.q.
																									// email.work.value

					if (secondLoopAttributeNameParts[0].equals(mainAttributeName)) {
						subAttributeLayerSet.add(j);
					}
				}

				String canonicaltypeName = "";
				JSONArray jArray = new JSONArray();

				ArrayList<String> checkedTypeNames = new ArrayList<String>();
				for (Attribute k : subAttributeLayerSet) {

					String nameFromSubSet = k.getName();
					String[] nameFromSubSetParts = nameFromSubSet.split("\\."); // e.q.
																				// email.work.value

					if (checkedTypeNames.contains(nameFromSubSetParts[1].intern())) {
					} else {
						JSONObject multivalueObject = new JSONObject();
						canonicaltypeName = nameFromSubSetParts[1].intern();

						checkedTypeNames.add(canonicaltypeName);
						for (Attribute l : subAttributeLayerSet) {

							String secondLoopNameFromSubSetParts = l.getName();
							String[] finalSubAttributeNameParts = secondLoopNameFromSubSetParts.split("\\."); // e.q.
																												// email.work.value

							if (finalSubAttributeNameParts[1].intern().equals(canonicaltypeName)) {
								multivalueObject.put(finalSubAttributeNameParts[2].intern(),
										AttributeUtil.getSingleValue(l));
							}
						}
						if (!nameFromSubSetParts[1].intern().equals("")
								&& !nameFromSubSetParts[1].intern().equals("default")) {
							multivalueObject.put("type", nameFromSubSetParts[1].intern());
						}
						jArray.put(multivalueObject);
					}
					json.put(nameFromSubSetParts[0], jArray);
				}

			}
		}
		return json;
	}

	public JSONObject buildMultivalueAttribute(Set<Attribute> multiValueAttribute, JSONObject json) {

		String mainAttributeName = "";

		ArrayList<String> checkedNames = new ArrayList<String>();

		Set<Attribute> specialMlAttributes = new HashSet<Attribute>();
		for (Attribute i : multiValueAttribute) {
			String attributeName = i.getName();
			String[] attributeNameParts = attributeName.split("\\."); // e.g.
																		// name.givenName

			if (checkedNames.contains(attributeNameParts[0].intern())) {
			} else {
				JSONObject jObject = new JSONObject();
				mainAttributeName = attributeNameParts[0].intern();
				checkedNames.add(mainAttributeName);
				for (Attribute j : multiValueAttribute) {
					String secondLoopAttributeName = j.getName();
					String[] secondLoopAttributeNameParts = secondLoopAttributeName.split("\\."); // e.g.
																									// name.givenName
					if (secondLoopAttributeNameParts[0].intern().equals(mainAttributeName)
							&& !mainAttributeName.equals("schema")) {
						jObject.put(secondLoopAttributeNameParts[1], AttributeUtil.getSingleValue(j));
					} else if (secondLoopAttributeNameParts[0].intern().equals(mainAttributeName)
							&& mainAttributeName.equals("schema")) {
						specialMlAttributes.add(j);

					}
				}
				if (specialMlAttributes.isEmpty()) {
					json.put(attributeNameParts[0], jObject);
				}
				//
				else {
					String sMlAttributeName = "No schema type";
					Boolean nameWasSet = false;

					for (Attribute specialAtribute : specialMlAttributes) {
						String innerName = specialAtribute.getName();
						String[] innerKeyParts = innerName.split("\\."); // e.g.
																			// name.givenName
						if (innerKeyParts[1].intern().equals("type") && !nameWasSet) {
							sMlAttributeName = AttributeUtil.getAsStringValue(specialAtribute);
							nameWasSet = true;
						} else if (!innerKeyParts[1].intern().equals("type")) {

							jObject.put(innerKeyParts[1], AttributeUtil.getSingleValue(specialAtribute));
						}
					}
					if (nameWasSet) {

						json.put(sMlAttributeName, jObject);
						specialMlAttributes.removeAll(specialMlAttributes);

					} else {
						LOGGER.error(
								"Schema type not speciffied {0}. Error ocourance while translating user object attribute set: {0}",
								sMlAttributeName);
						throw new InvalidAttributeValueException(
								"Schema type not speciffied. Error ocourance while translating user object attribute set");
					}

				}
			}
		}

		return json;
	}

	@Override
	public JSONObject translateSetToJson(Set<Attribute> imsAttributes, Set<Attribute> injectedAttributes) {
		// // method not implemented in this class
		return null;
	}
}
