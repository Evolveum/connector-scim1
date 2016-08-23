package com.evolveum.polygon.scim;

import java.util.Map;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.ObjectClassInfoBuilder;

/**
 * A class containing the methods used for building a schema representation
 * which will be published by the connector. The schema representation is
 * generated out of a Map object containing the attributes present in the schema
 * described by the service provider.
 **/
public class SchemaObjectBuilderGeneric {

	/**
	 * Used to populate the variable "providerName" with the name of the service
	 * provider. Used mainly for workaround purposes.
	 **/
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
	public ObjectClassInfo buildSchema(Map<String, Map<String, Object>> attributeMap, String objectTypeName,
			String providerName) {
		ObjectClassInfoBuilder builder = new ObjectClassInfoBuilder();
		builder.addAttributeInfo(Name.INFO);

		HandlingStrategy strategy;

		if ("salesforce".equals(providerName)) {
			strategy = new SalesforceHandlingStrategy();
		} else if ("slack".equals(providerName)) {
			strategy = new SlackHandlingStrategy();
		} else {
			strategy = new StandardScimHandlingStrategy();
		}

		for (String attributeName : attributeMap.keySet()) {

			builder = strategy.schemaBuilderProcedure(attributeName, attributeMap, builder, this);

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
	public AttributeInfoBuilder subPropertiesChecker(AttributeInfoBuilder infoBuilder,
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
}
