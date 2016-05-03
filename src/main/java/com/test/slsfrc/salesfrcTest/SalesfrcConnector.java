package com.test.slsfrc.salesfrcTest;

import java.util.Collection;
import java.util.Set;

import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.AndFilter;
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
import org.json.JSONObject;

@ConnectorClass(displayNameKey = "Salesfrc.connector.display",
configurationClass = SalesFrcConfiguration.class)

public class SalesfrcConnector implements Connector, CreateOp, DeleteOp, SchemaOp,
SearchOp<Filter>, TestOp, UpdateOp {

	public static final ObjectClass ORG_UNIT = new ObjectClass("OrgUnit");

    public static final ObjectClass MEMBER = new ObjectClass("Member");
    
    public static final ObjectClass ALIAS = new ObjectClass("Alias");
    
    public static final ObjectClass LICENSE_ASSIGNMENT = new ObjectClass("LicenseAssignment");
    
    
    
    public static final String ALIASES_ATTR = "aliases";
    
    public static final String ORG_UNIT_PATH_ATTR = "orgUnitPath";
    
    public static final String GROUP_KEY_ATTR = "groupKey";
    
    public static final String EMAIL_ATTR = "email";

    public static final String ALIAS_ATTR = "alias";

    
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
		return this.configuration;
	}

	@Override
	public void init(Configuration configuration) {
		this.configuration = (SalesFrcConfiguration)configuration;
		
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
	
	
		if (ObjectClass.ACCOUNT.equals(objectClass)){
			
		//if (nemameEntityID){
			
			// entitMngr.qeueryEntity
			
			//}	else 
		}
		
	}
	/*
	protected Attribute getKeyFromFilter(ObjectClass objectClass, Filter filter) {
        Attribute key = null;
        if (filter instanceof EqualsFilter) {
            // Account, Group, OrgUnit object classes
            Attribute filterAttr = ((EqualsFilter) filter).getAttribute();
            if (filterAttr instanceof Uid) {
                key = filterAttr;
            } else if (ObjectClass.ACCOUNT.equals(objectClass) || ObjectClass.GROUP.equals(objectClass)
                    && (filterAttr instanceof Name || filterAttr.getName().equalsIgnoreCase(ALIASES_ATTR))) {
                key = filterAttr;
            } else if (ORG_UNIT.equals(objectClass) && filterAttr.getName().equalsIgnoreCase(ORG_UNIT_PATH_ATTR)) {
                key = filterAttr;
            }
        } else if (filter instanceof AndFilter) {
            // Member object class
            if (MEMBER.equals(objectClass)) {
                Attribute groupKey = null;
                Attribute memberKey = null;
                StringBuilder memberId = new StringBuilder();

                Collection<Filter> filters = ((AndFilter) filter).getFilters();
                for (Filter f : filters) {
                    if (f instanceof EqualsFilter) {
                        Attribute filterAttr = ((EqualsFilter) f).getAttribute();
                        if (filterAttr.getName().equalsIgnoreCase(GROUP_KEY_ATTR)) {
                            groupKey = filterAttr;
                        } else if (filterAttr.getName().equalsIgnoreCase(EMAIL_ATTR) || filterAttr.getName().
                                equalsIgnoreCase(ALIAS_ATTR) || filterAttr instanceof Uid) {
                            memberKey = filterAttr;
                        } else {
                            throw new UnsupportedOperationException(
                                    "Only AndFilter('groupKey','memberKey') is supported");
                        }
                    } else {
                        throw new UnsupportedOperationException(
                                "Only AndFilter('groupKey','memberKey') is supported");
                    }
                }
                if (memberKey != null && groupKey != null) {
                    memberId.append(groupKey.getValue().get(0));
                    memberId.append("/");
                    memberId.append(memberKey.getValue().get(0));
                    key = new Uid(memberId.toString());
                }
            }
        }
        return key;
    } */


}
