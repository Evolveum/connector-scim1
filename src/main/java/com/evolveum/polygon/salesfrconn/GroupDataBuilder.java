package com.evolveum.polygon.salesfrconn;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.json.JSONArray;
import org.json.JSONObject;

public class GroupDataBuilder {
	
	
	private static Map<String, String> nameDictionaryUser = CollectionUtil.newCaseInsensitiveMap();
	private static final Log LOGGER = Log.getLog(UserDataBuilder.class);
	
	
	/// problem with adding multiple members into group, no way to identify the relationship between an object and its attributes
	
	static {
	nameDictionaryUser.put("displayName","displayName");
	

	nameDictionaryUser.put("members..value","value");
	nameDictionaryUser.put("members..display","display");
	}
	
	public JSONObject setUserObject(Set<Attribute> attributes){

		JSONObject userObj = new JSONObject();

		Set<Attribute> multilaierAttribute = new HashSet<Attribute>();
		
		for(Attribute at: attributes){

			String attributeName = at.getName();

		if(nameDictionaryUser.containsKey(attributeName)){
			if(attributeName.contains(".")){
				
			

					 multilaierAttribute.add(at);
			}else{
				
				userObj.put(attributeName, AttributeUtil.getSingleValue(at));
			}
			
		}else{LOGGER.error("Attribute name not defined in dictionary {0}", attributeName);}
		
		}
		
		if(multilaierAttribute != null){
			
			
			buildLayeredAtrribute(multilaierAttribute, userObj);
			}
		return userObj;

	}
	
	private JSONObject buildLayeredAtrribute(Set<Attribute> attr, JSONObject json){

		String name="";
		ArrayList<String> checkedNames= new ArrayList<String>();
		for(Attribute i: attr){
			
			String attributeName = i.getName();
			String[] keyParts = attributeName.split("\\.");
				
				if(checkedNames.contains(keyParts[0])){

				}else{
					Set<Attribute> innerLayer = new HashSet<Attribute>();
					name=keyParts[0].intern();
					checkedNames.add(name);
					for(Attribute j: attr){
						
						String innerName = j.getName();
						String[] innerKeyParts = innerName.split("\\.");
							
						if(innerKeyParts[0].equals(name)){
									innerLayer.add(j); 
							}
					}

					String typeName = "";
					JSONArray jArray = new JSONArray();	
					
					ArrayList<String> checkedTypeNames= new ArrayList<String>();
					for(Attribute k: innerLayer){
					
						String secondName = k.getName();
						String[] secondKeyPart = secondName.split("\\.");
							
						if(checkedTypeNames.contains(secondKeyPart[1].intern())){
						}
						else{
							JSONObject multivalueObject = new JSONObject();
							typeName=secondKeyPart[1].intern();
							
							checkedTypeNames.add(typeName);
								for( Attribute l: innerLayer){
									
										String innerTypeName = l.getName();
										String[] finalKey = innerTypeName.split("\\.");
												
										if(finalKey[1].intern().equals(typeName)){
													multivalueObject.put(finalKey[2].intern(), AttributeUtil.getSingleValue(l));
												}	
								}
								jArray.put(multivalueObject);
								
						}
						json.put(secondKeyPart[0], jArray);
					}
					
					}			
		}
		
		return json;
	}

}
