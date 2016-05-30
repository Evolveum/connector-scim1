package com.evolveum.polygon.salesfrconn;

import java.util.HashMap;
import java.util.Map;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.InvalidAttributeValueException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.filter.AndFilter;
import org.identityconnectors.framework.common.objects.filter.AttributeFilter;
import org.identityconnectors.framework.common.objects.filter.ContainsAllValuesFilter;
import org.identityconnectors.framework.common.objects.filter.ContainsFilter;
import org.identityconnectors.framework.common.objects.filter.EndsWithFilter;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.identityconnectors.framework.common.objects.filter.Filter;
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

	private static Map<String, String> objectNameDictionaryUser = CollectionUtil.newCaseInsensitiveMap();

	private static Map<String, String> objectNameDictionaryGroup = CollectionUtil.newCaseInsensitiveMap();

	private static Map< String, HashMap<String, String>> arrayNameDictionary =  new HashMap<String, HashMap<String, String>>() ;

	private static HashMap<String, String> aObjectDictionaryGroup =  new HashMap<String, String>() ;
	static {
		objectNameDictionaryUser.put("userName","userName");
		objectNameDictionaryUser.put("name","formatted");
		objectNameDictionaryUser.put("displayName","displayName");
		objectNameDictionaryUser.put("nickName","nickName");
		objectNameDictionaryUser.put("profileUrl","profileUrl");
		objectNameDictionaryUser.put("title","title");
		objectNameDictionaryUser.put("userType","userType");
		objectNameDictionaryUser.put("id","id");
		objectNameDictionaryUser.put("externalId","externalId");


		objectNameDictionaryUser.put("preferredLanguage","preferredLanguage");
		objectNameDictionaryUser.put("locale","locale");
		objectNameDictionaryUser.put("timezone","timezone");
		objectNameDictionaryUser.put("active","active");

		objectNameDictionaryUser.put("email","email");


		///Group dictionary

		objectNameDictionaryGroup.put("id", "id");
		objectNameDictionaryGroup.put("externalId", "externalId");

		objectNameDictionaryGroup.put("displayName", "displayName");
		//nameDictionaryGroup.put("members", "members");

		// TODO define the rest of array dictionaries for complex attributes
		// array dictionaries and values
		aObjectDictionaryGroup.put("type", "email.type");
		aObjectDictionaryGroup.put("value", "email.value");

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

			Map<String, String> nameDictionary = setDictionary(p, filter);

			if (nameDictionary.containsKey(filter.getName())){
				return BuildString(filter.getAttribute(),CONTAINS , nameDictionary.get(filter.getName()));
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

			Map<String, String> nameDictionary = setDictionary(p, filter);

			if (nameDictionary.containsKey(filter.getName())){

				return BuildString(filter.getAttribute(),EQUALS,nameDictionary.get(filter.getName()));

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
	public StringBuilder visitExtendedFilter(ObjectClass p, Filter filter) {
		LOGGER.error("Usuported filter",filter);
		throw new NoSuchMethodError("Usuported queuery filter");
	}

	@Override
	public StringBuilder visitGreaterThanFilter(ObjectClass p, GreaterThanFilter filter) {

		if (!filter.getName().isEmpty()){

			Map<String, String> nameDictionary = setDictionary(p, filter);

			if (nameDictionary.containsKey(filter.getName())){

				return BuildString(filter.getAttribute(), GREATERTHAN, nameDictionary.get(filter.getName()));
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

			Map<String, String> nameDictionary = setDictionary(p, filter);

			if (nameDictionary.containsKey(filter.getName())){

				return BuildString(filter.getAttribute(), GREATEROREQ, nameDictionary.get(filter.getName()));
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

			Map<String, String> nameDictionary = setDictionary(p, filter);

			if (nameDictionary.containsKey(filter.getName())){

				return BuildString(filter.getAttribute(), LESSTHAN, nameDictionary.get(filter.getName()));
			}
		}
		return null;
	}

	@Override
	public StringBuilder visitLessThanOrEqualFilter(ObjectClass p, LessThanOrEqualFilter filter) {

		if (!filter.getName().isEmpty()){

			Map<String, String> nameDictionary = setDictionary(p, filter);

			if (nameDictionary.containsKey(filter.getName())){

				return BuildString(filter.getAttribute(), LESSOREQ, nameDictionary.get(filter.getName()));
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

			Map<String, String> nameDictionary = setDictionary(p, filter);

			if (nameDictionary.containsKey(filter.getName())){
				return BuildString(filter.getAttribute(), STARTSWITH, nameDictionary.get(filter.getName()));
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

			Map<String, String> nameDictionary = setDictionary(p, filter);


			if (nameDictionary.containsKey(filter.getName())){

				return BuildString(filter.getAttribute(), ENDSWITH, nameDictionary.get(filter.getName()));
			}else{
				LOGGER.error("Usuported attribute name procesed by queuery filter: {0}",filter.getName());
				throw new InvalidAttributeValueException("Usuported attribute name procesed by queuery filter");
			}
		}else{

			LOGGER.error("Filter atribute key name EMPTY");
			throw new InvalidAttributeValueException("No atribute key name provided");
		}
	}

	private StringBuilder BuildString(Attribute atr, String operator, String name){

		LOGGER.info("String builder processing filter: {0}", operator);

		StringBuilder resultString = new StringBuilder();
		if(atr == null ){

			LOGGER.error("Filter atribude value is EMPTY, please provide atribute value ", atr );
		}else {
			resultString.append(name).append(SPACE).append(operator).append(SPACE).append(QUOTATION).append(AttributeUtil.getAsStringValue(atr)).append(QUOTATION);
		}

		return resultString;
	}

	private Map<String,String> setDictionary(ObjectClass objectClass, AttributeFilter filter){

		Map<String, String> nameDictionary = null;

		//TODO question, do we need to check if the filter request is for an account or a group ?

		//if(AnameDictionaryGroup.containsKey(filter.getName())){

		//	nameDictionary = AnameDictionaryGroup.get(filter.getName());
		//}else // TODO remove if we dont

		if (ObjectClass.ACCOUNT.equals(objectClass)){

			nameDictionary=objectNameDictionaryUser;
		}else if(ObjectClass.GROUP.equals(objectClass)) {
			nameDictionary=objectNameDictionaryUser;

		}
		return nameDictionary;
	}

}
