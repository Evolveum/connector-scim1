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

public class GenericDataBuilder implements ObjectTranslator{

	private static Map<String, String> objectNameDictionary = CollectionUtil.newCaseInsensitiveMap();
	private static final Log LOGGER = Log.getLog(UserDataBuilder.class);


	public JSONObject translateSetToJson(Set<Attribute> imattributes, Set<Attribute> connattributes,
			Map<String, Map<String, Object>> attributeMap) {

		LOGGER.info("Building account JsonObject");
		
		JSONObject userObj = new JSONObject();

		Set<Attribute> multiValueAttribute = new HashSet<Attribute>();
		Set<Attribute> multiLayerAttribute = new HashSet<Attribute>();
		
		if (connattributes !=null){
			for (Attribute at: connattributes){				
				multiValueAttribute.add(at);
			}
			
		}

		for (Attribute at : imattributes) {

			String attributeName = at.getName();

			if (attributeMap.containsKey(attributeName)) {
				if (attributeName.contains(".")) {

					String[] keyParts = attributeName.split("\\.");
					if (keyParts.length == 2) {

						multiValueAttribute.add(at);
					} else {
						multiLayerAttribute.add(at);
					}

				} else {

					userObj.put(attributeName, AttributeUtil.getSingleValue(at));
				}

			} else {
				LOGGER.warn("Attribute name not defined in dictionary {0}", attributeName);
			}
		}

		if (multiValueAttribute != null) {
			buildMultivalueAttribute(multiValueAttribute, userObj);
		}

		if (multiLayerAttribute != null) {
			buildLayeredAtrribute(multiLayerAttribute, userObj);
		}
		return userObj;

	}

	private JSONObject buildLayeredAtrribute(Set<Attribute> attr, JSONObject json) {

		String name = "";
		ArrayList<String> checkedNames = new ArrayList<String>();
		for (Attribute i : attr) {

			String attributeName = i.getName();
			String[] keyParts = attributeName.split("\\.");

			if (checkedNames.contains(keyParts[0])) {

			} else {
				Set<Attribute> innerLayer = new HashSet<Attribute>();
				name = keyParts[0].intern();
				checkedNames.add(name);
				for (Attribute j : attr) {

					String innerName = j.getName();
					String[] innerKeyParts = innerName.split("\\.");

					if (innerKeyParts[0].equals(name)) {
						innerLayer.add(j);
					}
				}

				String typeName = "";
				JSONArray jArray = new JSONArray();

				ArrayList<String> checkedTypeNames = new ArrayList<String>();
				for (Attribute k : innerLayer) {

					String secondName = k.getName();
					String[] secondKeyPart = secondName.split("\\.");

					if (checkedTypeNames.contains(secondKeyPart[1].intern())) {
					} else {
						JSONObject multivalueObject = new JSONObject();
						typeName = secondKeyPart[1].intern();

						checkedTypeNames.add(typeName);
						for (Attribute l : innerLayer) {

							String innerTypeName = l.getName();
							String[] finalKey = innerTypeName.split("\\.");

							if (finalKey[1].intern().equals(typeName)) {
								multivalueObject.put(finalKey[2].intern(), AttributeUtil.getSingleValue(l));
							}
						}
						if (!secondKeyPart[1].intern().equals("")&&!secondKeyPart[1].intern().equals("default") ) {
							multivalueObject.put("type", secondKeyPart[1].intern());
						}
						jArray.put(multivalueObject);
					}
					json.put(secondKeyPart[0], jArray);
				}

			}
		}
		return json;
	}

	public JSONObject buildMultivalueAttribute(Set<Attribute> attr, JSONObject json) {

		String name = "";

		ArrayList<String> checkedNames = new ArrayList<String>();

		Set<Attribute> specialMlAttributes = new HashSet<Attribute>();
		for (Attribute i : attr) {
			String attributeName = i.getName();
			String[] keyParts = attributeName.split("\\.");

			if (checkedNames.contains(keyParts[0].intern())) {
			} else {
				JSONObject jObject = new JSONObject();
				name = keyParts[0].intern();
				checkedNames.add(name);
				for (Attribute j : attr) {
					String innerName = j.getName();
					String[] innerKeyParts = innerName.split("\\.");
					if (innerKeyParts[0].intern().equals(name) && !name.equals("schema")) {
						jObject.put(innerKeyParts[1], AttributeUtil.getSingleValue(j));
					} else if (innerKeyParts[0].intern().equals(name) && name.equals("schema")) {
						specialMlAttributes.add(j);

					}
				}
				if (specialMlAttributes.isEmpty()) {
					json.put(keyParts[0], jObject);
				}
				//
				else {
					String attrName = "No schema type";
					Boolean nameSet = false;

					for (Attribute sa : specialMlAttributes) {
						String innerName = sa.getName();
						String[] innerKeyParts = innerName.split("\\.");
						if (innerKeyParts[1].intern().equals("type") && !nameSet) {
							attrName = AttributeUtil.getAsStringValue(sa);
							nameSet = true;
						} else if (!innerKeyParts[1].intern().equals("type")) {

							jObject.put(innerKeyParts[1], AttributeUtil.getSingleValue(sa));
						}
					}
					if (nameSet) {

						json.put(attrName, jObject);
						specialMlAttributes.removeAll(specialMlAttributes);

					} else {
						LOGGER.error("Schema type not speciffied {0}. Error ocourance while translating user object attribute set: {0}", attrName);
						throw new InvalidAttributeValueException("Schema type not speciffied. Error ocourance while translating user object attribute set");
					}

				}
			}
		}

		return json;
	}

	@Override
	public JSONObject translateSetToJson(Set<Attribute> imattributes, Set<Attribute> connattributes) {
		// TODO Auto-generated method stub
		return null;
	}
}
