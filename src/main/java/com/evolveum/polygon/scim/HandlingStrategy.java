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

/**
 * 
 * @author Matus
 *
 *         Interface which defines the mandatory handling strategy methods and
 *         common variables.
 */

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
	String VALUE = "value";
	String FIRSTFLAG = "schemaparser-workaround";
	String SECONDFLAG = "schemabuilder-workaround";

	Header PRETTYPRINTHEADER = new BasicHeader("X-PrettyPrint", "1");

	/**
	 * Sends queries for object creation to the service providers endpoints. If
	 * successful the service provider returns a uid of the created object.
	 * 
	 * @param resourceEndPoint
	 *            The resource endpoint name. A string value representing the
	 *            resource endpoint name (e.q. "Users")
	 * @param objectTranslator
	 *            Instance of object translator which contains methods for the
	 *            creation of an json object out of a provided set of
	 *            attributes.
	 * @param attributes
	 *            The provided attributes set containing information for object
	 *            creation.
	 * @param injectedAttributeSet
	 *            A set of attributes which are to be injected into an object.
	 * @param conf
	 *            Instance of the connector configuration class, which contains
	 *            the provided configuration parameters.
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
	 * @param conf
	 *            Instance of the connector configuration class, which contains
	 *            the provided configuration parameters.
	 *
	 */

	public void query(Object query, String resourceEndPoint, ResultsHandler resultHandler,
			ScimConnectorConfiguration conf);

	/**
	 * Sends queries for object update to the service providers endpoints. After
	 * successful object update the service provider returns the uid of the
	 * updated object.
	 * 
	 * @param uid
	 *            The uid of the resource object which should be updated.
	 * @param resourceEndPoint
	 *            The name of the resource endpoint. A string value representing
	 *            the resource endpoint name (e.q. "Users")
	 * @param jsonObject
	 *            The json object which carries the information which should be
	 *            updated.
	 * @param conf
	 *            Instance of the connector configuration class, which contains
	 *            the provided configuration parameters.
	 * @return the uid of the created object.
	 */

	public Uid update(Uid uid, String resourceEndPoint, JSONObject jsonObject, ScimConnectorConfiguration conf);

	/**
	 * Sends queries for object deletion to the service providers endpoints.
	 * 
	 * @param uid
	 *            The uid of the resource object which should be deleted.
	 * @param resourceEndPoint
	 *            The name of the resource endpoint. A string value representing
	 *            the resource endpoint name (e.q. "Users")
	 * @param conf
	 *            Instance of the connector configuration class, which contains
	 *            the provided configuration parameters.
	 */

	public void delete(Uid uid, String resourceEndPoint, ScimConnectorConfiguration conf);

	/**
	 * Sends queries to the service provider endpoints and retrieves the queried
	 * information for further processing. The response should be an json
	 * representation of the service schema information. This method sends
	 * queries only to the endpoints containing the schema information of the
	 * services resources.
	 * 
	 * @param providerName
	 *            The name of the provider. A string value representing the
	 *            service provider name (e.q. "slack")
	 * @param resourceEndPoint
	 *            The resource endpoint name. A string value representing the
	 *            resource endpoint name (e.q. "Users")
	 * @param conf
	 *            Instance of the connector configuration class, which contains
	 *            the provided configuration parameters.
	 * @return an instance of "ParserSchemaScim" containing the schema
	 *         information of all endpoints.
	 */

	public ParserSchemaScim querySchemas(String providerName, String resourceEndPoint, ScimConnectorConfiguration conf);

	/**
	 * Workaround method which checks the json schema representation returned
	 * from the service provider for inconsistencies. When such are found an
	 * json object is build containing an schema representation of attribute
	 * which are missing. This object is injected into the json returned from
	 * the service provider and later processed.
	 * 
	 * @param resourceName
	 *            The resource endpoint name. A string value representing the
	 *            resource endpoint name (e.q. "Users")
	 * @param jsonObject
	 *            The processed json object.
	 * @return an json object representing the full schema representation of the
	 *         connected service endpoint.
	 */

	public JSONObject injectMissingSchemaAttributes(String resourceName, JSONObject jsonObject);

	/**
	 * Processes the response json object returned by the service provider on an
	 * schema query. If needed the json object is divided to several json
	 * objects each containing the schema representation of an individual
	 * resource endpoint. The objects are then handed over to the schema parser
	 * for further processing.
	 * 
	 * @param responseObject
	 *            The schema json response which will be processed.
	 * @return an instance of "ParserSchemaScim" containing the schema
	 *         information of all endpoints.
	 */

	public ParserSchemaScim processSchemaResponse(JSONObject responseObject);

	/**
	 * Processes the json representation of an concrete attribute. The attribute
	 * parameters or sub attributes are set based on the information provided
	 * from the schema json representation. The data are then used to extend a
	 * map representation of all the provided attributes and their parameters
	 * and sub attributes.
	 * 
	 * @param attribute
	 *            A json representation of the attribute which will be
	 *            processed.
	 * @param attributeMap
	 *            The map representation of all the attributes of an concrete
	 *            endpoint which should be extended.
	 * @param parser
	 *            The instance of "ParserSchemaScim" from which this method was
	 *            called and used to call additional helper methods.
	 * @return a map containing the parameters and subAttributes of all
	 *         processed attributes.
	 */
	public Map<String, Map<String, Object>> parseSchemaAttribute(JSONObject attribute,
			Map<String, Map<String, Object>> attributeMap, ParserSchemaScim parser);

	/**
	 * A getter method used to return a list representation of all attributes
	 * from all the resource endpoints. Used for workaround purposes to inject
	 * additional schema attributes for schema object translation.
	 * 
	 * @param attributeMapList
	 *            The list of maps which represent the attributes and their
	 *            parameters and sub attributes of all the resource endpoints .
	 * @return a list of map which represent the attributes and their parameters
	 *         and sub attributes of all the resource endpoints .
	 */
	public List<Map<String, Map<String, Object>>> getAttributeMapList(
			List<Map<String, Map<String, Object>>> attributeMapList);

	/**
	 * Method which is used to process the type sub attribute of complex
	 * attributes. Changes the type flag of an complex attribute depending on
	 * the "canonical value" or the "reference types" type sub attribute value.
	 * 
	 * @param attributeMap
	 *            The map representation of all the resources attributes which
	 *            is to be extended by the processed attribute.
	 * @param referenceValues
	 *            An json array which contains the canonical values or reference
	 *            type values of the processed attribute type sub attribute.
	 * @param subAttributeMap
	 *            Map containing the sub attribute parameters and values of the
	 *            processed attribute.
	 * @param position
	 *            The position of the processed reference value in the evaluated
	 *            sub attribute json array.
	 * @param attributeName
	 *            The name of the processed attribute. A string value
	 *            representing the attribute name (e.q. "Emails").
	 * @return a map representation of all the attributes of the evaluated
	 *         endpoint extended by the processed attribute.
	 * 
	 */

	public Map<String, Object> translateReferenceValues(Map<String, Map<String, Object>> attributeMap,
			JSONArray referenceValues, Map<String, Object> subAttributeMap, int position, String attributeName);

	/**
	 * Extends the json object representation with the injected set of
	 * attributes.
	 * 
	 * @param injectedAttributeSet
	 *            The set of attributes which should be extended.
	 * @return The extended set of attributes.
	 */
	public Set<Attribute> addAttributesToInject(Set<Attribute> injectedAttributeSet);

	/**
	 * A workaround method used to update the group object resource
	 * representation on the Salesforce service. Method is called after and
	 * unsuccessful query response with the response status code "500". The
	 * methods queries the group object which should be updated and changes the
	 * "members" attribute of the returned object. Then such object is send as
	 * update to the resource provider.
	 * 
	 * @param response
	 *            The http response object of the first unsuccessful query.
	 * @param jsonObject
	 *            The json object carrying the update for the group resource.
	 * @param uri
	 *            The uri of the queried resource object (e.q.
	 *            ".../services/scim/v1/Groups" ).
	 * 
	 * @param authHeader
	 *            The authentication header provided by the login method.
	 * @return if successful the uid of the updated resource.
	 */
	public Uid groupUpdateProcedure(HttpResponse response, JSONObject jsonObject, String uri, Header authHeader);

	/**
	 * Method used as an workaround for the unsupported "contains all values"
	 * query filter on the Slack service. The methods lists all the groups in
	 * which the user is a member of.
	 * 
	 * @param uid
	 *            The uid of the queried user.
	 * @param resourceEndPoint
	 *            The resource endpoint name.
	 * @param resultHandler
	 *            The provided result handler.
	 * @param membershipResourceEndpoint
	 *            The endpoint name of the resource of which the user is a
	 *            member. A string value representing the endpoint name (e.q.
	 *            "Groups").
	 * @param conf
	 *            An instance of the connector configuration class which
	 *            contains the provided configuration.
	 */

	public void queryMembershipData(Uid uid, String resourceEndPoint, ResultsHandler resultHandler,
			String membershipResourceEndpoint, ScimConnectorConfiguration conf);

	/**
	 * Builds an connector object representation of the provided json object.
	 * 
	 * @param resourceJsonObject
	 *            The provided json object representing an resource object.
	 * @param resourceEndPoint
	 *            The resource endpoint name. A string value representing the
	 *            endpoint name (e.q. "Groups").
	 * @return The resource object connector object representation.
	 * @throws ConnectorException
	 */

	public ConnectorObject buildConnectorObject(JSONObject resourceJsonObject, String resourceEndPoint)
			throws ConnectorException;

	/**
	 * Populates a list of string values with the names of attributes which
	 * should be excluded from connector object assembly.
	 * 
	 * @param excludedAttributes
	 *            The list of excluded attributes.
	 * @return The list of excluded attributes populated with attribute names.
	 */

	public List<String> excludeFromAssembly(List<String> excludedAttributes);

	/**
	 * Depending from the handling strategy the methods extends a set of
	 * attributes which will be injected into the json object representation of
	 * an resource. The "authorizationData" parameter is used to provide
	 * additional data used for attribute injection (e.g. organization ID),
	 * 
	 * @param injectedAttributeSet
	 *            A provided set of attributes which should be injected into an
	 *            json object representation of an resource.
	 * @param authoriazationData
	 *            The authorization data provided returned from the login
	 *            method.
	 * @return a set of attributes used for injection into the json object which
	 *         should be created.
	 */

	public Set<Attribute> attributeInjection(Set<Attribute> injectedAttributeSet,
			Map<String, Object> authoriazationData);

	/**
	 * Processes a contains all values filter query.
	 * 
	 * @param p
	 *            Helper string parameter which can contain the provider name.
	 * @param filter
	 *            The instance of the processed contains values filter.
	 * @param filterHandler
	 *            Instance of a filterHandler class.
	 * @return a representation of the assembled query.
	 * 
	 */
	public StringBuilder processContainsAllValuesFilter(String p, ContainsAllValuesFilter filter,
			FilterHandler filterHandler);

	/**
	 * Assembles and sets up an ObjectClassInfoBuilder object for the processed
	 * attribute. The object parameters are set up accordingly to the provided
	 * map object.
	 * 
	 * @param attributeName
	 *            The name of the processed attribute. A string value
	 *            representing the attribute name (e.q. "groups").
	 * @param attributeMap
	 *            Map containing the sub properties of the processed attributes
	 * @param builder
	 *            The object class info builder which is extended by this
	 *            method.
	 * @param schemaBuilder
	 *            The instance of the schema builder from which this method is
	 *            called. Used to access helper methods.
	 * @return an object which contains the schema representation information
	 *         for the processed attribute.
	 */
	public ObjectClassInfoBuilder schemaBuilder(String attributeName, Map<String, Map<String, Object>> attributeMap,
			ObjectClassInfoBuilder builder, SchemaObjectBuilderGeneric schemaBuilder);

	/**
	 * Workaround method used to inject "ObjectClassInfoBuilder" data to extend
	 * the schema resource representation with additional attributes or
	 * operational information.
	 * 
	 * @param builder
	 *            The object class info builder which is extended by this
	 *            method.
	 * @param attributeName
	 *            The name of the processed attribute. A string value
	 *            representing the attribute name (e.q. "groups").
	 * @param infoBuilder
	 *            An attribute info instance to extend the parameters of the
	 *            processed attribute
	 * @return an object which contains the schema representation information
	 *         for the processed attribute.
	 */
	public ObjectClassInfoBuilder injectObjectClassInfoBuilderData(ObjectClassInfoBuilder builder, String attributeName,
			AttributeInfoBuilder infoBuilder);

	/**
	 * Used to inject additional property data to the processed attribute.
	 * 
	 * @param infoBuilder
	 *            The attribute info builder object which is extended.
	 * @param attributeName
	 *            The name of the processed attribute. A string value
	 *            representing the attribute name (e.q. "groups").
	 * @return the extended attribute info builder object.
	 */

	public AttributeInfoBuilder injectAttributeInfoBuilderData(AttributeInfoBuilder infoBuilder, String attributeName);

	/**
	 * Method used to populate a list dictionary of strings. Those are used for
	 * evaluation of conditional statements. The strings are chosen depending on
	 * the provided flag.
	 * 
	 * @param flag
	 *            The provided flag string. A string value representing a flag
	 *            used to resolve a conditional statement on which the method
	 *            decides which values to add to an dictionary. The flag values:
	 *            <li>"schemaparser-workaround" - Defined as the "FIRSTFLAG"
	 *            variable
	 *            <li>"schemabuilder-workaround" - Defined as the "SECONDFLAG"
	 *            variable
	 * 
	 * @return the populated dictionary.
	 */
	public List<String> populateDictionary(String flag);

	/**
	 * Method used to evaluate the provided query filter. Depending on the
	 * handling strategy a string value is returned used for evaluation of
	 * conditional statements.
	 * 
	 * @param filter
	 *            The provided query filter.
	 * @param endpointName
	 *            The name of the queried endpoint. A string value representing
	 *            the endpoint name (e.q. "Groups").
	 * 
	 * @return a flag string value used for conditional evaluation.
	 */

	public String checkFilter(Filter filter, String endpointName);

}
