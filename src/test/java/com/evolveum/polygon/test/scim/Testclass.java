package com.evolveum.polygon.test.scim;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.AttributeFilter;
import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.Reporter;
import org.testng.annotations.AfterMethod;
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
import com.evolveum.polygon.scim.CrudManagerScim;


public class Testclass {

	
	TestConfiguration testConfiguration = null;
	public static Uid userUid;
	public static Uid groupUid;
	public Uid etitlementUid= null;

	private static final Log LOGGER = Log.getLog(Testclass.class);

	@DataProvider(name = "filterMethodTestProvider")
	public static Object[][] filterMethodTestResourcesProvider() {

		//TODO test issues with eq filter slack
		
		return new Object[][] {{"users","contains"},{"groups","contains"},{"users","uid"},{"groups","uid"},{"users","startswith"},{"groups","startswith"}, {"groups","containsall"} };
	}

	@DataProvider(name = "updateUserResourceObjectTestProvider")
	public static Object[][] updateUserResourceObjectTestResourceProvider() throws Exception {
		Uid uid = getUid("user");

		return new Object[][] {{"single", uid},{"multi", uid},{"disabled", uid},{"enabled", uid}};
	}
	@DataProvider(name = "updateGroupResourceObjectTestProvider")
	public static Object[][] updateGroupResourceObjectTestResourceProvider() throws Exception {
		Uid uid = getUid("group");

		return new Object[][] {{"single", uid},{"multi", uid}};
	}


	@DataProvider(name = "listAllfromResourcesProvider")
	public static Object[][] listAllfromResourcesProvider() {
		return new Object[][] {{1, "users"}, {1, "groups"}};
	}


	@DataProvider(name = "deletetObjectfromResourcesProvider")
	public static Object[][] deletetObjectfromResourcesResourceProvider() {


		return new Object[][] {{"users"},{"groups"}};
	}

	@DataProvider(name = "createTestProvider")
	public static Object[][] createTestResourceProvider() {

		return new Object[][] {{"users",true},{"groups",true}};
	}

	@DataProvider(name = "tesConfigProvider")
	public static Object[][] tesConfigResourcesProvider() {

		HashMap<String, String> configurationParameters = new HashMap<String, String>();
		configurationParameters.put("clientID","");
		configurationParameters.put("clientSecret", "xx");
		configurationParameters.put("endpoint", "/scim");
		configurationParameters.put("loginUrl", "https://api.slack.com");
		configurationParameters.put("password", "xx");
		configurationParameters.put("service", "xx");
		configurationParameters.put("userName", "xx");
		configurationParameters.put("version", "/v1");
		configurationParameters.put("authentication", "token");

		return new Object[][] {{configurationParameters,true}};
	}


	///////////////////////////TestSuite////////////////////////////

	@Test (priority=1, dataProvider = "tesConfigProvider")
	public void configurationTest(HashMap <String,String> configurationParameters, Boolean assertionVariable){

		groupUid = null;
		userUid = null;	

		testConfiguration = new TestConfiguration(configurationParameters);

		Boolean isValid= testConfiguration.isConfigurationValid();

		Assert.assertEquals(isValid, assertionVariable);

	}


	@Test ( priority=2, dependsOnMethods = {"configurationTest"} , dataProvider = "createTestProvider")
	private void createObjectOnResourcesTest(String resourceName, Boolean assertParameter){

		Boolean resourceWasCreated = false;

		if(testConfiguration == null){
			tesConfigResourcesProvider();
		}

		if("users".equals(resourceName)){
			userUid= testConfiguration.createResourceTestHelper(resourceName);
			testConfiguration.setUserTestUid(userUid);
			if(userUid !=null){
				resourceWasCreated = true;
			}
		}else if ("groups".equals(resourceName)){

			groupUid= testConfiguration.createResourceTestHelper(resourceName);
			testConfiguration.setGroupTestUid(groupUid);
			if(groupUid !=null){
				resourceWasCreated = true;
			}
		}

		if(userUid !=null){
			resourceWasCreated = true;
		}

		Assert.assertEquals(resourceWasCreated, assertParameter);

	}

	@Test (priority=6,dependsOnMethods = {"createObjectOnResourcesTest"}, dataProvider = "filterMethodTestProvider")
	public void filterMethodTest(String resourceName,String filterType ){

		testConfiguration.filterMethodsTest(filterType, resourceName);

		Assert.assertFalse(testConfiguration.getHandlerResult().isEmpty());



	}
	@Test ( priority=5, dependsOnMethods = {"createObjectOnResourcesTest"}, dataProvider = "listAllfromResourcesProvider")
	private void listAllfromResourcesTest(int numberOfResources, String resourceName){

		testConfiguration.listAllfromResourcesTestHelper(resourceName);
		Assert.assertEquals(testConfiguration.getHandlerResult().size(), numberOfResources);

	}

	@Test ( priority=3, dependsOnMethods = {"createObjectOnResourcesTest"}, dataProvider = "updateUserResourceObjectTestProvider")
	private void updateUserResourceObjectTest(String updateType, Uid uid){

		Uid returnedUid = testConfiguration.updateResourceTestHelper("users", updateType);

		Assert.assertEquals(uid,returnedUid );


	}
	@Test (priority=4,dependsOnMethods = {"createObjectOnResourcesTest"},  dataProvider = "updateGroupResourceObjectTestProvider")
	private void updateGroupResourceObjectTest(String updateType, Uid uid){



		Uid returnedUid = testConfiguration.updateResourceTestHelper("groups", updateType);

		Assert.assertEquals(uid,returnedUid );


	}
	
	@AfterMethod
	private void cleanup(ITestResult result){
		 if (result.getStatus() == ITestResult.FAILURE) {
			 if (userUid !=null){
				 LOGGER.warn("Atempting to delete resource: {0}", "users");
			 deleteObjectfromResourcesTest("users");
			 
			 }else 
			 if(groupUid !=null){
				 LOGGER.warn("Atempting to delete resource: {0}", "groups");
			 deleteObjectfromResourcesTest("groups");
			 }else{
			 LOGGER.warn("Test failure, uid values of resource objects are null. No resource deletion operation was atempted");
			 }}      
		
	}

	@Test (priority=7,dependsOnMethods= {"createObjectOnResourcesTest"}, dataProvider = "deletetObjectfromResourcesProvider")
	private void deleteObjectfromResourcesTest(String resourceName){

		testConfiguration.deleteResourceTestHelper(resourceName);
		testConfiguration.filterMethodsTest("uid", resourceName);

		Assert.assertTrue(testConfiguration.getHandlerResult().isEmpty());

	}


	public static Uid getUid(String resourceName) throws Exception{
		Uid uid =null;

		if("user".equals(resourceName)){
			uid = userUid;

		}else if ("group".equals(resourceName)){

			uid = groupUid;	
		} else {
			LOGGER.warn("Resource name not defined: {0}", resourceName);

		} 

		if (uid == null){

			throw new Exception("Uid not set");

		}

		return uid;
	}



}

