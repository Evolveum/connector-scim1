package com.evolveum.polygon.scim;

import java.util.HashMap;
import java.util.Map;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.ObjectClassInfoBuilder;
import org.identityconnectors.framework.common.objects.OperationalAttributeInfos;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * A class containing the methods used for building a schema representation
 * which will be published by the connector. The schema representation is
 * generated out of a Map object containing the attributes present in the schema
 * described by the service provider.
 **/
public class SchemaObjectBuilderGeneric {

	private String providerName = "";

	// Constructor for Salesforce workaround purposes

	/**
	 * Used to populate the variable "providerName" with the name of the service
	 * provider. Used mainly for workaround purposes.
	 **/
	public SchemaObjectBuilderGeneric(String providerName) {
		this.providerName = providerName;

	}

	private static final Log LOGGER = Log.getLog(ScimConnector.class);

	/**
	 * Builds the "ObjectClassInfo" object which carries the schema information
	 * for a single resource.
	 * 
	 * @param attributeMap
	 *            The map which carries the attribute present in the schema
	 *            described by the service provider.
	 * @param objectTypeName
	 *            The name of the resource which is used to match the resource
	 *            to a "objectType".
	 * @return An instance of ObjectClassInfo with the constructed schema
	 *         information.
	 **/
	public ObjectClassInfo buildSchema(Map<String, Map<String, Object>> attributeMap, String objectTypeName) {
		ObjectClassInfoBuilder builder = new ObjectClassInfoBuilder();
		builder.addAttributeInfo(Name.INFO);

		for (String attributeName : attributeMap.keySet()) {

			AttributeInfoBuilder infoBuilder = new AttributeInfoBuilder(attributeName.intern());

			if (!attributeName.equals("active")) {
				Map<String, Object> schemaSubPropertiesMap = new HashMap<String, Object>();
				schemaSubPropertiesMap = attributeMap.get(attributeName);
				for (String subPropertieName : schemaSubPropertiesMap.keySet()) {
					if ("subAttributes".equals(subPropertieName.intern())) {
						// TODO check positive cases
						infoBuilder = new AttributeInfoBuilder(attributeName.intern());
						JSONArray jsonArray = new JSONArray();

						jsonArray = ((JSONArray) schemaSubPropertiesMap.get(subPropertieName));
						for (int i = 0; i < jsonArray.length(); i++) {
							JSONObject attribute = new JSONObject();
							attribute = jsonArray.getJSONObject(i);
						}
						break;
					} else {
						subPropertiesChecker(infoBuilder, schemaSubPropertiesMap, subPropertieName);
						// Salesforce workaround

						if ("salesforce".equals(providerName)) {

							if ("members.User.value".equals(attributeName)
									|| "members.Group.value".equals(attributeName)
									|| "members.default.value".equals(attributeName)
									|| "members.default.display".equals(attributeName)) {
								infoBuilder.setMultiValued(true);
							}
						}

					}

				}
				builder.addAttributeInfo(infoBuilder.build());
			} else {
				builder.addAttributeInfo(OperationalAttributeInfos.ENABLE);

			}
		}

		if ("/Users".equals(objectTypeName.intern())) {
			builder.setType(ObjectClass.ACCOUNT_NAME);
			
			if ("slack".equals(providerName)){
				
				slackWorkaround(builder);
			}

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

	/**
	 * Checks the attributes present in the evaluated Map object for predefined
	 * sub-properties. The method sets the matching schema object properties
	 * accordingly to the attribute values.
	 * 
	 * @param infoBuilder
	 *            "AttributeInfoBuilder" type object instance which defines the
	 *            properties of the attribute.
	 * @param schemaAttributeMap
	 *            A map containing the sub properties of the evaluated
	 *            attribute.
	 * @param subPropertieName
	 *            String which represents the name of the sub-propertie.
	 * @return The "AttributeInfoBuilder" object populated with the
	 *         sub-propertie values set.
	 **/
	private AttributeInfoBuilder subPropertiesChecker(AttributeInfoBuilder infoBuilder,
			Map<String, Object> schemaAttributeMap, String subPropertieName) {

		if ("readOnly".equals(subPropertieName.intern())) {

			infoBuilder.setUpdateable((!(Boolean) schemaAttributeMap.get(subPropertieName)));
			infoBuilder.setCreateable((!(Boolean) schemaAttributeMap.get(subPropertieName)));

		} else if ("mutability".equals(subPropertieName.intern())) {
			String value = schemaAttributeMap.get(subPropertieName).toString();
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

		} else if ("type".equals(subPropertieName.intern())) {

			if ("string".equals(schemaAttributeMap.get(subPropertieName).toString().intern())) {

				infoBuilder.setType(String.class);
			} else if ("boolean".equals(schemaAttributeMap.get(subPropertieName).toString().intern())) {

				infoBuilder.setType(Boolean.class);
			}

		} else if ("required".equals(subPropertieName.intern())) {
			infoBuilder.setRequired(((Boolean) schemaAttributeMap.get(subPropertieName)));
		} else if ("multiValued".equals(subPropertieName.intern())) {
			infoBuilder.setMultiValued(((Boolean) schemaAttributeMap.get(subPropertieName)));
		}

		return infoBuilder;
	}
	
	private void slackWorkaround(ObjectClassInfoBuilder builder){
		
		AttributeInfoBuilder infoBuilder = new AttributeInfoBuilder("emails.default.value");
		infoBuilder.setMultiValued(true);
		infoBuilder.setRequired(true);
		infoBuilder.setType(String.class);
		builder.addAttributeInfo(infoBuilder.build());
		
		 infoBuilder = new AttributeInfoBuilder("emails.default.primary");
		infoBuilder.setMultiValued(false);
		infoBuilder.setRequired(true);
		infoBuilder.setType(Boolean.class);
		builder.addAttributeInfo(infoBuilder.build());
		
	}
}
