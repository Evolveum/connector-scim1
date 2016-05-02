package com.test.slsfrc.salesfrcTest;

import java.util.Set;

import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.framework.spi.Configuration;
import org.identityconnectors.framework.spi.Connector;
import org.identityconnectors.framework.spi.operations.CreateOp;
import org.identityconnectors.framework.spi.operations.DeleteOp;
import org.identityconnectors.framework.spi.operations.SchemaOp;
import org.identityconnectors.framework.spi.operations.SearchOp;
import org.identityconnectors.framework.spi.operations.TestOp;
import org.identityconnectors.framework.spi.operations.UpdateOp;

public class SalesfrcConnector implements Connector, CreateOp, DeleteOp, SchemaOp,
SearchOp<Filter>, TestOp, UpdateOp {

	public static final ObjectClass ORG_UNIT = new ObjectClass("OrgUnit");

    public static final ObjectClass MEMBER = new ObjectClass("Member");
    
    public static final ObjectClass ALIAS = new ObjectClass("Alias");
    
    public static final ObjectClass LICENSE_ASSIGNMENT = new ObjectClass("LicenseAssignment");
    
    private SalesFrcConfiguration configuration; 
	
	@Override
	public Schema schema() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void delete(ObjectClass arg0, Uid arg1, OperationOptions arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Uid create(ObjectClass arg0, Set<Attribute> arg1, OperationOptions arg2) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Configuration getConfiguration() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void init(Configuration arg0) {
		// TODO Auto-generated method stub
		
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
		//final Set<String> attributesToGet = getAttributesToGet(objectClass, options);
      //  Attribute key = getKeyFromFilter(objectClass, query);
		
	}

}
