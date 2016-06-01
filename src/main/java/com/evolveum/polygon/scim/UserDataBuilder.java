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

public class UserDataBuilder {

	private static Map<String, String> objectNameDictionary = CollectionUtil.newCaseInsensitiveMap();
	private static final Log LOGGER = Log.getLog(UserDataBuilder.class);

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
		// nameDictionaryUser.put("profileUrl","profileUrl");

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

		objectNameDictionary.put("phoneNumbers.home.value", "value");

		objectNameDictionary.put("phoneNumbers.mobile.value", "value");

		objectNameDictionary.put("phoneNumbers.fax.value", "value");

		objectNameDictionary.put("phoneNumbers.pager.value", "value");

		objectNameDictionary.put("photos.photo.value", "value");

		objectNameDictionary.put("photos.thumbnail.value", "value");

		objectNameDictionary.put("ims.aim.value", "type");

		objectNameDictionary.put("ims.gtalk.value", "type");

		objectNameDictionary.put("ims.icq.value", "type");

		objectNameDictionary.put("ims.msn.value", "type");

		objectNameDictionary.put("ims.xmpp.value", "type");

		objectNameDictionary.put("ims.skype.value", "type");

		objectNameDictionary.put("ims.qq.value", "type");

		objectNameDictionary.put("ims.yahoo.value", "type");

		objectNameDictionary.put("ims.other.value", "type");

		objectNameDictionary.put("userType", "userType");
		objectNameDictionary.put("title", "title");
		objectNameDictionary.put("preferredLanguage", "preferredLanguage");
		objectNameDictionary.put("locale", "locale");

		objectNameDictionary.put("id", "id");
		objectNameDictionary.put("externalId", "externalId");
		objectNameDictionary.put("timezone", "timezone");
		objectNameDictionary.put("active", "active");
		objectNameDictionary.put("password", "password");

		// nameDictionaryUser.put("groups.value","value");
		// nameDictionaryUser.put("groups.display","display");

		objectNameDictionary.put("x509Certificates", "x509Certificates");
		objectNameDictionary.put("x509Certificates.value", "value");

		// TODO it might be a problem that this is an array
		objectNameDictionary.put("entitlements..value", "value");
		objectNameDictionary.put("entitlements..primary", "primary");

		objectNameDictionary.put("schemaExtension.type", "type");
		objectNameDictionary.put("schemaExtension.organization", "organization");
	}

	public UserDataBuilder() {
	}

	public JSONObject setUserObject(Set<Attribute> attributes) {

		JSONObject userObj = new JSONObject();

		Set<Attribute> multiValueAttribute = new HashSet<Attribute>();
		Set<Attribute> multiLayerAttribute = new HashSet<Attribute>();

		for (Attribute at : attributes) {

			String attributeName = at.getName();

			if (objectNameDictionary.containsKey(attributeName)) {
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
				LOGGER.error("Attribute name not defined in dictionary {0}", attributeName);
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
					String attrName = "No shchema type";
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
						LOGGER.error("Schema type not speciffied {0}. Error ocourance while translating user object attribute set", attrName);
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

		return builder.build();
	}
}
