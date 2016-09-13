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

		StrategyFetcher fetch = new StrategyFetcher();
		HandlingStrategy strategy = fetch.fetchStrategy(providerName);

		for (String attributeName : attributeMap.keySet()) {

			builder = strategy.schemaBuilder(attributeName, attributeMap, builder, this);

		}

		if ("/Users".equals(objectTypeName)) {
			builder.setType(ObjectClass.ACCOUNT_NAME);

		} else if ("/Groups".equals(objectTypeName)) {
			builder.setType(ObjectClass.GROUP_NAME);
		} else {
			String[] splitTypeName = objectTypeName.split("\\/"); // e.q.
			// /Entitlements
			ObjectClass objectClass = new ObjectClass(splitTypeName[1]);
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
	 * @param subPropertyName
	 *            String which represents the name of the sub-property.
	 * @return The "AttributeInfoBuilder" object populated with the sub-property
	 *         values set.
	 **/
	public AttributeInfoBuilder subPropertiesChecker(AttributeInfoBuilder infoBuilder,
			Map<String, Object> schemaAttributeMap, String subPropertyName) {

		if ("readOnly".equals(subPropertyName)) {

			infoBuilder.setUpdateable((!(Boolean) schemaAttributeMap.get(subPropertyName)));
			infoBuilder.setCreateable((!(Boolean) schemaAttributeMap.get(subPropertyName)));

		} else if ("mutability".equals(subPropertyName)) {
			String value = schemaAttributeMap.get(subPropertyName).toString();
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
					LOGGER.warn("Unknown mutability attribute in schema translation: {0} ", value);
				}
			}

		} else if ("type".equals(subPropertyName)) {

			if ("string".equals(schemaAttributeMap.get(subPropertyName).toString())) {

				infoBuilder.setType(String.class);
			} else if ("boolean".equals(schemaAttributeMap.get(subPropertyName).toString())) {

				infoBuilder.setType(Boolean.class);
			}

		} else if ("required".equals(subPropertyName)) {
			infoBuilder.setRequired(((Boolean) schemaAttributeMap.get(subPropertyName)));
		} else if ("multiValued".equals(subPropertyName)) {
			infoBuilder.setMultiValued(((Boolean) schemaAttributeMap.get(subPropertyName)));
		}

		return infoBuilder;
	}
}
