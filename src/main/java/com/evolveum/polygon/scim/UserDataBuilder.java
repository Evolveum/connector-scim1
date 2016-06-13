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

public class UserDataBuilder implements ObjectTranslator{

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

		// TODO it might be a problem that this is an array
		objectNameDictionaryUser.put("entitlements..value", "value");
		objectNameDictionaryUser.put("entitlements..primary", "primary");

		objectNameDictionaryUser.put("schemaExtension.type", "type");
		objectNameDictionaryUser.put("schemaExtension.organization", "organization");
	}

	public UserDataBuilder() {
	}

	public JSONObject translateSetToJson(Set<Attribute> attributes) {

		LOGGER.info("Building account JsonObject");
		
		JSONObject userObj = new JSONObject();

		Set<Attribute> multiValueAttribute = new HashSet<Attribute>();
		Set<Attribute> multiLayerAttribute = new HashSet<Attribute>();

		for (Attribute at : attributes) {

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
						if (!secondKeyPart[1].intern().equals("")) {
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
		
		
		//
		builder.addAttributeInfo(AttributeInfoBuilder.define("id").build());
		//
		//builder.addAttributeInfo(AttributeInfoBuilder.define("profileUrl").build());
		
		builder.addAttributeInfo(AttributeInfoBuilder.define("emails").setMultiValued(true).build());
		
		builder.addAttributeInfo(AttributeInfoBuilder.define("emails.work.value").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("emails.work.primary").build());
		
		builder.addAttributeInfo(AttributeInfoBuilder.define("emails.home.value").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("emails.home.primary").build());
		
		builder.addAttributeInfo(AttributeInfoBuilder.define("emails.other.value").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("emails.other.primary").build());
		
		builder.addAttributeInfo(AttributeInfoBuilder.define("schemaExtension.type").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("schemaExtension.organization").build());
		
		builder.addAttributeInfo(AttributeInfoBuilder.define("entitlements..value").build());
		builder.addAttributeInfo(AttributeInfoBuilder.define("entitlements..primary").build());
		
		
		return builder.build();
	}
}
