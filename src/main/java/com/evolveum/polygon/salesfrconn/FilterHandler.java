package com.evolveum.polygon.salesfrconn;

import java.util.Map;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.InvalidAttributeValueException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.filter.AndFilter;
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



// Missing filterVisitor methods/filters from scim spec: not equal, present
public class FilterHandler implements FilterVisitor<StringBuilder, Void> {
	
	private static final Log LOGGER = Log.getLog(FilterHandler.class);
	
	private static final String SPACE = "%20";
	
	private static final String QUOTATION = "%22";
	
	private static final String EQUALS = "eq";
	
	//	private static final String NOTEQUAL ="ne";
	
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
	
	private static Map<String, String> nameDictionary = CollectionUtil.newCaseInsensitiveMap();
		static {
			nameDictionary.put("userName","userName");
			nameDictionary.put("name","name");
			nameDictionary.put("displayName","displayName");
			nameDictionary.put("nickName","nickName");
			nameDictionary.put("profileUrl","profileUrl");
			nameDictionary.put("title","title");
			nameDictionary.put("userType","userType");
			nameDictionary.put("preferredLanguage","preferredLanguage");
			nameDictionary.put("locale","locale");
			nameDictionary.put("timezone","timezone");
			nameDictionary.put("active","active");
			nameDictionary.put("password","password");
			//nameDictionary.put("","");
		}
	
	@Override
	public StringBuilder visitAndFilter(Void p, AndFilter filter) {
		StringBuilder finalQuery = new StringBuilder();
		
		boolean isFirst = true;
		
		for (Filter f: filter.getFilters()){
			
			
			if (isFirst){
				finalQuery=f.accept(this,null);
				finalQuery.append(SPACE);
				finalQuery.append(AND);
				isFirst=false;
				
			}else {
				finalQuery.append(SPACE);
				finalQuery.append(f.accept(this,null).toString());
			}
			
		}
		
		return finalQuery;
	}

	@Override
	public StringBuilder visitContainsFilter(Void p, ContainsFilter filter) {
		if (!filter.getName().isEmpty()){
		if (nameDictionary.get(filter.getName())!=null){
			
			return BuildString(filter.getAttribute(),CONTAINS , filter.getName());
		}else{
			LOGGER.error("Usuported attribute name",filter.getName());
			throw new InvalidAttributeValueException("Usuported attribute name");
			}
			}else{

			LOGGER.error("Filter atribute key name EMPTY");
			throw new InvalidAttributeValueException("No atribute key name provided");
			}
	}

	@Override
	public StringBuilder visitContainsAllValuesFilter(Void p, ContainsAllValuesFilter filter) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StringBuilder visitEqualsFilter(Void p, EqualsFilter filter) {
		if (!filter.getName().isEmpty()){
		
		if(nameDictionary.get(filter.getName())!=null){
			
			return BuildString(filter.getAttribute(),EQUALS,filter.getName());
			
		}else{
			LOGGER.error("Usuported attribute name",filter.getName());
			throw new InvalidAttributeValueException("Usuported attribute name");
			}
			}else{

			LOGGER.error("Filter atribute key name EMPTY");
			throw new InvalidAttributeValueException("No atribute key name provided");
			}
	}
	
	

	@Override
	public StringBuilder visitExtendedFilter(Void p, Filter filter) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StringBuilder visitGreaterThanFilter(Void p, GreaterThanFilter filter) {
		
		if (!filter.getName().isEmpty()){
	 if(nameDictionary.get(filter.getName())!=null){
		 
		 return BuildString(filter.getAttribute(), GREATERTHAN, filter.getName());
	 }else{
		LOGGER.error("Usuported attribute name",filter.getName());
		throw new InvalidAttributeValueException("Usuported attribute name");
		}
		}else{

		LOGGER.error("Filter atribute key name EMPTY");
		throw new InvalidAttributeValueException("No atribute key name provided");
		}
	}

	@Override
	public StringBuilder visitGreaterThanOrEqualFilter(Void p, GreaterThanOrEqualFilter filter) {
		if (!filter.getName().isEmpty()){
		 if(nameDictionary.get(filter.getName())!=null){
			 
			 return BuildString(filter.getAttribute(), GREATEROREQ, filter.getName());
		 }else{
				LOGGER.error("Usuported attribute name",filter.getName());
				throw new InvalidAttributeValueException("Usuported attribute name");
				}
				}else{

				LOGGER.error("Filter atribute key name EMPTY");
				throw new InvalidAttributeValueException("No atribute key name provided");
				}
	}

	@Override
	public StringBuilder visitLessThanFilter(Void p, LessThanFilter filter) {
		if (!filter.getName().isEmpty()){
		 if(nameDictionary.get(filter.getName())!=null){
			 
			 return BuildString(filter.getAttribute(), LESSTHAN, filter.getName());
		 }
		}
		return null;
	}

	@Override
	public StringBuilder visitLessThanOrEqualFilter(Void p, LessThanOrEqualFilter filter) {
		
		if (!filter.getName().isEmpty()){
		 if(nameDictionary.get(filter.getName())!=null){
			 
			 return BuildString(filter.getAttribute(), LESSOREQ, filter.getName());
		 }else{
				LOGGER.error("Usuported attribute name",filter.getName());
				throw new InvalidAttributeValueException("Usuported attribute name");
				}
				}else{

				LOGGER.error("Filter atribute key name EMPTY");
				throw new InvalidAttributeValueException("No atribute key name provided");
				}
	}

	@Override
	public StringBuilder visitNotFilter(Void p, NotFilter filter) {
		StringBuilder finalQuery = new StringBuilder();
		
		 finalQuery.append(NOT).append(SPACE).append(filter.getFilter().accept(this, null));

		
		return finalQuery;
	}

	@Override
	public StringBuilder visitOrFilter(Void p, OrFilter filter) {
	StringBuilder finalQuery = new StringBuilder();
		
		boolean isFirst = true;
		
		for (Filter f: filter.getFilters()){
			
			if (isFirst){
				finalQuery=f.accept(this,null);
				finalQuery.append(SPACE);
				finalQuery.append(OR);
				isFirst=false;
				
			}else {
				
				finalQuery.append(SPACE);
				finalQuery.append(f.accept(this,null).toString());
			}
			
		}
		
		return finalQuery;
	
	}

	@Override
	public StringBuilder visitStartsWithFilter(Void p, StartsWithFilter filter) {
		if (!filter.getName().isEmpty()){
		if(nameDictionary.get(filter.getName())!=null){
			return BuildString(filter.getAttribute(), STARTSWITH, filter.getName());
		}else{
			LOGGER.error("Usuported attribute name",filter.getName());
			throw new InvalidAttributeValueException("Usuported attribute name");
			}
			}else{

			LOGGER.error("Filter atribute key name EMPTY");
			throw new InvalidAttributeValueException("No atribute key name provided");
			}
	}

	@Override
	public StringBuilder visitEndsWithFilter(Void p, EndsWithFilter filter) {
		if (!filter.getName().isEmpty()){
		if(nameDictionary.get(filter.getName())!=null){
			
			return BuildString(filter.getAttribute(), ENDSWITH, filter.getName());
		}else{
			LOGGER.error("Usuported attribute name",filter.getName());
			throw new InvalidAttributeValueException("Usuported attribute name");
			}
			}else{

			LOGGER.error("Filter atribute key name EMPTY");
			throw new InvalidAttributeValueException("No atribute key name provided");
			}
	}
	
	public StringBuilder BuildString(Attribute atr, String operator, String name){
		
		LOGGER.info("String builder processing filter: {0}", operator);
		
		StringBuilder resultString = new StringBuilder();
		if(atr.equals(null)){
			
			LOGGER.error("Filter atribude value is EMPTY, please provide atribute value", atr );
		}else {
			resultString.append(name).append(SPACE).append(operator).append(SPACE).append(QUOTATION).append(AttributeUtil.getAsStringValue(atr)).append(QUOTATION);
		
		}
		
		return resultString;
	}

}
