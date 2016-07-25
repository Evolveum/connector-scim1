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
	public Uid groupUid;
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
	 
	 @DataProvider(name = "providerUpdateResourceObjecttest")
	   public static Object[][] updateResourceObjectTestResourceProvider() {
		// Uid uid = new Uid("00558000001K3NZAA0");
		 
	      return new Object[][] {{"users", userUId}};
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
	 
	 @DataProvider(name = "providerTestCreate")
	   public static Object[][] resourcesProviderTestCreate() {
		 
	      return new Object[][] {{"users",true}};
	   }
	 
	 @DataProvider(name = "providerTesConfig")
	   public static Object[][] resourcesProviderTesConfig() {
		 
	HashMap<String, String> configurationParameters = new HashMap<String, String>();
		configurationParameters.put("clientID","");
		configurationParameters.put("clientSecret", "");
		configurationParameters.put("endpoint", "");
		configurationParameters.put("loginUrl", "https://login.salesforce.com");
		configurationParameters.put("password", "");
		configurationParameters.put("service", "");
		configurationParameters.put("userName", "");
		configurationParameters.put("version", "/v1");
		 
	      return new Object[][] {{configurationParameters,true}};
	   }
	 
	 
	 ///////////////////////////TestSuite///////////////
	 
	//@Test (priority=1, dataProvider = "providerTesConfig")
	 public void configurationTest(HashMap <String,String> configurationParameters, Boolean assertionVariable){
		 
		 testConfiguration = new TestConfiguration(configurationParameters);
		  
		 Boolean isValid= testConfiguration.isConfigurationValid();
		 
		 Assert.assertEquals(isValid, assertionVariable);
		 
	 }
	 
	 
    //@Test (dataProvider = "providerTestCreate")
	 private void createObjectOnResourcesTest(String resourceName, Boolean assertParameter){
		userUId = null;	
		 Boolean notNull = false;
		 
		 if(testConfiguration == null){
			 resourcesProviderTesConfig();
		 }
		 userUId= testConfiguration.createResourceTest(resourceName);
			
			if(userUId !=null){
				notNull = true;
			}
			
			Assert.assertEquals(notNull, assertParameter);
			
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
	private void ListAllfromResourcesTest(int numberOfResources, String resourceName){
		
		testConfiguration.listAllfromResources(resourceName);
		Assert.assertEquals(testConfiguration.getHandlerResult().size(), numberOfResources);
		
	}
	
	 //@Test (priority=3, dataProvider = "providerUpdateResourceObjecttest")
		private void testUpdateResourceObject(String resourceName, Uid uid){
		 
			if(testConfiguration == null){
				 resourcesProviderTesConfig();
			 }
			
			Uid returnedUid = testConfiguration.updateResourceTest(resourceName);
			
			Assert.assertEquals(uid,returnedUid );
			
			
		}
	
	//@Test (priority=9, dataProvider = "deletetObjectfromResourcesProvider")
	private void deletetObjectfromResourcesTest(String resourceName, Uid uid){
		
		testConfiguration.deleteResourceTest(uid, resourceName);
		AttributeFilter filter=  testConfiguration.getFilter("uid", null, uid);
		 testConfiguration.filterMethodsTest(filter, resourceName);
		
		
		
		Assert.assertTrue(testConfiguration.getHandlerResult().isEmpty());
		
	}
	

	
	
}

