package com.evolveum.polygon.scim;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.message.BasicHeader;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ObjectClassInfoBuilder;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.ContainsAllValuesFilter;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.json.JSONArray;
import org.json.JSONObject;

public interface HandlingStrategy {

	String TYPE = "type";
	String DEFAULT = "default";
	String SUBATTRIBUTES = "subAttributes";
	String MULTIVALUED = "multiValued";
	String AUTHHEADER = "authHeader";
	String URI = "uri";
	String LOGININSTANCE = "loginInstance";
	String SLASH = "/";
	String CONTENTTYPE = "application/json";
	String ID = "id";
	String META = "meta";
	String GROUPS = "Groups";
	String USERS = "Users";
	String NAME = "name";
	String ISCOMPLEX = "isComplex";
	String DOT = ".";
	String DISPLAYNAME = "displayName";
	String ACTIVE = "active";
	String FIRSTFLAG = "schemaparser-workaround";
	String SECONDFLAG = "schemabuilder-workaround";

	Header PRETTYPRINTHEADER = new BasicHeader("X-PrettyPrint", "1");

	public Uid create(String resourceEndPoint, ObjectTranslator objectTranslator, Set<Attribute> attributes,
			Set<Attribute> injectedAttributeSet, ScimConnectorConfiguration conf);

	public void qeuery(Object query, String resourceEndPoint, ResultsHandler resultHandler,
			ScimConnectorConfiguration conf);

	public Uid update(Uid uid, String resourceEndPoint, JSONObject jsonObject, ScimConnectorConfiguration conf);

	public void delete(Uid uid, String resourceEndPoint, ScimConnectorConfiguration conf);

	public ParserSchemaScim qeuerySchemas(String providerName, String resourceEndPoint,
			ScimConnectorConfiguration conf);

	public JSONObject injectMissingSchemaAttributes(String resourceName, JSONObject jsonObject);

	public ParserSchemaScim processSchemaResponse(JSONObject responseObject);

	public Map<String, Map<String, Object>> parseAttribute(JSONObject attribute,
			Map<String, Map<String, Object>> attributeMap, ParserSchemaScim parser);

	public List<Map<String, Map<String, Object>>> getAttributeMapList(
			List<Map<String, Map<String, Object>>> attributeMapList);

	public Map<String, Object> translateReferenceValues(Map<String, Map<String, Object>> attributeMap,
			JSONArray referenceValues, Map<String, Object> subAttributeMap, int position, String attributeName);

	public Set<Attribute> addAttributeToInject(Set<Attribute> injectetAttributeSet);

	public Uid groupUpdateProcedure(HttpResponse response, JSONObject jsonObject, String uri, Header authHeader);

	public void queryMembershipData(Uid uid, String resourceEndPoint, ResultsHandler resultHandler,
			String membershipResourceEndpoin, ScimConnectorConfiguration conf);

	public ConnectorObject buildConnectorObject(JSONObject resourceJsonObject, String resourceEndPoint,
			String providerName) throws ConnectorException;

	public List<String> excludeFromAssembly(List<String> excludedAttributes);

	public Set<Attribute> attributeInjection(Set<Attribute> injectedAttributeSet,
			Map<String, Object> autoriazationData);

	public StringBuilder processContainsAllValuesFilter(String p, ContainsAllValuesFilter filter,
			FilterHandler filterHandler);

	public ObjectClassInfoBuilder schemaBuilder(String attributeName, Map<String, Map<String, Object>> attributeMap,
			ObjectClassInfoBuilder builder, SchemaObjectBuilderGeneric schemaBuilder);

	public ObjectClassInfoBuilder injectObjectClassInfoBuilderData(ObjectClassInfoBuilder builder, String attributeName,
			AttributeInfoBuilder infoBuilder);

	public AttributeInfoBuilder injectAttributeInfoBuilderData(AttributeInfoBuilder infoBuilder, String attributeName);

	public List<String> populateDictionary(String flag);

	public String checkFilter(Filter filter, String endpointName);

}
