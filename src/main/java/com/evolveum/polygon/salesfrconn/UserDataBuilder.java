package com.evolveum.polygon.salesfrconn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.json.JSONArray;
import org.json.JSONObject;

public class UserDataBuilder {
	
	private static Map<String, String> nameDictionaryUser = CollectionUtil.newCaseInsensitiveMap();
	
	static {
		nameDictionaryUser.put("userName","userName");
		
		nameDictionaryUser.put("name","name");
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
		
		nameDictionaryUser.put("groups","groups");
		nameDictionaryUser.put("groups.value","value");
		nameDictionaryUser.put("groups.display","display");
		
		nameDictionaryUser.put("x509Certificates","x509Certificates");
		nameDictionaryUser.put("x509Certificates.value","value");
		

	}
	


	public UserDataBuilder(){

	}

	public JSONObject setUserObject(Set<Attribute> attributes){

		JSONObject userObj = new JSONObject();

		for(Attribute at: attributes){

			String attributeName = at.getName();

		if(nameDictionaryUser.containsKey(attributeName)){
			if(attributeName.contains(".")){
				
				String[] keyParts = attributeName.split("\\.");
				
				
				translateToMap(at, attributeName);
				
			}else{
				
				userObj.put(attributeName, AttributeUtil.getSingleValue(at));
			}
			
		}else{System.out.println("not here");}
		
		}

		return userObj;

	}


	private JSONArray buildLayeredAtrribute(Attribute at, String attributeName){

		JSONArray jArray = new JSONArray();

		JSONObject arrayElement = new JSONObject();
		
		String[] keyParts = attributeName.split("\\.");
		
		arrayElement.put(keyParts[1], AttributeUtil.getSingleValue(at));
		
		jArray.put(arrayElement);

		//for(String typeKey: keys.keySet()){
			//System.out.println(nameSet.get(key));
		//	Map<String,Object> typeLayer = keys.get(typeKey);

			//for(String key: typeLayer.keySet()){

				//arrayElement.put(key, typeLayer.get(key));

			//}
		//	jArray.put(arrayElement);
		//}

		return jArray;
	}
	
 public void translateToMap(Attribute at, String attributeName){
	 
	 Map<String, Collection<Map<String, Object>>> multiLayerAttribute = new HashMap<String, Collection<Map<String, Object>>>();
	 
	 String[] keyParts = attributeName.split("\\.");
	 
	 Map<String, Collection<Map <String, Object>>> map = new HashMap<String, Collection<Map <String, Object>>>();
	 
	 if(keyParts.length == 2){
		
		 Map<String, Object> ma = new HashMap<String, Object>();
		
		
	 }
	 
 }

}
