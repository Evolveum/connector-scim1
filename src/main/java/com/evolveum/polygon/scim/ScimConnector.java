package com.evolveum.polygon.scim;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.SchemaBuilder;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.AttributeFilter;
import org.identityconnectors.framework.common.objects.filter.ContainsAllValuesFilter;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.common.objects.filter.FilterBuilder;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.framework.common.objects.filter.StringFilter;
import org.identityconnectors.framework.spi.Configuration;
import org.identityconnectors.framework.spi.Connector;
import org.identityconnectors.framework.spi.ConnectorClass;
import org.identityconnectors.framework.spi.operations.CreateOp;
import org.identityconnectors.framework.spi.operations.DeleteOp;
import org.identityconnectors.framework.spi.operations.SchemaOp;
import org.identityconnectors.framework.spi.operations.SearchOp;
import org.identityconnectors.framework.spi.operations.TestOp;
import org.identityconnectors.framework.spi.operations.UpdateOp;
import org.json.JSONObject;

import groovy.json.JsonBuilder;

@ConnectorClass(displayNameKey = "ScimConnector.connector.display", configurationClass = ScimConnectorConfiguration.class)

public class ScimConnector implements Connector, CreateOp, DeleteOp, SchemaOp, SearchOp<Filter>, TestOp, UpdateOp {

	private ScimConnectorConfiguration configuration;
	private ScimCrudManager crudManager;
	private ScimSchemaParser schemaParser;
	private Boolean genericsCanBeApplied = false;
	
	private static final String SCHEMAS ="Schemas/";
	private static final String USERS ="Users";
	private static final String GROUPS ="Groups";

	private Schema schema = null;

	private static final Log LOGGER = Log.getLog(ScimConnector.class);

	@Override
	public Schema schema() {

		if (schema == null) {

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

	@Override
	public void delete(ObjectClass object, Uid uid, OperationOptions arg2) {

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
					if (hlAtrribute!=null){


								crudManager.deleteEntity(uid, USERS);
					}
			} else if (endpointName.equals(ObjectClass.GROUP.getObjectClassValue())) {
				Map<String, String> hlAtrribute;
				hlAtrribute = fetchHighLevelAttributeMap("/Groups");
					if (hlAtrribute!=null){
								crudManager.deleteEntity(uid, GROUPS);
					}
			} else {
				
				Map<String, String> hlAtrribute;
				hlAtrribute = fetchHighLevelAttributeMap(endpointName);
					if (hlAtrribute!=null){
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

	@Override
	public Uid create(ObjectClass object, Set<Attribute> attr, OperationOptions arg2) {

		if (attr == null || attr.isEmpty()) {
			LOGGER.error("Set of Attributes can not be null or empty", attr);
			throw new IllegalArgumentException("Set of Attributes value is null or empty");
		}

		if (genericsCanBeApplied) {
			Uid uid = new Uid("default");
			GenericDataBuilder jsonDataBuilder = new GenericDataBuilder();
			String endpointName = object.getObjectClassValue();

			if (endpointName.equals(ObjectClass.ACCOUNT.getObjectClassValue())) {
				Map<String, String> hlAtrribute;
				hlAtrribute = fetchHighLevelAttributeMap("/Users");
					if (hlAtrribute!=null){
			
						Map<String, Map<String, Object>> attributeMap = new HashMap<String, Map<String, Object>>();
						attributeMap=fetchAttributeMap(hlAtrribute,attributeMap);
						
								uid = crudManager.createEntity(USERS, jsonDataBuilder, attr, attributeMap);
					}
					
			} else if (endpointName.equals(ObjectClass.GROUP.getObjectClassValue())) {
					Map<String, String> hlAtrribute;
					hlAtrribute = fetchHighLevelAttributeMap("/Groups");
						if (hlAtrribute!=null){
				
								
							Map<String, Map<String, Object>> attributeMap = new HashMap<String, Map<String, Object>>();
							attributeMap=fetchAttributeMap(hlAtrribute,attributeMap);
							
								uid = crudManager.createEntity(GROUPS, jsonDataBuilder, attr, attributeMap);

						}

			} else {

				Map<String, String> hlAtrribute;
				hlAtrribute = fetchHighLevelAttributeMap(endpointName);
					if (hlAtrribute!=null){
			
						Map<String, Map<String, Object>> attributeMap = new HashMap<String, Map<String, Object>>();
						attributeMap=fetchAttributeMap(hlAtrribute,attributeMap);

								String[] endpointNameParts = endpointName.split("\\/"); // eg./Entitlements										

								String endpointNamePart = endpointNameParts[1];

								uid = crudManager.createEntity(endpointNamePart, jsonDataBuilder, attr, attributeMap);

					}
			}

			return uid;
		} else {

			if (ObjectClass.ACCOUNT.equals(object)) {
				ObjectTranslator userBuild = new UserDataBuilder();

				Uid uid = crudManager.createEntity(USERS, userBuild, attr, null);

				if (uid == null) {
					LOGGER.error("No uid returned by the create method: {0} ", uid);
					throw new IllegalArgumentException("No uid returned by the create method");
				}

				return uid;

			} else if (ObjectClass.GROUP.equals(object)) {

				GroupDataBuilder groupBuild = new GroupDataBuilder();

				Uid uid = crudManager.createEntity(GROUPS, groupBuild, attr, null);

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
		configuration = null;
		crudManager= null;
		schemaParser= null;
	}

	@Override
	public Configuration getConfiguration() {

		return this.configuration;
	}

	@Override
	public void init(Configuration configuration) {
		this.configuration = (ScimConnectorConfiguration) configuration;
		this.configuration.validate();
		this.crudManager = new ScimCrudManager((ScimConnectorConfiguration) configuration);
		this.schemaParser = crudManager.qeueryEntity("", SCHEMAS);
	}

	@Override
	public Uid update(ObjectClass object, Uid id, Set<Attribute> attr, OperationOptions arg3) {

		if (attr == null || attr.isEmpty()) {
			LOGGER.error("Set of Attributes can not be null or empty: {}", attr);
			throw new IllegalArgumentException("Set of Attributes value is null or empty");
		}
		if (genericsCanBeApplied) {
			Uid uid = new Uid("default");
			GenericDataBuilder genericDataBuilder = new GenericDataBuilder();

			String endpointName = object.getObjectClassValue();

			if (endpointName.equals(ObjectClass.ACCOUNT.getObjectClassValue())) {
				Map<String, String> hlAtrribute;
				hlAtrribute = fetchHighLevelAttributeMap("/Users");
					if (hlAtrribute!=null){
						
						Map<String, Map<String, Object>> attributeMap = new HashMap<String, Map<String, Object>>();
						attributeMap=fetchAttributeMap(hlAtrribute,attributeMap);

								uid = crudManager.updateEntity(id, USERS,
										genericDataBuilder.translateSetToJson(attr, null, attributeMap));
					}

			} else if (endpointName.equals(ObjectClass.GROUP.getObjectClassValue())) {
				
				Map<String, String> hlAtrribute;
				hlAtrribute = fetchHighLevelAttributeMap("/Groups");
					if (hlAtrribute!=null){
		

						Map<String, Map<String, Object>> attributeMap = new HashMap<String, Map<String, Object>>();
						attributeMap=fetchAttributeMap(hlAtrribute,attributeMap);

								uid = crudManager.updateEntity(id, GROUPS,
										genericDataBuilder.translateSetToJson(attr, null, attributeMap));
					}

			} else {
				Map<String, String> hlAtrribute;
				hlAtrribute = fetchHighLevelAttributeMap(endpointName);
					if (hlAtrribute!=null){
				
								
						Map<String, Map<String, Object>> attributeMap = new HashMap<String, Map<String, Object>>();
						attributeMap=fetchAttributeMap(hlAtrribute,attributeMap);

								String[] endpointNameParts = endpointName.split("\\/"); // eg.
																						// /Entitlements

								String endpointNamePart = endpointNameParts[1];

								uid = crudManager.updateEntity(id, endpointNamePart,
										genericDataBuilder.translateSetToJson(attr, null, attributeMap));
					}
					}
					
			return uid;
		} else {
			if (ObjectClass.ACCOUNT.equals(object)) {
				UserDataBuilder userJson = new UserDataBuilder();
				JSONObject userJsonObject = new JSONObject();
				
				userJsonObject = userJson.translateSetToJson(attr, null);

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
				
				groupJsonObject = groupJson.translateSetToJson(attr, null);

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
		
		
		
		LOGGER.ok("in test method/test OK");
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

	@Override
	public void executeQuery(ObjectClass objectClass, Filter query, ResultsHandler handler, OperationOptions options) {
		LOGGER.info("Object class value {0}", objectClass.getDisplayNameKey());

		LOGGER.info("The filter which is beaing processed: {0}", query);

		if (handler == null) {

			LOGGER.error("Result handler for queuery is null");
			throw new ConnectorException("Result handler for queuery can not be null");
		}

		if (isSupportedQuery(objectClass, query)) {

			if (genericsCanBeApplied) {

				String endpointName = objectClass.getObjectClassValue();
				if (endpointName.intern() == ObjectClass.ACCOUNT.getObjectClassValue().intern()) {
					if (query == null) {
						
						
						crudManager.qeueryEntity("", USERS, handler);

					} else if (query instanceof EqualsFilter && qIsUid(USERS, query, handler)) {

					} else {
						Map<String, String> hlAtrribute;
						hlAtrribute = fetchHighLevelAttributeMap("/Users");
						Map<String, Map<String, Object>> attributeMap = new HashMap<String, Map<String, Object>>();
						attributeMap=fetchAttributeMap(hlAtrribute, attributeMap);
						
						qIsFilter("Users/", query, handler,attributeMap);
					}
				} else if (endpointName.intern() == ObjectClass.GROUP.getObjectClassValue().intern()) {
					if (query == null) {
						crudManager.qeueryEntity("", GROUPS, handler);

					} else if (query instanceof EqualsFilter && qIsUid(GROUPS, query, handler)) {

					} else {
						Map<String, String> hlAtrribute;
						hlAtrribute = fetchHighLevelAttributeMap("/Users");
						Map<String, Map<String, Object>> attributeMap = new HashMap<String, Map<String, Object>>();
						attributeMap=fetchAttributeMap(hlAtrribute, attributeMap);
						
						qIsFilter("Groups/", query, handler,attributeMap);
					}
				} else {
					String[] endpointNameParts = endpointName.split("\\/"); // eg./Entitlements										
					String endpointNamePart = endpointNameParts[1];
					if (query == null) {
						crudManager.qeueryEntity("", endpointNamePart, handler);
					} else if (query instanceof EqualsFilter
							&& qIsUid(endpointNamePart, query, handler)) {

					} else {
						Map<String, String> hlAtrribute;
						hlAtrribute = fetchHighLevelAttributeMap(endpointName);
						Map<String, Map<String, Object>> attributeMap = new HashMap<String, Map<String, Object>>();
						attributeMap=fetchAttributeMap(hlAtrribute, attributeMap);
						StringBuilder setEndpointFormat = new StringBuilder(endpointNamePart);
						
						qIsFilter(setEndpointFormat.append("/").toString(), query, handler,attributeMap);
					}
				}
			} else {

				if (ObjectClass.ACCOUNT.equals(objectClass)) {

					if (query == null) {

						crudManager.qeueryEntity("", USERS, handler);
					} else if (query instanceof EqualsFilter && qIsUid(USERS, query, handler)) {

					} else {

						qIsFilter("Users/", query, handler,null);
					}
				} else if (ObjectClass.GROUP.equals(objectClass)) {
					if (query == null) {
						crudManager.qeueryEntity("", GROUPS, handler);
					} else if (query instanceof EqualsFilter && qIsUid(GROUPS, query, handler)) {

					} else {

						qIsFilter("Groups/", query, handler, null);
					}
				} else {
					LOGGER.error("The provided objectClass is not supported: {0}", objectClass.getDisplayNameKey());
					throw new IllegalArgumentException("ObjectClass is not supported");
				}
			}
		}

	}

	protected boolean isSupportedQuery(ObjectClass objectClass, Filter filter) {

		if ((filter instanceof AttributeFilter && !(filter instanceof ContainsAllValuesFilter)) || filter == null) {

			return true;
		} else {
			// LOGGER.error("Provided filter is not supported: {0}", filter);
			throw new IllegalArgumentException("Provided filter is not supported");
		}
	}

	private boolean qIsUid(String endPoint, Filter query, ResultsHandler resultHandler) {
		Attribute filterAttr = ((EqualsFilter) query).getAttribute();

		if (filterAttr instanceof Uid) {
			crudManager.qeueryEntity((Uid) filterAttr, endPoint, resultHandler);
			return true;
		} else
			return false;
	}

	private void qIsFilter(String endPoint, Filter query, ResultsHandler resultHandler,Map<String,Map<String,Object>> schemaMap) {

		StringBuilder build = query.accept(new FilterHandler(schemaMap), endPoint);

		build.insert(0, "?filter=");
		crudManager.qeueryEntity(build.toString(), endPoint, resultHandler);

	}

	private SchemaBuilder buildSchemas(SchemaBuilder schemaBuilder) {

		GenericSchemaObjectBuilder schemaObjectBuilder = new GenericSchemaObjectBuilder();
		int iterator = 0;
		Map<String, String> hlAtrribute = new HashMap<String, String>();
		for (Map<String, Map<String, Object>> attributeMap : schemaParser.getAttributeMapList()) {
			hlAtrribute = schemaParser.gethlAttributeMapList().get(iterator);

			for (String key : hlAtrribute.keySet()) {
				if (key.intern() == "endpoint") {
					String schemaName = hlAtrribute.get(key);
					ObjectClassInfo oclassInfo = schemaObjectBuilder.buildSchema(attributeMap, schemaName);
					schemaBuilder.defineObjectClass(oclassInfo);
				}
			}
			iterator++;
		}
		genericsCanBeApplied = true;
		return schemaBuilder;
	}
	
	private Map<String,String> fetchHighLevelAttributeMap(String endpointValue){
		
		for (Map<String, String> hlAtrribute : schemaParser.gethlAttributeMapList()) {
			for (String attributeKeys : hlAtrribute.keySet()) {
				if ("endpoint".equals(attributeKeys)) {
					if (hlAtrribute.containsValue(endpointValue)) {
						
						return hlAtrribute;
					}
					else{
						LOGGER.error("The endpoind name value does not correspond to the schema deffinition, endpoint value : [0]", endpointValue);
						throw new ConnectorException("The endpoind name value does not correspond to the schema deffinition");
					}
					}
				}
			}
	
		return null;
	}
	
	private Map<String,Map<String, Object>> fetchAttributeMap(Map<String,String> hlAtrribute, Map<String, Map<String, Object>> attributeMap ){
		
		if (hlAtrribute!=null){
			int position = schemaParser.gethlAttributeMapList().indexOf(hlAtrribute);
			attributeMap = schemaParser.getAttributeMapList().get(position);
			return attributeMap;
	} else {
		LOGGER.error("No high level attribute map for schema attribute endpoint definition present");
		throw new ConnectorException("No high level attribute map for schema attribute endpoint definition present");
	}
	}

}
