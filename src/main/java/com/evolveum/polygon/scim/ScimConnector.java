package com.evolveum.polygon.scim;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.SchemaBuilder;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.AttributeFilter;
import org.identityconnectors.framework.common.objects.filter.CompositeFilter;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.framework.spi.Configuration;
import org.identityconnectors.framework.spi.Connector;
import org.identityconnectors.framework.spi.ConnectorClass;
import org.identityconnectors.framework.spi.operations.CreateOp;
import org.identityconnectors.framework.spi.operations.DeleteOp;
import org.identityconnectors.framework.spi.operations.SchemaOp;
import org.identityconnectors.framework.spi.operations.SearchOp;
import org.identityconnectors.framework.spi.operations.TestOp;
import org.identityconnectors.framework.spi.operations.UpdateAttributeValuesOp;
import org.identityconnectors.framework.spi.operations.UpdateOp;
import org.json.JSONObject;

@ConnectorClass(displayNameKey = "ScimConnector.connector.display", configurationClass = ScimConnectorConfiguration.class)

public class ScimConnector implements Connector, CreateOp, DeleteOp, SchemaOp, SearchOp<Filter>, TestOp, UpdateOp,
UpdateAttributeValuesOp {

	private ScimConnectorConfiguration configuration;
	private ScimCrudManager crudManager;
	private ScimSchemaParser schemaParser;
	private Boolean genericsCanBeApplied = false;

	private static final String SCHEMAS = "Schemas/";
	private static final String USERS = "Users";
	private static final String GROUPS = "Groups";

	private Schema schema = null;
	private String providerName = "";

	private static final Log LOGGER = Log.getLog(ScimConnector.class);

	@Override
	public Schema schema() {

		LOGGER.info("Building schema definition");

		if (schema == null) {
			// test log delete
			SchemaBuilder schemaBuilder = new SchemaBuilder(ScimConnector.class);
			if (this.schemaParser != null) {
				buildSchemas(schemaBuilder);
			} else {

				ObjectClassInfo userSchemaInfo = UserDataBuilder.getUserSchema();
				ObjectClassInfo groupSchemaInfo = GroupDataBuilder.getGroupSchema();
				schemaBuilder.defineObjectClass(userSchemaInfo);
				schemaBuilder.defineObjectClass(groupSchemaInfo);
			}
			return schemaBuilder.build();
		}
		return this.schema;
	}

	/**
	 * Implementation of the connId delete method. The method evaluates if
	 * generic methods can be applied to the query. If not the methods
	 * implemented for core schema processing are applied.
	 * 
	 * @throws IllegalArgumentException
	 *             if the object value is not provided.
	 **/
	@Override
	public void delete(ObjectClass object, Uid uid, OperationOptions options) {
		LOGGER.info("Resource object delete");
		if (uid.getUidValue() == null || uid.getUidValue().isEmpty()) {
			LOGGER.error("Uid not provided or empty: {0} ", uid.getUidValue());
			throw new IllegalArgumentException("Uid value not provided or empty");
		}

		if (object == null) {
			LOGGER.error("Object value not provided {0} ", object);
			throw new IllegalArgumentException("Object value not provided");
		}

		if (genericsCanBeApplied) {
			String endpointName = object.getObjectClassValue();

			if (endpointName.equals(ObjectClass.ACCOUNT.getObjectClassValue())) {

				Map<String, String> hlAtrribute;
				hlAtrribute = fetchHighLevelAttributeMap("/Users");
				if (hlAtrribute != null) {

					crudManager.deleteEntity(uid, USERS);
				}
			} else if (endpointName.equals(ObjectClass.GROUP.getObjectClassValue())) {
				Map<String, String> hlAtrribute;
				hlAtrribute = fetchHighLevelAttributeMap("/Groups");
				if (hlAtrribute != null) {
					crudManager.deleteEntity(uid, GROUPS);
				}
			} else {

				Map<String, String> hlAtrribute;
				StringBuilder setEndpointFormat = new StringBuilder("/").append(endpointName);
				hlAtrribute = fetchHighLevelAttributeMap(setEndpointFormat.toString());
				if (hlAtrribute != null) {
					crudManager.deleteEntity(uid, endpointName);
				}
			}

		} else {

			if (ObjectClass.ACCOUNT.equals(object)) {
				crudManager.deleteEntity(uid, USERS);
			} else if (ObjectClass.GROUP.equals(object)) {
				crudManager.deleteEntity(uid, GROUPS);
			} else {
				LOGGER.error("Provided object value is not valid: {0}", object);
				throw new IllegalArgumentException("Object value not valid");
			}
		}
	}

	/**
	 * Implementation of the connId create method. The method evaluates if
	 * generic methods can be applied to the query. If not the methods
	 * implemented for core schema processing are applied.
	 * 
	 * @throws IllegalArgumentException
	 *             if the value of set is not provided.
	 */
	@Override
	public Uid create(ObjectClass object, Set<Attribute> attribute, OperationOptions options) {
		LOGGER.info("Resource object create");
		if (attribute == null || attribute.isEmpty()) {
			LOGGER.error("Set of Attributes can not be null or empty", attribute);
			throw new IllegalArgumentException("Set of Attributes value is null or empty");
		}

		if (genericsCanBeApplied) {
			Uid uid = new Uid("default");
			GenericDataBuilder jsonDataBuilder = new GenericDataBuilder("");
			String endpointName = object.getObjectClassValue();

			if (endpointName.equals(ObjectClass.ACCOUNT.getObjectClassValue())) {
				Map<String, String> hlAtrribute;
				hlAtrribute = fetchHighLevelAttributeMap("/Users");
				if (hlAtrribute != null) {

					Map<String, Map<String, Object>> attributeMap = new HashMap<String, Map<String, Object>>();
					attributeMap = fetchAttributeMap(hlAtrribute, attributeMap);

					uid = crudManager.createEntity(USERS, jsonDataBuilder, attribute, attributeMap);
				}

			} else if (endpointName.equals(ObjectClass.GROUP.getObjectClassValue())) {
				Map<String, String> hlAtrribute;
				hlAtrribute = fetchHighLevelAttributeMap("/Groups");
				if (hlAtrribute != null) {

					Map<String, Map<String, Object>> attributeMap = new HashMap<String, Map<String, Object>>();
					attributeMap = fetchAttributeMap(hlAtrribute, attributeMap);

					uid = crudManager.createEntity(GROUPS, jsonDataBuilder, attribute, attributeMap);

				}

			} else {

				Map<String, String> hlAtrribute;
				StringBuilder endpointNameFormat = new StringBuilder("/").append(endpointName);
				hlAtrribute = fetchHighLevelAttributeMap(endpointNameFormat.toString());
				if (hlAtrribute != null) {

					Map<String, Map<String, Object>> attributeMap = new HashMap<String, Map<String, Object>>();
					attributeMap = fetchAttributeMap(hlAtrribute, attributeMap);

					uid = crudManager.createEntity(endpointName, jsonDataBuilder, attribute, attributeMap);
				}
			}

			return uid;
		} else {

			if (ObjectClass.ACCOUNT.equals(object)) {
				ObjectTranslator userBuild = new UserDataBuilder();

				Uid uid = crudManager.createEntity(USERS, userBuild, attribute, null);

				if (uid == null) {
					LOGGER.error("No uid returned by the create method: {0} ", uid);
					throw new IllegalArgumentException("No uid returned by the create method");
				}

				return uid;

			} else if (ObjectClass.GROUP.equals(object)) {

				GroupDataBuilder groupBuild = new GroupDataBuilder();

				Uid uid = crudManager.createEntity(GROUPS, groupBuild, attribute, null);

				if (uid == null) {
					LOGGER.error("No uid returned by the create method: {0} ", uid);
					throw new IllegalArgumentException("No uid returned by the create method");
				}
				return uid;
			} else {

				LOGGER.error("Provided object value is not valid: {0}", object);
				throw new IllegalArgumentException("Object value not valid");
			}
		}
	}

	@Override
	public void dispose() {
		LOGGER.info("Configuration cleanup");
		configuration = null;
		crudManager = null;
		schemaParser = null;
	}

	@Override
	public Configuration getConfiguration() {
		LOGGER.info("Fetch configuration");
		return this.configuration;
	}

	@Override
	public void init(Configuration configuration) {
		LOGGER.info("Initiation");
		this.configuration = (ScimConnectorConfiguration) configuration;
		this.configuration.validate();
		this.crudManager = new ScimCrudManager((ScimConnectorConfiguration) configuration);

		// For Salesforce workaround purposes

		String[] loginUrlParts = this.configuration.getLoginURL().split("\\."); // e.g.
		// https://login.salesforce.com
		if (loginUrlParts.length >= 2) {

			providerName = loginUrlParts[1];
		}
		//

		this.schemaParser = crudManager.qeueryEntity(providerName, SCHEMAS);

		if (this.schemaParser != null) {

			// TODO switch to true, just for test purposess
			genericsCanBeApplied = true;
		}

	}

	/**
	 * Implementation of the connId update method. The method evaluates if
	 * generic methods can be applied to the query. If not the methods
	 * implemented for core schema processing are applied. This method is used
	 * to update singular and non complex attributes, e.g. name.familyname.
	 * 
	 * @throws IllegalArgumentException
	 *             if the provided set of attributes is null or empty.
	 * @return the Uid of the updated object.
	 **/
	@Override
	public Uid update(ObjectClass object, Uid id, Set<Attribute> attributes, OperationOptions options) {
		LOGGER.info("Resource object update");
		if (attributes == null || attributes.isEmpty()) {
			LOGGER.error("Set of Attributes can not be null or empty: {}", attributes);
			throw new IllegalArgumentException("Set of Attributes value is null or empty");
		}
		if (genericsCanBeApplied) {
			Uid uid = new Uid("default");
			GenericDataBuilder genericDataBuilder = new GenericDataBuilder("");

			String endpointName = object.getObjectClassValue();

			if (endpointName.equals(ObjectClass.ACCOUNT.getObjectClassValue())) {
				Map<String, String> hlAtrribute;
				hlAtrribute = fetchHighLevelAttributeMap("/Users");
				if (hlAtrribute != null) {

					Map<String, Map<String, Object>> attributeMap = new HashMap<String, Map<String, Object>>();
					attributeMap = fetchAttributeMap(hlAtrribute, attributeMap);

					uid = crudManager.updateEntity(id, USERS,
							genericDataBuilder.translateSetToJson(attributes, null, attributeMap));
				}

			} else if (endpointName.equals(ObjectClass.GROUP.getObjectClassValue())) {

				Map<String, String> hlAtrribute;
				hlAtrribute = fetchHighLevelAttributeMap("/Groups");
				if (hlAtrribute != null) {

					Map<String, Map<String, Object>> attributeMap = new HashMap<String, Map<String, Object>>();
					attributeMap = fetchAttributeMap(hlAtrribute, attributeMap);

					uid = crudManager.updateEntity(id, GROUPS,
							genericDataBuilder.translateSetToJson(attributes, null, attributeMap));
				}

			} else {
				Map<String, String> hlAtrribute;
				StringBuilder setEndpointFormat = new StringBuilder("/").append(endpointName);
				hlAtrribute = fetchHighLevelAttributeMap(setEndpointFormat.toString());
				if (hlAtrribute != null) {

					Map<String, Map<String, Object>> attributeMap = new HashMap<String, Map<String, Object>>();
					attributeMap = fetchAttributeMap(hlAtrribute, attributeMap);
					uid = crudManager.updateEntity(id, endpointName,
							genericDataBuilder.translateSetToJson(attributes, null, attributeMap));
				}
			}

			return uid;
		} else {
			if (ObjectClass.ACCOUNT.equals(object)) {
				UserDataBuilder userJson = new UserDataBuilder();
				JSONObject userJsonObject = new JSONObject();

				userJsonObject = userJson.translateSetToJson(attributes, null);

				Uid uid = crudManager.updateEntity(id, USERS, userJsonObject);

				LOGGER.info("Json response: {0}", userJsonObject.toString(1));

				if (uid == null) {
					LOGGER.error("No uid returned by the create method: {0} ", uid);
					throw new IllegalArgumentException("No uid returned by the create method");
				}
				return uid;

			} else if (ObjectClass.GROUP.equals(object)) {

				GroupDataBuilder groupJson = new GroupDataBuilder();
				JSONObject groupJsonObject = new JSONObject();

				groupJsonObject = groupJson.translateSetToJson(attributes, null);

				Uid uid = crudManager.updateEntity(id, GROUPS, groupJsonObject);

				LOGGER.info("Json response: {0}", groupJsonObject.toString(1));

				if (uid == null) {
					LOGGER.error("No uid returned by the create method: {0} ", uid);
					throw new IllegalArgumentException("No uid returned by the create method");
				}
				return uid;
			} else {
				LOGGER.error("Provided object value is not valid: {0}", object);
				throw new IllegalArgumentException("Object value not valid");
			}
		}
	}

	@Override
	public void test() {

		LOGGER.info("Test");

		if (crudManager != null && configuration != null) {
			if (crudManager.logIntoService() != null) {
				LOGGER.info("Test was succesfull");
			} else {

				LOGGER.warn("Connection was not established while testing");
			}
			crudManager.logOut();
		}

	}

	@Override
	public FilterTranslator<Filter> createFilterTranslator(ObjectClass arg0, OperationOptions arg1) {
		return new FilterTranslator<Filter>() {
			@Override
			public List<Filter> translate(Filter filter) {
				return CollectionUtil.newList(filter);
			}
		};
	}

	/**
	 * Implementation of the connId executeQuery method. The method evaluates if
	 * generic methods can be applied to the query. If not the methods
	 * implemented for core schema processing are applied. This method is used
	 * to execute any query define via the Filter "query" parameter.
	 * 
	 * @throws IllegalArgumentException
	 *             if the provided object class is not supported.
	 * @throws ConnectorException
	 *             if the handler attribute is null.
	 */
	@Override
	public void executeQuery(ObjectClass objectClass, Filter query, ResultsHandler handler, OperationOptions options) {
		LOGGER.info("Connector object execute query");
		LOGGER.info("Object class value {0}", objectClass.getDisplayNameKey());
		StringBuilder queryUriSnippet = new StringBuilder("");

		if (options != null) {
			queryUriSnippet = processOptions(options);
		}

		LOGGER.info("The operation options: {0}", options);
		LOGGER.info("The filter which is beaing processed: {0}", query);

		if (handler == null) {

			LOGGER.error("Result handler for queuery is null");
			throw new ConnectorException("Result handler for queuery can not be null");
		}

		if (isSupportedQuery(query)) {

			if (genericsCanBeApplied) {

				String endpointName = objectClass.getObjectClassValue();
				if (endpointName.intern() == ObjectClass.ACCOUNT.getObjectClassValue().intern()) {
					if (query == null) {

						crudManager.qeueryEntity(queryUriSnippet.toString(), USERS, handler);

					} else if (query instanceof EqualsFilter && qIsUid(USERS, query, handler)) {

					} else {
						Map<String, String> hlAtrribute;
						hlAtrribute = fetchHighLevelAttributeMap("/Users");
						Map<String, Map<String, Object>> attributeMap = new HashMap<String, Map<String, Object>>();
						attributeMap = fetchAttributeMap(hlAtrribute, attributeMap);

						qIsFilter("Users", query, handler, attributeMap, queryUriSnippet);
					}
				} else if (endpointName.intern() == ObjectClass.GROUP.getObjectClassValue().intern()) {
					if (query == null) {
						crudManager.qeueryEntity(queryUriSnippet.toString(), GROUPS, handler);

					} else if (query instanceof EqualsFilter && qIsUid(GROUPS, query, handler)) {

					} else {
						Map<String, String> hlAtrribute;
						hlAtrribute = fetchHighLevelAttributeMap("/Groups");
						Map<String, Map<String, Object>> attributeMap = new HashMap<String, Map<String, Object>>();
						attributeMap = fetchAttributeMap(hlAtrribute, attributeMap);

						qIsFilter("Groups", query, handler, attributeMap, queryUriSnippet);
					}
				} else {

					if (query == null) {

						crudManager.qeueryEntity(queryUriSnippet.toString(), endpointName, handler);
					} else if (query instanceof EqualsFilter && qIsUid(endpointName, query, handler)) {

					} else {
						Map<String, String> hlAtrribute;
						StringBuilder setEndpointFormat = new StringBuilder("/").append(endpointName);
						hlAtrribute = fetchHighLevelAttributeMap(setEndpointFormat.toString());
						Map<String, Map<String, Object>> attributeMap = new HashMap<String, Map<String, Object>>();
						attributeMap = fetchAttributeMap(hlAtrribute, attributeMap);
						qIsFilter(endpointName, query, handler, attributeMap, queryUriSnippet);
					}
				}
			} else {

				if (ObjectClass.ACCOUNT.equals(objectClass)) {

					if (query == null) {
						crudManager.qeueryEntity(queryUriSnippet.toString(), USERS, handler);
					} else if (query instanceof EqualsFilter && qIsUid(USERS, query, handler)) {

					} else {
						qIsFilter("Users", query, handler, null, queryUriSnippet);
					}
				} else if (ObjectClass.GROUP.equals(objectClass)) {
					if (query == null) {
						crudManager.qeueryEntity(queryUriSnippet.toString(), GROUPS, handler);
					} else if (query instanceof EqualsFilter && qIsUid(GROUPS, query, handler)) {

					} else {

						qIsFilter("Groups", query, handler, null, queryUriSnippet);
					}
				} else {
					LOGGER.error("The provided objectClass is not supported: {0}", objectClass.getDisplayNameKey());
					throw new IllegalArgumentException("ObjectClass is not supported");
				}
			}
		}

	}

	/**
	 * Evaluates if the provided filter query is supported.
	 * 
	 * @param filter
	 *            the provided filter query.
	 * @return the boolean value of true is query is supported.
	 * @throws IllegalArgumentException
	 *             if the provided filter is no supported.
	 **/
	protected boolean isSupportedQuery(Filter filter) {

		if (filter instanceof AttributeFilter || filter == null || filter instanceof CompositeFilter) {

			return true;
		} else {
			LOGGER.error("Provided filter is not supported: {0}", filter);
			throw new IllegalArgumentException("Provided filter is not supported");
		}
	}

	/**
	 * Used to evaluate if the queried attribute in the provided filter query is
	 * an instance of Uid. if yes, the method used to process such query is
	 * called.
	 * 
	 * @param endPoint
	 *            The name of the endpoint which should be queried.
	 * @param query
	 *            The provided filter query.
	 * @param resultHandler
	 *            The provided result handler used to handle the query result.
	 * @return true if filter attribute value is an instance of uid and executes
	 *         an successful query to the service provider. Else returns false.
	 */
	private boolean qIsUid(String endPoint, Filter query, ResultsHandler resultHandler) {
		Attribute filterAttr = ((EqualsFilter) query).getAttribute();

		if (filterAttr instanceof Uid) {
			crudManager.qeueryEntity((Uid) filterAttr, endPoint, resultHandler);
			return true;
		} else
			return false;
	}

	/**
	 * Called when the query is evaluated as an filter not containing an uid
	 * type attribute.
	 * 
	 * @param endPoint
	 *            The name of the endpoint which should be queried.
	 * @param query
	 *            The provided filter query.
	 * @param resultHandler
	 *            The provided result handler used to handle the query result.
	 * @param schemaMap
	 *            A map representation of the schema provided from the service
	 *            provider.
	 * @param queryUriSnippet
	 *            A part of the query uri which will build a larger query.
	 */
	private void qIsFilter(String endPoint, Filter query, ResultsHandler resultHandler,
			Map<String, Map<String, Object>> schemaMap, StringBuilder queryUriSnippet) {

		String prefixChar;

		if (queryUriSnippet.toString().isEmpty()) {
			prefixChar = "?";

		} else {

			prefixChar = "&";
		}
		// For salesforce workaroud purposess=
		if ("salesforce".equals(providerName)) {

			queryUriSnippet.append(prefixChar).append("filter=")
			.append(query.accept(new FilterHandler(schemaMap), providerName));

		} else {
			queryUriSnippet.append(prefixChar).append("filter=").append(query.accept(new FilterHandler(schemaMap), ""));
		}

		crudManager.qeueryEntity(queryUriSnippet.toString(), endPoint, resultHandler);
	}

	/**
	 * Calls the "schemaObjectbuilder" class "buildSchema" methods for all the
	 * individual schema resource objects.
	 * 
	 * @param schemaBuilder
	 *            The "SchemaBuilder" object which will be populated with the
	 *            data representing the schemas of resource objects.
	 * @return an the instance of "SchemaBuilder" populated with the data
	 *         representing the schemas of resource objects.
	 */
	private SchemaBuilder buildSchemas(SchemaBuilder schemaBuilder) {
		LOGGER.info("Building schemas from provided data");

		GenericSchemaObjectBuilder schemaObjectBuilder = new GenericSchemaObjectBuilder(providerName);
		int iterator = 0;
		Map<String, String> hlAtrribute = new HashMap<String, String>();
		for (Map<String, Map<String, Object>> attributeMap : schemaParser.getAttributeMapList()) {
			hlAtrribute = schemaParser.gethlAttributeMapList().get(iterator);

			for (String key : hlAtrribute.keySet()) {
				if ("endpoint".equals(key.intern())) {
					String schemaName = hlAtrribute.get(key);
					ObjectClassInfo oclassInfo = schemaObjectBuilder.buildSchema(attributeMap, schemaName);
					schemaBuilder.defineObjectClass(oclassInfo);
				}
			}
			iterator++;
		}

		return schemaBuilder;
	}

	/**
	 * Evaluates if the processed endpoint name value correstponds to the schema
	 * definition of the service provider.
	 * 
	 * @param endpointValue
	 *            The name of the evaluated endpoint.
	 * @return the corresponding name defined in the providers schema.
	 */
	private Map<String, String> fetchHighLevelAttributeMap(String endpointValue) {

		for (Map<String, String> hlAtrribute : schemaParser.gethlAttributeMapList()) {
			for (String attributeKeys : hlAtrribute.keySet()) {
				if ("endpoint".equals(attributeKeys)) {
					if (hlAtrribute.containsValue(endpointValue)) {

						return hlAtrribute;
					}
				}
			}
		}
		LOGGER.error("The endpoind name value does not correspond to the schema deffinition, endpoint value : {0}",
				endpointValue);
		throw new ConnectorException("The endpoind name value does not correspond to the schema deffinition");
	}

	/**
	 * Evaluates if the options attribute contains information for pagination
	 * configuration for the query.
	 * 
	 * @return a "StringBuilder" instance containing the query snippet with
	 *         pagination information of or is no pagination information is
	 *         provided an empty snippet.
	 */
	private StringBuilder processOptions(OperationOptions options) {
		StringBuilder queryBuilder = new StringBuilder();

		Integer pageSize = options.getPageSize();
		Integer PagedResultsOffset = options.getPagedResultsOffset();
		if (pageSize != null && PagedResultsOffset != null) {
			queryBuilder.append("?startIndex=").append(PagedResultsOffset).append("&").append("count=")
			.append(pageSize);

			return queryBuilder;
		}
		return queryBuilder.append("");
	}

	/**
	 * Pouplates an instance of Map with the schema definition of attributes of
	 * an endpoint. The Map is later used as an dictionary for endpoind object
	 * update or creation attribute information cross-checks.
	 */
	private Map<String, Map<String, Object>> fetchAttributeMap(Map<String, String> hlAtrribute,
			Map<String, Map<String, Object>> attributeMap) {

		if (hlAtrribute != null) {
			int position = schemaParser.gethlAttributeMapList().indexOf(hlAtrribute);
			attributeMap = schemaParser.getAttributeMapList().get(position);
			return attributeMap;
		} else {
			LOGGER.error("No high level attribute map for schema attribute endpoint definition present");
			throw new ConnectorException(
					"No high level attribute map for schema attribute endpoint definition present");
		}
	}

	/**
	 * Implementation of the connId addAttributeValues method. The method
	 * evaluates if generic methods can be applied to the query. If not the
	 * methods implemented for core schema processing are applied. This method
	 * is used to update multivalued and complex attributes, e.g.
	 * members.default.value .
	 * 
	 * @throws IllegalArgumentException
	 *             if the provided set of attributes is null or empty.
	 * @return the Uid of the updated object.
	 **/
	@Override
	public Uid addAttributeValues(ObjectClass object, Uid id, Set<Attribute> attributes, OperationOptions options) {

		LOGGER.info("Resource object update for addition of values");
		if (attributes == null || attributes.isEmpty()) {
			LOGGER.error("Set of Attributes can not be null or empty: {}", attributes);
			throw new IllegalArgumentException("Set of Attributes value is null or empty");
		}
		if (genericsCanBeApplied) {
			Uid uid = new Uid("default");
			GenericDataBuilder genericDataBuilder = new GenericDataBuilder("");

			String endpointName = object.getObjectClassValue();

			if (endpointName.equals(ObjectClass.ACCOUNT.getObjectClassValue())) {
				Map<String, String> hlAtrribute;
				hlAtrribute = fetchHighLevelAttributeMap("/Users");
				if (hlAtrribute != null) {

					Map<String, Map<String, Object>> attributeMap = new HashMap<String, Map<String, Object>>();
					attributeMap = fetchAttributeMap(hlAtrribute, attributeMap);

					uid = crudManager.updateEntity(id, USERS,
							genericDataBuilder.translateSetToJson(attributes, null, attributeMap));
				}

			} else if (endpointName.equals(ObjectClass.GROUP.getObjectClassValue())) {

				Map<String, String> hlAtrribute;
				hlAtrribute = fetchHighLevelAttributeMap("/Groups");
				if (hlAtrribute != null) {

					Map<String, Map<String, Object>> attributeMap = new HashMap<String, Map<String, Object>>();
					attributeMap = fetchAttributeMap(hlAtrribute, attributeMap);

					uid = crudManager.updateEntity(id, GROUPS,
							genericDataBuilder.translateSetToJson(attributes, null, attributeMap));
				}

			} else {
				Map<String, String> hlAtrribute;
				StringBuilder setEndpointFormat = new StringBuilder("/").append(endpointName);
				hlAtrribute = fetchHighLevelAttributeMap(setEndpointFormat.toString());
				if (hlAtrribute != null) {

					Map<String, Map<String, Object>> attributeMap = new HashMap<String, Map<String, Object>>();
					attributeMap = fetchAttributeMap(hlAtrribute, attributeMap);
					uid = crudManager.updateEntity(id, endpointName,
							genericDataBuilder.translateSetToJson(attributes, null, attributeMap));
				}
			}

			return uid;
		} else {
			if (ObjectClass.ACCOUNT.equals(object)) {
				UserDataBuilder userJson = new UserDataBuilder();
				JSONObject userJsonObject = new JSONObject();

				userJsonObject = userJson.translateSetToJson(attributes, null);

				Uid uid = crudManager.updateEntity(id, USERS, userJsonObject);

				LOGGER.info("Json response: {0}", userJsonObject.toString(1));

				if (uid == null) {
					LOGGER.error("No uid returned by the create method: {0} ", uid);
					throw new IllegalArgumentException("No uid returned by the create method");
				}
				return uid;

			} else if (ObjectClass.GROUP.equals(object)) {

				GroupDataBuilder groupJson = new GroupDataBuilder();
				JSONObject groupJsonObject = new JSONObject();

				groupJsonObject = groupJson.translateSetToJson(attributes, null);

				Uid uid = crudManager.updateEntity(id, GROUPS, groupJsonObject);

				LOGGER.info("Json response: {0}", groupJsonObject.toString(1));

				if (uid == null) {
					LOGGER.error("No uid returned by the create method: {0} ", uid);
					throw new IllegalArgumentException("No uid returned by the create method");
				}
				return uid;
			} else {
				LOGGER.error("Provided object value is not valid: {0}", object);
				throw new IllegalArgumentException("Object value not valid");
			}
		}

	}

	/**
	 * Implementation of the connId removeAttributeValues method. The method
	 * evaluates if generic methods can be applied to the query. If not the
	 * methods implemented for core schema processing are applied. This method
	 * is used to update multivalued and complex attributes, e.g.
	 * members.default.value . The updates are used for removal of attribute
	 * values of multivalued and complex attributes.
	 * 
	 * @throws IllegalArgumentException
	 *             if the provided set of attributes is null or empty.
	 * @return the Uid of the updated object.
	 **/
	@Override
	public Uid removeAttributeValues(ObjectClass object, Uid id, Set<Attribute> attributes, OperationOptions options) {

		LOGGER.info("Resource object update for removal of attribute values");
		if (attributes == null || attributes.isEmpty()) {
			LOGGER.error("Set of Attributes can not be null or empty: {}", attributes);
			throw new IllegalArgumentException("Set of Attributes value is null or empty");
		}
		if (genericsCanBeApplied) {
			Uid uid = new Uid("default");
			GenericDataBuilder genericDataBuilder = new GenericDataBuilder("delete");

			String endpointName = object.getObjectClassValue();

			if (endpointName.equals(ObjectClass.ACCOUNT.getObjectClassValue())) {
				Map<String, String> hlAtrribute;
				hlAtrribute = fetchHighLevelAttributeMap("/Users");
				if (hlAtrribute != null) {

					Map<String, Map<String, Object>> attributeMap = new HashMap<String, Map<String, Object>>();
					attributeMap = fetchAttributeMap(hlAtrribute, attributeMap);

					uid = crudManager.updateEntity(id, USERS,
							genericDataBuilder.translateSetToJson(attributes, null, attributeMap));
				}

			} else if (endpointName.equals(ObjectClass.GROUP.getObjectClassValue())) {

				Map<String, String> hlAtrribute;
				hlAtrribute = fetchHighLevelAttributeMap("/Groups");
				if (hlAtrribute != null) {

					Map<String, Map<String, Object>> attributeMap = new HashMap<String, Map<String, Object>>();
					attributeMap = fetchAttributeMap(hlAtrribute, attributeMap);

					uid = crudManager.updateEntity(id, GROUPS,
							genericDataBuilder.translateSetToJson(attributes, null, attributeMap));
				}

			} else {
				Map<String, String> hlAtrribute;
				StringBuilder setEndpointFormat = new StringBuilder("/").append(endpointName);
				hlAtrribute = fetchHighLevelAttributeMap(setEndpointFormat.toString());
				if (hlAtrribute != null) {

					Map<String, Map<String, Object>> attributeMap = new HashMap<String, Map<String, Object>>();
					attributeMap = fetchAttributeMap(hlAtrribute, attributeMap);
					uid = crudManager.updateEntity(id, endpointName,
							genericDataBuilder.translateSetToJson(attributes, null, attributeMap));
				}
			}

			return uid;
		} else {
			if (ObjectClass.ACCOUNT.equals(object)) {
				UserDataBuilder userJson = new UserDataBuilder();
				JSONObject userJsonObject = new JSONObject();

				userJsonObject = userJson.translateSetToJson(attributes, null);

				Uid uid = crudManager.updateEntity(id, USERS, userJsonObject);

				LOGGER.info("Json response: {0}", userJsonObject.toString(1));

				if (uid == null) {
					LOGGER.error("No uid returned by the create method: {0} ", uid);
					throw new IllegalArgumentException("No uid returned by the create method");
				}
				return uid;

			} else if (ObjectClass.GROUP.equals(object)) {

				GroupDataBuilder groupJson = new GroupDataBuilder();
				JSONObject groupJsonObject = new JSONObject();

				groupJsonObject = groupJson.translateSetToJson(attributes, null);

				Uid uid = crudManager.updateEntity(id, GROUPS, groupJsonObject);

				LOGGER.info("Json response: {0}", groupJsonObject.toString(1));

				if (uid == null) {
					LOGGER.error("No uid returned by the create method: {0} ", uid);
					throw new IllegalArgumentException("No uid returned by the create method");
				}
				return uid;
			} else {
				LOGGER.error("Provided object value is not valid: {0}", object);
				throw new IllegalArgumentException("Object value not valid");
			}
		}

	}

}
