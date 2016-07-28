package com.evolveum.polygon.scim;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.ObjectClassInfoBuilder;
import org.json.JSONArray;
import org.json.JSONObject;

public class GroupDataBuilder implements ObjectTranslator {

	private static Map<String, String> objectNameDictionary = CollectionUtil.newCaseInsensitiveMap();
	private static final Log LOGGER = Log.getLog(UserDataBuilder.class);

	static {
		objectNameDictionary.put("displayName", "displayName");
	/*	objectNameDictionary.put("members.User.value", "value");
		objectNameDictionary.put("members.User.display", "display");
		objectNameDictionary.put("members.Group.value", "value");
		objectNameDictionary.put("members.Group.display", "display");
	*/
		objectNameDictionary.put("members.default.value", "value");
		objectNameDictionary.put("members.default.display", "display");
		
	}

	public JSONObject translateSetToJson(Set<Attribute> imsAttributes, Set<Attribute> injectedAttributes) {
		LOGGER.info("Building Json data from group attributes");

		JSONObject groupJsonObj = new JSONObject();

		Set<Attribute> multiLayerAttribute = new HashSet<Attribute>();

		for (Attribute attribute : imsAttributes) {

			String attributeName = attribute.getName();

			if (objectNameDictionary.containsKey(attributeName)) {
				if (attributeName.contains(".")) {

					String[] keyParts = attributeName.split("\\."); // e.g.
					// emails.work.value
					if (keyParts.length == 3) {
						multiLayerAttribute.add(attribute);
					} else {
						LOGGER.warn(
								"Attribute name not defined in group dictionary: {0}. Error ocourance while translating attribute set.",
								attributeName);
					}

				} else {

					groupJsonObj.put(attributeName, AttributeUtil.getSingleValue(attribute));
				}

			} else {
				LOGGER.warn(
						"Attribute name not defined in group dictionary: {0}. Error ocourance while translating attribute set.",
						attributeName);
			}
		}
		if (multiLayerAttribute != null) {
			buildLayeredAtrribute(multiLayerAttribute, groupJsonObj);
		}
		return groupJsonObj;
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

	public static ObjectClassInfo getGroupSchema() {

		ObjectClassInfoBuilder builder = new ObjectClassInfoBuilder();

		builder.setType(ObjectClass.GROUP_NAME);
		builder.addAttributeInfo(Name.INFO);

		builder.addAttributeInfo(AttributeInfoBuilder.define("displayName").setRequired(true).build());
		/*
		builder.addAttributeInfo(AttributeInfoBuilder.define("members.Group.value").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("members.Group.display").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("members.User.value").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("members.User.display").build());
		*/
		builder.addAttributeInfo(AttributeInfoBuilder.define("members.default.value").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("members.default.display").build());
		
		return builder.build();
	}

	@Override
	public JSONObject translateSetToJson(Set<Attribute> imattributes, Set<Attribute> connattributes,
			Map<String, Map<String, Object>> attributeMap) {
		// Method not implemented in this class.
		return null;
	}

}
