package com.evolveum.polygon.test.scim;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.AttributeFilter;
import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.evolveum.polygon.scim.ScimConnectorConfiguration;
/*
 * @Test(groups = "a")
public void f1() {}

@Test(groups = "a")
public void f2() {}

@Test(dependsOnGroups = "a")
public void g() {}
 * */


public class Testclass {
	
	TestConfiguration testConfiguration = null;
	public static Uid userUId;
	public static Uid groupUid;
	public Uid etitlementUid= null;
	
	
	 
	 @DataProvider(name = "filterMethodTestProvider")
	   public static Object[][] filterMethodTestResourcesProvider() {
		 
HashMap<String, HashMap<String,Object>> filtermap = new HashMap<String, HashMap<String,Object>>(); 
	HashMap<String, Object> equalsAttributeMap = new HashMap<String, Object>();
	
	 Uid uid  = new Uid("00558000001JLTlAAO");
	
	equalsAttributeMap.put("userName", "seventhtestuser@ectestdomain.com");

	filtermap.put("equals", equalsAttributeMap);
		 
	      return new Object[][] {{"users",filtermap}};
	   }
	 
	 @DataProvider(name = "updateUserResourceObjectTestProvider")
	   public static Object[][] updateUserResourceObjectTestResourceProvider() throws Exception {
		 Uid uid = getUid("user");
		 
	      return new Object[][] {{"single", userUId},{"multi", userUId},{"enabled", userUId}};
	   }
	 

	 @DataProvider(name = "listAllfromResourcesProvider")
	   public static Object[][] listAllfromResourcesProvider() {
	      return new Object[][] {{1, "users"}, {1, "groups"}, {1, "entitlements"}};
	   }
	 
	 
	 @DataProvider(name = "deletetObjectfromResourcesProvider")
	   public static Object[][] deletetObjectfromResourcesResourceProvider() {
		 
		 Uid uid  = new Uid("00558000001KJBCAA4");
		 
		 
	      return new Object[][] {{"users",uid}};
	   }
	 
	 @DataProvider(name = "createTestProvider")
	   public static Object[][] createTestResourceProvider() {
		 
	      return new Object[][] {{"users",true},{"groups",true}};
	   }
	 
	 @DataProvider(name = "providerTesConfig")
	   public static Object[][] resourcesProviderTesConfig() {
		 
	HashMap<String, String> configurationParameters = new HashMap<String, String>();
		configurationParameters.put("clientID","");
		configurationParameters.put("clientSecret", "");
		configurationParameters.put("endpoint", "/services/scim");
		configurationParameters.put("loginUrl", "https://login.salesforce.com");
		configurationParameters.put("password", "");
		configurationParameters.put("service", "");
		configurationParameters.put("userName", "");
		configurationParameters.put("version", "/v1");
		 
	      return new Object[][] {{configurationParameters,true}};
	   }
	 
	 
	 ///////////////////////////TestSuite////////////////////////////
	 
	//@Test (priority=1, dataProvider = "providerTesConfig")
	 public void configurationTest(HashMap <String,String> configurationParameters, Boolean assertionVariable){
		 
		 testConfiguration = new TestConfiguration(configurationParameters);
		  
		 Boolean isValid= testConfiguration.isConfigurationValid();
		 
		 Assert.assertEquals(isValid, assertionVariable);
		 
	 }
	 
	 
    //@Test (priority=2, dataProvider = "createTestProvider")
	 private void createObjectOnResourcesTest(String resourceName, Boolean assertParameter){
		 
		groupUid = null;
		userUId = null;	
		 Boolean resourceWasCreated = false;
		 
		 if(testConfiguration == null){
			 resourcesProviderTesConfig();
		 }
		 
		 if("groups".equals(resourceName)){
		userUId= testConfiguration.createResourceTestHelper(resourceName);
		 }else{
			 
		groupUid= testConfiguration.createResourceTestHelper(resourceName);
		 }
			
			if(userUId !=null||groupUid !=null){
				resourceWasCreated = true;
			}
			
			Assert.assertEquals(resourceWasCreated, assertParameter);
			
		}
	
	//@Test (priority=2, dataProvider = "filterMethodTestProvider")
	public void filterMethodTest(String resourceName,HashMap<String, HashMap<String,Object>> filtermap ){

		
		 for (String filterName: filtermap.keySet()){
			 
			 HashMap<String, Object> attributeMap = filtermap.get(filterName);
			
			 for (String leftAttribute: attributeMap.keySet()){
			 Object rigthAttribute = attributeMap.get(leftAttribute);
			 
			 
			 AttributeFilter filter=  testConfiguration.getFilter(filterName, leftAttribute, rigthAttribute);
			 
			 testConfiguration.filterMethodsTest(filter, resourceName);
			 
			 Assert.assertFalse(testConfiguration.getHandlerResult().isEmpty());
			 }
		 }
		
	}
	// @Test (dataProvider = "listAllfromResourcesProvider")
	private void listAllfromResourcesTest(int numberOfResources, String resourceName){
		
		testConfiguration.listAllfromResourcesTestHelper(resourceName);
		Assert.assertEquals(testConfiguration.getHandlerResult().size(), numberOfResources);
		
	}
	// TODO mod to test different types of updates e.q. single value attribute, multi value attribute, activation/deactivation
	 //@Test (dependsOnMethods = { "createObjectOnResourcesTest", "configurationTest" }, priority=3, dataProvider = "updateUserResourceObjectTestProvider")
		private void updateUserResourceObjectTest(String updateType, Uid uid){
		
			Uid returnedUid = testConfiguration.updateResourceTestHelper("users");
			
			Assert.assertEquals(uid,returnedUid );
			
			
		}
	
	//@Test (priority=9, dataProvider = "deletetObjectfromResourcesProvider")
	private void deletetObjectfromResourcesTest(String resourceName, Uid uid){
		
		testConfiguration.deleteResourceTestHelper(uid, resourceName);
		AttributeFilter filter=  testConfiguration.getFilter("uid", null, uid);
		 testConfiguration.filterMethodsTest(filter, resourceName);
		
		
		
		Assert.assertTrue(testConfiguration.getHandlerResult().isEmpty());
		
	}
	
	
	public static Uid getUid(String resourceName) throws Exception{
		Uid uid =null;
		
		
		if("user".equals(resourceName)){
			uid = userUId;
			
		}else if ("group".equals(resourceName)){
			
			uid = groupUid;	
		}
		
		if (uid == null){
			
			throw new Exception("Uid not set");
			
		}
		
		return uid;
	}

	
	
}

