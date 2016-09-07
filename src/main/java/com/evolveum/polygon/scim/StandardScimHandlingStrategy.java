package com.evolveum.polygon.scim;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.ConnectorIOException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.ObjectClassInfoBuilder;
import org.identityconnectors.framework.common.objects.OperationalAttributeInfos;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.ContainsAllValuesFilter;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.json.JSONArray;
import org.json.JSONObject;

public class StandardScimHandlingStrategy implements HandlingStrategy {

	private static final Log LOGGER = Log.getLog(StandardScimHandlingStrategy.class);
	private static final String TYPE = "type";
	private static final String DEFAULT = "default";
	private static final String SUBATTRIBUTES = "subAttributes";
	private static final String MULTIVALUED = "multiValued";
	private static final String CANONICALVALUES = "canonicalValues";
	private static final String REFERENCETYPES = "referenceTypes";

	@Override
	public Uid groupUpdateProcedure(HttpResponse response, JSONObject jsonObject, String uri, Header authHeader,
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
	public StringBuilder processContainsAllValuesFilter(String p, ContainsAllValuesFilter filter,
			FilterHandler handler) {
		StringBuilder preprocessedFilter = null;
		preprocessedFilter = handler.processArrayQ(filter, p);
		return preprocessedFilter;
	}

	@Override
	public Map<String, Map<String, Object>> parseAttribute(JSONObject attribute,
			Map<String, Map<String, Object>> attributeMap, ParserSchemaScim parser) {

		String attributeName = null;
		Boolean isComplex = false;
		Boolean isMultiValued = false;
		Map<String, Object> attributeObjects = new HashMap<String, Object>();
		Map<String, Object> subAttributeMap = new HashMap<String, Object>();

		if (attribute.has(SUBATTRIBUTES)) {
			boolean hasTypeValues = false;
			JSONArray subAttributes = new JSONArray();
			subAttributes = (JSONArray) attribute.get(SUBATTRIBUTES);
			if (attributeName == null) {
				for (String subAttributeNameKeys : attribute.keySet()) {
					if ("name".equals(subAttributeNameKeys)) {
						attributeName = attribute.get(subAttributeNameKeys).toString();
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

				if ("name".equals(attributeNameKeys)) {
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

		if (!"active".equals(attributeName)) {
			Map<String, Object> schemaSubPropertiesMap = new HashMap<String, Object>();
			schemaSubPropertiesMap = attributeMap.get(attributeName);
			for (String subPropertieName : schemaSubPropertiesMap.keySet()) {
				if (SUBATTRIBUTES.equals(subPropertieName)) {
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
			builder.addAttributeInfo(OperationalAttributeInfos.ENABLE);
		}
		return builder;
	}

	@Override
	public List<Map<String, Map<String, Object>>> getAttributeMapList(
			List<Map<String, Map<String, Object>>> attributeMapList) {
		return attributeMapList;
	}

	@Override
	public Set<Attribute> attributeInjection(Set<Attribute> injectedAttributeSet,
			Map<String, Object> autoriazationData) {
		return injectedAttributeSet;
	}

	@Override
	public JSONObject injectMissingSchemaAttributes(String resourceName, JSONObject jsonObject) {
		return jsonObject;
	}

	@Override
	public String checkFilter(Filter filter, String endpointName) {
		return "";
	}

	@Override
	public StringBuilder retrieveFilterQuery(StringBuilder queryUriSnippet, char prefixChar, Filter query) {

		StringBuilder filterSnippet = new StringBuilder();
		filterSnippet = query.accept(new FilterHandler(), "");

		queryUriSnippet.append(prefixChar).append("filter=").append(filterSnippet.toString());
		return queryUriSnippet;
	}

	@Override
	public Set<Attribute> addAttributeToInject(Set<Attribute> injectetAttributeSet) {
		return injectetAttributeSet;
	}

	@Override
	public List<String> excludeFromAssembly(List<String> excludedAttributes) {

		excludedAttributes.add("meta");
		excludedAttributes.add("schemas");

		return excludedAttributes;
	}

	@Override
	public Map<String, Object> translateReferenceValues(Map<String, Map<String, Object>> attributeMap,
			JSONArray referenceValues, Map<String, Object> subAttributeMap, int position, String attributeName) {

		Boolean isComplex = null;
		Map<String, Object> processedParameters = new HashMap<String, Object>();

		String sringReferenceValue = (String) referenceValues.get(position);

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
				attributeMap.put(complexAttrName.append(".").append(sringReferenceValue).append(".")
						.append(subAttributeKeyNames).toString(),
						(HashMap<String, Object>) subAttributeMap.get(subAttributeKeyNames));
				isComplex = true;

			}
		}
		if (isComplex != null) {
			processedParameters.put("isComplex", isComplex);
		}
		processedParameters.put("attributeMap", attributeMap);

		return processedParameters;
	}

}
