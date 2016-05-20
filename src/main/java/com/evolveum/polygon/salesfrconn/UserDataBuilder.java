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
import org.identityconnectors.framework.common.objects.AttributeUtil;
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
		nameDictionaryUser.put("profileUrl","profileUrl");
		
		
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
		
		nameDictionaryUser.put("groups.value","value");
		nameDictionaryUser.put("groups.display","display");
		
		nameDictionaryUser.put("x509Certificates","x509Certificates");
		nameDictionaryUser.put("x509Certificates.value","value");
		

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
			
		}else{System.out.println("not here");}
		
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
		for(Attribute i: attr){
			
			String attributeName = i.getName();
			String[] keyParts = attributeName.split("\\.");
				
				if(keyParts[0].intern().equals(name)){
				}else{
					JSONArray jArray = new JSONArray();		
					name=keyParts[0].intern();
					for(Attribute j: attr){
						
						String innerName = j.getName();
						String[] innerKeyParts = innerName.split("\\.");
							if(innerKeyParts[0].equals(name)){
								
								Set<Attribute> innerLayer = new HashSet<Attribute>();
									innerLayer.add(j); /// TODO here is an error <-----------------------<-------------------
								
									String typeName = "";
									
									for(Attribute k: innerLayer){
										
										String secondName = k.getName();
										String[] secondKeyPart = secondName.split("\\.");
											
										if(secondKeyPart[1].intern().equals(typeName)){
										}
										else{
											JSONObject multivalueObject = new JSONObject();
											typeName=secondKeyPart[1].intern();
											System.out.println(typeName);
												for( Attribute l: innerLayer){
													System.out.print(innerLayer.toString());
														String innerTypeName = l.getName();
														String[] finalKey = innerTypeName.split("\\.");
																if(finalKey[1].intern().equals(typeName)){
																	multivalueObject.put(finalKey[2].intern(), AttributeUtil.getSingleValue(l));
																}
																System.out.println(finalKey[1].intern());
												}
												multivalueObject.put("type", secondKeyPart[1].intern());
												jArray.put(multivalueObject);
										}
									}
							}
						
					}
					json.put(keyParts[0], jArray);
				}			
		}
		
		return json;
	}
	
 public JSONObject buildMultivalueAttribute(Set<Attribute> attr, JSONObject json){
	 	
	 String name="";
	for(Attribute i: attr){
		String attributeName = i.getName();
		String[] keyParts = attributeName.split("\\.");
		
		if(keyParts[0].intern().equals(name)){
		}else{
			JSONObject jObject = new JSONObject();
			name=keyParts[0].intern();	
			for(Attribute j: attr){
				String innerName = j.getName();
				String[] innerKeyParts = innerName.split("\\.");
				if(innerKeyParts[0].intern().equals(name)){
					jObject.put(innerKeyParts[1], AttributeUtil.getSingleValue(j));
				}
			}
			json.put(keyParts[0], jObject);
		}
	}
	return json;

	 
 }

}
