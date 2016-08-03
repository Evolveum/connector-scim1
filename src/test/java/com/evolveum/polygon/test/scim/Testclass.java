package com.evolveum.polygon.test.scim;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.identityconnectors.common.logging.Log;
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
import com.evolveum.polygon.scim.ScimCrudManager;


public class Testclass {

	//TODO bug to be fixed
	// The update JSON object wich is beaing send: {"addresses":[{"region":"region","type":"work"},{"postalCode":"postalCode","type":"work"},{"streetAddress":"streetAddress","type":"work"},{"locality":"locality","type":"work"},{"country":"country","type":"work"}]}

	TestConfiguration testConfiguration = null;
	public static Uid userUid;
	public static Uid groupUid;
	public Uid etitlementUid= null;

	private static final Log LOGGER = Log.getLog(Testclass.class);

	@DataProvider(name = "filterMethodTestProvider")
	public static Object[][] filterMethodTestResourcesProvider() {

		return new Object[][] {{"users",""}};
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
		configurationParameters.put("clientSecret", "");
		configurationParameters.put("endpoint", "");
		configurationParameters.put("loginUrl", "");
		configurationParameters.put("password", "");
		configurationParameters.put("service", "");
		configurationParameters.put("userName", "");
		configurationParameters.put("version", "");

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


	@Test (dependsOnMethods = {"configurationTest"} , priority=2, dataProvider = "createTestProvider")
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

	@Test (dependsOnMethods = {"updateUserResourceObjectTest","updateGroupResourceObjectTest"},priority=6, dataProvider = "filterMethodTestProvider")
	public void filterMethodTest(String resourceName,String filterType ){

		testConfiguration.filterMethodsTest(filterType, resourceName);

		Assert.assertFalse(testConfiguration.getHandlerResult().isEmpty());



	}
	@Test (dependsOnMethods = {"createObjectOnResourcesTest"}, priority=5, dataProvider = "listAllfromResourcesProvider")
	private void listAllfromResourcesTest(int numberOfResources, String resourceName){

		testConfiguration.listAllfromResourcesTestHelper(resourceName);
		Assert.assertEquals(testConfiguration.getHandlerResult().size(), numberOfResources);

	}

	@Test (dependsOnMethods = { "createObjectOnResourcesTest"}, priority=3, dataProvider = "updateUserResourceObjectTestProvider")
	private void updateUserResourceObjectTest(String updateType, Uid uid){

		Uid returnedUid = testConfiguration.updateResourceTestHelper("users", updateType);

		Assert.assertEquals(uid,returnedUid );


	}
	@Test (dependsOnMethods = { "updateUserResourceObjectTest"}, priority=4, dataProvider = "updateGroupResourceObjectTestProvider")
	private void updateGroupResourceObjectTest(String updateType, Uid uid){



		Uid returnedUid = testConfiguration.updateResourceTestHelper("users", updateType);

		Assert.assertEquals(uid,returnedUid );


	}

	@Test (dependsOnMethods = { "createObjectOnResourcesTest"},priority=9, dataProvider = "deletetObjectfromResourcesProvider")
	private void deleteObjectfromResourcesTest(String resourceName){

		testConfiguration.deleteResourceTestHelper(resourceName);
		testConfiguration.filterMethodsTest("uid", resourceName);

		Assert.assertTrue(testConfiguration.getHandlerResult().isEmpty());

	}


	public static Uid getUid(String resourceName) throws Exception{
		Uid uid =null;

		System.out.println("##resourceName "+resourceName.toString());
		System.out.println("##group "+groupUid.toString());
		System.out.println("##user "+userUid.toString());
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

