package com.evolveum.polygon.test.scim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.SearchResult;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.AttributeFilter;
import org.identityconnectors.framework.common.objects.filter.ContainsAllValuesFilter;
import org.identityconnectors.framework.common.objects.filter.ContainsFilter;
import org.identityconnectors.framework.common.objects.filter.EndsWithFilter;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.common.objects.filter.FilterBuilder;
import org.identityconnectors.framework.common.objects.filter.StartsWithFilter;
import org.identityconnectors.framework.spi.SearchResultsHandler;

import com.evolveum.polygon.scim.ScimConnector;
import com.evolveum.polygon.scim.ScimConnectorConfiguration;


public class TestConfiguration {


	private static Integer testNumber = 45;
	private  Uid userTestUid = null;
	private  Uid groupTestUid = null;
	private static final ArrayList<ConnectorObject> result = new ArrayList<>();
	private static final Log LOGGER = Log.getLog(TestConfiguration.class);

	private static final ObjectClass userClass = ObjectClass.ACCOUNT;
	private static final ObjectClass groupClass = ObjectClass.GROUP;
	private static final ObjectClass entitlementClass = new ObjectClass("Entitlements");
/*	private static Collection <String> mandatoriParameters = new ArrayList<String>();

	static {
		mandatoriParameters.add("authentication");
		mandatoriParameters.add("clientID");
		mandatoriParameters.add("clientSecret");
		mandatoriParameters.add("endpoint");
		mandatoriParameters.add("loginUrl");
		mandatoriParameters.add("password");
		mandatoriParameters.add("service");
		mandatoriParameters.add("userName");
		mandatoriParameters.add("version");

	}
*/

	private static OperationOptions options ;


	ScimConnectorConfiguration scimConnectorConfiguration;
	private static ScimConnector conn;


	public TestConfiguration(HashMap configuration) {
		this.scimConnectorConfiguration= buildConfiguration(configuration);
		this.conn= initConnector(scimConnectorConfiguration);
		this.options =getOptions();
	}

	private static ScimConnectorConfiguration buildConfiguration(HashMap<String,String> configuration){
		ScimConnectorConfiguration scimConnectorConfiguration = new ScimConnectorConfiguration();


		for(String configurationParameter: configuration.keySet()){

			if("clientID".equals(configurationParameter)){
				scimConnectorConfiguration.setClientID(configuration.get(configurationParameter));
			}else if("clientSecret".equals(configurationParameter)){
				scimConnectorConfiguration.setClientSecret(configuration.get(configurationParameter));
			}else if("endpoint".equals(configurationParameter)){
				scimConnectorConfiguration.setEndpoint(configuration.get(configurationParameter));
			}else if("loginUrl".equals(configurationParameter)){
				scimConnectorConfiguration.setLoginURL(configuration.get(configurationParameter));
			}else if("password".equals(configurationParameter)){
				scimConnectorConfiguration.setPassword(configuration.get(configurationParameter));
			}else if("service".equals(configurationParameter)){
				scimConnectorConfiguration.setService(configuration.get(configurationParameter));
			}else if("userName".equals(configurationParameter)){
				scimConnectorConfiguration.setUserName(configuration.get(configurationParameter));
			}else if("version".equals(configurationParameter)){
				scimConnectorConfiguration.setVersion(configuration.get(configurationParameter));
			}else if("authentication".equals(configurationParameter)){
				scimConnectorConfiguration.setAuthentication(configuration.get(configurationParameter));
			}else if("baseurl".equals(configurationParameter)){
				scimConnectorConfiguration.setBaseUrl(configuration.get(configurationParameter));
			}else if("token".equals(configurationParameter)){
				scimConnectorConfiguration.setToken(configuration.get(configurationParameter));
			}
			else{

				LOGGER.warn("Occurrence of an non defined parameter");
			}
		}
		return scimConnectorConfiguration;


	}

	private static Set<Attribute> userCreateBuilder() {

		StringBuilder testAttributeString= new StringBuilder();


		Set<Attribute> attributeSet = new HashSet<Attribute>();

		testAttributeString.append(testNumber.toString()).append("testuser@testdomain.com");

		attributeSet.add(AttributeBuilder.build("userName", testAttributeString.toString()));
		attributeSet.add(AttributeBuilder.build("nickName", testAttributeString.toString()));
		attributeSet.add(AttributeBuilder.build("emails.work.value", testAttributeString.toString()));
		attributeSet.add(AttributeBuilder.build("emails.work.primary",true));
		attributeSet.add(AttributeBuilder.build("nickName", testAttributeString.toString()));

		attributeSet.add(AttributeBuilder.build("title", "Mr."));
		attributeSet.add(AttributeBuilder.build("name.familyName", "User"));
		attributeSet.add(AttributeBuilder.build("name.givenName", "Test"));


		attributeSet.add(AttributeBuilder.build("entitlements.default.value", "00e58000000qvhqAAA"));


		attributeSet.add(AttributeBuilder.build("__ENABLE__", true));

		return attributeSet;
	}

	private static Set<Attribute> userSingleValUpdateBuilder() {

		Set<Attribute> attributeSet = new HashSet<Attribute>();

		attributeSet.add(AttributeBuilder.build("nickName", testNumber.toString()));

		attributeSet.add(AttributeBuilder.build("name.familyName", "TestUpdate"));


		return attributeSet;

	}private static Set<Attribute> userMultiValUpdateBuilder() {

		StringBuilder buildUpdateEmailAdress = new StringBuilder(testNumber.toString()).append("testupdateuser@testdomain.com");
		
		Set<Attribute> attributeSet = new HashSet<Attribute>();

		attributeSet.add(AttributeBuilder.build("emails.work.value", buildUpdateEmailAdress.toString()));
		attributeSet.add(AttributeBuilder.build("emails.work.primary", false));


		return attributeSet;
	}

	private static Set<Attribute> userEnableUpdate() {

		Set<Attribute> attributeSet = new HashSet<Attribute>();

		attributeSet.add(AttributeBuilder.build("__ENABLE__", true));

		return attributeSet;
	}private static Set<Attribute> userDisableUpdate() {

		Set<Attribute> attributeSet = new HashSet<Attribute>();

		attributeSet.add(AttributeBuilder.build("__ENABLE__", false));

		return attributeSet;
	}

	private static Set<Attribute> groupCreateBuilder() {

		StringBuilder testAttributeString = new StringBuilder();

		testAttributeString.append(testNumber.toString()).append("TestGroup");			

		Set<Attribute> attributeSet = new HashSet<Attribute>();

		attributeSet.add(AttributeBuilder.build("displayName", testAttributeString.toString()));
		
		attributeSet.add(AttributeBuilder.build("schemas","urn:scim:schemas:core:1.0"));

		return attributeSet;
	}

	private  Set<Attribute> groupSingleValUpdateBuilder() {

		Set<Attribute> attributeSet = new HashSet<Attribute>();

		attributeSet.add(AttributeBuilder.build("displayName",testNumber.toString()));

		return attributeSet;
	}
	private  Set<Attribute> groupMultiValUpdateBuilder() {
	

		Set<Attribute> attributeSet = new HashSet<Attribute>();


		attributeSet.add(AttributeBuilder.build("members.default.value", userTestUid.getUidValue()));
		return attributeSet;
	}

	private static Set<Attribute> resourceCreateBuilder() {
		Set<Attribute> attr = new HashSet<Attribute>();

		attr.add(AttributeBuilder.build("displayName", "My Custom Test1 Entitlement"));


		return attr;
	}

	public static SearchResultsHandler handler = new SearchResultsHandler() {

		@Override
		public boolean handle(ConnectorObject connectorObject) {
			result.add(connectorObject);
			return true;
		}

		@Override
		public void handleResult(SearchResult result) {
			LOGGER.info("im handling {0}", result.getRemainingPagedResults());

		}
	};

	public static void listAllfromResourcesTestHelper(String resourceName) {
		result.clear();


		if("users".equalsIgnoreCase(resourceName)){
			conn.executeQuery(userClass, null, handler, options);

		}else if("groups".equalsIgnoreCase(resourceName)){
			conn.executeQuery(groupClass, null, handler, options);

		}else if("entitlements".equalsIgnoreCase(resourceName)){

			conn.executeQuery(entitlementClass, null, handler, options);
		}
	}



	public static OperationOptions getOptions(){

		Map<String, Object> operationOptions = new HashMap<String, Object>();

		operationOptions.put("ALLOW_PARTIAL_ATTRIBUTE_VALUES", true);
		operationOptions.put("PAGED_RESULTS_OFFSET", 1);
		operationOptions.put("PAGE_SIZE", 1);

		OperationOptions options = new OperationOptions(operationOptions);

		return options;
	}

	public  void deleteResourceTestHelper(String resourceName) {

		if("users".equalsIgnoreCase(resourceName)){
			conn.delete(userClass, userTestUid, null);

		}else if("groups".equalsIgnoreCase(resourceName)){
			conn.delete(groupClass, groupTestUid, null);

		}else {

			LOGGER.warn("Resource not supported", resourceName);
			throw new ConnectorException("Resource not supported");
		}

	}

	public static Uid createResourceTestHelper(String resourceName) {
		Uid uid= null;

		if("users".equals(resourceName)) {
			uid= conn.create(userClass, userCreateBuilder(), null);
		}else if("groups".equals(resourceName)) {
			uid= conn.create(groupClass, groupCreateBuilder(), null);
		}
		else {
			LOGGER.warn("Non defined resource name provided for resource creation: {0}", resourceName);
		}


		//TODO test negative in salesforce/ free-plan 
		// conn.create(entitlementClass, BuilderTestResource(), null);
		return uid;
	}

	public  Uid updateResourceTestHelper(String resourceName, String updateType) {
		Uid uid= null;

		if("users".equals(resourceName)) {
			if("single".equals(updateType)){

				uid =conn.update(userClass, userTestUid, userSingleValUpdateBuilder(), null);
			}else if("multi".equals(updateType)){

				uid =conn.update(userClass, userTestUid, userMultiValUpdateBuilder(), null);

			}else if("enabled".equals(updateType)){

				uid =conn.update(userClass, userTestUid, userEnableUpdate(), null);

			}
			else if("disabled".equals(updateType)){

				uid =conn.update(userClass, userTestUid, userDisableUpdate(), null);

			}

		}else if("groups".equals(resourceName)) {
			if("single".equals(updateType)){

				uid= conn.update(groupClass, groupTestUid, groupSingleValUpdateBuilder(), null);
			}else if("multi".equals(updateType)){
				uid= conn.update(groupClass, groupTestUid, groupMultiValUpdateBuilder(), null);

			}
		}
		else {
			LOGGER.warn("Non defined resource name provided for resource creation: {0}", resourceName);
		}
		return uid;

		//conn.update(userClass, TEST_UID, updateTestUser(), null);
		//conn.update(groupClass, BLANC_TEST_UID, BuilderTestGroup(), null);
		// conn.update(entitlementClass, ,attr, null);

	}

	public  Uid addAttributeValuesTestHelper(String resourceName) {
		Uid uid= null;

		if("users".equals(resourceName)) {
			uid =conn.update(userClass, userTestUid, userSingleValUpdateBuilder(), null);
		}else if("groups".equals(resourceName)) {
			uid= conn.update(groupClass, groupTestUid, groupCreateBuilder(), null);
		} 
		else {
			LOGGER.warn("Non defined resource name provided for resource creation: {0}", resourceName);
		}
		return uid;

		//conn.update(userClass, TEST_UID, updateTestUser(), null);
		//conn.update(groupClass, BLANC_TEST_UID, BuilderTestGroup(), null);
		// conn.update(entitlementClass, ,attr, null);

	}

	public  Uid removeAttributeValuesTestHelper(String resourceName) {
		Uid uid= null;

		if("users".equals(resourceName)) {
			uid =conn.update(userClass, userTestUid, userSingleValUpdateBuilder(), null);
		}else if("groups".equals(resourceName)) {
			uid= conn.update(groupClass, groupTestUid, groupCreateBuilder(), null);
		} 
		else {
			LOGGER.warn("Non defined resource name provided for resource creation: {0}", resourceName);
		}
		return uid;

		//conn.update(userClass, TEST_UID, updateTestUser(), null);
		//conn.update(groupClass, BLANC_TEST_UID, BuilderTestGroup(), null);
		// conn.update(entitlementClass, ,attr, null);

	}

	public  void filterMethodsTest(String filterType, String resourceName) {
		result.clear();

		Filter filter = getFilter(filterType,resourceName);

		try {
			if("users".equalsIgnoreCase(resourceName)){
				conn.executeQuery(userClass, filter, handler, options);
			}else if("groups".equalsIgnoreCase(resourceName)){
				conn.executeQuery(groupClass, filter, handler, options);
			}else if("entitlements".equalsIgnoreCase(resourceName)){
				conn.executeQuery(entitlementClass, filter, handler, options);
			}
		} catch (Exception e) {
			LOGGER.warn("An exception has ocoured while processing the filter method test: {0}", e.getMessage());;
		}
	}

	private static ScimConnector initConnector(ScimConnectorConfiguration conf) {
		ScimConnector conn = new ScimConnector();

		conn.init(conf);
		conn.schema();

		return conn;
	}

	private  AttributeFilter getFilter(String filterType, String resourceName){
		AttributeFilter filter = null ;

		if ("contains".equalsIgnoreCase(filterType)){
			if ("users".equals(resourceName)){
				filter = (ContainsFilter) FilterBuilder
						.contains(AttributeBuilder.build("userName", testNumber.toString()));
			}else if("groups".equals(resourceName)){

				filter = (ContainsFilter) FilterBuilder
						.contains(AttributeBuilder.build("displayName", testNumber.toString()));
			}
		}else if ("equals".equalsIgnoreCase(filterType)){
			if ("users".equals(resourceName)){
				filter = (EqualsFilter) FilterBuilder
						.equalTo(AttributeBuilder.build("userName", testNumber.toString()));
			}
			else if("groups".equals(resourceName)){
				filter = (EqualsFilter) FilterBuilder
						.equalTo(AttributeBuilder.build("displayName", testNumber.toString()));
			}
		}else if ("uid".equalsIgnoreCase(filterType)){
			if ("users".equals(resourceName)){
				filter = (EqualsFilter) FilterBuilder.equalTo(userTestUid);
			}else if("groups".equals(resourceName)){
				filter = (EqualsFilter) FilterBuilder.equalTo(groupTestUid); 
			}
		}else if ("startswith".equalsIgnoreCase(filterType)){
			if ("users".equals(resourceName)){
				
				filter = (StartsWithFilter)
						FilterBuilder.startsWith(AttributeBuilder.build("userName",testNumber.toString()));
			}else if("groups".equals(resourceName)){
				
				filter = (StartsWithFilter)
						FilterBuilder.startsWith(AttributeBuilder.build("displayName",testNumber.toString()));
			}
			// TODO not working with salesforce  {"Errors":[{"description":"Unsupported Operator : ew","code":400}]}
		}else if ("endswith".equalsIgnoreCase(filterType)){
			if ("users".equals(resourceName)){
				filter =
						(EndsWithFilter)FilterBuilder.endsWith(AttributeBuilder.build("userName","testdomain.com")); 
			}else if("groups".equals(resourceName)){
				filter =
						(EndsWithFilter)FilterBuilder.endsWith(AttributeBuilder.build("displayName",testNumber.toString())); 
			}
		}else if ("containsall".equalsIgnoreCase(filterType)){
			if("groups".equals(resourceName)){
				filter =
						(ContainsAllValuesFilter)FilterBuilder.containsAllValues(AttributeBuilder.build("members.default.value",userTestUid.getUidValue() ));
			}
		}

		return filter;
	}

	public ArrayList<ConnectorObject> getHandlerResult(){

		return this.result;
	}

	public boolean isConfigurationValid(){


		try {
			scimConnectorConfiguration.validate();
		} catch (Exception e) {
			return false; 
		}
		return true;
	}

	public Set<Attribute> getAttributeSet(String resourceName){

		Set<Attribute> attributeSet= new HashSet<>();

		if ("users".equals(resourceName)){
			attributeSet = userCreateBuilder();

		}else if ("groups".equals(resourceName)){

			attributeSet = groupCreateBuilder();
		}


		return attributeSet;
	}

	public  void setGroupTestUid(Uid groupUid){
		groupTestUid = groupUid;

	}

	public  void setUserTestUid(Uid userUid){

		userTestUid = userUid;
	}

}


