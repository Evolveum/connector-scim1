package com.evolveum.polygon.salesfrconn;


import java.util.List;
import java.util.Set;

import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.logging.Log;
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
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.framework.spi.Configuration;
import org.identityconnectors.framework.spi.Connector;
import org.identityconnectors.framework.spi.ConnectorClass;
import org.identityconnectors.framework.spi.operations.CreateOp;
import org.identityconnectors.framework.spi.operations.DeleteOp;
import org.identityconnectors.framework.spi.operations.SchemaOp;
import org.identityconnectors.framework.spi.operations.SearchOp;
import org.identityconnectors.framework.spi.operations.TestOp;
import org.identityconnectors.framework.spi.operations.UpdateOp;


@ConnectorClass(displayNameKey = "Salesfrc.connector.display",
configurationClass = SalesFrcConfiguration.class)

public class SalesfrcConnector implements Connector, CreateOp, DeleteOp, SchemaOp,
SearchOp<Filter>, TestOp, UpdateOp {

	private SalesFrcConfiguration configuration; 
	private SalesfrcManager ForceManager;

	 private Schema schema = null;
	
	private static final Log LOGGER = Log.getLog(SalesfrcConnector.class);

	@Override
	public Schema schema() {
		
		if(schema == null){
		SchemaBuilder schemaBuilder = new SchemaBuilder(SalesfrcConnector.class);
		
		ObjectClassInfo user = UserDataBuilder.getUserSchema();
		
		schemaBuilder.defineObjectClass(user);
		
		return schemaBuilder.build() ;
		}
		return null;
	}

	@Override
	public void delete(ObjectClass object, Uid uid, OperationOptions arg2) {
	
		if(uid.getUidValue()==null|| uid.getUidValue().isEmpty()){
			LOGGER.error("Uid not provided or empty: {0} ", uid.getUidValue());
			throw new IllegalArgumentException("Uid value not provided or empty");
		}
		
		if(object==null){
			LOGGER.error("Object value not provided{0} ", object);
			throw new IllegalArgumentException("Object value not provided");
		}
		
		if(ObjectClass.ACCOUNT.equals(object)){
			ForceManager.deleteEntity(uid, "Users");
		}
		else if(ObjectClass.GROUP.equals(object)){
			ForceManager.deleteEntity(uid, "Groups");
		}else {
			LOGGER.error("Provided object value is not valid: {0}", object);
			throw new IllegalArgumentException("Object value not valid");
		}
	}

	@Override
	public Uid create(ObjectClass object, Set<Attribute> attr, OperationOptions arg2) {
		
		if(attr==null|| attr.isEmpty()){
			LOGGER.error("Set of Attributes can not be null or empty: {}", attr);
			throw new IllegalArgumentException("Set of Attributes value is null or empty");
		}
		
		if(ObjectClass.ACCOUNT.equals(object)){
		UserDataBuilder userJson = new UserDataBuilder();
		
	     Uid uid = ForceManager.createEntity("Users/", userJson.setUserObject(attr));

		LOGGER.info("Json response: {0}", userJson.setUserObject(attr).toString(1));
		
		if(uid==null){
			LOGGER.error("No uid returned by the create method: {0} ", uid);
			throw new IllegalArgumentException("No uid returned by the create method");
		}
		
		return uid;
		
		}else if(ObjectClass.GROUP.equals(object)){
			
			GroupDataBuilder groupJson = new GroupDataBuilder();
			
			Uid uid = ForceManager.createEntity("Groups/", groupJson.setUserObject(attr));
			
			LOGGER.info("Json response: {0}", groupJson.setUserObject(attr).toString(1));
			
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
	public void dispose() {
		  configuration = null;
	}

	@Override
	public Configuration getConfiguration() {
		// TODO Auto-generated method stub
		return this.configuration;
	}

	@Override
	public void init(Configuration configuration) {
		this.configuration = (SalesFrcConfiguration)configuration;
		this.configuration.validate();
		this.ForceManager = new SalesfrcManager((SalesFrcConfiguration)configuration);
	}

	@Override
	public Uid update(ObjectClass object, Uid id, Set<Attribute> attr, OperationOptions arg3) {

		if(attr==null|| attr.isEmpty()){
			LOGGER.error("Set of Attributes can not be null or empty: {}", attr);
			throw new IllegalArgumentException("Set of Attributes value is null or empty");
		}
		
		if(ObjectClass.ACCOUNT.equals(object)){
		UserDataBuilder userJson = new UserDataBuilder();
		
	    Uid uid = ForceManager.updateEntity(id, "Users", userJson.setUserObject(attr));

		LOGGER.info("Json response: {0}", userJson.setUserObject(attr).toString(1));
		
		if(uid==null){
			LOGGER.error("No uid returned by the create method: {0} ", uid);
			throw new IllegalArgumentException("No uid returned by the create method");
		}
		
		return uid;
		
		}else if(ObjectClass.GROUP.equals(object)){
			
			GroupDataBuilder groupJson = new GroupDataBuilder();
			
			Uid uid = ForceManager.updateEntity(id, "groups", groupJson.setUserObject(attr));
			
			LOGGER.info("Json response: {0}", groupJson.setUserObject(attr).toString(1));
			
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
	public void executeQuery(ObjectClass objectClass, Filter query, ResultsHandler arg2, OperationOptions options) {
		LOGGER.info("Object class value {0}", objectClass.getDisplayNameKey());

		if (isSupportedQuery(objectClass, query)){

			if (ObjectClass.ACCOUNT.equals(objectClass)){

				if(query == null){

					ForceManager.qeueryEntity("", "Users/");

					///TODO Dont know if good practice, should ask !
				}else if(query instanceof EqualsFilter && qIsUid(objectClass,query)){

				}else{ 

					qIsFilter(objectClass,query);
				}
			}else if(ObjectClass.GROUP.equals(objectClass)){
				if(query == null){
					ForceManager.qeueryEntity("", "Groups/");
				}
				else if(query instanceof EqualsFilter && qIsUid(objectClass,query)){

				}else { 

					qIsFilter(objectClass,query);
				}
			}
			else{
				LOGGER.error("The provided objectClass is not supported: {0}", objectClass.getDisplayNameKey());
				throw new IllegalArgumentException("ObjectClass is not supported");
			}
		}

	}

	private void buildSchema(){
		SchemaBuilder schemaBuilder = new SchemaBuilder(SalesfrcConnector.class);
	}

	protected boolean isSupportedQuery(ObjectClass objectClass, Filter filter){

		if ((filter instanceof AttributeFilter && !(filter instanceof ContainsAllValuesFilter ))|| filter == null){

			return true;
		}	else{
			LOGGER.error("Provided filter is not supported: {0}", filter);
			throw new IllegalArgumentException("Provided filter is not supported");
		}
	}

	private boolean qIsUid(ObjectClass objectClass, Filter query){
		Attribute filterAttr = ((EqualsFilter) query).getAttribute();

		if(filterAttr instanceof Uid){

			if(ObjectClass.ACCOUNT.equals(objectClass)){
				ForceManager.qeueryEntity(((Uid) filterAttr).getUidValue(), "Users/");
			}else 
				if(ObjectClass.GROUP.equals(objectClass)){
					ForceManager.qeueryEntity(((Uid) filterAttr).getUidValue(), "Groups/");
				}

			return true;
		}else
			return false;
	}

	private void qIsFilter(ObjectClass objectClass, Filter query){

		StringBuilder build =  
				query.accept(new FilterHandler(),objectClass);

		build.insert(0, "?filter=");


		if(ObjectClass.ACCOUNT.equals(objectClass)){
			ForceManager.qeueryEntity(build.toString(), "Users/");
		}else 
			if(ObjectClass.GROUP.equals(objectClass)){
				ForceManager.qeueryEntity(build.toString(), "Groups/");
			}

	}

}
