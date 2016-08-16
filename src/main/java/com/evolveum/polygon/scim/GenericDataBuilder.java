package com.evolveum.polygon.scim;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.InvalidAttributeValueException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * A class that contains the methods needed for construction of json object
 * representations of provided data sets. Attributes are translated to json
 * objects and arrays of json objects depending on the attributes and
 * dictionary.
 */
public class GenericDataBuilder implements ObjectTranslator {

	private static final Log LOGGER = Log.getLog(UserDataBuilder.class);

	private static final String DELETE = "delete";

	private String operation;

	/**
	 * Costructor used to populate the local variable "operation".
	 * 
	 * @param operation
	 *            String variable indicating that the "delete" operation
	 *            parameter should be added in the constructed json object.
	 **/
	public GenericDataBuilder(String operation) {
		this.operation = operation;

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
	 * @param attributeMap
	 *            A dictionary containing the attributes translated from the
	 *            provided schema. This dictionary is cross checked with the
	 *            processed attribute set and non matching attributes are
	 *            discarded.
	 * @return The complete json representation of the provided data set.
	 */
	public JSONObject translateSetToJson(Set<Attribute> imsAttributes, Set<Attribute> injectedAttributes,
			Map<String, Map<String, Object>> attributeMap) {

		LOGGER.info("Building account JsonObject");

		JSONObject completeJsonObj = new JSONObject();

		Set<Attribute> multiValueAttribute = new HashSet<Attribute>(); // e.g.
		// name.givenName
		Set<Attribute> multiLayerAttribute = new HashSet<Attribute>(); // e.g.
		// emails.work.value

		if (injectedAttributes != null) {
			for (Attribute injectedAttribute : injectedAttributes) {
				String attributeName = injectedAttribute.getName();
				multiValueAttribute.add(injectedAttribute);

				if (attributeName.contains(".")) {

					String[] keyParts = attributeName.split("\\."); // e.g.
					// schemas.default.blank
					if (keyParts.length == 2) {

						multiValueAttribute.add(injectedAttribute);
					} else {
						multiLayerAttribute.add(injectedAttribute);
					}
				} else {

					completeJsonObj.put(attributeName, AttributeUtil.getSingleValue(injectedAttribute));
				}
			}

		}

		for (Attribute attribute : imsAttributes) {

			LOGGER.info("Update or create set attribute: {0}", attribute);

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

			} else if ("__ENABLE__".equals(attributeName)) {
				completeJsonObj.put("active", AttributeUtil.getSingleValue(attribute));
			} else {
				LOGGER.warn("Attribute name not defined in dictionary: {0}", attributeName);
			}
		}

		if (multiValueAttribute != null) {
			buildMultivalueAttribute(multiValueAttribute, completeJsonObj);
		}

		if (multiLayerAttribute != null) {
			buildLayeredAtrribute(multiLayerAttribute, completeJsonObj);
		}
		LOGGER.info("Json object returned from json data builder: {0}", completeJsonObj);

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
	// TODO more efficient
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
				boolean writeToArray = true;
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
						for (Attribute subSetAttribute : subAttributeLayerSet) {
							String secondLoopNameFromSubSetParts = subSetAttribute.getName();
							String[] finalSubAttributeNameParts = secondLoopNameFromSubSetParts.split("\\."); // e.q.
							// email.work.value
							if (finalSubAttributeNameParts[1].intern().equals(canonicaltypeName)) {
								if (subSetAttribute.getValue() != null && subSetAttribute.getValue().size() > 1) {
									writeToArray = false;
									List<Object> valueList = subSetAttribute.getValue();

									for (Object attributeValue : valueList) {
										multivalueObject = new JSONObject();
										multivalueObject.put(finalSubAttributeNameParts[2].intern(), attributeValue);

										if (!"default".equals(nameFromSubSetParts[1].intern())) {
											multivalueObject.put("type", nameFromSubSetParts[1].intern());
										}
										if (operation != null) {
											if (DELETE.equals(operation)) {
												multivalueObject.put("operation", DELETE);
											}
										}
										jArray.put(multivalueObject);

									}

								} else {

									if (!"blank".equals(finalSubAttributeNameParts[2].intern())) {
										multivalueObject.put(finalSubAttributeNameParts[2].intern(),
												AttributeUtil.getSingleValue(subSetAttribute));
									} else {

										jArray.put(AttributeUtil.getSingleValue(subSetAttribute));
										writeToArray = false;
									}

									if (!"default".equals(nameFromSubSetParts[1].intern())) {
										multivalueObject.put("type", nameFromSubSetParts[1].intern());
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
	 * Builds a json object representation out of a provided set of
	 * "multi value attributes". This type of attributes represent a complex
	 * json object containing other key value pairs.
	 * 
	 * @param multiValueAttribute
	 *            A provided set of attributes.
	 * @param json
	 *            A json representation of the provided data set.
	 * 
	 * @return A json representation of the provided data set.
	 */
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

	/**
	 * Method not implemented in this class.
	 * 
	 * @return null
	 **/
	@Override
	public JSONObject translateSetToJson(Set<Attribute> imsAttributes, Set<Attribute> injectedAttributes) {

		return null;
	}
}
