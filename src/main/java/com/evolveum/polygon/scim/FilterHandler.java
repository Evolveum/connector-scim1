package com.evolveum.polygon.scim;

import java.util.HashMap;
import java.util.Map;


import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.InvalidAttributeValueException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ObjectClass;
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

public class FilterHandler implements FilterVisitor<StringBuilder, ObjectClass> {

	private static final Log LOGGER = Log.getLog(FilterHandler.class);

	private static final String SPACE = "%20";

	private static final String QUOTATION = "%22";

	private static final String EQUALS = "eq";

	private static final String CONTAINS= "co";

	private static final String STARTSWITH ="sw";

	private static final String ENDSWITH ="ew";

	private static final String GREATERTHAN ="gt";

	private static final String GREATEROREQ ="ge";

	private static final String LESSTHAN ="lt";

	private static final String LESSOREQ ="le";

	private static final String AND = "and";

	private static final String OR = "or";

	private static final String NOT ="not";

	private static Map<String, String> objectNameDictionary = CollectionUtil.newCaseInsensitiveMap();


	private static Map< String, HashMap<String, String>> arrayNameDictionary =  new HashMap<String, HashMap<String, String>>() ;

	private static HashMap<String, String> aObjectDictionaryGroup =  new HashMap<String, String>() ;
	static {
		objectNameDictionary.put("userName","userName");
		objectNameDictionary.put("name","formatted");
		objectNameDictionary.put("displayName","displayName");
		objectNameDictionary.put("nickName","nickName");
		objectNameDictionary.put("profileUrl","profileUrl");
		objectNameDictionary.put("title","title");
		objectNameDictionary.put("userType","userType");
		objectNameDictionary.put("id","id");
		objectNameDictionary.put("externalId","externalId");

		
		objectNameDictionary.put("name.formatted", "name.formatted");
		objectNameDictionary.put("name.familyName", "name.familyName");
		objectNameDictionary.put("name.givenName", "name.givenName");
		objectNameDictionary.put("name.middleName", "name.middleName");
		objectNameDictionary.put("name.honorificPrefix", "name.honorificPrefix");
		objectNameDictionary.put("name.honorificSuffix", "name.honorificSuffix");

		objectNameDictionary.put("preferredLanguage","preferredLanguage");
		objectNameDictionary.put("locale","locale");
		objectNameDictionary.put("timezone","timezone");
		objectNameDictionary.put("active","active");

		
		objectNameDictionary.put("emails.work.value","emails.value");
		objectNameDictionary.put("emails.work.primary","emails.primary");
		
		objectNameDictionary.put("emails.home.value","emails.value");
		objectNameDictionary.put("emails.home.primary","emails.primary");
		
		objectNameDictionary.put("emails.value","emails.value");
		objectNameDictionary.put("emails.type","emails.type");
		
		
		objectNameDictionary.put("emails.other.primary","emails.primary");
		 
		///Group dictionary

		objectNameDictionary.put("id", "id");
		objectNameDictionary.put("externalId", "externalId");

		objectNameDictionary.put("displayName", "displayName");
		//nameDictionaryGroup.put("members", "members");

		// TODO define the rest of array dictionaries for complex attributes
		// array dictionaries and values

		arrayNameDictionary.put("emails", aObjectDictionaryGroup);
	}



	@Override
	public StringBuilder visitAndFilter(ObjectClass p, AndFilter filter) {
		LOGGER.info("Processing request trought and filter");

		StringBuilder completeQuery = new StringBuilder();

		boolean isFirst = true;

		for (Filter f: filter.getFilters()){


			if (isFirst){
				completeQuery=f.accept(this,p);
				completeQuery.append(SPACE);
				completeQuery.append(AND);
				isFirst=false;

			}else {

				completeQuery.append(SPACE);
				if (f instanceof OrFilter || f instanceof AndFilter){
					completeQuery.append("(");
					completeQuery.append(f.accept(this,p).toString());
					completeQuery.append(")");
				}else {
					completeQuery.append(f.accept(this,p).toString());
				}
			}

		}

		return completeQuery;
	}

	@Override
	public StringBuilder visitContainsFilter(ObjectClass p, ContainsFilter filter) {
		if (!filter.getName().isEmpty()){

			//Map<String, String> nameDictionary = setDictionary(p, filter);

			if (objectNameDictionary.containsKey(filter.getName())){
				return BuildString(filter.getAttribute(),CONTAINS , objectNameDictionary.get(filter.getName()));
			}else{
				LOGGER.error("Usuported attribute name procesed by queuery filter: {0}",filter.getName());
				throw new InvalidAttributeValueException("Usuported attribute name procesed by queuery filter");
			}
		}else{

			LOGGER.error("Filter atribute key name EMPTY!");
			throw new InvalidAttributeValueException("No atribute key name provided");
		}
	}

	@Override
	public StringBuilder visitContainsAllValuesFilter(ObjectClass p, ContainsAllValuesFilter filter) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StringBuilder visitEqualsFilter(ObjectClass p, EqualsFilter filter) {
		if (!filter.getName().isEmpty()){
			StringBuilder preprocessedFilter = processArrayQ(filter, p);
			if (preprocessedFilter == null){

			if (objectNameDictionary.containsKey(filter.getName())){

				return BuildString(filter.getAttribute(),EQUALS,objectNameDictionary.get(filter.getName()));

			}else{
				LOGGER.error("Usuported attribute name procesed by queuery filter: {0}",filter.getName());
				throw new InvalidAttributeValueException("Usuported attribute name procesed by queuery filter");
			}
			
		}else 
		{	
			return preprocessedFilter;
		}
		}else{

			LOGGER.error("Filter atribute key name EMPTY");
			throw new InvalidAttributeValueException("No atribute key name provided");
		}
	}



	@Override
	public StringBuilder visitExtendedFilter(ObjectClass p, Filter filter) {
		LOGGER.error("Usuported filter",filter);
		throw new NoSuchMethodError("Usuported queuery filter");
	}

	@Override
	public StringBuilder visitGreaterThanFilter(ObjectClass p, GreaterThanFilter filter) {

		if (!filter.getName().isEmpty()){

			//Map<String, String> objectNameDictionary = setDictionary(p, filter);

			if (objectNameDictionary.containsKey(filter.getName())){

				return BuildString(filter.getAttribute(), GREATERTHAN, objectNameDictionary.get(filter.getName()));
			}else{
				LOGGER.error("Usuported attribute name procesed by queuery filter: {0}",filter.getName());
				throw new InvalidAttributeValueException("Usuported attribute name procesed by queuery filter");
			}
		}else{

			LOGGER.error("Filter atribute key name EMPTY");
			throw new InvalidAttributeValueException("No atribute key name provided");
		}
	}

	@Override
	public StringBuilder visitGreaterThanOrEqualFilter(ObjectClass p, GreaterThanOrEqualFilter filter) {
		if (!filter.getName().isEmpty()){

			//Map<String, String> objectNameDictionary = setDictionary(p, filter);

			if (objectNameDictionary.containsKey(filter.getName())){

				return BuildString(filter.getAttribute(), GREATEROREQ, objectNameDictionary.get(filter.getName()));
			}else{
				LOGGER.error("Usuported attribute name procesed by queuery filter: {0}",filter.getName());
				throw new InvalidAttributeValueException("Usuported attribute name procesed by queuery filter");
			}
		}else{

			LOGGER.error("Filter atribute key name EMPTY");
			throw new InvalidAttributeValueException("No atribute key name provided");
		}
	}

	@Override
	public StringBuilder visitLessThanFilter(ObjectClass p, LessThanFilter filter) {
		if (!filter.getName().isEmpty()){

			//Map<String, String> objectNameDictionary = setDictionary(p, filter);

			if (objectNameDictionary.containsKey(filter.getName())){

				return BuildString(filter.getAttribute(), LESSTHAN, objectNameDictionary.get(filter.getName()));
			}
		}
		return null;
	}

	@Override
	public StringBuilder visitLessThanOrEqualFilter(ObjectClass p, LessThanOrEqualFilter filter) {

		if (!filter.getName().isEmpty()){

			//Map<String, String> objectNameDictionary = setDictionary(p, filter);

			if (objectNameDictionary.containsKey(filter.getName())){

				return BuildString(filter.getAttribute(), LESSOREQ, objectNameDictionary.get(filter.getName()));
			}else{
				LOGGER.error("Usuported attribute name procesed by queuery filter: {0}",filter.getName());
				throw new InvalidAttributeValueException("Usuported attribute name procesed by queuery filter");
			}
		}else{

			LOGGER.error("Filter atribute key name EMPTY");
			throw new InvalidAttributeValueException("No atribute key name provided");
		}
	}

	@Override
	public StringBuilder visitNotFilter(ObjectClass p, NotFilter filter) {
		StringBuilder completeQuery = new StringBuilder();

		completeQuery.append(NOT).append(SPACE).append(filter.getFilter().accept(this, p));


		return completeQuery;
	}

	@Override
	public StringBuilder visitOrFilter(ObjectClass p, OrFilter filter) {
		StringBuilder completeQuery = new StringBuilder();

		boolean isFirst = true;

		for (Filter f: filter.getFilters()){

			if (isFirst){
				completeQuery=f.accept(this,p);
				completeQuery.append(SPACE);
				completeQuery.append(OR);
				isFirst=false;

			}else {

				completeQuery.append(SPACE);
				if (f instanceof OrFilter || f instanceof AndFilter){
					completeQuery.append("(");
					completeQuery.append(f.accept(this,p).toString());
					completeQuery.append(")");
				}else {

					completeQuery.append(f.accept(this,p).toString());
				}

			}

		}

		return completeQuery;

	}

	@Override
	public StringBuilder visitStartsWithFilter(ObjectClass p, StartsWithFilter filter) {
		if (!filter.getName().isEmpty()){

			//Map<String, String> objectNameDictionary = setDictionary(p, filter);

			if (objectNameDictionary.containsKey(filter.getName())){
				return BuildString(filter.getAttribute(), STARTSWITH, objectNameDictionary.get(filter.getName()));
			}else{
				LOGGER.error("Usuported attribute name procesed by queuery filter: {0}",filter.getName());
				throw new InvalidAttributeValueException("Usuported attribute name procesed by queuery filter");
			}
		}else{

			LOGGER.error("Filter atribute key name EMPTY");
			throw new InvalidAttributeValueException("No atribute key name provided");
		}
	}

	@Override
	public StringBuilder visitEndsWithFilter(ObjectClass p, EndsWithFilter filter) {
		if (!filter.getName().isEmpty()){

			//Map<String, String> objectNameDictionary = setDictionary(p, filter);


			if (objectNameDictionary.containsKey(filter.getName())){

				return BuildString(filter.getAttribute(), ENDSWITH, objectNameDictionary.get(filter.getName()));
			}else{
				LOGGER.error("Usuported attribute name procesed by queuery filter: {0}",filter.getName());
				throw new InvalidAttributeValueException("Usuported attribute name procesed by queuery filter");
			}
		}else{

			LOGGER.error("Filter atribute key name EMPTY while processing an ends with filter");
			throw new InvalidAttributeValueException("No atribute key name provided while processing an ends with filter");
		}
	}

	private StringBuilder BuildString(Attribute atr, String operator, String name){

		LOGGER.info("String builder processing filter: {0}", operator);

		StringBuilder resultString = new StringBuilder();
		if(atr == null ){

			LOGGER.error("Filter atribude value is EMPTY while building filter queuery, please provide atribute value ", atr );
			throw new InvalidAttributeValueException("No atribute value provided while building filter queuery");
		}else {
			resultString.append(name).append(SPACE).append(operator).append(SPACE).append(QUOTATION).append(AttributeUtil.getAsStringValue(atr)).append(QUOTATION);
		}

		return resultString;
	}

	private StringBuilder processArrayQ(AttributeFilter filter, ObjectClass p){
		
		//TODO question // does this follow the scim specs ? email.tye eq work and email.value eq someone@someplace.com
		
		StringBuilder processedString = new StringBuilder();
		
		if (filter.getName().contains(".")) {
			
					String[] keyParts = filter.getName().split("\\.");
					if (keyParts.length == 3) {
					
						AttributeFilter variableFilter = null;
						
					if(filter instanceof EqualsFilter){

						StringBuilder keyName = new StringBuilder(keyParts[0]).append(".").append(keyParts[2]);
						EqualsFilter eqfilter = (EqualsFilter)FilterBuilder.equalTo(AttributeBuilder.build(keyName.toString(),AttributeUtil.getAsStringValue(filter.getAttribute())));
						variableFilter =eqfilter;
					}
						StringBuilder type = new StringBuilder(keyParts[0]).append(".").append("type");
						
						EqualsFilter eq = (EqualsFilter)FilterBuilder.equalTo(AttributeBuilder.build(type.toString(),keyParts[1]));
						AndFilter and = (AndFilter) FilterBuilder.and(eq, variableFilter);
						
						System.out.println(and);
						processedString = and.accept(this, p);
						return processedString;
					} 
					return null;
					}
		return null;
	}

}
