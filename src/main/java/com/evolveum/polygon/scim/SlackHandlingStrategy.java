package com.evolveum.polygon.scim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfoBuilder;
import org.identityconnectors.framework.common.objects.OperationalAttributeInfos;
import org.identityconnectors.framework.common.objects.filter.AttributeFilter;
import org.identityconnectors.framework.common.objects.filter.ContainsAllValuesFilter;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.json.JSONArray;
import org.json.JSONObject;

public class SlackHandlingStrategy extends StandardScimHandlingStrategy implements HandlingStrategy {

	private static final Log LOGGER = Log.getLog(SlackHandlingStrategy.class);
	private static final String TYPE = "type";
	private static final String DEFAULT = "default";
	private static final String MULTIVALUED = "multiValued";
	private static final String CANONICALVALUES = "canonicalValues";
	private static final String REFERENCETYPES = "referenceTypes";
	private static final String SCHEMAVALUE = "urn:scim:schemas:core:1.0";
	private static final String NAME = "name";
	private static final String NICKNAME = "nickName";
	private static final String USERNAME = "userName";
	private static final String TITLE = "title";
	private static final String SCHEMAS = "schemas";
	private static final String PROFILEURL = "profileUrl";
	private static final String DISPLAYNAME = "displayName";
	private static final String TIMEZONE = "timezone";
	private static final String EXTERNALID = "externalId";
	private static final String ACTIVE = "active";
	private static final String PHOTOS = "photos";
	private static final String READONLY = "readOnly";
	private static final String SCHEMA = "schema";
	private static final String REQUIRED = "required";
	private static final String CASEEXSACT = "caseExact";
	private static final String STRING = "string";

	@Override
	public StringBuilder processContainsAllValuesFilter(String p, ContainsAllValuesFilter filter,
			FilterHandler handler) {
		return null;
	}

	// TODO simplifi
	@Override
	public Map<String, Map<String, Object>> parseAttribute(JSONObject attribute,
			Map<String, Map<String, Object>> attributeMap, ParserSchemaScim parser) {
		String attributeName = null;
		Boolean isComplex = false;
		Boolean isMultiValued = false;
		Map<String, Object> attributeObjects = new HashMap<String, Object>();
		Map<String, Object> subAttributeMap = new HashMap<String, Object>();

		if (attribute.has("subAttributes") || attribute.has("subattributes")) {
			boolean hasTypeValues = false;
			JSONArray subAttributes = new JSONArray();

			if (attribute.has("subAttributes")) {
				subAttributes = (JSONArray) attribute.get("subAttributes");
			} else if (attribute.has("subattributes")) {
				LOGGER.warn(
						"Slack attribute \"subAttributes\" invalid naming workaround. The attribute name is defined as \"subattributes\"");
				subAttributes = (JSONArray) attribute.get("subattributes");
			}

			if (attributeName == null) {
				for (String subAttributeNameKeys : attribute.keySet()) {
					if (NAME.equals(subAttributeNameKeys)) {
						attributeName = attribute.get(subAttributeNameKeys).toString();
						LOGGER.info("The attribute which is being processed is: {0}", attributeName);
						break;
					}
				}
			}

			for (String nameKey : attribute.keySet()) {
				if (MULTIVALUED.equals(nameKey)) {
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
				if (TYPE.equals(typeKey)) {
					hasTypeValues = true;
					break;
				}
			}

			if (hasTypeValues) {
				Map<String, Object> typeObject = new HashMap<String, Object>();
				typeObject = (HashMap<String, Object>) subAttributeMap.get(TYPE);
				if (typeObject.containsKey(CANONICALVALUES) || typeObject.containsKey(REFERENCETYPES)) {
					JSONArray referenceValues = new JSONArray();
					if (typeObject.containsKey(CANONICALVALUES)) {
						referenceValues = (JSONArray) typeObject.get(CANONICALVALUES);
					} else {
						referenceValues = (JSONArray) typeObject.get(REFERENCETYPES);
					}

					for (int j = 0; j < referenceValues.length(); j++) {

						String sringReferenceValue = (String) referenceValues.get(j);
						for (String subAttributeKeyNames : subAttributeMap.keySet()) {
							if (!TYPE.equals(subAttributeKeyNames)) { // TODO
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
					List<String> defaultReferenceTypeValues = new ArrayList<String>();
					defaultReferenceTypeValues.add("User");
					defaultReferenceTypeValues.add("Group");

					defaultReferenceTypeValues.add("external");
					defaultReferenceTypeValues.add("uri");

					for (String subAttributeKeyNames : subAttributeMap.keySet()) {
						if (!TYPE.equals(subAttributeKeyNames)) {
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

						Map<String, Object> subattributeKeyMap = (HashMap<String, Object>) subAttributeMap
								.get(subAttributeKeyNames);

						for (String attributePropertie : subattributeKeyMap.keySet()) {

							if (MULTIVALUED.equals(attributePropertie)) {
								subattributeKeyMap.put(MULTIVALUED, true);
							}
						}

						attributeMap.put(complexAttrName.append(".").append(DEFAULT).append(".")
								.append(subAttributeKeyNames).toString(), subattributeKeyMap);
						isComplex = true;
					}

				}
			}

		} else {

			for (String attributeNameKeys : attribute.keySet()) {

				if (NAME.equals(attributeNameKeys)) {
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
	public ObjectClassInfoBuilder schemaBuilder(String attributeName, Map<String, Map<String, Object>> attributeMap,
			ObjectClassInfoBuilder builder, SchemaObjectBuilderGeneric schemaBuilder) {

		AttributeInfoBuilder infoBuilder = new AttributeInfoBuilder(attributeName);

		if (!ACTIVE.equals(attributeName) && !(("emails.default.primary".equals(attributeName)
				|| "emails.default.value".equals(attributeName)))) {
			Map<String, Object> schemaSubPropertiesMap = new HashMap<String, Object>();
			schemaSubPropertiesMap = attributeMap.get(attributeName);
			for (String subPropertieName : schemaSubPropertiesMap.keySet()) {
				if ("subattributes".equals(subPropertieName) || "subAttributes".equals(subPropertieName)) {
					// TODO check positive cases
					infoBuilder = new AttributeInfoBuilder(attributeName);
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
			if (ACTIVE.equals(attributeName)) {
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

				if (resources.containsKey(USERNAME) && !resources.containsKey("emails.default.primary")
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
	public Set<Attribute> attributeInjection(Set<Attribute> injectedAttributeSet,
			Map<String, Object> autoriazationData) {
		return injectedAttributeSet;
	}

	@Override
	public JSONObject injectMissingSchemaAttributes(String resourceName, JSONObject jsonObject) {

		LOGGER.warn("Processing trought slack missing schema attributes workaround for the resource: \"{0}\"",
				resourceName);
		if ("Users".equals(resourceName)) {

			Map<String, String> missingAttirbutes = new HashMap<String, String>();
			missingAttirbutes.put(USERNAME, USERNAME);
			missingAttirbutes.put(NICKNAME, NICKNAME);
			missingAttirbutes.put(TITLE, TITLE);
			missingAttirbutes.put(SCHEMAS, SCHEMAS);

			missingAttirbutes.put(PROFILEURL, PROFILEURL);
			missingAttirbutes.put(DISPLAYNAME, DISPLAYNAME);
			missingAttirbutes.put(TIMEZONE, TIMEZONE);
			missingAttirbutes.put(EXTERNALID, EXTERNALID);
			missingAttirbutes.put(ACTIVE, ACTIVE);
			missingAttirbutes.put(PHOTOS, PHOTOS);

			if (jsonObject.has("attributes")) {

				JSONArray attributesArray = new JSONArray();

				attributesArray = jsonObject.getJSONArray("attributes");
				for (int indexValue = 0; indexValue < attributesArray.length(); indexValue++) {
					JSONObject subAttributes = attributesArray.getJSONObject(indexValue);

					if (subAttributes.has(NAME)) {

						if (USERNAME.equals(subAttributes.get(NAME))) {

							missingAttirbutes.remove(USERNAME);
						} else if (NICKNAME.equals(subAttributes.get(NAME))) {

							missingAttirbutes.remove(NICKNAME);
						} else if (TITLE.equals(subAttributes.get(NAME))) {

							missingAttirbutes.remove(TITLE);
						} else if (SCHEMAS.equals(subAttributes.get(NAME))) {

							missingAttirbutes.remove(SCHEMAS);
						} else if (PROFILEURL.equals(subAttributes.get(NAME))) {

							missingAttirbutes.remove(PROFILEURL);
						} else if (DISPLAYNAME.equals(subAttributes.get(NAME))) {

							missingAttirbutes.remove(DISPLAYNAME);
						} else if (TIMEZONE.equals(subAttributes.get(NAME))) {

							missingAttirbutes.remove(TIMEZONE);
						} else if (EXTERNALID.equals(subAttributes.get(NAME))) {

							missingAttirbutes.remove(EXTERNALID);
						} else if (ACTIVE.equals(subAttributes.get(NAME))) {

							missingAttirbutes.remove(ACTIVE);
						} else if (PHOTOS.equals(subAttributes.get(NAME))) {

							missingAttirbutes.remove(PHOTOS);
						}
					}
				}

				for (String missingAttributeName : missingAttirbutes.keySet()) {

					LOGGER.warn("Building schema deffinition for the attribute: \"{0}\"", missingAttributeName);

					if (USERNAME.equals(missingAttributeName)) {
						JSONObject userName = new JSONObject();

						userName.put(SCHEMA, SCHEMAVALUE);
						userName.put(NAME, USERNAME);
						userName.put(READONLY, false);
						userName.put(TYPE, STRING);
						userName.put(REQUIRED, true);
						userName.put(CASEEXSACT, false);

						attributesArray.put(userName);

					} else if (ACTIVE.equals(missingAttributeName)) {
						JSONObject active = new JSONObject();

						active.put(SCHEMA, SCHEMAVALUE);
						active.put(NAME, ACTIVE);
						active.put(READONLY, false);
						active.put(TYPE, "boolean");
						active.put(REQUIRED, false);
						active.put(CASEEXSACT, false);

						attributesArray.put(active);

					} else if (EXTERNALID.equals(missingAttributeName)) {
						JSONObject externalId = new JSONObject();

						externalId.put(SCHEMA, SCHEMAVALUE);
						externalId.put(NAME, EXTERNALID);
						externalId.put(READONLY, false);
						externalId.put(TYPE, STRING);
						externalId.put(REQUIRED, false);
						externalId.put(CASEEXSACT, true);

						attributesArray.put(externalId);

					} else if (TIMEZONE.equals(missingAttributeName)) {
						JSONObject timezone = new JSONObject();

						timezone.put(SCHEMA, SCHEMAVALUE);
						timezone.put(NAME, TIMEZONE);
						timezone.put(READONLY, false);
						timezone.put(TYPE, STRING);
						timezone.put(REQUIRED, false);
						timezone.put(CASEEXSACT, false);

						attributesArray.put(timezone);

					} else if (DISPLAYNAME.equals(missingAttributeName)) {
						JSONObject displayName = new JSONObject();

						displayName.put(SCHEMA, SCHEMAVALUE);
						displayName.put(NAME, DISPLAYNAME);
						displayName.put(READONLY, false);
						displayName.put(TYPE, STRING);
						displayName.put(REQUIRED, false);
						displayName.put(CASEEXSACT, false);

						attributesArray.put(displayName);

					} else if (PROFILEURL.equals(missingAttributeName)) {
						JSONObject profileUrl = new JSONObject();

						profileUrl.put(SCHEMA, SCHEMAVALUE);
						profileUrl.put(NAME, PROFILEURL);
						profileUrl.put(READONLY, false);
						profileUrl.put(TYPE, STRING);
						profileUrl.put(REQUIRED, false);
						profileUrl.put(CASEEXSACT, false);

						attributesArray.put(profileUrl);

					} else if (NICKNAME.equals(missingAttributeName)) {
						JSONObject nickName = new JSONObject();

						nickName.put(SCHEMA, SCHEMAVALUE);
						nickName.put(NAME, NICKNAME);
						nickName.put(READONLY, false);
						nickName.put(TYPE, STRING);
						nickName.put(REQUIRED, true);
						nickName.put(CASEEXSACT, false);

						attributesArray.put(nickName);

					} else if (TITLE.equals(missingAttributeName)) {
						JSONObject title = new JSONObject();

						title.put(SCHEMA, SCHEMAVALUE);
						title.put(NAME, TITLE);
						title.put(READONLY, false);
						title.put(TYPE, STRING);
						title.put(REQUIRED, false);
						title.put(CASEEXSACT, false);

						attributesArray.put(title);
					} else if (SCHEMAS.equals(missingAttributeName)) {
						JSONObject schemas = new JSONObject();
						JSONArray subattributeArray = new JSONArray();

						JSONObject valueBlank = new JSONObject();

						schemas.put(SCHEMA, SCHEMAVALUE);
						schemas.put(NAME, SCHEMAS);
						schemas.put(READONLY, false);
						schemas.put(TYPE, "complex");
						schemas.put(MULTIVALUED, true);
						schemas.put(REQUIRED, false);
						schemas.put(CASEEXSACT, false);

						valueBlank.put(NAME, "blank");
						valueBlank.put(READONLY, true);
						valueBlank.put(REQUIRED, false);
						valueBlank.put(MULTIVALUED, false);

						subattributeArray.put(valueBlank);

						schemas.put("subAttributes", subattributeArray);

						attributesArray.put(schemas);
					}

				}

				jsonObject.put("attributes", attributesArray);
			}

		}
		return jsonObject;

	}

	// TODO simplify
	@Override
	public String checkFilter(Filter filter, String endpointName) {

		if (endpointName.equals(ObjectClass.GROUP.getObjectClassValue())) {
			if (filter instanceof ContainsAllValuesFilter) {
				List<Object> valueList = ((AttributeFilter) filter).getAttribute().getValue();
				if (valueList.size() == 1) {
					Object uidString = valueList.get(0);
					if (uidString instanceof String) {
						LOGGER.warn("Processing trough group object class \"contains all values\" filter workaround.");
						return (String) uidString;

					} else {

						return "";
					}
				} else {

					return "";

				}
			} else if (filter instanceof EqualsFilter) {
				Attribute filterAttr = ((EqualsFilter) filter).getAttribute();
				String attributeName = filterAttr.getName();
				String attributeValue;

				if ("members.default.value".equals(attributeName)) {
					LOGGER.warn("Processing trough group object class \"equals\" filter workaround.");
					List<Object> valueList = ((AttributeFilter) filter).getAttribute().getValue();
					if (valueList.size() == 1) {
						Object uidString = valueList.get(0);
						if (uidString instanceof String) {
							LOGGER.warn(
									"Processing trough group object class \"contains all values\" filter workaround.");
							return (String) uidString;

						} else {

							return "";
						}
					} else {

						attributeValue = "";
					}

					return attributeValue;
				} else {

					return "";
				}
			} else {

				return "";
			}
		}
		return "";
	}

	@Override
	public StringBuilder retrieveFilterQuery(StringBuilder queryUriSnippet, char prefixChar, Filter query) {
		LOGGER.info("Processing trought the \"filter\" workaround for the provider: {0}", "slack");

		StringBuilder filterSnippet = new StringBuilder();
		filterSnippet = query.accept(new FilterHandler(), "slack");

		return queryUriSnippet.append(prefixChar).append("filter=").append(filterSnippet.toString());
	}

	@Override
	public Set<Attribute> addAttributeToInject(Set<Attribute> injectetAttributeSet) {
		Attribute schemaAttribute = AttributeBuilder.build("schemas.default.blank", SCHEMAVALUE);
		injectetAttributeSet.add(schemaAttribute);
		return injectetAttributeSet;
	}

	@Override
	public List<String> excludeFromAssembly(List<String> excludedAttributes) {
		excludedAttributes.add("meta");
		excludedAttributes.add("schemas");
		excludedAttributes.add(PHOTOS);

		return excludedAttributes;
	}

}
