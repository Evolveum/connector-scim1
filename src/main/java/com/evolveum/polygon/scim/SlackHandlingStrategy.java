package com.evolveum.polygon.scim;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.ConnectorIOException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfoBuilder;
import org.identityconnectors.framework.common.objects.OperationalAttributeInfos;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.ContainsAllValuesFilter;
import org.json.JSONArray;
import org.json.JSONObject;

public class SlackHandlingStrategy implements HandlingStrategy {

	private static final Log LOGGER = Log.getLog(SlackHandlingStrategy.class);

	@Override
	public ConnectorObject buildConnectorObject(JSONObject resourceJsonObject, String resourceEndPoint)
			throws ConnectorException {

		LOGGER.info("Building the connector object from provided json");

		if (resourceJsonObject == null) {
			LOGGER.error(
					"Empty json object was passed from data provider. Error ocourance while building connector object");
			throw new ConnectorException(
					"Empty json object was passed from data provider. Error ocourance while building connector object");
		}

		ConnectorObjectBuilder cob = new ConnectorObjectBuilder();
		cob.setUid(resourceJsonObject.getString("id"));

		if ("Users".equals(resourceEndPoint)) {
			cob.setName(resourceJsonObject.getString("userName"));
		} else if ("Groups".equals(resourceEndPoint)) {

			cob.setName(resourceJsonObject.getString("displayName"));
			cob.setObjectClass(ObjectClass.GROUP);
		} else {
			cob.setName(resourceJsonObject.getString("displayName"));
			ObjectClass objectClass = new ObjectClass(resourceEndPoint);
			cob.setObjectClass(objectClass);

		}
		for (String key : resourceJsonObject.keySet()) {
			Object attribute = resourceJsonObject.get(key);
			if ("meta".equals(key.intern()) || "schemas".equals(key.intern()) || "photos".equals(key.intern())) {

				LOGGER.warn(
						"Processing trought \"schema inconsistencies\" workaround. Because of the \"{0}\" resoure attribute.",
						key.intern());
			} else

			if (attribute instanceof JSONArray) {

				JSONArray jArray = (JSONArray) attribute;

				Map<String, Collection<Object>> multivaluedAttributeMap = new HashMap<String, Collection<Object>>();
				Collection<Object> attributeValues = new ArrayList<Object>();

				for (Object o : jArray) {
					StringBuilder objectNameBilder = new StringBuilder(key.intern());
					String objectKeyName = "";
					if (o instanceof JSONObject) {
						for (String s : ((JSONObject) o).keySet()) {
							if ("type".equals(s.intern())) {
								objectKeyName = objectNameBilder.append(".").append(((JSONObject) o).get(s)).toString();
								objectNameBilder.delete(0, objectNameBilder.length());
								break;
							}
						}

						for (String s : ((JSONObject) o).keySet()) {

							if ("type".equals(s.intern())) {
							} else {

								if (!"".equals(objectKeyName)) {
									objectNameBilder = objectNameBilder.append(objectKeyName).append(".")
											.append(s.intern());
								} else {
									objectKeyName = objectNameBilder.append(".").append("default").toString();
									objectNameBilder = objectNameBilder.append(".").append(s.intern());
								}

								if (attributeValues.isEmpty()) {
									attributeValues.add(((JSONObject) o).get(s));
									multivaluedAttributeMap.put(objectNameBilder.toString(), attributeValues);
								} else {
									if (multivaluedAttributeMap.containsKey(objectNameBilder.toString())) {
										attributeValues = multivaluedAttributeMap.get(objectNameBilder.toString());
										attributeValues.add(((JSONObject) o).get(s));
									} else {
										Collection<Object> newAttributeValues = new ArrayList<Object>();
										newAttributeValues.add(((JSONObject) o).get(s));
										multivaluedAttributeMap.put(objectNameBilder.toString(), newAttributeValues);
									}

								}
								objectNameBilder.delete(0, objectNameBilder.length());

							}
						}

					} else {
						objectKeyName = objectNameBilder.append(".").append(o.toString()).toString();
						cob.addAttribute(objectKeyName, o);
					}
				}

				if (!multivaluedAttributeMap.isEmpty()) {
					for (String attributeName : multivaluedAttributeMap.keySet()) {
						cob.addAttribute(attributeName, multivaluedAttributeMap.get(attributeName));
					}

				}

			} else if (attribute instanceof JSONObject) {
				for (String s : ((JSONObject) attribute).keySet()) {

					StringBuilder objectNameBilder = new StringBuilder(key.intern());
					cob.addAttribute(objectNameBilder.append(".").append(s).toString(),
							((JSONObject) attribute).get(s));

				}

			} else {

				if ("active".equals(key)) {
					cob.addAttribute("__ENABLE__", resourceJsonObject.get(key));
				} else {

					if (!resourceJsonObject.get(key).equals(null)) {

						cob.addAttribute(key.intern(), resourceJsonObject.get(key));
					} else {
						cob.addAttribute(key.intern(), "");

					}
				}
			}
		}
		ConnectorObject finalConnectorObject = cob.build();
		LOGGER.info("The connector object returned for the processed json: {0}", finalConnectorObject);
		return finalConnectorObject;

	}

	@Override
	public Uid specialGroupUpdateProcedure(HttpResponse response, JSONObject jsonObject, String uri, Header authHeader,
			CrudManagerScim manager) {
		try {
			manager.onNoSuccess(response, "updating object");
		} catch (ParseException e) {

			LOGGER.error("An exception has occurred while parsing the http response : {0}", e.getLocalizedMessage());
			LOGGER.info("An exception has occurred while parsing the http response : {0}", e);

			throw new ConnectorException("An exception has occurred while parsing the http response : {0}", e);

		} catch (IOException e) {

			LOGGER.error(
					"An error has occurred while processing the http response. Occurrence in the process of updating a resource object: {0}",
					e.getLocalizedMessage());
			LOGGER.info(
					"An error has occurred while processing the http response. Occurrence in the process of creating a resource object: {0}",
					e);

			throw new ConnectorIOException(
					"An error has occurred while processing the http response. Occurrence in the process of creating a resource object",
					e);

		}
		return null;
	}

	@Override
	public StringBuilder containsAllValuesFilterProcedure(String p, ContainsAllValuesFilter filter,
			FilterHandler handler) {
		return null;
	}

	@Override
	public Map<String, Map<String, Object>> parseAttribute(JSONObject attribute,
			Map<String, Map<String, Object>> attributeMap, ParserSchemaScim parser) {
		String attributeName = null;
		Boolean isComplex = false;
		Boolean isMultiValued = false;
		Map<String, Object> attributeObjects = new HashMap<String, Object>();
		Map<String, Object> subAttributeMap = new HashMap<String, Object>();

		if (attribute.has("subattributes")) {
			boolean hasTypeValues = false;
			JSONArray subAttributes = new JSONArray();
			LOGGER.warn(
					"slack attribute \"subAttributes\" invalid naming workaround. The attribute name is defined as \"subattributes\"");
			subAttributes = (JSONArray) attribute.get("subattributes");

			if (attributeName == null) {
				for (String subAttributeNameKeys : attribute.keySet()) {
					if ("name".equals(subAttributeNameKeys.intern())) {
						attributeName = attribute.get(subAttributeNameKeys).toString();
						break;
					}
				}
			}

			for (String nameKey : attribute.keySet()) {
				if ("multiValued".equals(nameKey.intern())) {
					isMultiValued = (Boolean) attribute.get(nameKey);
					break;
				}

			}

			for (int i = 0; i < subAttributes.length(); i++) {
				JSONObject subAttribute = new JSONObject();
				subAttribute = subAttributes.getJSONObject(i);
				subAttributeMap = parser.parseSubAttribute(subAttribute, subAttributeMap);
			}
			for (String typeKey : subAttributeMap.keySet()) {
				if ("type".equals(typeKey.intern())) {
					hasTypeValues = true;
					break;
				}
			}

			if (hasTypeValues) {
				Map<String, Object> typeObject = new HashMap<String, Object>();
				typeObject = (Map<String, Object>) subAttributeMap.get("type");
				if (typeObject.containsKey("canonicalValues") || typeObject.containsKey("referenceTypes")) {
					JSONArray referenceValues = new JSONArray();
					if (typeObject.containsKey("canonicalValues")) {
						referenceValues = (JSONArray) typeObject.get("canonicalValues");
					} else {
						referenceValues = (JSONArray) typeObject.get("referenceTypes");
					}

					for (int j = 0; j < referenceValues.length(); j++) {

						String sringReferenceValue = (String) referenceValues.get(j);
						for (String subAttributeKeyNames : subAttributeMap.keySet()) {
							if (!"type".equals(subAttributeKeyNames.intern())) { // TODO
								// some
								// other
								// complex
								// attribute
								// names
								// may
								// be
								// used
								StringBuilder complexAttrName = new StringBuilder(attributeName);
								attributeMap.put(
										complexAttrName.append(".").append(sringReferenceValue).append(".")
												.append(subAttributeKeyNames).toString(),
										(HashMap<String, Object>) subAttributeMap.get(subAttributeKeyNames));
								isComplex = true;

							}
						}
					}
				} else {
					ArrayList<String> defaultReferenceTypeValues = new ArrayList<String>();
					defaultReferenceTypeValues.add("User");
					defaultReferenceTypeValues.add("Group");

					defaultReferenceTypeValues.add("external");
					defaultReferenceTypeValues.add("uri");

					for (String subAttributeKeyNames : subAttributeMap.keySet()) {
						if (!"type".equals(subAttributeKeyNames.intern())) {
							for (String defaultTypeReferenceValues : defaultReferenceTypeValues) {
								StringBuilder complexAttrName = new StringBuilder(attributeName);
								complexAttrName.append(".").append(defaultTypeReferenceValues);
								attributeMap.put(complexAttrName.append(".").append(subAttributeKeyNames).toString(),
										(HashMap<String, Object>) subAttributeMap.get(subAttributeKeyNames));
								isComplex = true;
							}
						}

					}

				}

			} else {
				if (!isMultiValued) {
					for (String subAttributeKeyNames : subAttributeMap.keySet()) {
						StringBuilder complexAttrName = new StringBuilder(attributeName);
						attributeMap.put(complexAttrName.append(".").append(subAttributeKeyNames).toString(),
								(HashMap<String, Object>) subAttributeMap.get(subAttributeKeyNames));
						isComplex = true;
					}
				} else {
					for (String subAttributeKeyNames : subAttributeMap.keySet()) {
						StringBuilder complexAttrName = new StringBuilder(attributeName);

						HashMap<String, Object> subattributeKeyMap = (HashMap<String, Object>) subAttributeMap
								.get(subAttributeKeyNames);

						for (String attributePropertie : subattributeKeyMap.keySet()) {

							if ("multiValued".equals(attributePropertie)) {
								subattributeKeyMap.put("multiValued", true);
							}
						}

						attributeMap.put(complexAttrName.append(".").append("default").append(".")
								.append(subAttributeKeyNames).toString(), subattributeKeyMap);
						isComplex = true;
					}

				}
			}

		} else {

			for (String attributeNameKeys : attribute.keySet()) {

				if ("name".equals(attributeNameKeys.intern())) {
					attributeName = attribute.get(attributeNameKeys).toString();

				} else {
					attributeObjects.put(attributeNameKeys, attribute.get(attributeNameKeys));
				}

			}
		}
		if (!isComplex) {
			attributeMap.put(attributeName, attributeObjects);
		}
		return attributeMap;
	}

	@Override
	public ObjectClassInfoBuilder schemaBuilderProcedure(String attributeName,
			Map<String, Map<String, Object>> attributeMap, ObjectClassInfoBuilder builder,
			SchemaObjectBuilderGeneric schemaBuilder) {

		AttributeInfoBuilder infoBuilder = new AttributeInfoBuilder(attributeName.intern());

		if (!"active".equals(attributeName) && !(("emails.default.primary".equals(attributeName)
				|| "emails.default.value".equals(attributeName)))) {
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
					schemaBuilder.subPropertiesChecker(infoBuilder, schemaSubPropertiesMap, subPropertieName);
				}
			}
			builder.addAttributeInfo(infoBuilder.build());
		} else {
			if ("active".equals(attributeName)) {
				builder.addAttributeInfo(OperationalAttributeInfos.ENABLE);
			} else {
				buildMissingAttributes(builder, attributeName, infoBuilder);
			}
		}
		return builder;
	}

	private void buildMissingAttributes(ObjectClassInfoBuilder builder, String attributeName,
			AttributeInfoBuilder infoBuilder) {

		if ("emails.default.value".equals(attributeName)) {
			infoBuilder.setMultiValued(true);
			infoBuilder.setRequired(true);
			infoBuilder.setType(String.class);
			builder.addAttributeInfo(infoBuilder.build());
		} else {
			infoBuilder.setMultiValued(false);
			infoBuilder.setRequired(true);
			infoBuilder.setType(Boolean.class);
			builder.addAttributeInfo(infoBuilder.build());
		}

	}

	@Override
	public List<Map<String, Map<String, Object>>> getAttributeMapList(
			List<Map<String, Map<String, Object>>> attributeMapList) {

		if (!attributeMapList.isEmpty()) {

			for (int i = 0; i < attributeMapList.size(); i++) {
				Map<String, Map<String, Object>> resources = attributeMapList.get(i);

				if (resources.containsKey("userName") && !resources.containsKey("emails.default.primary")
						&& !resources.containsKey("emails.default.value")) {

					resources.put("emails.default.primary", null);
					resources.put("emails.default.value", null);

					attributeMapList.remove(i);
					attributeMapList.add(i, resources);
				}

			}
		}
		return attributeMapList;

	}

	@Override
	public HashSet<Attribute> attributeInjection(HashSet<Attribute> injectedAttributeSet,
			HashMap<String, Object> autoriazationData) {
		return injectedAttributeSet;
	}

}
