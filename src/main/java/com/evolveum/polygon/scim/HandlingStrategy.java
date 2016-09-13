package com.evolveum.polygon.scim;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.message.BasicHeader;
import org.identityconnectors.framework.common.exceptions.ConnectionFailedException;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.ConnectorIOException;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
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

	/**
	 * Sends queries for object creation to the service providers endpoints.
	 * After successful object creation the service provider returns the uid of
	 * the created object.
	 * 
	 * @param objectTranslator
	 *            An instance of object translator containing methods for the
	 *            creation of an json object out of an provided set of
	 *            attributes.
	 * @param resourceEndPoint
	 *            The resource endpoint name.
	 * @param attributes
	 *            The provided attributes set containing information for object
	 *            creation.
	 * @throws ConnectorException
	 * @throws ConnectorIOException
	 * @throws ConnectionFailedException
	 * @throws UnknownUidException
	 *
	 * @return the uid of the created object.
	 */

	public Uid create(String resourceEndPoint, ObjectTranslator objectTranslator, Set<Attribute> attributes,
			Set<Attribute> injectedAttributeSet, ScimConnectorConfiguration conf);

	/**
	 * Sends queries to the service provider endpoints to retrieve the queried
	 * information and processes responses which are handed over to the provided
	 * result handler.
	 * 
	 * @param query
	 *            The query object which can be a string or an Uid type object.
	 * @param resourceEndPoint
	 *            The resource endpoint name.
	 * @param resultHandler
	 *            The provided result handler which handles results.
	 *
	 * @throws ConnectorException
	 * @throws ConnectorIOException
	 */

	public void qeuery(Object query, String resourceEndPoint, ResultsHandler resultHandler,
			ScimConnectorConfiguration conf);

	/**
	 * Sends queries for object update to the service providers endpoints. After
	 * successful object update the service provider returns the uid of the
	 * updated object.
	 * 
	 * @param uid
	 *            The uid of the resource object which should be updated.
	 * @param resourceEndPoint
	 *            The name of the resource endpoint.
	 * @param jsonObject
	 *            The json object which carries the information which should be
	 *            updated.
	 * 
	 * @throws ConnectorException
	 * @throws UnknownUidException
	 * @throws ConnectorIOException
	 * @throws ConnectionFailedException
	 *
	 * @return the uid of the created object.
	 */

	public Uid update(Uid uid, String resourceEndPoint, JSONObject jsonObject, ScimConnectorConfiguration conf);

	/**
	 * Sends queries for object deletion to the service providers endpoints.
	 * 
	 * @param uid
	 *            The uid of the resource object which should be deleted.
	 * @param resourceEndPoint
	 *            The name of the resource endpoint.
	 * 
	 * @throws ConnectorIOException
	 * @throws ConnectionFailedException
	 *
	 */

	public void delete(Uid uid, String resourceEndPoint, ScimConnectorConfiguration conf);

	/**
	 * Sends queries to the service provider endpoints to retrieve the queried
	 * information and processes responses which are handed over to the provided
	 * result handler. This method send queries only to the endpoints containing
	 * the schema information of the services resources.
	 * 
	 * @param providerName
	 *            The name of the provider.
	 * @param resourceEndPoint
	 *            The resource endpoint name.
	 *
	 * @throws ConnectorException
	 * @throws ConnectorIOException
	 *
	 * @return an instance of "ScimSchemaParser" containing the schema
	 *         information of all endpoint.
	 */

	public ParserSchemaScim qeuerySchemas(String providerName, String resourceEndPoint,
			ScimConnectorConfiguration conf);

	public JSONObject injectMissingSchemaAttributes(String resourceName, JSONObject jsonObject);

	public ParserSchemaScim processSchemaResponse(JSONObject responseObject);

	public Map<String, Map<String, Object>> parseSchemaAttribute(JSONObject attribute,
			Map<String, Map<String, Object>> attributeMap, ParserSchemaScim parser);

	public List<Map<String, Map<String, Object>>> getAttributeMapList(
			List<Map<String, Map<String, Object>>> attributeMapList);

	public Map<String, Object> translateReferenceValues(Map<String, Map<String, Object>> attributeMap,
			JSONArray referenceValues, Map<String, Object> subAttributeMap, int position, String attributeName);

	public Set<Attribute> addAttributesToInject(Set<Attribute> injectetAttributeSet);

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
