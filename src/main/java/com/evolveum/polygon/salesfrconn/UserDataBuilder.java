package com.evolveum.polygon.salesfrconn;

import java.util.Map;
import java.util.Set;

import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.json.JSONArray;
import org.json.JSONObject;

public class UserDataBuilder {


	public UserDataBuilder(){


	}

	public JSONObject setUserObject(Set<Attribute> attributes){

		JSONObject userObj = new JSONObject();

		for(Attribute at: attributes){

			String atributeName = at.getName();

			if(atributeName == "schemas" ){

				JSONArray schemasArray = new JSONArray();
				
				Map<String, String> set = (Map<String, String>) AttributeUtil.getSingleValue(at);

				for(String key: set.keySet()){
					schemasArray.put(set.get(key));
				}
				
			}else if(atributeName =="externalId"){

				userObj.put(atributeName, AttributeUtil.getSingleValue(at));

			}else if(atributeName =="userName"){

				userObj.put(atributeName, AttributeUtil.getSingleValue(at));

			}else if(atributeName == "name"){
				
				JSONObject nameElement = new JSONObject();

				Map<String, String> nameSet = (Map<String, String>) AttributeUtil.getSingleValue(at);

				for(String key: nameSet.keySet()){
					//System.out.println(nameSet.get(key));
					nameElement.put(key, nameSet.get(key));
				}
				userObj.put(atributeName, nameElement);
				
			}else if(atributeName == "urn:scim:schemas:extension:enterprise:1.0"){ // TODO Looks like something salesforce specific, must investigate
				
				JSONObject nameElement = new JSONObject();

				Map<String, String> nameSet = (Map<String, String>) AttributeUtil.getSingleValue(at);

				for(String key: nameSet.keySet()){
					//System.out.println(nameSet.get(key));
					nameElement.put(key, nameSet.get(key));
				}
				userObj.put(atributeName, nameElement);

			}else if(atributeName =="displayName"){

				userObj.put(atributeName, AttributeUtil.getSingleValue(at));

			}else if(atributeName =="nickName"){

				userObj.put(atributeName, AttributeUtil.getSingleValue(at));

			}else if(atributeName =="emails"){

				userObj.put(atributeName, buildLayeredAtrribute(at));

			}else if(atributeName =="entitlements"){ /// TODO this is NOT from the CORE shema !

				userObj.put(atributeName, buildLayeredAtrribute(at));

			}else if(atributeName =="addresses"){

				userObj.put(atributeName, buildLayeredAtrribute(at));

			}else if(atributeName =="phoneNumbers"){


				userObj.put(atributeName, buildLayeredAtrribute(at));


			}else if(atributeName =="ims"){

				userObj.put(atributeName, buildLayeredAtrribute(at));

			}else if(atributeName =="photos"){

				userObj.put(atributeName, buildLayeredAtrribute(at));

			}else if(atributeName =="userType"){

				userObj.put(atributeName, AttributeUtil.getSingleValue(at));


			}else if(atributeName =="title"){

				userObj.put(atributeName, AttributeUtil.getSingleValue(at));

			}else if(atributeName =="preferredLanguage"){

				userObj.put(atributeName, AttributeUtil.getSingleValue(at));

			}else if(atributeName =="locale"){

				userObj.put(atributeName, AttributeUtil.getSingleValue(at));

			}else if(atributeName =="timezone"){

				userObj.put(atributeName, AttributeUtil.getSingleValue(at));

			}else if(atributeName =="active"){

				userObj.put(atributeName, AttributeUtil.getSingleValue(at));

			}else if(atributeName =="groups"){

				userObj.put(atributeName, buildLayeredAtrribute(at));

			}

		}

		return userObj;

	}


	private JSONArray buildLayeredAtrribute(Attribute at){

		JSONArray jArray = new JSONArray();

		JSONObject arrayElement = new JSONObject();

		Map<String, Map<String,Object>> keys = (Map<String, Map<String,Object>>) AttributeUtil.getSingleValue(at);

		for(String typeKey: keys.keySet()){
			//System.out.println(nameSet.get(key));
			Map<String,Object> typeLayer = keys.get(typeKey);

			for(String key: typeLayer.keySet()){

				arrayElement.put(key, typeLayer.get(key));

			}
			jArray.put(arrayElement);
		}

		return jArray;
	}

}
