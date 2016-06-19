package com.evolveum.polygon.scim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.ObjectClassInfoBuilder;
import org.json.JSONArray;
import org.json.JSONObject;

public class GenericSchemaObjectBuilder {

	// TODO SOME attributes not defined in the Schemas/ endpoint and present in resources (salesForce)
	
	private static final Log LOGGER = Log.getLog(ScimConnector.class);
	
	public ObjectClassInfo  buildSchema (Map<String, Map<String, Object>> attributeMap, String objectTypeName ){
		ObjectClassInfoBuilder builder = new ObjectClassInfoBuilder();
		
		builder.addAttributeInfo(Name.INFO);
		
		
		for(String key: attributeMap.keySet()){
			
			AttributeInfoBuilder infoBuilder = new AttributeInfoBuilder(key.intern());
			
			Map<String, Object> schemaAttributeMap = new HashMap<String, Object>();
			schemaAttributeMap = attributeMap.get(key);
				for(String mapAttributeKey: schemaAttributeMap.keySet()){
					
						if(mapAttributeKey.intern() == "subAttributes"){
							
							 infoBuilder = new AttributeInfoBuilder(key.intern());
							 JSONArray jsonArray = new JSONArray();
							 jsonArray= ((JSONArray)schemaAttributeMap.get(mapAttributeKey));
							 for(int i=0; i<jsonArray.length();i++){
								 JSONObject attribute = new JSONObject();
								 attribute = jsonArray.getJSONObject(i);
							 }
						break;
					}else {
						keyChecker(infoBuilder, schemaAttributeMap, mapAttributeKey);
					}
						
				}
			builder.addAttributeInfo(infoBuilder.build());
				
		}
		
	if(objectTypeName.intern() =="/Users"){
		builder.setType(ObjectClass.ACCOUNT_NAME);
		
	}else if (objectTypeName.intern() =="/Groups"){
		builder.setType(ObjectClass.GROUP_NAME);
	}
	else {
		ObjectClass objectClass = new ObjectClass(objectTypeName);
		builder.setType(objectClass.getObjectClassValue());
	}
	LOGGER.error("Schema: {0}",builder.build());
		return builder.build();
	}
	
	private AttributeInfoBuilder keyChecker(AttributeInfoBuilder infoBuilder, Map<String, Object> schemaAttributeMap, String mapAttributeKey){

		
			if(mapAttributeKey.intern() == "readOnly"){
				infoBuilder.setUpdateable((!(Boolean)schemaAttributeMap.get(mapAttributeKey)));
				infoBuilder.setCreateable((!(Boolean)schemaAttributeMap.get(mapAttributeKey)));
			}
			else if(mapAttributeKey.intern() == "type"){
				
				/*
				 * String.class
				 * long.class
				 * Long.class
				 * char.class
				 * Character.class
				 * double.class
				 * Double.class
				 * float.class
				 * Float.class
				 * int.class
				 * Integer.class
				 * boolean.class
				 * Boolean.class
				 * byte.class
				 * Byte.class
				 * byte[].class
				 * BigDecimal.class
				 * BigInteger.class
				 * Map.class
				 * */
				// TODO question
				// type "complex" and "datetime" not supported
				
				if(schemaAttributeMap.get(mapAttributeKey).toString().intern() == "string"){
					
					infoBuilder.setType(String.class);
				}else if ( schemaAttributeMap.get(mapAttributeKey).toString().intern() == "boolean"){
				
					infoBuilder.setType(Boolean.class);
				} // TODO find how to work with "complex" type
				
			}
			else if(mapAttributeKey.intern() == "required"){
				infoBuilder.setRequired(((Boolean)schemaAttributeMap.get(mapAttributeKey)));
			}
			else if(mapAttributeKey.intern() == "multiValued"){
				infoBuilder.setMultiValued(((Boolean)schemaAttributeMap.get(mapAttributeKey)));
			}
			
		
		return infoBuilder;
	}
}
