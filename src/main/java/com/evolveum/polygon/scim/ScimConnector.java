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


@ConnectorClass(displayNameKey = "ScimConnector.connector.display",
configurationClass = ScimConnectorConfiguration.class)

public class ScimConnector implements Connector, CreateOp, DeleteOp, SchemaOp,
SearchOp<Filter>, TestOp, UpdateOp {

	private ScimConnectorConfiguration configuration; 
	private ScimCrudManager crudManager;
	private ScimSchemaParser schemaParser;
	private Boolean genericsCanBeApplied = false;

	 private Schema schema = null;
	
	private static final Log LOGGER = Log.getLog(ScimConnector.class);

	@Override
	public Schema schema() {
		
		if(schema == null){
			
		SchemaBuilder schemaBuilder = new SchemaBuilder(ScimConnector.class);
		if(this.schemaParser !=null){
		buildSchemas(schemaBuilder);
		}else {
			
			ObjectClassInfo userSchemaInfo = UserDataBuilder.getUserSchema();
			ObjectClassInfo groupSchemaInfo = GroupDataBuilder.getGroupSchema();
			schemaBuilder.defineObjectClass(userSchemaInfo);
			schemaBuilder.defineObjectClass(groupSchemaInfo);
		}
		return schemaBuilder.build() ;
		}
		return this.schema;
	}

	@Override
	public void delete(ObjectClass object, Uid uid, OperationOptions arg2) {
	
		if(uid.getUidValue()==null|| uid.getUidValue().isEmpty()){
			LOGGER.error("Uid not provided or empty: {0} ", uid.getUidValue());
			throw new IllegalArgumentException("Uid value not provided or empty");
		}
		
		if(object==null){
			LOGGER.error("Object value not provided {0} ", object);
			throw new IllegalArgumentException("Object value not provided");
		}
		
		if(ObjectClass.ACCOUNT.equals(object)){
			crudManager.deleteEntity(uid, "Users");
		}
		else if(ObjectClass.GROUP.equals(object)){
			crudManager.deleteEntity(uid, "Groups");
		}else {
			LOGGER.error("Provided object value is not valid: {0}", object);
			throw new IllegalArgumentException("Object value not valid");
		}
	}

	@Override
	public Uid create(ObjectClass object, Set<Attribute> attr, OperationOptions arg2) {
		
		if(attr==null|| attr.isEmpty()){
			LOGGER.error("Set of Attributes can not be null or empty", attr);
			throw new IllegalArgumentException("Set of Attributes value is null or empty");
		}

		if(genericsCanBeApplied){
			String endpointName = object.getObjectClassValue();
			
			if (endpointName.equals(ObjectClass.ACCOUNT.getObjectClassValue())){
				
				for(Map<String,String> hlAtrribute:  schemaParser.gethlAttributeMapList()){
					for(String attributeKeys: hlAtrribute.keySet()){
						if("endpoint".equals(attributeKeys)){
							if(hlAtrribute.containsValue("/Users")){
									int position= schemaParser.gethlAttributeMapList().indexOf(hlAtrribute);
								
							}
							
						}
					}
					
				}
				
			}else if(endpointName.equals(ObjectClass.GROUP.getObjectClassValue())){
				
				
			}else {
				
				StringBuilder setEndpointFormat = new StringBuilder(endpointName);
				
				
			}
			
			
			return null;
		}else{
		
		if(ObjectClass.ACCOUNT.equals(object)){
		ObjectTranslator userBuild = new UserDataBuilder();
		
	     Uid uid = crudManager.createEntity("Users/", userBuild, attr);

		
		if(uid==null){
			LOGGER.error("No uid returned by the create method: {0} ", uid);
			throw new IllegalArgumentException("No uid returned by the create method");
		}
		
		return uid;
		
		}else if(ObjectClass.GROUP.equals(object)){
			
			GroupDataBuilder groupBuild = new GroupDataBuilder();
			
			Uid uid = crudManager.createEntity("Groups/", groupBuild, attr);

			
			if(uid==null){
				LOGGER.error("No uid returned by the create method: {0} ", uid);
				throw new IllegalArgumentException("No uid returned by the create method");
			}
			return uid;
		}else {
			
			LOGGER.error("Provided object value is not valid: {0}", object);
			throw new IllegalArgumentException("Object value not valid");
		}}
	}

	@Override
	public void dispose() {
		  configuration = null;
	}

	@Override
	public Configuration getConfiguration() {
		
		return this.configuration;
	}

	@Override
	public void init(Configuration configuration) {
		this.configuration = (ScimConnectorConfiguration)configuration;
		this.configuration.validate();
		this.crudManager = new ScimCrudManager((ScimConnectorConfiguration)configuration);
		this.schemaParser = crudManager.qeueryEntity("", "Schemas/");
		// TODO delete method call ... just for test purposess 
		schema();
	}
	

	@Override
	public Uid update(ObjectClass object, Uid id, Set<Attribute> attr, OperationOptions arg3) {

		if(attr==null|| attr.isEmpty()){
			LOGGER.error("Set of Attributes can not be null or empty: {}", attr);
			throw new IllegalArgumentException("Set of Attributes value is null or empty");
		}
		
		if(ObjectClass.ACCOUNT.equals(object)){
		UserDataBuilder userJson = new UserDataBuilder();
		
	    Uid uid = crudManager.updateEntity(id, "Users", userJson.translateSetToJson(attr, null));

		LOGGER.info("Json response: {0}", userJson.translateSetToJson(attr, null).toString(1));
		
		if(uid==null){
			LOGGER.error("No uid returned by the create method: {0} ", uid);
			throw new IllegalArgumentException("No uid returned by the create method");
		}
		return uid;	
		
		}else if(ObjectClass.GROUP.equals(object)){
			
			GroupDataBuilder groupJson = new GroupDataBuilder();
			
			Uid uid = crudManager.updateEntity(id, "groups", groupJson.translateSetToJson(attr, null));
			
			LOGGER.info("Json response: {0}", groupJson.translateSetToJson(attr, null).toString(1));
			
			if(uid==null){
				LOGGER.error("No uid returned by the create method: {0} ", uid);
				throw new IllegalArgumentException("No uid returned by the create method");
			}
			return uid;
		}else {
			LOGGER.error("Provided object value is not valid: {0}", object);
			throw new IllegalArgumentException("Object value not valid");
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

		if(handler == null){
			
			LOGGER.error("Result handler for queuery is null");
			throw new ConnectorException("Result handler for queuery can not be null");
		}
		
		if (isSupportedQuery(objectClass, query)){
			
			if(genericsCanBeApplied){
				
				String endpointName = objectClass.getObjectClassValue();
				if(endpointName.intern() == ObjectClass.ACCOUNT.getObjectClassValue().intern()){
				if(query == null){
					crudManager.qeueryEntity("", "Users/",handler);
					
				}else if(query instanceof EqualsFilter && qIsUid("Users/",query, handler)){

				}else{ 

					qIsFilter("Users/",query, handler);
				}
				}
				else if(endpointName.intern() == ObjectClass.GROUP.getObjectClassValue().intern()){
					if(query == null){
						crudManager.qeueryEntity("", "Groups/",handler);
						
					}else if(query instanceof EqualsFilter && qIsUid("Groups/",query, handler)){

					}else{ 

						qIsFilter("Groups/",query, handler);
					}
					}
				else{
					StringBuilder setEndpointFormat = new StringBuilder(endpointName);
					if(query == null){
						crudManager.qeueryEntity("", setEndpointFormat.append("/").toString() ,handler);
					}else if(query instanceof EqualsFilter && qIsUid(setEndpointFormat.append("/").toString(),query, handler)){

					}else{ 

						qIsFilter(setEndpointFormat.append("/").toString(),query, handler);
					}
					}
			}else{

			if (ObjectClass.ACCOUNT.equals(objectClass)){

				if(query == null){

					crudManager.qeueryEntity("", "Users/", handler);
				}else if(query instanceof EqualsFilter && qIsUid("Users/",query, handler)){

				}else{ 

					qIsFilter("Users/",query, handler);
				}
			}else if(ObjectClass.GROUP.equals(objectClass)){
				if(query == null){
					crudManager.qeueryEntity("", "Groups/", handler);
				}
				else if(query instanceof EqualsFilter && qIsUid("Groups/",query, handler)){

				}else { 

					qIsFilter("Groups/",query, handler);
				}
			}
			else{
				LOGGER.error("The provided objectClass is not supported: {0}", objectClass.getDisplayNameKey());
				throw new IllegalArgumentException("ObjectClass is not supported");
			}
		}}

	}


	protected boolean isSupportedQuery(ObjectClass objectClass, Filter filter){

		if ((filter instanceof AttributeFilter && !(filter instanceof ContainsAllValuesFilter ))|| filter == null){

			return true;
		}	else{
	//		LOGGER.error("Provided filter is not supported: {0}", filter);
			throw new IllegalArgumentException("Provided filter is not supported");
		}
	}

	private boolean qIsUid(String endPoint, Filter query, ResultsHandler resultHandler){
		Attribute filterAttr = ((EqualsFilter) query).getAttribute();

		if(filterAttr instanceof Uid){
				crudManager.qeueryEntity((Uid) filterAttr, endPoint, resultHandler);
			return true;
		}else
			return false;
	}
	
	private void qIsFilter(String endPoint, Filter query, ResultsHandler resultHandler){

		StringBuilder build =  
				query.accept(new FilterHandler(),endPoint);

		build.insert(0, "?filter=");
			crudManager.qeueryEntity(build.toString(), endPoint, resultHandler);

	
	}
	private SchemaBuilder buildSchemas(SchemaBuilder schemaBuilder){
		
		GenericSchemaObjectBuilder schemaObjectBuilder = new GenericSchemaObjectBuilder();
		int iterator=0;
		Map<String,String> hlAtrribute = new HashMap<String,String>();
		for(Map<String, Map<String, Object>> attributeMap:  schemaParser.getAttributeMapList()){
			hlAtrribute = schemaParser.gethlAttributeMapList().get(iterator);
			
			for(String key: hlAtrribute.keySet()){
				if(key.intern() == "endpoint"){
				String schemaName = hlAtrribute.get(key);	
				ObjectClassInfo oclassInfo = schemaObjectBuilder.buildSchema(attributeMap,schemaName );
				schemaBuilder.defineObjectClass(oclassInfo);
				}
			}
			iterator++;
		}
		genericsCanBeApplied =true;
		return schemaBuilder;
	}

}
