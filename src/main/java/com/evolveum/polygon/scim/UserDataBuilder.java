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
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.ObjectClassInfoBuilder;
import org.json.JSONArray;
import org.json.JSONObject;

public class UserDataBuilder implements ObjectTranslator {

	private static Map<String, String> objectNameDictionaryUser = CollectionUtil.newCaseInsensitiveMap();
	private static final Log LOGGER = Log.getLog(UserDataBuilder.class);

	static {
		objectNameDictionaryUser.put("userName", "userName");

		objectNameDictionaryUser.put("name.formatted", "formatted");
		objectNameDictionaryUser.put("name.familyName", "familyName");
		objectNameDictionaryUser.put("name.givenName", "givenName");
		objectNameDictionaryUser.put("name.middleName", "middleName");
		objectNameDictionaryUser.put("name.honorificPrefix", "honorificPrefix");
		objectNameDictionaryUser.put("name.honorificSuffix", "honorificSuffix");

		objectNameDictionaryUser.put("displayName", "displayName");
		objectNameDictionaryUser.put("nickName", "nickName");
		// nameDictionaryUser.put("profileUrl","profileUrl");

		objectNameDictionaryUser.put("emails.work.value", "value");
		objectNameDictionaryUser.put("emails.work.primary", "primary");

		objectNameDictionaryUser.put("emails.home.value", "value");
		objectNameDictionaryUser.put("emails.home.primary", "primary");

		objectNameDictionaryUser.put("emails.other.value", "value");
		objectNameDictionaryUser.put("emails.other.primary", "primary");

		objectNameDictionaryUser.put("addresses.work.streetAddress", "streetAddress");
		objectNameDictionaryUser.put("addresses.work.locality", "locality");
		objectNameDictionaryUser.put("addresses.work.region", "region");
		objectNameDictionaryUser.put("addresses.work.postalCode", "postalCode");
		objectNameDictionaryUser.put("addresses.work.country", "country");
		objectNameDictionaryUser.put("addresses.work.formatted", "formatted");
		objectNameDictionaryUser.put("addresses.work.primary", "primary");

		objectNameDictionaryUser.put("addresses.home.streetAddress", "streetAddress");
		objectNameDictionaryUser.put("addresses.home.locality", "locality");
		objectNameDictionaryUser.put("addresses.home.region", "region");
		objectNameDictionaryUser.put("addresses.home.postalCode", "postalCode");
		objectNameDictionaryUser.put("addresses.home.country", "country");
		objectNameDictionaryUser.put("addresses.home.formatted", "formatted");
		objectNameDictionaryUser.put("addresses.home.primary", "primary");

		objectNameDictionaryUser.put("addresses.other.streetAddress", "streetAddress");
		objectNameDictionaryUser.put("addresses.other.locality", "locality");
		objectNameDictionaryUser.put("addresses.other.region", "region");
		objectNameDictionaryUser.put("addresses.other.postalCode", "postalCode");
		objectNameDictionaryUser.put("addresses.other.country", "country");
		objectNameDictionaryUser.put("addresses.other.formatted", "formatted");
		objectNameDictionaryUser.put("addresses.other.primary", "primary");

		objectNameDictionaryUser.put("phoneNumbers.work.value", "value");

		objectNameDictionaryUser.put("phoneNumbers.home.value", "value");

		objectNameDictionaryUser.put("phoneNumbers.mobile.value", "value");

		objectNameDictionaryUser.put("phoneNumbers.fax.value", "value");

		objectNameDictionaryUser.put("phoneNumbers.pager.value", "value");

		objectNameDictionaryUser.put("photos.photo.value", "value");

		objectNameDictionaryUser.put("photos.thumbnail.value", "value");

		objectNameDictionaryUser.put("ims.aim.value", "type");

		objectNameDictionaryUser.put("ims.gtalk.value", "type");

		objectNameDictionaryUser.put("ims.icq.value", "type");

		objectNameDictionaryUser.put("ims.msn.value", "type");

		objectNameDictionaryUser.put("ims.xmpp.value", "type");

		objectNameDictionaryUser.put("ims.skype.value", "type");

		objectNameDictionaryUser.put("ims.qq.value", "type");

		objectNameDictionaryUser.put("ims.yahoo.value", "type");

		objectNameDictionaryUser.put("ims.other.value", "type");

		objectNameDictionaryUser.put("userType", "userType");
		objectNameDictionaryUser.put("title", "title");
		objectNameDictionaryUser.put("preferredLanguage", "preferredLanguage");
		objectNameDictionaryUser.put("locale", "locale");

		objectNameDictionaryUser.put("id", "id");
		objectNameDictionaryUser.put("externalId", "externalId");
		objectNameDictionaryUser.put("timezone", "timezone");
		objectNameDictionaryUser.put("active", "active");
		objectNameDictionaryUser.put("password", "password");

		// nameDictionaryUser.put("groups.value","value");
		// nameDictionaryUser.put("groups.display","display");

		objectNameDictionaryUser.put("x509Certificates", "x509Certificates");
		objectNameDictionaryUser.put("x509Certificates.value", "value");

		objectNameDictionaryUser.put("entitlements.default.value", "value");
		objectNameDictionaryUser.put("entitlements.default.primary", "primary");

		objectNameDictionaryUser.put("schema.type", "type");
		objectNameDictionaryUser.put("schema.organization", "organization");
	}

	public UserDataBuilder() {
	}

	public JSONObject translateSetToJson(Set<Attribute> imsAttributes, Set<Attribute> injectedAttributes) {

		LOGGER.info("Building account JsonObject");

		JSONObject userObj = new JSONObject();

		Set<Attribute> multiValueAttribute = new HashSet<Attribute>();
		Set<Attribute> multiLayerAttribute = new HashSet<Attribute>();

		if (injectedAttributes != null) {
			for (Attribute at : injectedAttributes) {
				multiValueAttribute.add(at);
			}

		}

		for (Attribute at : imsAttributes) {

			String attributeName = at.getName();

			if (objectNameDictionaryUser.containsKey(attributeName)) {
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

	public static ObjectClassInfo getUserSchema() {

		ObjectClassInfoBuilder builder = new ObjectClassInfoBuilder();

		builder.addAttributeInfo(Name.INFO);

		builder.addAttributeInfo(AttributeInfoBuilder.define("userName").setRequired(true).build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("name.formatted").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("name.familyName").setRequired(false).build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("name.givenName").setRequired(false).build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("name.middleName").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("name.honorificPrefix").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("name.honorificSuffix").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("displayName").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("nickName").build());

		builder.addAttributeInfo(AttributeInfoBuilder.define("userType").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("locale").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("preferredLanguage").build());

		builder.addAttributeInfo(AttributeInfoBuilder.define("id").build());

		builder.addAttributeInfo(AttributeInfoBuilder.define("emails.work.value").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("emails.work.primary").build());

		builder.addAttributeInfo(AttributeInfoBuilder.define("emails.home.value").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("emails.home.primary").build());

		builder.addAttributeInfo(AttributeInfoBuilder.define("emails.other.value").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("emails.other.primary").build());

		builder.addAttributeInfo(AttributeInfoBuilder.define("entitlements..value").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("entitlements..primary").build());

		return builder.build();
	}

	@Override
	public JSONObject translateSetToJson(Set<Attribute> imsAttributes, Set<Attribute> injectedAttributes,
			Map<String, Map<String, Object>> attributeMap) {
		// Method not implemented in this class.
		return null;
	}
}
