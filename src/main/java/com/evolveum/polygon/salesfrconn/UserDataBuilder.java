package com.evolveum.polygon.salesfrconn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.ObjectClassInfoBuilder;
import org.json.JSONArray;
import org.json.JSONObject;

public class UserDataBuilder {
	
	private static Map<String, String> nameDictionaryUser = CollectionUtil.newCaseInsensitiveMap();
	private static final Log LOGGER = Log.getLog(UserDataBuilder.class);
	
	
	static {
		nameDictionaryUser.put("userName","userName");
		
		
		nameDictionaryUser.put("name.formatted","formatted");
		nameDictionaryUser.put("name.familyName","familyName");
		nameDictionaryUser.put("name.givenName","givenName");
		nameDictionaryUser.put("name.middleName","middleName");
		nameDictionaryUser.put("name.honorificPrefix","honorificPrefix");
		nameDictionaryUser.put("name.honorificSuffix","honorificSuffix");
		
		nameDictionaryUser.put("displayName","displayName");
		nameDictionaryUser.put("nickName","nickName");
		//nameDictionaryUser.put("profileUrl","profileUrl");
		
		
		nameDictionaryUser.put("emails.work.value","value");
		nameDictionaryUser.put("emails.work.primary","primary");
		
		nameDictionaryUser.put("emails.home.value","value");
		nameDictionaryUser.put("emails.home.primary","primary");
		
		nameDictionaryUser.put("emails.other.value","value");
		nameDictionaryUser.put("emails.other.primary","primary");
		
		

		nameDictionaryUser.put("addresses.work.streetAddress","streetAddress");
		nameDictionaryUser.put("addresses.work.locality","locality");
		nameDictionaryUser.put("addresses.work.region","region");
		nameDictionaryUser.put("addresses.work.postalCode","postalCode");
		nameDictionaryUser.put("addresses.work.country","country");
		nameDictionaryUser.put("addresses.work.formatted","formatted");
		nameDictionaryUser.put("addresses.work.primary","primary");
		

		nameDictionaryUser.put("addresses.home.streetAddress","streetAddress");
		nameDictionaryUser.put("addresses.home.locality","locality");
		nameDictionaryUser.put("addresses.home.region","region");
		nameDictionaryUser.put("addresses.home.postalCode","postalCode");
		nameDictionaryUser.put("addresses.home.country","country");
		nameDictionaryUser.put("addresses.home.formatted","formatted");
		nameDictionaryUser.put("addresses.home.primary","primary");
		
		nameDictionaryUser.put("addresses.other.streetAddress","streetAddress");
		nameDictionaryUser.put("addresses.other.locality","locality");
		nameDictionaryUser.put("addresses.other.region","region");
		nameDictionaryUser.put("addresses.other.postalCode","postalCode");
		nameDictionaryUser.put("addresses.other.country","country");
		nameDictionaryUser.put("addresses.other.formatted","formatted");
		nameDictionaryUser.put("addresses.other.primary","primary");

		
		nameDictionaryUser.put("phoneNumbers.work.value","value");

		nameDictionaryUser.put("phoneNumbers.home.value","value");
		
		nameDictionaryUser.put("phoneNumbers.mobile.value","value");
		
		nameDictionaryUser.put("phoneNumbers.fax.value","value");
		
		nameDictionaryUser.put("phoneNumbers.pager.value","value");
		
		
		nameDictionaryUser.put("photos.photo.value","value");
		
		nameDictionaryUser.put("photos.thumbnail.value","value");
		
		
		nameDictionaryUser.put("ims.aim.value","type");

		nameDictionaryUser.put("ims.gtalk.value","type");
		
		nameDictionaryUser.put("ims.icq.value","type");
		
		nameDictionaryUser.put("ims.msn.value","type");

		nameDictionaryUser.put("ims.xmpp.value","type");
		
		nameDictionaryUser.put("ims.skype.value","type");
		
		nameDictionaryUser.put("ims.qq.value","type");
		
		nameDictionaryUser.put("ims.yahoo.value","type");
		
		nameDictionaryUser.put("ims.other.value","type");
		
		
		nameDictionaryUser.put("userType","userType");
		nameDictionaryUser.put("title","title");
		nameDictionaryUser.put("preferredLanguage","preferredLanguage");
		nameDictionaryUser.put("locale","locale");
		
		nameDictionaryUser.put("id","id");
		nameDictionaryUser.put("externalId","externalId");
		nameDictionaryUser.put("timezone","timezone");
		nameDictionaryUser.put("active","active");
		//nameDictionaryUser.put("password","password");
		
		//nameDictionaryUser.put("groups.value","value");
	//	nameDictionaryUser.put("groups.display","display");
		
		nameDictionaryUser.put("x509Certificates","x509Certificates");
		nameDictionaryUser.put("x509Certificates.value","value");
		
		
		// it might be a problem that this is an array
		nameDictionaryUser.put("entitlements..value","value");
		nameDictionaryUser.put("entitlements..primary","primary");
		
		nameDictionaryUser.put("schemaExtension.type","type");
		nameDictionaryUser.put("schemaExtension.organization","organization");
	}
	


	public UserDataBuilder(){

	}

	public JSONObject setUserObject(Set<Attribute> attributes){

		JSONObject userObj = new JSONObject();
		
		Set<Attribute> multivalueAttribute = new HashSet<Attribute>();
		Set<Attribute> multilaierAttribute = new HashSet<Attribute>();
		
		for(Attribute at: attributes){

			String attributeName = at.getName();

		if(nameDictionaryUser.containsKey(attributeName)){
			if(attributeName.contains(".")){
				
			
				String[] keyParts = attributeName.split("\\.");
				 if(keyParts.length ==2){
					 
					multivalueAttribute.add(at);
				 }
				 else{
					 multilaierAttribute.add(at);
				 }
				
			}else{
				
				userObj.put(attributeName, AttributeUtil.getSingleValue(at));
			}
			
		}else{LOGGER.error("Attribute name not defined in dictionary {0}", attributeName);}
		
		}
		
		if(multivalueAttribute != null){
			
			
		buildMultivalueAttribute(multivalueAttribute,userObj);
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
								if(!secondKeyPart[1].intern().equals("")){
								multivalueObject.put("type", secondKeyPart[1].intern());
								}
								jArray.put(multivalueObject);
								
						}
						json.put(secondKeyPart[0], jArray);
					}
					
					}			
		}
		
		
		
		
		
		
		return json;
	}
	
 public JSONObject buildMultivalueAttribute(Set<Attribute> attr, JSONObject json){
	 	
	 int count= 0;
	 String name="";
	 
	 ArrayList<String> checkedNames= new ArrayList<String>();
	 
	 Set<Attribute> specialMlAttributes = new HashSet<Attribute>();
	for(Attribute i: attr){
		String attributeName = i.getName();
		String[] keyParts = attributeName.split("\\.");
		
		if(checkedNames.contains(keyParts[0].intern())){
		}else{
			JSONObject jObject = new JSONObject();
			name=keyParts[0].intern();	
			checkedNames.add(name);
			for(Attribute j: attr){
				String innerName = j.getName();
				String[] innerKeyParts = innerName.split("\\.");
				if(innerKeyParts[0].intern().equals(name)&&!name.equals("schema")){
					jObject.put(innerKeyParts[1], AttributeUtil.getSingleValue(j));
				}else if(innerKeyParts[0].intern().equals(name)&&name.equals("schema")){
					specialMlAttributes.add(j);			
					
				}
			}
			if(specialMlAttributes.isEmpty()){
			json.put(keyParts[0], jObject);
			}
			//
			else {
				String attrName="No shchema type";
				Boolean nameSet= false;
				
				for(Attribute sa: specialMlAttributes){
					String innerName = sa.getName();
					String[] innerKeyParts = innerName.split("\\.");
					if(innerKeyParts[1].intern().equals("type")&& !nameSet){
						attrName = AttributeUtil.getAsStringValue(sa);
						nameSet = true;
					}else if(!innerKeyParts[1].intern().equals("type")){
						
						jObject.put(innerKeyParts[1], AttributeUtil.getSingleValue(sa));
					}
					
				}
				if(nameSet){
					
					json.put(attrName, jObject);
					specialMlAttributes.removeAll(specialMlAttributes);
					
				}else{
					
					LOGGER.error("Schema type not speciffied {0}", attrName);
				}
				
			}//
		}
	}
	
	return json;

	 
 }
 
 public static ObjectClassInfo getUserSchema(){
	 
	 ObjectClassInfoBuilder builder = new ObjectClassInfoBuilder();
	 
	 builder.addAttributeInfo(Name.INFO);

     builder.addAttributeInfo(AttributeInfoBuilder.define("userName").setRequired(true)
             .build());
     builder.addAttributeInfo(AttributeInfoBuilder.define("name.formatted")
             .build());
     builder.addAttributeInfo(AttributeInfoBuilder.define("name.familyName").setRequired(true)
    		 .build());
     builder.addAttributeInfo(AttributeInfoBuilder.define("name.givenName").setRequired(true)
    		 .build());
     builder.addAttributeInfo(AttributeInfoBuilder.define("name.middleName")
    		 .build());
     builder.addAttributeInfo(AttributeInfoBuilder.define("name.honorificPrefix")
    		 .build());
     builder.addAttributeInfo(AttributeInfoBuilder.define("name.honorificSuffix")
    		 .build());
     
     
	 
	return null;
	 
	 
 }

}
