package com.evolveum.polygon.scim;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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

/**
 * A class that contains the methods needed for construction of json object
 * representations of provided data sets. Attributes are translated to json
 * objects and arrays of json objects depending on the attributes and
 * dictionary. The dictionary is set to translate the attributes to correspond
 * to the SCIM group core schema representation
 */
public class GroupDataBuilder implements ObjectTranslator {

	private static Map<String, String> objectNameDictionary = CollectionUtil.newCaseInsensitiveMap();

	private static final Log LOGGER = Log.getLog(UserDataBuilder.class);
	private static final String DELETE = "delete";
	private static final String DELIMITER = "\\.";
	private static final String DEFAULT = "default";
	private static final String TYPE = "type";

	private String operation;

	public GroupDataBuilder(String operation) {
		this.operation = operation;
	}

	static {
		objectNameDictionary.put("displayName", "displayName");

		objectNameDictionary.put("members.User.value", "");
		objectNameDictionary.put("members.User.display", "");
		objectNameDictionary.put("members.Group.value", "");
		objectNameDictionary.put("members.Group.display", "");

		objectNameDictionary.put("schemas", "schemas");
		objectNameDictionary.put("members.default.value", "");
		objectNameDictionary.put("members.default.display", "");

	}

	/**
	 * Constructs a json object representation out of the provided data set and
	 * schema dictionary. The json object representation will contain only
	 * attributes which comply to the provided schema and operation attributes
	 * as defined in the SCIM patch specification.
	 * 
	 * @param imsAttributes
	 *            A set of attributes provided by the identity management
	 *            system.
	 * @param injectedAttributes
	 *            A set of attributes which are injected into the provided set.
	 * @return The complete json representation of the provided data set.
	 */
	public JSONObject translateSetToJson(Set<Attribute> imsAttributes, Set<Attribute> injectedAttributes) {
		LOGGER.info("Building Json data from group attributes");

		JSONObject completeJsonObj = new JSONObject();

		Set<Attribute> multiLayerAttribute = new HashSet<Attribute>();

		if (injectedAttributes != null) {
			for (Attribute injectedAttribute : injectedAttributes) {
				String attributeName = injectedAttribute.getName();

				if (attributeName.contains(".")) {

					multiLayerAttribute.add(injectedAttribute);
				} else {

					completeJsonObj.put(attributeName, AttributeUtil.getSingleValue(injectedAttribute));
				}
			}

		}

		for (Attribute attribute : imsAttributes) {

			String attributeName = attribute.getName();

			if (objectNameDictionary.containsKey(attributeName)) {
				if (attributeName.contains(".")) {

					String[] keyParts = attributeName.split(DELIMITER); // e.g.
					// emails.work.value
					if (keyParts.length == 3) {
						multiLayerAttribute.add(attribute);
					} else {
						LOGGER.warn(
								"Attribute name not defined in group dictionary: {0}. Error ocourance while translating attribute set.",
								attributeName);
					}

				} else {

					completeJsonObj.put(attributeName, AttributeUtil.getSingleValue(attribute));
				}

			} else {
				LOGGER.warn(
						"Attribute name not defined in group dictionary: {0}. Error ocourance while translating attribute set.",
						attributeName);
			}
		}
		if (multiLayerAttribute != null) {
			buildLayeredAtrribute(multiLayerAttribute, completeJsonObj);
		}
		return completeJsonObj;
	}

	/**
	 * Builds a json object representation out of a provided set of
	 * "multi layered attributes". This type of attributes represent an array of
	 * simple or complex json objects.
	 * 
	 * @param multiLayerAttribute
	 *            A provided set of attributes.
	 * @param json
	 *            A json object which may already contain data added in previous
	 *            methods.
	 * @return A json representation of the provided data set.
	 */

	private JSONObject buildLayeredAtrribute(Set<Attribute> multiLayerAttribute, JSONObject json) {

		String mainAttributeName = "";
		List<String> checkedNames = new ArrayList<String>();
		for (Attribute i : multiLayerAttribute) {

			String attributeName = i.getName();
			String[] attributeNameParts = attributeName.split(DELIMITER); // e.q.
			// email.work.value

			if (checkedNames.contains(attributeNameParts[0])) {

			} else {
				Set<Attribute> subAttributeLayerSet = new HashSet<Attribute>();
				mainAttributeName = attributeNameParts[0];
				checkedNames.add(mainAttributeName);
				for (Attribute j : multiLayerAttribute) {

					String secondLoopAttributeName = j.getName();
					String[] secondLoopAttributeNameParts = secondLoopAttributeName.split(DELIMITER); // e.q.
					// email.work.value

					if (secondLoopAttributeNameParts[0].equals(mainAttributeName)) {
						subAttributeLayerSet.add(j);
					}
				}

				String canonicaltypeName = "";
				boolean writeToArray = true;
				JSONArray jArray = new JSONArray();

				List<String> checkedTypeNames = new ArrayList<String>();
				for (Attribute k : subAttributeLayerSet) {

					String nameFromSubSet = k.getName();
					String[] nameFromSubSetParts = nameFromSubSet.split(DELIMITER); // e.q.
					// email.work.value

					if (checkedTypeNames.contains(nameFromSubSetParts[1])) {
					} else {
						JSONObject multivalueObject = new JSONObject();
						canonicaltypeName = nameFromSubSetParts[1];

						checkedTypeNames.add(canonicaltypeName);
						for (Attribute subSetAttribute : subAttributeLayerSet) {
							String secondLoopNameFromSubSetParts = subSetAttribute.getName();
							String[] finalSubAttributeNameParts = secondLoopNameFromSubSetParts.split(DELIMITER); // e.q.
							// email.work.value
							if (finalSubAttributeNameParts[1].equals(canonicaltypeName)) {

								if (subSetAttribute.getValue() != null && subSetAttribute.getValue().size() > 1) {
									writeToArray = false;
									List<Object> valueList = subSetAttribute.getValue();

									for (Object attributeValue : valueList) {
										multivalueObject = new JSONObject();
										multivalueObject.put(finalSubAttributeNameParts[2], attributeValue);

										if (!DEFAULT.equals(nameFromSubSetParts[1])) {
											multivalueObject.put(TYPE, nameFromSubSetParts[1]);
										}
										if (operation != null) {
											if (DELETE.equals(operation)) {
												multivalueObject.put("operation", DELETE);
											}
										}
										jArray.put(multivalueObject);

									}

								} else {

									if (!"blank".equals(finalSubAttributeNameParts[2])) {
										multivalueObject.put(finalSubAttributeNameParts[2],
												AttributeUtil.getSingleValue(subSetAttribute));
									} else {

										jArray.put(AttributeUtil.getSingleValue(subSetAttribute));
										writeToArray = false;
									}

									if (!DEFAULT.equals(nameFromSubSetParts[1])) {
										multivalueObject.put(TYPE, nameFromSubSetParts[1]);
									}
									if (operation != null) {
										if (DELETE.equals(operation)) {
											multivalueObject.put("operation", DELETE);
										}
									}

								}
							}
						}
						if (writeToArray) {

							jArray.put(multivalueObject);
						}
					}
					json.put(nameFromSubSetParts[0], jArray);
				}

			}
		}

		return json;
	}

	/**
	 * Builds the "ObjectClassInfo" object which carries the schema information
	 * for a single resource.
	 * 
	 * @return An instance of ObjectClassInfo with the constructed schema
	 *         information.
	 **/
	public static ObjectClassInfo getGroupSchema() {

		ObjectClassInfoBuilder builder = new ObjectClassInfoBuilder();

		builder.setType(ObjectClass.GROUP_NAME);
		builder.addAttributeInfo(Name.INFO);

		builder.addAttributeInfo(AttributeInfoBuilder.define("displayName").setRequired(true).build());

		builder.addAttributeInfo(AttributeInfoBuilder.define("members.Group.value").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("members.Group.display").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("members.User.value").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("members.User.display").build());

		builder.addAttributeInfo(AttributeInfoBuilder.define("members.default.value").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("members.default.display").build());

		return builder.build();
	}
}