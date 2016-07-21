package com.evolveum.polygon.scim;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.InvalidAttributeValueException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.filter.AndFilter;
import org.identityconnectors.framework.common.objects.filter.AttributeFilter;
import org.identityconnectors.framework.common.objects.filter.ContainsAllValuesFilter;
import org.identityconnectors.framework.common.objects.filter.ContainsFilter;
import org.identityconnectors.framework.common.objects.filter.EndsWithFilter;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.common.objects.filter.FilterBuilder;
import org.identityconnectors.framework.common.objects.filter.FilterVisitor;
import org.identityconnectors.framework.common.objects.filter.GreaterThanFilter;
import org.identityconnectors.framework.common.objects.filter.GreaterThanOrEqualFilter;
import org.identityconnectors.framework.common.objects.filter.LessThanFilter;
import org.identityconnectors.framework.common.objects.filter.LessThanOrEqualFilter;
import org.identityconnectors.framework.common.objects.filter.NotFilter;
import org.identityconnectors.framework.common.objects.filter.OrFilter;
import org.identityconnectors.framework.common.objects.filter.StartsWithFilter;

// Missing filterVisitor methods/filters from SCIM v1 specification: not equal, present

public class FilterHandler implements FilterVisitor<StringBuilder, String> {

	public FilterHandler(Map<String, Map<String, Object>> schemaMap) {
		translateSchemaMapToDictionary(schemaMap);
	}

	private static final Log LOGGER = Log.getLog(FilterHandler.class);

	private static final String SPACE = "%20";

	private static final String QUOTATION = "%22";

	private static final String EQUALS = "eq";

	private static final String CONTAINS = "co";

	private static final String STARTSWITH = "sw";

	private static final String ENDSWITH = "ew";

	private static final String GREATERTHAN = "gt";

	private static final String GREATEROREQ = "ge";

	private static final String LESSTHAN = "lt";

	private static final String LESSOREQ = "le";

	private static final String AND = "and";

	private static final String OR = "or";

	private static final String NOT = "not";

	private static Map<String, String> objectNameDictionary = CollectionUtil.newCaseInsensitiveMap();

	static {
		objectNameDictionary.put("userName", "userName");
		objectNameDictionary.put("name", "formatted");
		objectNameDictionary.put("displayName", "displayName");
		objectNameDictionary.put("nickName", "nickName");
		objectNameDictionary.put("profileUrl", "profileUrl");
		objectNameDictionary.put("title", "title");
		objectNameDictionary.put("userType", "userType");
		objectNameDictionary.put("id", "id");
		objectNameDictionary.put("externalId", "externalId");

		objectNameDictionary.put("name.formatted", "name.formatted");
		objectNameDictionary.put("name.familyName", "name.familyName");
		objectNameDictionary.put("name.givenName", "name.givenName");
		objectNameDictionary.put("name.middleName", "name.middleName");
		objectNameDictionary.put("name.honorificPrefix", "name.honorificPrefix");
		objectNameDictionary.put("name.honorificSuffix", "name.honorificSuffix");

		objectNameDictionary.put("preferredLanguage", "preferredLanguage");
		objectNameDictionary.put("locale", "locale");
		objectNameDictionary.put("timezone", "timezone");
		objectNameDictionary.put("active", "active");

		objectNameDictionary.put("emails.work.value", "emails.value");
		objectNameDictionary.put("emails.work.primary", "emails.primary");

		objectNameDictionary.put("emails.home.value", "emails.value");
		objectNameDictionary.put("emails.home.primary", "emails.primary");

		objectNameDictionary.put("emails.value", "emails.value");
		objectNameDictionary.put("emails.type", "emails.type");

		objectNameDictionary.put("emails.other.primary", "emails.primary");

		/// Group dictionary

		objectNameDictionary.put("id", "id");
		objectNameDictionary.put("externalId", "externalId");

		objectNameDictionary.put("displayName", "displayName");
	}
	//TODO modify for containsAllValues filter
	@Override
	public StringBuilder visitAndFilter(String p, AndFilter filter) {
		LOGGER.info("Processing request trought AND filter");

		StringBuilder completeQuery = new StringBuilder();
		int i =0;
		int size =filter.getFilters().size();
		boolean isFirst = true;

		for (Filter f : filter.getFilters()) {
			i++;

			if (isFirst) {
				if("default".equals(p) ){
					completeQuery.append(p);
					completeQuery.append("[");
					completeQuery.append(f.accept(this, p));
					isFirst = false;
					if (i==size){
						completeQuery.append("]");
						isFirst = false;
					}
				}else{

					completeQuery = f.accept(this, p);
					isFirst = false;
				}
			} else {
				completeQuery.append(SPACE);
				completeQuery.append(AND);
				completeQuery.append(SPACE);
				if (f instanceof OrFilter || f instanceof AndFilter) {
					completeQuery.append("(");
					completeQuery.append(f.accept(this, p).toString());
					completeQuery.append(")");
				} else {
					completeQuery.append(f.accept(this, p).toString());
				}
				if(i==size){
					if ("default".equals(p)){
						completeQuery.append("]");
					}
				}
			}

		}

		return completeQuery;
	}

	@Override
	public StringBuilder visitContainsFilter(String p, ContainsFilter filter) {
		LOGGER.info("Processing request trought CONTAINS filter");
		if (!filter.getName().isEmpty()) {

			StringBuilder preprocessedFilter = processArrayQ(filter, p);
			if (preprocessedFilter == null) {

				if (objectNameDictionary.containsKey(filter.getName())) {
					return BuildString(filter.getAttribute(), CONTAINS, objectNameDictionary.get(filter.getName()));
				} else {
					LOGGER.error("Usuported attribute name procesed by queuery filter: {0}", filter.getName());
					throw new InvalidAttributeValueException("Usuported attribute name procesed by queuery filter");
				}
			} else {
				return preprocessedFilter;
			}
		} else {

			LOGGER.error("Filter atribute key name EMPTY!");
			throw new InvalidAttributeValueException("No atribute key name provided");
		}
	}
	// TODO filter modified to support the salesforce scim endpoint functionality 
	@Override
	public StringBuilder visitContainsAllValuesFilter(String p, ContainsAllValuesFilter filter) {

		StringBuilder preprocessedFilter = null;//processArrayQ(filter, p);

		if(null != preprocessedFilter){
			return preprocessedFilter;
		}else{

			Collection<Filter> filterList= buildValueList(filter,"members");
			//TODO
			for(Filter f: filterList){

				if(f instanceof EqualsFilter){
					objectNameDictionary.put("members", "members");
					return f.accept(this,p);

				}

			}
			//

			objectNameDictionary.put("members", "members");
			AndFilter andFilterTest = (AndFilter) FilterBuilder.and(filterList);


			return andFilterTest.accept(this,p);

		}


	}

	@Override
	public StringBuilder visitEqualsFilter(String p, EqualsFilter filter) {
		LOGGER.info("Processing request trought EQUALS filter");

		if (!filter.getName().isEmpty()) {
			StringBuilder preprocessedFilter = processArrayQ(filter, p);
			if (preprocessedFilter == null) {

				if (objectNameDictionary.containsKey(filter.getName())) {

					return BuildString(filter.getAttribute(), EQUALS, objectNameDictionary.get(filter.getName()));

				} else {
					LOGGER.error("Usuported attribute name procesed by queuery filter: {0}", filter.getName());
					throw new InvalidAttributeValueException("Usuported attribute name procesed by queuery filter");
				}

			} else {
				return preprocessedFilter;
			}
		} else {

			LOGGER.error("Filter atribute key name EMPTY");
			throw new InvalidAttributeValueException("No atribute key name provided");
		}
	}

	@Override
	public StringBuilder visitExtendedFilter(String p, Filter filter) {
		LOGGER.error("Usuported filter", filter);
		throw new NoSuchMethodError("Usuported queuery filter");
	}

	@Override
	public StringBuilder visitGreaterThanFilter(String p, GreaterThanFilter filter) {
		LOGGER.info("Processing request trought GREATHERTHAN filter");

		if (!filter.getName().isEmpty()) {

			StringBuilder preprocessedFilter = processArrayQ(filter, p);
			if (preprocessedFilter == null) {

				if (objectNameDictionary.containsKey(filter.getName())) {

					return BuildString(filter.getAttribute(), GREATERTHAN, objectNameDictionary.get(filter.getName()));
				} else {
					LOGGER.error("Usuported attribute name procesed by queuery filter: {0}", filter.getName());
					throw new InvalidAttributeValueException("Usuported attribute name procesed by queuery filter");
				}
			} else {
				return preprocessedFilter;
			}
		} else {

			LOGGER.error("Filter atribute key name EMPTY");
			throw new InvalidAttributeValueException("No atribute key name provided");
		}
	}

	@Override
	public StringBuilder visitGreaterThanOrEqualFilter(String p, GreaterThanOrEqualFilter filter) {
		LOGGER.info("Processing request trought GREATHERTHANOREQUAL filter");
		if (!filter.getName().isEmpty()) {

			StringBuilder preprocessedFilter = processArrayQ(filter, p);
			if (preprocessedFilter == null) {

				if (objectNameDictionary.containsKey(filter.getName())) {

					return BuildString(filter.getAttribute(), GREATEROREQ, objectNameDictionary.get(filter.getName()));
				} else {
					LOGGER.error("Usuported attribute name procesed by queuery filter: {0}", filter.getName());
					throw new InvalidAttributeValueException("Usuported attribute name procesed by queuery filter");
				}
			} else {
				return preprocessedFilter;
			}
		} else {

			LOGGER.error("Filter atribute key name EMPTY");
			throw new InvalidAttributeValueException("No atribute key name provided");
		}
	}

	@Override
	public StringBuilder visitLessThanFilter(String p, LessThanFilter filter) {
		LOGGER.info("Processing request trought LESSTHAN filter");
		if (!filter.getName().isEmpty()) {

			StringBuilder preprocessedFilter = processArrayQ(filter, p);
			if (preprocessedFilter == null) {

				if (objectNameDictionary.containsKey(filter.getName())) {

					return BuildString(filter.getAttribute(), LESSTHAN, objectNameDictionary.get(filter.getName()));
				}
			} else {
				return preprocessedFilter;
			}
		}
		return null;
	}

	@Override
	public StringBuilder visitLessThanOrEqualFilter(String p, LessThanOrEqualFilter filter) {
		LOGGER.info("Processing request trought LESSTHANOREQUAL filter");
		if (!filter.getName().isEmpty()) {

			StringBuilder preprocessedFilter = processArrayQ(filter, p);
			if (preprocessedFilter == null) {

				if (objectNameDictionary.containsKey(filter.getName())) {

					return BuildString(filter.getAttribute(), LESSOREQ, objectNameDictionary.get(filter.getName()));
				} else {
					LOGGER.error("Usuported attribute name procesed by queuery filter: {0}", filter.getName());
					throw new InvalidAttributeValueException("Usuported attribute name procesed by queuery filter");
				}
			} else {
				return preprocessedFilter;
			}
		} else {

			LOGGER.error("Filter atribute key name EMPTY");
			throw new InvalidAttributeValueException("No atribute key name provided");
		}
	}

	@Override
	public StringBuilder visitNotFilter(String p, NotFilter filter) {
		LOGGER.info("Processing request trought NOT filter");
		StringBuilder completeQuery = new StringBuilder();

		completeQuery.append(NOT).append(SPACE).append(filter.getFilter().accept(this, p));

		return completeQuery;
	}

	@Override
	public StringBuilder visitOrFilter(String p, OrFilter filter) {
		LOGGER.info("Processing request trought OR filter");
		StringBuilder completeQuery = new StringBuilder();

		boolean isFirst = true;

		for (Filter f : filter.getFilters()) {

			if (isFirst) {
				completeQuery = f.accept(this, p);
				completeQuery.append(SPACE);
				completeQuery.append(OR);
				isFirst = false;

			} else {

				completeQuery.append(SPACE);
				if (f instanceof OrFilter || f instanceof AndFilter) {
					completeQuery.append("(");
					completeQuery.append(f.accept(this, p).toString());
					completeQuery.append(")");
				} else {

					completeQuery.append(f.accept(this, p).toString());
				}

			}

		}

		return completeQuery;

	}

	@Override
	public StringBuilder visitStartsWithFilter(String p, StartsWithFilter filter) {
		LOGGER.info("Processing request trought STARTSWITH filter");
		if (!filter.getName().isEmpty()) {

			StringBuilder preprocessedFilter = processArrayQ(filter, p);
			if (preprocessedFilter == null) {

				if (objectNameDictionary.containsKey(filter.getName())) {
					return BuildString(filter.getAttribute(), STARTSWITH, objectNameDictionary.get(filter.getName()));
				} else {
					LOGGER.error("Usuported attribute name procesed by queuery filter: {0}", filter.getName());
					throw new InvalidAttributeValueException("Usuported attribute name procesed by queuery filter");
				}
			} else {
				return preprocessedFilter;
			}
		} else {

			LOGGER.error("Filter atribute key name EMPTY");
			throw new InvalidAttributeValueException("No atribute key name provided");
		}
	}

	@Override
	public StringBuilder visitEndsWithFilter(String p, EndsWithFilter filter) {
		LOGGER.info("Processing request trought ENDSWITH filter");
		if (!filter.getName().isEmpty()) {

			StringBuilder preprocessedFilter = processArrayQ(filter, p);
			if (preprocessedFilter == null) {

				if (objectNameDictionary.containsKey(filter.getName())) {

					return BuildString(filter.getAttribute(), ENDSWITH, objectNameDictionary.get(filter.getName()));
				} else {
					LOGGER.error("Usuported attribute name procesed by queuery filter: {0}", filter.getName());
					throw new InvalidAttributeValueException("Usuported attribute name procesed by queuery filter");
				}
			} else {
				return preprocessedFilter;
			}
		} else {

			LOGGER.error("Filter atribute key name EMPTY while processing an ends with filter");
			throw new InvalidAttributeValueException(
					"No atribute key name provided while processing an ends with filter");
		}
	}

	private StringBuilder BuildString(Attribute atr, String operator, String name) {

		LOGGER.info("String builder processing filter: {0}", operator);

		StringBuilder resultString = new StringBuilder();
		if (atr == null) {

			LOGGER.error("Filter atribude value is EMPTY while building filter queuery, please provide atribute value ",
					atr);
			throw new InvalidAttributeValueException("No atribute value provided while building filter queuery");
		} else {
			resultString.append(name).append(SPACE).append(operator).append(SPACE).append(QUOTATION)
			.append(AttributeUtil.getAsStringValue(atr)).append(QUOTATION);
		}

		return resultString;
	}
	// TODO modifi to support contains all values filter 
	private StringBuilder processArrayQ(AttributeFilter filter, String p) {
		if (filter.getName().contains(".")) {

			String[] keyParts = filter.getName().split("\\."); // eq.
			// email.work.value
			if (keyParts.length == 3) {

				StringBuilder processedString = new StringBuilder();
				Collection<Filter> filterList =new ArrayList<Filter>();
				if (filter instanceof EqualsFilter) {

					StringBuilder keyName = new StringBuilder(keyParts[0]).append(".").append(keyParts[2]);
					EqualsFilter eqfilter = (EqualsFilter) FilterBuilder.equalTo(AttributeBuilder
							.build(keyName.toString(), AttributeUtil.getAsStringValue(filter.getAttribute())));


					StringBuilder type = new StringBuilder(keyParts[0]).append(".").append("type");
					objectNameDictionary.put(type.toString(), type.toString());

					EqualsFilter eq = (EqualsFilter) FilterBuilder
							.equalTo(AttributeBuilder.build(type.toString(), keyParts[1]));
					filterList.add(eqfilter);
					filterList.add(eq);

					objectNameDictionary.put(type.toString(), type.toString());
					objectNameDictionary.put(keyName.toString(), keyName.toString());
				}
				else if (filter instanceof ContainsAllValuesFilter){
					p =keyParts[0];

					StringBuilder keyName = new StringBuilder(keyParts[0]).append(".").append(keyParts[2]);
					filterList= buildValueList((ContainsAllValuesFilter)filter,keyParts[2]);

					StringBuilder type = new StringBuilder(keyParts[0]).append(".").append("type");


					EqualsFilter eq = (EqualsFilter) FilterBuilder
							.equalTo(AttributeBuilder.build("type", keyParts[1]));
					filterList.add(eq);

					objectNameDictionary.put("type", "type");
					objectNameDictionary.put(keyParts[2], keyParts[2]);
				}else {
					return null;
				}
				AndFilter and = (AndFilter) FilterBuilder.and(filterList);

				processedString = and.accept(this, p);
				return processedString;
			}
			return null;
		}
		return null;
	}

	private void translateSchemaMapToDictionary(Map<String, Map<String, Object>> schemaMap) {

		if (schemaMap != null) {
			for (String attributeNameKey : schemaMap.keySet()) {
				String[] attributeNameKeyParts = attributeNameKey.split("\\."); // eg.
				// emails.work.value
				if (attributeNameKeyParts.length == 3) {
					StringBuilder buildAttributeDictionaryValue = new StringBuilder(attributeNameKeyParts[0])
							.append(".").append(attributeNameKeyParts[2]);
					objectNameDictionary.put(attributeNameKey, buildAttributeDictionaryValue.toString());

				} else {

					objectNameDictionary.put(attributeNameKey, attributeNameKey);
				}

			}
			LOGGER.info("The filter dictionary which was build from the provided schema: {0}", objectNameDictionary);

		} else {
			LOGGER.warn("No schema provided, switching to default filter dictionary ");
		}
	}

	private Collection<Filter> buildValueList(ContainsAllValuesFilter filter, String attributeName){

		List<Object> valueList = filter.getAttribute().getValue();
		Collection<Filter> filterList= new ArrayList<Filter>();

		for(Object o:valueList){		
			if(attributeName.isEmpty()){

				ContainsFilter containsSingleAtribute = (ContainsFilter) FilterBuilder
						.contains(AttributeBuilder.build(filter.getName(), o));
				filterList.add(containsSingleAtribute);
			}else{
				// ContainsFilter containsSingleAtribute = (ContainsFilter) FilterBuilder
				//			.contains(AttributeBuilder.build(attributeName, o));

				//TODO
				EqualsFilter containsSingleAtribute = (EqualsFilter) FilterBuilder.equalTo(AttributeBuilder.build(attributeName,o));
				filterList.add(containsSingleAtribute); 

			}

		}

		return filterList;
	}

}
