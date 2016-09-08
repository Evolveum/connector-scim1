package com.evolveum.polygon.scim;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.message.BasicHeader;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.ObjectClassInfoBuilder;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.ContainsAllValuesFilter;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.json.JSONArray;
import org.json.JSONObject;

public interface HandlingStrategy {

	Header PRETTYPRINTHEADER = new BasicHeader("X-PrettyPrint", "1");

	public void qeuery(Object query, String resourceEndPoint, ResultsHandler resultHandler,
			ScimConnectorConfiguration conf);

	public ParserSchemaScim qeuerySchemas(String providerName, String resourceEndPoint,
			ScimConnectorConfiguration conf);

	public Uid create(String resourceEndPoint, ObjectTranslator objectTranslator, Set<Attribute> attributes,
			Set<Attribute> injectedAttributeSet, ScimConnectorConfiguration conf);

	public Uid update(Uid uid, String resourceEndPoint, JSONObject jsonObject, ScimConnectorConfiguration conf);

	public void delete(Uid uid, String resourceEndPoint, ScimConnectorConfiguration conf);

	public ParserSchemaScim processSchemaResponse(JSONObject responseObject, String providerName);

	public void queryMembershipData(Uid uid, String resourceEndPoint, ResultsHandler resultHandler,
			String membershipResourceEndpoin, ScimConnectorConfiguration conf);

	public List<String> excludeFromAssembly(List<String> excludedAttributes);

	public Uid groupUpdateProcedure(HttpResponse response, JSONObject jsonObject, String uri, Header authHeader,
			CrudManagerScim manager);

	public Set<Attribute> attributeInjection(Set<Attribute> injectedAttributeSet,
			Map<String, Object> autoriazationData);

	public StringBuilder processContainsAllValuesFilter(String p, ContainsAllValuesFilter filter,
			FilterHandler filterHandler);

	public Map<String, Object> translateReferenceValues(Map<String, Map<String, Object>> attributeMap,
			JSONArray referenceValues, Map<String, Object> subAttributeMap, int position, String attributeName);

	public List<String> populateDictionary(String flag);

	public ObjectClassInfoBuilder injectObjectClassInfoBuilderData(ObjectClassInfoBuilder builder, String attributeName,
			AttributeInfoBuilder infoBuilder);

	public AttributeInfoBuilder injectAttributeInfoBuilderData(AttributeInfoBuilder infoBuilder, String attributeName);

	public ObjectClassInfoBuilder schemaBuilder(String attributeName, Map<String, Map<String, Object>> attributeMap,
			ObjectClassInfoBuilder builder, SchemaObjectBuilderGeneric schemaBuilder);

	public List<Map<String, Map<String, Object>>> getAttributeMapList(
			List<Map<String, Map<String, Object>>> attributeMapList);

	public JSONObject injectMissingSchemaAttributes(String resourceName, JSONObject jsonObject);

	public String checkFilter(Filter filter, String endpointName);

	public StringBuilder retrieveFilterQuery(StringBuilder queryUriSnippet, char prefixChar, Filter query);

	public Set<Attribute> addAttributeToInject(Set<Attribute> injectetAttributeSet);
}
