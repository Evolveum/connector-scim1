/*
 * Copyright (c) 2016 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.polygon.scim;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.Header;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.message.BasicHeader;
import org.identityconnectors.framework.common.exceptions.AlreadyExistsException;
import org.identityconnectors.framework.common.exceptions.ConnectionFailedException;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.ConnectorIOException;
import org.identityconnectors.framework.common.exceptions.OperationTimeoutException;
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

/**
 * 
 * @author Macik
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
	String CONTENTTYPE = "application/json; charset=utf-8";
	String ID = "id";
	String META = "meta";
	String GROUPS = "Groups";
	String USERS = "Users";
	String NAME = "name";
	String ISCOMPLEX = "isComplex";
	String DOT = ".";
	String DISPLAYNAME = "displayName";
	String USERNAME = "userName";
	String ACTIVE = "active";
	String VALUE = "value";

	Header PRETTYPRINTHEADER = new BasicHeader("X-PrettyPrint", "1");

	/**
	 * Sends queries for object creation to the service providers endpoints. If
	 * successful the service provider returns a uid of the created object.
	 * 
	 * @param resourceEndPoint
	 *            The resource endpoint name. A string value representing the
	 *            resource endpoint name
	 *            <p>
	 *            e.q. "Users"
	 * @param objectTranslator
	 *            Instance of object translator which contains methods for the
	 *            creation of an json object out of a provided set of attributes
	 * @param attributes
	 *            The provided attributes set containing information for object
	 *            creation
	 *            <p>
	 *            e.g. [Attribute: {Name=name.familyName, Value=[Watson]},
	 *            Attribute: {Name=name.givenName, Value=[John]}]
	 * @param injectedAttributeSet
	 *            A set of attributes which are to be injected into an object
	 *            <p>
	 *            e.g. [Attribute: {Name=name.middleName, Value=[Hamish]}]
	 * @param conf
	 *            Instance of the connector configuration class, which contains
	 *            the provided configuration parameters
	 * 
	 * @throws ConnectorException
	 *             is thrown when:
	 * 
	 *             <li>the data needed for authorization of request to the
	 *             provider was not found
	 *             <li>a JSONexception has occurred while processing an json
	 *             object
	 *             <li>an Unsupported encoding exception has occurred while
	 *             processing an json object
	 * 
	 * @throws ConnectionFailedException
	 *             is thrown when a protocol exception has occurred while in the
	 *             process of creating a new resource object
	 *             
	 *@throws AlreadyExistsException
	 *				when an "409" (CONFLICT) status code is returned by the service provider as an response to the create query.
	 *
	 *@throws ConnectorIOException
	 *			if some other IO exception than a connection time out occurs while processing the current method	
	 *
	 *@throws OperationTimeoutException 
	 *			thrown when the connection times out while processing the current method
	 *
	 * @return the uid of the created object
	 */

	public Uid create(String resourceEndPoint, ObjectTranslator objectTranslator, Set<Attribute> attributes,
			Set<Attribute> injectedAttributeSet, ScimConnectorConfiguration conf);

	/**
	 * Sends queries to the service provider endpoints to retrieve the queried
	 * information and processes responses which are handed over to the provided
	 * result handler.
	 * 
	 * @param query
	 *            The query object which is of the type "Filter".
	 *            <p>
	 *            e.g. EQUALS: Attribute: {Name=__UID__, Value=[007]
	 * @param resourceEndPoint
	 *            The resource endpoint name. A string representation of the
	 *            resource endpoint name.
	 *            <p>
	 *            e.g. "Users"
	 * @param resultHandler
	 *            The provided result handler which handles results
	 * @param conf
	 *            Instance of the connector configuration class, which contains
	 *            the provided configuration parameters
	 * 
	 * @throws ConnectorException
	 *             is thrown when:
	 * 
	 *             <li>the data needed for authorization of request to the
	 *             provider was not found
	 *             <li>an error has occurred while building a connId object
	 *             <li>a JSONException has occurred while processing an json
	 *             object
	 *             <li>is thrown if no uid is returned in the process of resource
	 *             creation
	 *             
	 * @throws ConnectorIOException
	 *             is thrown when an IOException has occurred while processing
	 *             of the HTTP query response.
	 *             
	 * @throws OperationTimeoutException 
	 *			thrown when the connection times out while processing the current method
	 *
	 */

	public void query(Filter query, StringBuilder queryUriSnippet, String resourceEndPoint,
			ResultsHandler resultHandler, ScimConnectorConfiguration conf);

	/**
	 * Sends queries for object update to the service providers endpoints. After
	 * successful object update the service provider returns the uid of the
	 * updated object.
	 * 
	 * @param uid
	 *            The uid of the resource object which should be updated.
	 * @param resourceEndPoint
	 *            The name of the resource endpoint. A string value representing
	 *            the resource endpoint name
	 *            <p>
	 *            e.q. "Users"
	 * @param objectTranslator
	 *            Instance of object translator which contains methods for the
	 *            creation of an json object out of a provided set of attributes
	 * @param attributes
	 *            The provided attributes set containing information for object
	 *            creation
	 *            <p>
	 *            e.g. [Attribute: {Name=name.familyName, Value=[Watson]},
	 *            Attribute: {Name=name.givenName, Value=[John]}]
	 * @param conf
	 *            Instance of the connector configuration class, which contains
	 *            the provided configuration parameters
	 * @return the uid of the created object
	 * 
	 * @throws ConnectorException
	 *             is thrown when:
	 * 
	 *             <li>the data needed for authorization of request to the
	 *             provider was not found
	 *             <li>an UnsupportedEncodingException has occurred
	 *             <li>a JSONException has occurred while processing an json
	 *             object
	 *             <li> a query for a concrete Uid value was unsuccessful
	 * 
	 * @throws ConnectionFailedException
	 *             a protocol exception has occurred while in the process of
	 *             updating a resource object
	 *             
	 * @throws ConnectorIOException
	 *             is thrown when an IOException has occurred while processing
	 *             of the HTTP query response
	 *
	 * @throws OperationTimeoutException 
	 *			thrown when the connection times out while processing the current method
	 *
	 * @throws UnknownUidException
	 *             is thrown if no UID is present in fetched object
	 * 
	 */

	public Uid update(Uid uid, String resourceEndPoint, ObjectTranslator objectTranslator, Set<Attribute> attributes,
			ScimConnectorConfiguration conf);

	/**
	 * Sends queries for object deletion to the service providers endpoints.
	 * 
	 * @param uid
	 *            The uid of the resource object which should be deleted.
	 * @param resourceEndPoint
	 *            The name of the resource endpoint. A string value representing
	 *            the resource endpoint name
	 * 
	 *            <p>
	 *            e.q. "Users"
	 * 
	 * @param conf
	 *            Instance of the connector configuration class, which contains
	 *            the provided configuration parameters
	 *
	 * @throws ConnectorException
	 *             is thrown when:
	 * 
	 *             <li>the data needed for authorization of request to the
	 *             provider was not found
	 * 
	 * @throws ConnectionFailedException
	 *             a protocol exception has occurred while in the process of
	 *             deleting a resource object
	 * 
	 * @throws ConnectorIOException
	 *             is thrown when an IOException has occurred while processing
	 *             of the HTTP query response
	 *             
	 * @throws OperationTimeoutException 
	 *			thrown when the connection times out while processing the current method
	 *
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
	 *            service provider name
	 *            <p>
	 *            e.q. "slack"
	 * @param resourceEndPoint
	 *            The resource endpoint name. A string value representing the
	 *            resource endpoint name
	 *            <p>
	 *            e.q. "Users"
	 * @param conf
	 *            Instance of the connector configuration class, which contains
	 *            the provided configuration parameters
	 * @return an instance of "ParserSchemaScim" containing the schema
	 *         information of all endpoints
	 * @throws ConnectorException
	 *             is thrown when:
	 * 
	 *             <li>the data needed for authorization of request to the
	 *             provider was not found
	 *             <li>a protocol exception has occurred while in the process of
	 *             querying the provider Schemas resource object
	 * 
	 * @throws ConnectorIOException
	 *             an error has occurred while processing the http response
	 *
	 * @throws OperationTimeoutException 
	 *			thrown when the connection times out while processing the current method
	 *      
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
	 *            resource endpoint name
	 *            <p>
	 *            e.q. "Users"
	 * @param jsonObject
	 *            The processed json object e.g.
	 * 
	*            <pre>
	 *            {@code { 
	 *            "schema": "urn:scim:schemas:core:1.0",
	 *            "name":"nickName",
	 *            "readOnly": false,
	 *            "type": "string",
	 *            "caseExact":false,
	 *			  "required": true
	 * 			  },{ 
	 * 			  "schema":"urn:scim:schemas:core:1.0",
	 *            "name": "userName",
	 *            "readOnly":false,
	 *            "type": "string",
	 *            "caseExact": false,
	 *            "required": true
	 *            },{
	 *            "schema": "urn:scim:schemas:core:1.0",
	 *            "name": "title",
	 *            "readOnly": false,
	 *            "type": "string",
	 *            "caseExact": false,
	 *            "required": false
	 *            }}
	 * 
	 *            <pre>
	 * 
	 * @return an json object representing the full schema representation of the
	 *         connected service endpoint.
	 *         
	 *         
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
	 *            The schema json response which will be processed e.g.
	 * 
	 *            <pre>
	 *            {@code { 
	 *            "schema": "urn:scim:schemas:core:1.0",
	 *            "name":"nickName",
	 *            "readOnly": false,
	 *            "type": "string",
	 *            "caseExact":false,
	 *			  "required": true
	 * 			  },{ 
	 * 			  "schema":"urn:scim:schemas:core:1.0",
	 *            "name": "userName",
	 *            "readOnly":false,
	 *            "type": "string",
	 *            "caseExact": false,
	 *            "required": true
	 *            },{
	 *            "schema": "urn:scim:schemas:core:1.0",
	 *            "name": "title",
	 *            "readOnly": false,
	 *            "type": "string",
	 *            "caseExact": false,
	 *            "required": false
	 *            }}
	 * 
	 *            <pre>
	 * 
	 * @return an instance of "ParserSchemaScim" containing the schema
	 *         information of all endpoints
	 * 
	 * @throws ConnectorException
	 *             if no endpoint identifier present in fetched object while
	 *             processing query result
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
	 *            processed. e.g.
	 * 
	 *            <pre>
	 *            {@code 
	 *            "emails": [{ 
	 *            "type": "work",
	 *            "value":"175testuser@testdomain.com",
	 *            "primary": true 
	 *            }]
	 *            }
	 * 
	 *            <pre>
	 * @param attributeMap
	 *            The map representation of all the attributes of an concrete
	 *            endpoint which should be extended
	 * @param parser
	 *            The instance of "ParserSchemaScim" from which this method was
	 *            called and used to call additional helper methods
	 * @return a map containing the parameters and subAttributes of all
	 *         processed attributes e.g.
 	 *<pre>
	 *            {@code 
	 *            username:{ 
	 *            readOnly:false 
	 *            type:string 
	 *            multiValued:false 
	 *            caseExact:false 
	 *            required:false 
	 *            } 
	 *            }
	 * 			  .
	 * 			  .
	 * 			  .
	 *<pre>
	 *           
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
	 *            parameters and sub attributes of all the resource endpoints
	 * @return a list of map which represent the attributes and their parameters
	 *         and sub attributes of all the resource endpoints.
	 *         An example of a map of maps inside the list:
	 *<pre>
	 *            {@code 
	 *            username:{ 
	 *            readOnly:false 
	 *            type:string 
	 *            multiValued:false 
	 *            caseExact:false 
	 *            required:false 
	 *            } 
	 *            }
	 * 			  .
	 * 			  .
	 * 			  .
	 *<pre>
	 */
	public List<Map<String, Map<String, Object>>> getAttributeMapList(
			List<Map<String, Map<String, Object>>> attributeMapList);

	/**
	 * Method which is used to process the type sub attribute of complex
	 * attributes. Appends a type flag of an complex attribute depending on the
	 * "canonical value" or the "reference types" type sub attribute value. The
	 * method populates a map with the attribute name of an complex attribute
	 * appended by the type value which represents a flag for the attribute and
	 * the sub attribute name. This naming convention is used to identify
	 * different value entries of sub attributes for the same parent attribute.
	 * 
	 * A Json representation of an complex multivalue attribute:
	 * 
	 * <pre>
	 * {@code 
	 * "emails": [{ 
	 * "type": "work", 
	 * "value": "testuser@work.com",
	 * "primary": true 
	 * },{
	 * "type": "home", 
	 * "value": "testuser@home.com",
	 * "primary": true
	 * },{
	 * "type": "other",
	 * "value": "testuser@someotherplace.com",
	 * "primary": true }]}
	 * 
	 * <pre>
	 * 
	 * And the corresponding map entry which will be created: emails.work.value:
	 * testuser@work.com
	 * 
	 * @param attributeMap
	 *            The map representation of all the resources attributes which
	 *            is to be extended by the processed attribute
	 * @param referenceValues
	 *            A json array which contains the canonical values or reference
	 *            type values of the processed attribute type sub attribute
	 *             <p>e.g. [work,home,other]
	 * @param subAttributeMap
	 *            Map containing the sub attribute parameters and values of the
	 *            processed attribute e.g.
	 * 
	 *            <pre>
	 *            {@code           
	 *A map representing the sub attributes of the "Emails" attribute:
	 *
	 *type: work
	 *value: testuser@work.com
	 *primary: true
	 *}
	 * 
	 *            <pre>
	 * @param position
	 *            The position of the processed reference value in the evaluated
	 *            sub attribute json array
	 * @param attributeName
	 *            The name of the processed attribute. A string value
	 *            representing the attribute name
	 *            <p>
	 *            e.q. "Emails"
	 * @return a map representation of all the attributes of the evaluated
	 *         endpoint extended by the processed attribute e.g.
	 *            <pre>
	 *            {@code             
	 *         emails.work.value: testuser@work.com,
	 *         emails.work.primary: true,
	 *         emails.home.value: testuser@home.com,
	 *         emails.home.primary: false,
	 *         }
	 *         <pre>
	 */

	public Map<String, Object> translateReferenceValues(Map<String, Map<String, Object>> attributeMap,
			JSONArray referenceValues, Map<String, Object> subAttributeMap, int position, String attributeName);

	/**
	 * Defines a list of attributes which should be excluded from propagating to the schema builder.
	 * 
	 * @return the list of excluded attributes
	 */
	public List<String> defineExcludedAttributes();
	
	/**
	 * Extends the json object representation with the injected set of
	 * attributes.
	 * 
	 * @param injectedAttributeSet
	 *            The set of attributes which should be extended
	 *            <p>
	 *            e.g. [Attribute: {Name=schemas.default.blank,
	 *            Value=[urn:scim:schemas:core:1.0]}]
	 * 
	 * @return the extended set of attributes
	 */
	
	public Set<Attribute> addAttributesToInject(Set<Attribute> injectedAttributeSet);

	/**
	 * A workaround method used to update the group object resource
	 * representation on some specific services. Method is called after and
	 * unsuccessful query response with the response status code "500". The
	 * methods queries the group object which should be updated and changes the
	 * "members" attribute of the returned object. Then such object is send as
	 * update to the resource provider.
	 * 
	 * 
	 * Used for example by the SalesForce service.
	 * 
	 * @param response
	 *            The http response object of the first unsuccessful query
	 * @param jsonObject
	 *            The json object carrying the update for the group resource
	 *            <p>
	 *            e.g. {"members": []}
	 * @param uri
	 *            The uri of the queried resource object
	 *            <p>
	 *            e.g. ".../services/scim/v1/Groups" .
	 * 
	 * @param authHeader
	 *            The authentication header provided by the login method
	 * @throws ConnectorException
	 *             if an parse exception has occurred while parsing the http
	 *             response
	 * @throws ConnectorIOException
	 *             if an error has occurred while processing the http response.
	 *             Occurrence in the process of creating a resource object
	 *             
	 * @throws OperationTimeoutException 
	 *			thrown when the connection times out while processing the current method
	 *
	 * @throws ConnectionFailedException
	 *             if an "ClientProtocolException" exception has occurred while
	 *             in the process of updating a resource object
	 * 
	 * @return if successful the uid of the updated resource.
	 */
	public Uid groupUpdateProcedure(Integer statusCode, JSONObject jsonObject, String uri, Header authHeader, ScimConnectorConfiguration conf);


	/**
	 * Builds an connector object representation of the provided json object.
	 * 
	 * @param resourceJsonObject
	 *            The provided json object representing an resource object. e.g.
	 * 
	 *            <pre>
	 *            {@code  
	 *"nickName": "175testuser",
	 *"displayName": "Test User",
	 *"timezone": "Pacific Daylight Time",
	 *"externalId": null,
	 *"active": true,
	 *.
	 *.
	 *.
	 *.}
	 * 
	 *            <pre>
	 * @param resourceEndPoint
	 *            The resource endpoint name. A string value representing the
	 *            endpoint name
	 *            <p>
	 *            e.q. "Groups"
	 * 
	 * @throws ConnectorException
	 *             if an empty json object was passed from data provider
	 * @return the resource object connector object representation.
	 * 
	 * 
	 */

	public ConnectorObject buildConnectorObject(JSONObject resourceJsonObject, String resourceEndPoint)
			throws ConnectorException;

	/**
	 * Populates a list of string values with the names of attributes which
	 * should be excluded from connector object assembly.
	 * 
	 * @param excludedAttributes
	 *            The list of excluded attributes.
	 *            <p>
	 *            e.g. "meta","name","location"
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
	 *            <p>
	 *            e.g. [Attribute: {Name=name.familyName, Value=[Watson]},
	 *            Attribute: {Name=name.givenName, Value=[John]}]
	 * @param authoriazationData
	 *            The authorization data provided returned from the login
	 *            method.
	 * @return a set of attributes used for injection into the json object which
	 *         should be created.
	 */

	public Set<Attribute> attributeInjection(Set<Attribute> injectedAttributeSet,
			JSONObject loginJson);

	/**
	 * Processes a contains all values filter query.
	 * 
	 * @param p
	 *            Helper string parameter which can contain the provider name
	 *            <p>
	 *            e.g. "slack"
	 * @param filter
	 *            The instance of the processed "contains all values" filter
	 * @param filterHandler
	 *            Instance of a filterHandler class
	 * @return a representation of the assembled query
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
	 *            representing the attribute name
	 *            <p>
	 *            e.q. "userName"
	 * @param attributeMap
	 *            Map containing the sub properties of the processed attributes
	 * 
	 *            <pre>
	 *            {@code 
	 *            username:{ 
	 *            readOnly: false 
	 *            type: string
	 *            multiValued:false 
	 *            caseExact: false 
	 *            required: false 
	 *            }
	 *            }
	 * 
	 *            <pre>
	 * @param builder
	 *            The object class info builder which is extended by this method
	 * @param schemaBuilder
	 *            The instance of the schema builder from which this method is
	 *            called. Used to access helper methods
	 * @return an object which contains the schema representation information
	 *         for the processed attribute
	 */
	public ObjectClassInfoBuilder schemaBuilder(String attributeName, Map<String, Map<String, Object>> attributeMap,
			ObjectClassInfoBuilder builder, SchemaObjectBuilderGeneric schemaBuilder);

	/**
	 * Workaround method used to inject "ObjectClassInfoBuilder" data to extend
	 * the schema resource representation with additional attributes or
	 * operational information.
	 * 
	 * @param builder
	 *            The object class info builder which is extended by this method
	 * @param attributeName
	 *            The name of the processed attribute. A string value
	 *            representing the attribute name
	 *            <p>
	 *            e.q. "groups"
	 * @param infoBuilder
	 *            An attribute info instance to extend the parameters of the
	 *            processed attribute
	 * @return an object which contains the schema representation information
	 *         for the processed attribute
	 */
	public ObjectClassInfoBuilder schemaObjectInjection(ObjectClassInfoBuilder builder, String attributeName,
			AttributeInfoBuilder infoBuilder);

	/**
	 * Used to inject additional property data to the processed attribute.
	 * 
	 * @param infoBuilder
	 *            The attribute info builder object which is extended
	 * @param attributeName
	 *            The name of the processed attribute. A string value
	 *            representing the attribute name
	 *            <p>
	 *            e.q. "groups"
	 * @return the extended attribute info builder object
	 */

	public AttributeInfoBuilder schemaObjectParametersInjection(AttributeInfoBuilder infoBuilder, String attributeName);

	/**
	 * Method used to populate a list dictionary of strings. Those are used for
	 * evaluation of conditional statements. The strings are chosen depending on
	 * the provided flag.
	 * 
	 * @param flag
	 *            The provided flag enumeration. A representation of a flag
	 *            used to resolve a conditional statement on which the method
	 *            decides which values to add to an dictionary. The flag values:
	 *            <li>"schemaparser-workaround" - Defined as "PARSERFLAG"
	 *            
	 *            <li>"schemabuilder-workaround" - Defined as "BUILDERFLAG"
	 *            
	 * 
	 * @return the populated dictionary
	 */
	public List<String> populateDictionary(WorkaroundFlags flag);

	/**
	 * Method used to evaluate the provided query filter. Depending on the
	 * handling strategy a string value is returned used for evaluation of
	 * conditional statements.
	 * 
	 * @param filter
	 *            The provided query filter
	 * @param endpointName
	 *            The name of the queried endpoint. A string value representing
	 *            the endpoint name
	 *            <p>
	 *            e.q. "Groups"
	 * 
	 * @return a flag string value used for conditional evaluation
	 */

	public Boolean checkFilter(Filter filter, String endpointName);

	/**
	 * Handles a contains all values filter query for some specific provider
	 * resource "Groups" endpoint. The filter value is set into an equals filter
	 * query which is sent to the "Users" endpoint. The method then processes
	 * the returned json object, takes the UID values of the groups in which the
	 * user is a member of and queries them. The query response is then
	 * processed an passed to the provided handler.
	 * 
	 * For example used by the Slack provider.
	 * 
	 * @param jsonObject
	 *            the "User" json representation
	 * @param resourceEndPoint
	 *            the endpoint name of the object of which the user is a member
	 *            <p>
	 *            e.g. group
	 * @param handler
	 *            the provided resource handler
	 * @param scimBaseUri
	 *            the base URI snippet which will be processed for message
	 *            transfer
	 * @param authHeader
	 *            the provided authentication header.
	 * @throws ConnectorException
	 *             if no uid present in fetched object while processing query
	 *             result
	 * @throws ClientProtocolException
	 *             if an exception protocol exception has occurred
	 * @throws IOException
	 *             an error occurred while processing the query http response
	 */
	public void handleCAVGroupQuery(JSONObject jsonObject, String resourceEndPoint, ResultsHandler handler,
			String scimBaseUri, Header authHeader, ScimConnectorConfiguration conf) throws ClientProtocolException, IOException;

}
