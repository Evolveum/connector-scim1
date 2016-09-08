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
	public ObjectClassInfoBuilder schemaBuilder(String attributeName, Map<String, Map<String, Object>> attributeMap,
			ObjectClassInfoBuilder builder, SchemaObjectBuilderGeneric schemaBuilder) {

		AttributeInfoBuilder infoBuilder = new AttributeInfoBuilder(attributeName);
		Boolean containsDictionaryValue = false;

		List<String> dictionary = populateDictionary("schemabuilder-workaround");

		if (dictionary.contains(attributeName)) {
			containsDictionaryValue = true;
		}

		if (!containsDictionaryValue) {
			dictionary.clear();
			Map<String, Object> schemaSubPropertiesMap = new HashMap<String, Object>();
			schemaSubPropertiesMap = attributeMap.get(attributeName);

			for (String subPropertieName : schemaSubPropertiesMap.keySet()) {
				containsDictionaryValue = false;
				dictionary = populateDictionary("schemaparser-workaround");
				if (dictionary.contains(subPropertieName)) {
					containsDictionaryValue = true;
				}

				if (containsDictionaryValue) {
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

					infoBuilder = schemaBuilder.subPropertiesChecker(infoBuilder, schemaSubPropertiesMap,
							subPropertieName);
					infoBuilder = injectAttributeInfoBuilderData(infoBuilder, attributeName);

				}

			}
			builder.addAttributeInfo(infoBuilder.build());
		} else {
			builder = injectObjectClassInfoBuilderData(builder, attributeName, infoBuilder);
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
			if (!TYPE.equals(subAttributeKeyNames)) {

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

	@Override
	public List<String> populateDictionary(String flag) {

		List<String> dictionary = new ArrayList<String>();

		if ("schemaparser-workaround".equals(flag)) {
			dictionary.add("subAttributes");
		} else if ("schemabuilder-workaround".equals(flag)) {

			dictionary.add("active");
		} else {

			LOGGER.warn("No such flag defined: {0}", flag);
		}
		return dictionary;

	}

	@Override
	public ObjectClassInfoBuilder injectObjectClassInfoBuilderData(ObjectClassInfoBuilder builder, String attributeName,
			AttributeInfoBuilder infoBuilder) {
		builder.addAttributeInfo(OperationalAttributeInfos.ENABLE);
		return builder;
	}

	@Override
	public AttributeInfoBuilder injectAttributeInfoBuilderData(AttributeInfoBuilder infoBuilder, String attributeName) {
		return infoBuilder;
	}

}
