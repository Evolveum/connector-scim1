package com.evolveum.polygon.scim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.ObjectClassInfoBuilder;
import org.identityconnectors.framework.common.objects.OperationalAttributeInfos;
import org.json.JSONArray;
import org.json.JSONObject;

public class GenericSchemaObjectBuilder {

	// TODO SOME attributes not defined in the Schemas/ endpoint and present in
	// resources (salesForce)

	private static final Log LOGGER = Log.getLog(ScimConnector.class);

	public ObjectClassInfo buildSchema(Map<String, Map<String, Object>> attributeMap, String objectTypeName) {
		ObjectClassInfoBuilder builder = new ObjectClassInfoBuilder();
		builder.addAttributeInfo(Name.INFO);

	
		
		
		
		for (String attributeName : attributeMap.keySet()) {
			//System.out.println(attributeName);
			
			AttributeInfoBuilder infoBuilder = new AttributeInfoBuilder(attributeName.intern());

			if(!attributeName.equals("active")){
			Map<String, Object> schemaSubPropertiesMap = new HashMap<String, Object>();
			schemaSubPropertiesMap = attributeMap.get(attributeName);
			for (String subPropertieName : schemaSubPropertiesMap.keySet()) {
				if ("subAttributes".equals(subPropertieName.intern())) {
					// TODO dead weight / check cases when this is true
					infoBuilder = new AttributeInfoBuilder(attributeName.intern());
					JSONArray jsonArray = new JSONArray();
					
					jsonArray = ((JSONArray) schemaSubPropertiesMap.get(subPropertieName));
					for (int i = 0; i < jsonArray.length(); i++) {
						JSONObject attribute = new JSONObject();
						attribute = jsonArray.getJSONObject(i);
					}
					break;
				} else {
					subPropertiesChecker(infoBuilder, schemaSubPropertiesMap, subPropertieName, attributeName);
				}

			}
			builder.addAttributeInfo(infoBuilder.build());
		}else {
			builder.addAttributeInfo(OperationalAttributeInfos.ENABLE);
			
		}
		}

		if ("/Users".equals(objectTypeName.intern())) {
			builder.setType(ObjectClass.ACCOUNT_NAME);

		} else if ("/Groups".equals(objectTypeName.intern())) {
			builder.setType(ObjectClass.GROUP_NAME);
		} else {
			String[] splitTypeMame = objectTypeName.split("\\/"); // e.q.
																	// /Entitlements
			ObjectClass objectClass = new ObjectClass(splitTypeMame[1]);
			builder.setType(objectClass.getObjectClassValue());
		}
		LOGGER.info("Schema: {0}", builder.build());
		return builder.build();
	}

	private AttributeInfoBuilder subPropertiesChecker(AttributeInfoBuilder infoBuilder,
			Map<String, Object> schemaAttributeMap, String mapAttributeKey, String key) {

		if ("readOnly".equals(mapAttributeKey.intern())) {

			infoBuilder.setUpdateable((!(Boolean) schemaAttributeMap.get(mapAttributeKey)));
			infoBuilder.setCreateable((!(Boolean) schemaAttributeMap.get(mapAttributeKey)));

		} else if ("mutability".equals(mapAttributeKey.intern())) {
			String value = schemaAttributeMap.get(mapAttributeKey).toString();
			if ("readWrite".equals(value)) {
				infoBuilder.setUpdateable(true);
				infoBuilder.setCreateable(true);
				infoBuilder.setReadable(true);
			} else if ("writeOnly".equals(value)) {
				infoBuilder.setUpdateable(true);
				infoBuilder.setCreateable(true);
				infoBuilder.setReadable(false);

			} else if ("readOnly".equals(value)) {
				infoBuilder.setUpdateable(false);
				infoBuilder.setCreateable(false);
				infoBuilder.setReadable(true);

			} else if ("immutable".equals(value)) {

				infoBuilder.setUpdateable(false);
				infoBuilder.setCreateable(false);
				infoBuilder.setReadable(false);
			} else {
				if (value.isEmpty()) {

				} else {
					LOGGER.warn("Unknown nmutability attribute in schema translation: {0} ", value);
				}
			}

		} else if ("type".equals(mapAttributeKey.intern())) {

			if ("string".equals(schemaAttributeMap.get(mapAttributeKey).toString().intern())) {

				infoBuilder.setType(String.class);
			} else if ("boolean".equals(schemaAttributeMap.get(mapAttributeKey).toString().intern())) {

				infoBuilder.setType(Boolean.class);
			}

		} else if ("required".equals(mapAttributeKey.intern())) {
			infoBuilder.setRequired(((Boolean) schemaAttributeMap.get(mapAttributeKey)));
		} else if ("multiValued".equals(mapAttributeKey.intern())) {
			infoBuilder.setMultiValued(((Boolean) schemaAttributeMap.get(mapAttributeKey)));
		}
		// TODO test delete
		if ("members.User.value".equals(key) || "members.Group.value".equals(key)) {

			infoBuilder.setMultiValued(true);
		}

		return infoBuilder;
	}
}
