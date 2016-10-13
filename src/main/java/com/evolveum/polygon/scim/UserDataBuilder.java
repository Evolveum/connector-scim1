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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.InvalidAttributeValueException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.ObjectClassInfoBuilder;
import org.identityconnectors.framework.common.objects.OperationalAttributeInfos;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.json.JSONArray;
import org.json.JSONObject;

import com.evolveum.polygon.common.GuardedStringAccessor;

/**
 * @author Macik
 * 
 *         A class that contains the methods needed for construction of json
 *         object representations of provided data sets. Attributes are
 *         translated to json objects and arrays of json objects depending on
 *         the attributes and dictionary. The dictionary is set to translate the
 *         attributes to correspond to the SCIM user core schema representation
 */

public class UserDataBuilder implements ObjectTranslator {

	private static final Log LOGGER = Log.getLog(UserDataBuilder.class);

	private static final String SCHEMAS = "schemas";

	private String operation;

	public UserDataBuilder(String operation) {
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
	 * @return The complete json representation of the provided data set.
	 */

	public JSONObject translateSetToJson(Set<Attribute> imsAttributes, Set<Attribute> injectedAttributes) {

		LOGGER.info("Building account JsonObject");

		JSONObject completeJsonObj = new JSONObject();

		Set<Attribute> multiValueAttribute = new HashSet<Attribute>();
		Set<Attribute> multiLayerAttribute = new HashSet<Attribute>();

		if (injectedAttributes != null) {
			for (Attribute injectedAttribute : injectedAttributes) {
				String attributeName = injectedAttribute.getName();
				multiValueAttribute.add(injectedAttribute);

				if (attributeName.contains(DOT)) {

					String[] keyParts = attributeName.split(DELIMITER); // e.g.
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

			String attributeName = attribute.getName();

			if (SCHEMAS.equals(attributeName)) {

				attributeName = SCHEMAS;
				attribute = AttributeBuilder.build("schemas.default.blank", attribute.getValue());

			} else if (OperationalAttributes.ENABLE_NAME.equals(attributeName)) {
				completeJsonObj.put("active", AttributeUtil.getSingleValue(attribute));

			} else if (OperationalAttributes.PASSWORD_NAME.equals(attributeName)) {

				GuardedString passString = (GuardedString) AttributeUtil.getSingleValue(attribute);
				GuardedStringAccessor gsa = new GuardedStringAccessor();
				passString.access(gsa);

				completeJsonObj.put("password", gsa.getClearString());

			} else if (attributeName.contains(DOT)) {

				String[] keyParts = attributeName.split(DELIMITER);
				if (keyParts.length == 2) {

					multiValueAttribute.add(attribute);
				} else {
					multiLayerAttribute.add(attribute);
				}

			} else {

				completeJsonObj.put(attributeName, AttributeUtil.getSingleValue(attribute));
			}

		}

		if (multiValueAttribute != null) {
			buildMultivalueAttribute(multiValueAttribute, completeJsonObj);
		}

		if (multiLayerAttribute != null) {
			buildLayeredAttribute(multiLayerAttribute, completeJsonObj);
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

	private JSONObject buildLayeredAttribute(Set<Attribute> multiLayerAttribute, JSONObject json) {

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

				String canonicalTypeName = "";
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
						canonicalTypeName = nameFromSubSetParts[1];

						checkedTypeNames.add(canonicalTypeName);
						for (Attribute subSetAttribute : subAttributeLayerSet) {
							String secondLoopNameFromSubSetParts = subSetAttribute.getName();
							String[] finalSubAttributeNameParts = secondLoopNameFromSubSetParts.split(DELIMITER); // e.q.
							// email.work.value
							if (finalSubAttributeNameParts[1].equals(canonicalTypeName)) {

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
												multivalueObject.put(OPERATION, DELETE);
											}
										}
										jArray.put(multivalueObject);

									}

								} else {

									if (!BLANK.equals(finalSubAttributeNameParts[2])) {
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
											multivalueObject.put(OPERATION, DELETE);
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

		List<String> checkedNames = new ArrayList<String>();

		Set<Attribute> specialMlAttributes = new HashSet<Attribute>();
		for (Attribute i : multiValueAttribute) {
			String attributeName = i.getName();
			String[] attributeNameParts = attributeName.split(DELIMITER); // e.g.
			// name.givenName

			if (checkedNames.contains(attributeNameParts[0])) {
			} else {
				JSONObject jObject = new JSONObject();
				mainAttributeName = attributeNameParts[0];
				checkedNames.add(mainAttributeName);
				for (Attribute j : multiValueAttribute) {
					String secondLoopAttributeName = j.getName();
					String[] secondLoopAttributeNameParts = secondLoopAttributeName.split(DELIMITER); // e.g.
					// name.givenName
					if (secondLoopAttributeNameParts[0].equals(mainAttributeName)
							&& !mainAttributeName.equals(SCHEMA)) {
						jObject.put(secondLoopAttributeNameParts[1], AttributeUtil.getSingleValue(j));
					} else if (secondLoopAttributeNameParts[0].equals(mainAttributeName)
							&& mainAttributeName.equals(SCHEMA)) {
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
						String[] innerKeyParts = innerName.split(DELIMITER); // e.g.
						// name.givenName
						if (innerKeyParts[1].equals(TYPE) && !nameWasSet) {
							sMlAttributeName = AttributeUtil.getAsStringValue(specialAtribute);
							nameWasSet = true;
						} else if (!innerKeyParts[1].equals(TYPE)) {

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
	 * Builds the "ObjectClassInfo" object which carries the schema information
	 * for a single resource.
	 * 
	 * @return An instance of ObjectClassInfo with the constructed schema
	 *         information.
	 **/
	public static ObjectClassInfo getUserSchema() {

		ObjectClassInfoBuilder builder = new ObjectClassInfoBuilder();

		builder.addAttributeInfo(Name.INFO);

		builder.addAttributeInfo(AttributeInfoBuilder.define("userName").setRequired(true).build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("name.formatted").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("name.familyName").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("name.givenName").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("name.middleName").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("name.honorificPrefix").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("name.honorificSuffix").build());

		builder.addAttributeInfo(OperationalAttributeInfos.ENABLE);

		builder.addAttributeInfo(OperationalAttributeInfos.PASSWORD);

		builder.addAttributeInfo(AttributeInfoBuilder.define("displayName").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("nickName").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("title").build());

		builder.addAttributeInfo(AttributeInfoBuilder.define("userType").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("locale").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("preferredLanguage").build());

		builder.addAttributeInfo(AttributeInfoBuilder.define("id").build());

		builder.addAttributeInfo(AttributeInfoBuilder.define("emails.work.value").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("emails.work.primary").setType(Boolean.class).build());

		builder.addAttributeInfo(AttributeInfoBuilder.define("emails.home.value").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("emails.home.primary").setType(Boolean.class).build());

		builder.addAttributeInfo(AttributeInfoBuilder.define("emails.other.value").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("emails.other.primary").setType(Boolean.class).build());

		builder.addAttributeInfo(AttributeInfoBuilder.define("entitlements.default.value").build());
		builder.addAttributeInfo(
				AttributeInfoBuilder.define("entitlements.default.primary").setType(Boolean.class).build());

		builder.addAttributeInfo(AttributeInfoBuilder.define("phoneNumbers.work.value").build());
		builder.addAttributeInfo(
				AttributeInfoBuilder.define("phoneNumbers.work.primary").setType(Boolean.class).build());

		builder.addAttributeInfo(AttributeInfoBuilder.define("phoneNumbers.other.value").build());
		builder.addAttributeInfo(
				AttributeInfoBuilder.define("phoneNumbers.other.primary").setType(Boolean.class).build());

		builder.addAttributeInfo(AttributeInfoBuilder.define("phoneNumbers.pager.value").build());
		builder.addAttributeInfo(
				AttributeInfoBuilder.define("phoneNumbers.pager.primary").setType(Boolean.class).build());

		builder.addAttributeInfo(AttributeInfoBuilder.define("phoneNumbers.fax.value").build());
		builder.addAttributeInfo(
				AttributeInfoBuilder.define("phoneNumbers.fax.primary").setType(Boolean.class).build());

		builder.addAttributeInfo(AttributeInfoBuilder.define("phoneNumbers.mobile.value").build());
		builder.addAttributeInfo(
				AttributeInfoBuilder.define("phoneNumbers.mobile.primary").setType(Boolean.class).build());

		builder.addAttributeInfo(AttributeInfoBuilder.define("ims.aim.value").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("ims.aim.primary").setType(Boolean.class).build());

		builder.addAttributeInfo(AttributeInfoBuilder.define("ims.xmpp.value").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("ims.xmpp.primary").setType(Boolean.class).build());

		builder.addAttributeInfo(AttributeInfoBuilder.define("ims.skype.value").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("ims.skype.primary").setType(Boolean.class).build());

		builder.addAttributeInfo(AttributeInfoBuilder.define("ims.qq.value").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("ims.qq.primary").setType(Boolean.class).build());

		builder.addAttributeInfo(AttributeInfoBuilder.define("ims.yahoo.value").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("ims.yahoo.primary").setType(Boolean.class).build());

		builder.addAttributeInfo(AttributeInfoBuilder.define("ims.other.value").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("ims.other.primary").setType(Boolean.class).build());

		builder.addAttributeInfo(AttributeInfoBuilder.define("ims.msn.value").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("ims.msn.primary").setType(Boolean.class).build());

		builder.addAttributeInfo(AttributeInfoBuilder.define("ims.icq.value").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("ims.icq.primary").setType(Boolean.class).build());

		builder.addAttributeInfo(AttributeInfoBuilder.define("ims.gtalk.value").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("ims.gtalk.primary").setType(Boolean.class).build());

		builder.addAttributeInfo(AttributeInfoBuilder.define("photos.photo.value").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("photos.photo.primary").setType(Boolean.class).build());

		builder.addAttributeInfo(AttributeInfoBuilder.define("photos.thumbnail.value").build());
		builder.addAttributeInfo(
				AttributeInfoBuilder.define("photos.thumbnail.primary").setType(Boolean.class).build());

		builder.addAttributeInfo(AttributeInfoBuilder.define("addresses.work.streetAddress").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("addresses.work.locality").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("addresses.work.region").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("addresses.work.postalCode").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("addresses.work.country").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("addresses.work.formatted").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("addresses.work.primary").setType(Boolean.class).build());

		builder.addAttributeInfo(AttributeInfoBuilder.define("addresses.home.streetAddress").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("addresses.home.locality").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("addresses.home.region").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("addresses.home.postalCode").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("addresses.home.country").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("addresses.home.formatted").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("addresses.home.primary").setType(Boolean.class).build());

		builder.addAttributeInfo(AttributeInfoBuilder.define("addresses.other.streetAddress").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("addresses.other.locality").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("addresses.other.region").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("addresses.other.postalCode").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("addresses.other.country").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("addresses.other.formatted").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("addresses.other.primary").setType(Boolean.class).build());

		builder.addAttributeInfo(AttributeInfoBuilder.define("groups.default.value").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("groups.default.display").build());

		builder.addAttributeInfo(AttributeInfoBuilder.define("entitlements.default.display").build());

		builder.addAttributeInfo(AttributeInfoBuilder.define("roles.default.value").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("roles.default.display").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("roles.default.primary").setType(Boolean.class).build());

		return builder.build();
	}
}
