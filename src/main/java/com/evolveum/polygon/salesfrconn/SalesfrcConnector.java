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
    private SalesfrcManager entityManager;
    
    private static final Log LOGGER = Log.getLog(SalesfrcConnector.class);
	
	@Override
	public Schema schema() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void delete(ObjectClass object, Uid uid, OperationOptions arg2) {

		if(ObjectClass.ACCOUNT.equals(object)){
			entityManager.deleteEntity(uid, "Users");
		}
		else if(ObjectClass.GROUP.equals(object)){
			entityManager.deleteEntity(uid, "Groups");
		}
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
		this.configuration.validate();
		this.entityManager = new SalesfrcManager((SalesFrcConfiguration)configuration);
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
	
		if (ObjectClass.ACCOUNT.equals(objectClass)){
			if(query == null){
				
		entityManager.qeueryEntity("", "Users/");
			}else { 
				if (isSupportedQueue(objectClass, query)){
				//Attribute filterAttr = ((EqualsFilter) query).getAttribute();
					
					StringBuilder build =  
							query.accept(new FilterHandler(),null);
					
					build.insert(0, "?filter=");
					
				entityManager.qeueryEntity(build.toString(), "Users");
				}
			}
		}else if(ObjectClass.GROUP.equals(objectClass)){
			
			entityManager.qeueryEntity("", "Groups");
		}
		else{
			LOGGER.error("The provided objectClass is not supported: {0}", objectClass.getDisplayNameKey());
			throw new IllegalArgumentException("objectClass " + objectClass.getDisplayNameKey()+ " is not supported");
		}
		
	}
	
	
	private void buildSchema(){
		 SchemaBuilder schemaBuilder = new SchemaBuilder(SalesfrcConnector.class);
	}
	
	
	protected boolean isSupportedQueue(ObjectClass objectClass, Filter filter){
		
if (filter instanceof EqualsFilter ){
			
			Attribute attribute = ((EqualsFilter) filter).getAttribute();
			
			if (attribute instanceof Uid){
			return true;	
			}
			
		}
		
		return true;
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
