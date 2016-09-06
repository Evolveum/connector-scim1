package com.evolveum.polygon.scim;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.InvalidAttributeValueException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.ObjectClassInfoBuilder;
import org.identityconnectors.framework.common.objects.OperationalAttributeInfos;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * A class that contains the methods needed for construction of json object
 * representations of provided data sets. Attributes are translated to json
 * objects and arrays of json objects depending on the attributes and
 * dictionary. The dictionary is set to translate the attributes to correspond
 * to the SCIM user core schema representation
 */

public class UserDataBuilder implements ObjectTranslator {

	private static Map<String, String> objectNameDictionary = CollectionUtil.newCaseInsensitiveMap();
	private static final Log LOGGER = Log.getLog(UserDataBuilder.class);
	private static final String DELETE = "delete";
	private static final String DELIMITER = "\\.";
	private static final String DEFAULT = "default";
	private static final String TYPE = "type";
	private static final String SCHEMA = "schema";

	private String operation;

	static {
		objectNameDictionary.put("userName", "userName");

		objectNameDictionary.put("name.formatted", "formatted");
		objectNameDictionary.put("name.familyName", "familyName");
		objectNameDictionary.put("name.givenName", "givenName");
		objectNameDictionary.put("name.middleName", "middleName");
		objectNameDictionary.put("name.honorificPrefix", "honorificPrefix");
		objectNameDictionary.put("name.honorificSuffix", "honorificSuffix");

		objectNameDictionary.put("displayName", "displayName");
		objectNameDictionary.put("nickName", "nickName");
		objectNameDictionary.put("schemas", "schemas");

		objectNameDictionary.put("emails.work.value", "value");
		objectNameDictionary.put("emails.work.primary", "primary");

		objectNameDictionary.put("emails.home.value", "value");
		objectNameDictionary.put("emails.home.primary", "primary");

		objectNameDictionary.put("emails.other.value", "value");
		objectNameDictionary.put("emails.other.primary", "primary");

		objectNameDictionary.put("addresses.work.streetAddress", "streetAddress");
		objectNameDictionary.put("addresses.work.locality", "locality");
		objectNameDictionary.put("addresses.work.region", "region");
		objectNameDictionary.put("addresses.work.postalCode", "postalCode");
		objectNameDictionary.put("addresses.work.country", "country");
		objectNameDictionary.put("addresses.work.formatted", "formatted");
		objectNameDictionary.put("addresses.work.primary", "primary");

		objectNameDictionary.put("addresses.home.streetAddress", "streetAddress");
		objectNameDictionary.put("addresses.home.locality", "locality");
		objectNameDictionary.put("addresses.home.region", "region");
		objectNameDictionary.put("addresses.home.postalCode", "postalCode");
		objectNameDictionary.put("addresses.home.country", "country");
		objectNameDictionary.put("addresses.home.formatted", "formatted");
		objectNameDictionary.put("addresses.home.primary", "primary");

		objectNameDictionary.put("addresses.other.streetAddress", "streetAddress");
		objectNameDictionary.put("addresses.other.locality", "locality");
		objectNameDictionary.put("addresses.other.region", "region");
		objectNameDictionary.put("addresses.other.postalCode", "postalCode");
		objectNameDictionary.put("addresses.other.country", "country");
		objectNameDictionary.put("addresses.other.formatted", "formatted");
		objectNameDictionary.put("addresses.other.primary", "primary");

		objectNameDictionary.put("phoneNumbers.work.value", "value");
		objectNameDictionary.put("phoneNumbers.work.primary", "primary");

		objectNameDictionary.put("phoneNumbers.home.value", "value");
		objectNameDictionary.put("phoneNumbers.home.primary", "primary");

		objectNameDictionary.put("phoneNumbers.mobile.value", "value");
		objectNameDictionary.put("phoneNumbers.mobile.primary", "primary");

		objectNameDictionary.put("phoneNumbers.fax.value", "value");
		objectNameDictionary.put("phoneNumbers.fax.primary", "primary");

		objectNameDictionary.put("phoneNumbers.pager.value", "value");
		objectNameDictionary.put("phoneNumbers.pager.primary", "primary");

		objectNameDictionary.put("phoneNumbers.other.value", "value");
		objectNameDictionary.put("phoneNumbers.other.primary", "primary");

		objectNameDictionary.put("photos.photo.value", "value");
		objectNameDictionary.put("photos.photo.primary", "primary");

		objectNameDictionary.put("photos.thumbnail.value", "value");
		objectNameDictionary.put("photos.thumbnail.primary", "primary");

		objectNameDictionary.put("ims.aim.value", "value");
		objectNameDictionary.put("ims.aim.primary", "primary");

		objectNameDictionary.put("ims.gtalk.value", "value");
		objectNameDictionary.put("ims.gtalk.primary", "primary");

		objectNameDictionary.put("ims.icq.value", "value");
		objectNameDictionary.put("ims.icq.primary", "primary");

		objectNameDictionary.put("ims.msn.value", "value");
		objectNameDictionary.put("ims.msn.primary", "primary");

		objectNameDictionary.put("ims.xmpp.value", "value");
		objectNameDictionary.put("ims.xmpp.primary", "primary");

		objectNameDictionary.put("ims.skype.value", "value");
		objectNameDictionary.put("ims.skype.primary", "primary");

		objectNameDictionary.put("ims.qq.value", "value");
		objectNameDictionary.put("ims.qq.primary", "primary");

		objectNameDictionary.put("ims.yahoo.value", "value");
		objectNameDictionary.put("ims.yahoo.primary", "primary");

		objectNameDictionary.put("ims.other.value", "value");
		objectNameDictionary.put("ims.other.primary", "primary");

		objectNameDictionary.put("userType", "userType");
		objectNameDictionary.put("title", "title");
		objectNameDictionary.put("preferredLanguage", "preferredLanguage");
		objectNameDictionary.put("locale", "locale");

		objectNameDictionary.put("id", "id");
		objectNameDictionary.put("externalId", "externalId");
		objectNameDictionary.put("timezone", "timezone");
		objectNameDictionary.put("password", "password");

		objectNameDictionary.put("x509Certificates", "x509Certificates");
		objectNameDictionary.put("x509Certificates.value", "value");

		objectNameDictionary.put("entitlements.default.value", "value");
		objectNameDictionary.put("entitlements.default.primary", "primary");

		objectNameDictionary.put("schema.type", TYPE);
		objectNameDictionary.put("schema.organization", "organization");

		objectNameDictionary.put("roles.default.value", "value");
		objectNameDictionary.put("roles.default.display", "value");
	}

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

				if (attributeName.contains(".")) {

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

			if ("schemas".equals(attributeName)) {

				attributeName = "schemas";
				attribute = AttributeBuilder.build("schemas.default.blank", attribute.getValue());
			}

			if (objectNameDictionary.containsKey(attributeName)) {
				if (attributeName.contains(".")) {

					String[] keyParts = attributeName.split(DELIMITER);
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

		builder.addAttributeInfo(AttributeInfoBuilder.define("password").setUpdateable(true).build());

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
