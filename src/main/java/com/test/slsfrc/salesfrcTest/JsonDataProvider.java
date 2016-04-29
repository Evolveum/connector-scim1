package com.test.slsfrc.salesfrcTest;

import org.json.JSONArray;
import org.json.JSONObject;

public class JsonDataProvider {
	
	
	public JsonDataProvider(){

	}
	

    public JSONObject setUserObject(){
    	
    	
    	JSONObject entitlementsElement = new JSONObject();
		entitlementsElement.put("display", "Read Only");
		entitlementsElement.put("value", "00e58000000qvhqAAA");
		entitlementsElement.put("primary", true);
		
		JSONArray entitlementsArray = new JSONArray();
		entitlementsArray.put(entitlementsElement);
		
		//Json Array with schema values as primitives
		JSONArray schemasArray = new JSONArray();
		schemasArray.put("urn:scim:schemas:core:1.0");
		schemasArray.put("urn:scim:schemas:extension:enterprise:1.0");
		
		// Define name key/value pairs 
		JSONObject nameElement = new JSONObject();
		nameElement.put("formatted", "John Snow");
		nameElement.put("familyName", "Snow");
		nameElement.put("givenName", "John");
		
		/*// Add name elements to an array 
		JSONArray nameArray = new JSONArray();
		nameArray.put(nameElement);
		*/
		
	// Define email key/value pairs 
		JSONObject emailElement = new JSONObject();
		emailElement.put("type", "work");
		emailElement.put("value", "johnsnow@thewall.com"); // if primary email is changed, the user has to confirm the change from his email
		emailElement.put("primary", true);
	
		// Add email elements to an array 
		JSONArray emailArray = new JSONArray();
		emailArray.put(emailElement);
		
		JSONObject userObj = new JSONObject();
		JSONObject orgObj = new JSONObject();
		
		orgObj.put("organization", "00D58000000YfgfEAC");
		
		userObj.put("displayName", "John Snow");
		userObj.put("nickName", "JohnSnow");
		userObj.put("userName", "johnsnow@winterfell.com");
		userObj.put("locale", "en_IE_EURO");
		userObj.put("preferredLanguage", "en_US");
		userObj.put("active", true);
		/*UNIQUE!!!!*/userObj.put("externalId", "exted");
		
		
		// Adding the arrays to the main JsonObject 
	/**/	userObj.put("entitlements", entitlementsArray);
		userObj.put("emails", emailArray);
		userObj.put("schemas", schemasArray);
		userObj.put("name", nameElement);
		
		userObj.put("alias", "JohnCrow");
		userObj.put("urn:scim:schemas:extension:enterprise:1.0", orgObj);
		userObj.put("userType", "Standard");
		
    	return userObj;
    }
    
public  JSONObject setEntitlementObject(){
    	
    	// Define membersElement key/value pairs 
		JSONObject membersElement = new JSONObject();
		//membersElement.put("type", "User");
		//membersElement.put("value", "00558000000VcXnAAK"); 
		
		// Add members elements to an array 
		JSONArray membersArray = new JSONArray();
		membersArray.put(membersElement);


		// Adding data to the main JsonObject
		JSONArray schemasArray = new JSONArray();
		schemasArray.put("urn:scim:schemas:core:1.0"); //unnecessary


    	// Adding data to the main JsonObject
    	JSONObject entitlementObj = new JSONObject();
    	
    	entitlementObj.put("displayName", "Standard Something Something"); // unnecessary
    	entitlementObj.put("schemas", schemasArray);
    	entitlementObj.put("members", membersArray);
    	
    	return entitlementObj;
    }
public  JSONObject setGroupObject(){
	
	/*
	 * "schemas": ["urn:scim:schemas:core:1.0"],
"Resources": [{
"displayName": "TestGroup",
"meta": {
"created": "2016-04-27T08:40:59Z",
"location": "https://eu6.salesforce.com/services/scim/v1/Groups/00G58000000e4TVEAY",
"lastModified": "2016-04-27T08:41:08Z",
"version": "3030d7c2d3b20d8ba11880e73a1fafa93284de1c"
},
"schemas": ["urn:scim:schemas:core:1.0"],
"members": [
{
"type": "User",
"value": "00558000000VcXnAAK"
},
{
"type": "User",
"value": "00558000000rRl0AAE"
}
],
"id": "00G58000000e4TVEAY"
}]
}
	
	 * */
	
	// Define membersElement key/value pairs 
			JSONObject membersElement = new JSONObject();
			membersElement.put("type", "User");
			membersElement.put("value", "00558000000VcXnAAK"); 
			
			// Add members elements to an array 
			JSONArray membersArray = new JSONArray();
			membersArray.put(membersElement);
	
	
	// Adding data to the main JsonObject
	JSONArray schemasArray = new JSONArray();
	schemasArray.put("urn:scim:schemas:core:1.0");
	
	JSONObject groupObj = new JSONObject();
	
	groupObj.put("displayName", "TesttestGroup2");
	groupObj.put("schemas", schemasArray);
	groupObj.put("members", membersArray);
	
	return groupObj;
}

}
