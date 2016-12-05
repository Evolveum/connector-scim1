/*
 * Copyright (c) 2016 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.polygon.test.scim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.AlreadyExistsException;
import org.identityconnectors.framework.common.exceptions.InvalidCredentialException;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.Uid;
import org.testng.Assert;

import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.evolveum.polygon.scim.ScimConnector;
import com.evolveum.polygon.scim.ScimConnectorConfiguration;

/**
 * 
 * @author Macik
 *
 */
public class SlackSpecificTestSuite extends StandardScimTestSuite {

	private Uid userUid;
	private Uid groupUid;

	private Integer pageSize;

	private Integer pageOffset;

	private Integer testNumber = 0;

	private ScimConnector connector;

	private ScimConnectorConfiguration configuration;

	private final static Log LOGGER = Log.getLog(SlackSpecificTestSuite.class);

	public PropertiesParser parser = new PropertiesParser("../connector-scim1/testProperties/slackTest.properties");

	@DataProvider(name = ICEXCEPTIONPROVIDER)
	public Object[][] invalidCredentialExceptionTestProvider() {

		int width = 2;

		List<String> nonConnectionParameters = new ArrayList<String>();

		nonConnectionParameters.add(PAGESIZE);
		nonConnectionParameters.add(PAGEOFFSET);
		nonConnectionParameters.add(TESTNUMBER);

		Map<String, String> configurationParameters = new HashMap<String, String>();

		Object[][] object = parser.fetchTestData(CONFIGTESTPROVIDER);
		String name = "";
		String value = "";
		for (int i = 0; i < object.length; i++) {

			for (int j = 0; j < width; j++) {
				if (j == 0) {
					name = (String) object[i][j];
				} else {
					value = (String) object[i][j];
				}
				if (nonConnectionParameters.contains(name)) {

					if (!value.isEmpty()) {

						if (name.equals(PAGESIZE)) {

							pageSize = Integer.parseInt(value);
							name = "";
							value = "";
						} else if (name.equals(PAGEOFFSET)) {
							pageOffset = Integer.parseInt(value);
							name = "";
							value = "";
						} else if (name.equals(TESTNUMBER)) {
							testNumber = Integer.parseInt(value);
							name = "";
							value = "";
						}

					}

				} else {
					if (!name.isEmpty() && !value.isEmpty()) {
						configurationParameters.put(name, value);
						name = "";
						value = "";
					}

				}

			}

		}
		return new Object[][] { { configurationParameters, "users" } };
	}
	
	@DataProvider(name = CONFIGTESTPROVIDER)
	public Object[][] configurationResourcesProvider() {

		int width = 2;

		List<String> nonConnectionParameters = new ArrayList<String>();

		nonConnectionParameters.add(PAGESIZE);
		nonConnectionParameters.add(PAGEOFFSET);
		nonConnectionParameters.add(TESTNUMBER);

		Map<String, String> configurationParameters = new HashMap<String, String>();

		Object[][] object = parser.fetchTestData(CONFIGTESTPROVIDER);
		String name = "";
		String value = "";
		for (int i = 0; i < object.length; i++) {

			for (int j = 0; j < width; j++) {
				if (j == 0) {
					name = (String) object[i][j];
				} else {
					value = (String) object[i][j];
				}
				if (nonConnectionParameters.contains(name)) {

					if (!value.isEmpty()) {

						if (name.equals(PAGESIZE)) {

							pageSize = Integer.parseInt(value);
							name = "";
							value = "";
						} else if (name.equals(PAGEOFFSET)) {
							pageOffset = Integer.parseInt(value);
							name = "";
							value = "";
						} else if (name.equals(TESTNUMBER)) {
							testNumber = Integer.parseInt(value);
							name = "";
							value = "";
						}

					}

				} else {
					if (!name.isEmpty() && !value.isEmpty()) {
						configurationParameters.put(name, value);
						name = "";
						value = "";
					}

				}

			}

		}
		return new Object[][] { { configurationParameters, true } };
	}

	@Test(priority = 1, dataProvider = CONFIGTESTPROVIDER)
	public void configurationTest(HashMap<String, String> configurationParameters, Boolean assertionVariable) {
		LOGGER.info("## The evaluated test: configurationTest for the resource: {0} ");
		groupUid = null;
		userUid = null;

		configuration = SlackSpecificTestUtils.buildConfiguration(configurationParameters);

		Boolean isValid = SlackSpecificTestUtils.isConfigurationValid(configuration);

		if (isValid) {

			connector = new ScimConnector();
			connector.init(configuration);
			connector.test();
			connector.schema();
		}

		Assert.assertEquals(isValid, assertionVariable);

	}

	@Test(priority = 1, dependsOnMethods = { CONFIGURATIONTEST }, dataProvider = CREATEPROVIDER)
	private void createObjectTest(String resourceName, Boolean assertParameter) {
		LOGGER.info("## The evaluated test: createObjectTest for the resource: {0} ", resourceName);
		Boolean resourceWasCreated = false;

		if (USERS.equals(resourceName)) {
			userUid = SlackSpecificTestUtils.createResourceTestHelper(resourceName, testNumber, connector);
			if (userUid != null) {
				resourceWasCreated = true;
			}
		} else if (GROUPS.equals(resourceName)) {

			groupUid = SlackSpecificTestUtils.createResourceTestHelper(resourceName, testNumber, connector);
			if (groupUid != null) {
				resourceWasCreated = true;
			}
		}

		if (userUid != null) {
			resourceWasCreated = true;
		}

		Assert.assertEquals(resourceWasCreated, assertParameter);

	}
	
	@Test (expectedExceptions = AlreadyExistsException.class, priority = 1, dependsOnMethods = { CREATEOBJECTTEST },dataProvider = AEEXCEPTIONPROVIDER)
	private void alreadyExistsExceptionTest(String resouceName, Boolean par){
		
	LOGGER.info("## The evaluated test: exceptionTest ");
		SlackSpecificTestUtils.createResourceTestHelper(resouceName, testNumber, connector);
		
	}

	@Test(priority = 2, dataProvider = TESTOBJECTBUILDPROVIDER)
	private void objectBuilderTest(String resourceName, Boolean containsObject) {
		LOGGER.info("## The evaluated test: objectBuilderTest for the resource: {0} ", resourceName);
		ConnectorObject cObject = SlackSpecificTestUtils.connObjectBuildTest(resourceName, testNumber);
		Assert.assertNotNull(cObject);
	}

	@Test(priority = 2, dependsOnMethods = { CREATEOBJECTTEST }, dataProvider = CONSISTENCYTESTPROVIDER)
	private void parameterConsistencyTest(String resourceName, String filterType) {
		LOGGER.info("## The evaluated test: parameterConsistencyTest for the resource: {0} ", resourceName);

		StringBuilder testType = new StringBuilder("createObject");

		List<ConnectorObject> result = new ArrayList<ConnectorObject>();

		OperationOptions options = SlackSpecificTestUtils.getOptions(pageSize, pageOffset);

		result = SlackSpecificTestUtils.filter(filterType, resourceName, testNumber, userUid, groupUid, connector,
				options);

		Map<String, String> evaluationResults = SlackSpecificTestUtils.processResult(result, resourceName,
				testType.toString(), userUid, testNumber);

		for (String attributeName : evaluationResults.keySet()) {

			String nameValue = evaluationResults.get(attributeName);

			Assert.assertEquals(nameValue, attributeName);
		}

	}

	@Test(priority = 6, dependsOnMethods = { CREATEOBJECTTEST }, dataProvider = FILTERMETHODTESTPROVIDER)
	public void filterMethodTest(String filterType, String resourceName) {
		LOGGER.info("## The evaluated test: filterMethodTest for the resource: {0} and filter type: {1}", resourceName,filterType);
		LOGGER.info(
				"Processing trought the \"filter methods test\" for the resource \"{0}\" and the evaluated filter is \"{1}\" ",
				resourceName, filterType);

		List<ConnectorObject> returnedObjects = new ArrayList<ConnectorObject>();

		OperationOptions options = SlackSpecificTestUtils.getOptions(pageSize, pageOffset);

		returnedObjects = SlackSpecificTestUtils.filter(filterType, resourceName, testNumber, userUid, groupUid,
				connector, options);

		Assert.assertFalse(returnedObjects.isEmpty());

	}

	@Test(priority = 5, dependsOnMethods = { CREATEOBJECTTEST }, dataProvider = LISTALLPROVIDER)
	private void listAllTest(String resourceName, int numberOfResources) {
		LOGGER.info("## The evaluated test: listAllTest for the resource: {0} ", resourceName);
		List<ConnectorObject> returnedObjects = new ArrayList<ConnectorObject>();

		OperationOptions options = SlackSpecificTestUtils.getOptions(pageSize, pageOffset);

		returnedObjects = SlackSpecificTestUtils.listAllfromResourcesTestUtil(resourceName, connector, options);

		Assert.assertEquals(returnedObjects.size(), numberOfResources);

	}

	@Test(priority = 3, dependsOnMethods = { CREATEOBJECTTEST }, dataProvider = UPDATEUSERPROVIDER)
	private void updateUserTest(String updateType, Uid uid) {
		LOGGER.info("## The evaluated test: updateUserTest for the update type: {0} ",updateType);
		Uid returnedUid = SlackSpecificTestUtils.updateResourceTest(USERS, updateType, userUid, groupUid, testNumber,
				connector);

		List<ConnectorObject> result = new ArrayList<ConnectorObject>();

		StringBuilder testType = new StringBuilder(UPDATE).append(MDASH).append(updateType);

		OperationOptions options = SlackSpecificTestUtils.getOptions(pageSize, pageOffset);

		result = SlackSpecificTestUtils.filter(UID, USERS, testNumber, userUid, groupUid, connector, options);

		Map<String, String> evaluationResults = SlackSpecificTestUtils.processResult(result, USERS, testType.toString(),
				userUid, testNumber);

		for (String attributeName : evaluationResults.keySet()) {

			String nameValue = evaluationResults.get(attributeName);

			Assert.assertEquals(nameValue, attributeName);
		}

		Assert.assertEquals(uid, returnedUid);

	}

	@Test(priority = 4, dependsOnMethods = { CREATEOBJECTTEST }, dataProvider = UPDATEGROUPPROVIDER)
	private void updateGroupTest(String updateType, Uid uid) {
		LOGGER.info("## The evaluated test: updateGroupTest for the update type: {0} ",updateType);


		Uid returnedUid = SlackSpecificTestUtils.updateResourceTest(GROUPS, updateType, userUid, groupUid, testNumber,
				connector);

		List<ConnectorObject> result = new ArrayList<ConnectorObject>();

		StringBuilder testType = new StringBuilder(UPDATE).append(MDASH).append(updateType);

		OperationOptions options = SlackSpecificTestUtils.getOptions(pageSize, pageOffset);

		result = SlackSpecificTestUtils.filter(UID, GROUPS, testNumber, userUid, groupUid, connector, options);

		Map<String, String> evaluationResults = SlackSpecificTestUtils.processResult(result, GROUPS,
				testType.toString(), userUid, testNumber);

		for (String attributeName : evaluationResults.keySet()) {

			String nameValue = evaluationResults.get(attributeName);

			Assert.assertEquals(nameValue, attributeName);
		}

		Assert.assertEquals(uid, returnedUid);

	}

	@Test(priority = 7, dependsOnMethods = { CREATEOBJECTTEST }, dataProvider = DELETEPROVIDER)
	private void deleteObjectTest(String resourceName, Uid uid) {
		LOGGER.info("## The evaluated test: deleteObjectTest for the resource: {0} ",resourceName);
		List<ConnectorObject> returnedObjects = new ArrayList<ConnectorObject>();

		OperationOptions options = SlackSpecificTestUtils.getOptions(pageSize, pageOffset);

		SlackSpecificTestUtils.deleteResourceTestHelper(resourceName, uid, connector);
		returnedObjects = SlackSpecificTestUtils.filter(UID, resourceName, testNumber, userUid, groupUid, connector,
				options);

		Assert.assertTrue(returnedObjects.isEmpty());

	}
	
	//@Test (expectedExceptions = InvalidCredentialException.class, priority = 7, dependsOnMethods = { "deleteObjectTest" },dataProvider = ICEXCEPTIONPROVIDER)
	private void invalidCredentialExceptionTest(HashMap<String, String> configurationParameters, String resourceName){
		
		Boolean hasToken = configurationParameters.containsKey("token");
		//Boolean hasPassword = configurationParameters.containsKey("password");
		String parameterName;
		if(hasToken){
			parameterName = "token";
		}else {
			LOGGER.info("Password detected, this attribute will be manipulated to trip the tested exception");
			parameterName = "password";
		}
		
		HashMap<String, String> changedConf = configurationParameters;
		String invalidValue= configurationParameters.get(parameterName);
		invalidValue = invalidValue.replaceAll("(?s).", "*");
		changedConf.put(parameterName, invalidValue);
		
		configuration = SlackSpecificTestUtils.buildConfiguration(configurationParameters);
		connector = new ScimConnector();
		connector.init(configuration);
		
		
		LOGGER.info("## The evaluated test: exceptionTest ");
		SlackSpecificTestUtils.createResourceTestHelper(resourceName, testNumber, connector);
		
	}

	public Uid getUid(String resourceName) throws Exception {
		Uid uid = null;

		if (USER.equals(resourceName)) {
			uid = userUid;

		} else if (GROUP.equals(resourceName)) {

			uid = groupUid;
		} else {
			LOGGER.warn("Resource name not defined: {0}", resourceName);

		}

		if (uid == null) {

			throw new Exception("Uid not set");

		}

		return uid;
	}

	@AfterMethod
	private void cleanup(ITestResult result) throws Exception {
		if (result.getStatus() == ITestResult.FAILURE) {

			String methodThatFailed = result.getMethod().getMethodName();

			if ("createObjectTest".equals(methodThatFailed)) {

				if (userUid != null) {
					LOGGER.warn("Atempting to delete resource: {0}", USERS);
					deleteObjectTest(USERS, userUid);
				} else {
					LOGGER.warn(
							"Test failure, uid value of resource \"User\" is null. No resource deletion operation was atempted");
				}
				if (groupUid != null) {
					LOGGER.warn("Atempting to delete resource: {0}", GROUPS);
					deleteObjectTest(GROUPS, groupUid);
				} else

				{
					LOGGER.warn(
							"Test failure, uid value of resource \"Groups\" is null. No resource deletion operation was atempted");
				}

				throw new Exception("Test failure while creating the resource objects, test suite will not continue.");

			} else if ("updateUserResourceObjectTest".equals(methodThatFailed)) {
				if (userUid != null) {
					LOGGER.warn("Atempting to delete resource: {0}", USERS);
					deleteObjectTest(USERS, userUid);
				}

				if (groupUid != null) {
					LOGGER.warn("Atempting to delete resource: {0}", GROUPS);
					deleteObjectTest(GROUPS, groupUid);
				}

				throw new Exception(
						"Test failure while updating the \"User\" resource objects, test suite will not continue.");
			}

		}

	}

	public PropertiesParser getParser() {

		return parser;
	}

}
