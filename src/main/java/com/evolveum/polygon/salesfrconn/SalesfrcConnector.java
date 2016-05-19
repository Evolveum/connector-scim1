package com.evolveum.polygon.salesfrconn;

import java.util.Collection;
import java.util.Set;

import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.SchemaBuilder;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.AndFilter;
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

	private static final Log LOGGER = Log.getLog(SalesfrcConnector.class);

	@Override
	public Schema schema() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void delete(ObjectClass object, Uid uid, OperationOptions arg2) {

		if(ObjectClass.ACCOUNT.equals(object)){
			ForceManager.deleteEntity(uid, "Users");
		}
		else if(ObjectClass.GROUP.equals(object)){
			ForceManager.deleteEntity(uid, "Groups");
		}
	}

	@Override
	public Uid create(ObjectClass arg0, Set<Attribute> arg1, OperationOptions arg2) {
		
		UserDataBuilder userJson = new UserDataBuilder();
		
		//ForceManager.createEntity("Users/", userJson.setUserObject(arg1));

		LOGGER.info("Json response: {0}", userJson.setUserObject(arg1).toString(1));
		return null;
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub

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
	public Uid update(ObjectClass arg0, Uid arg1, Set<Attribute> arg2, OperationOptions arg3) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void test() {
		// TODO Auto-generated method stub

	}

	@Override
	public FilterTranslator<Filter> createFilterTranslator(ObjectClass arg0, OperationOptions arg1) {
		// TODO Auto-generated method stub
		return null;
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
				throw new IllegalArgumentException("objectClass " + objectClass.getDisplayNameKey()+ " is not supported");
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
			LOGGER.error("Unsuported filter type", filter);
			return false;
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
