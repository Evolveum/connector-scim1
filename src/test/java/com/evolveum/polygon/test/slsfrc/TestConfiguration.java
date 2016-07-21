package com.evolveum.polygon.test.slsfrc;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.SearchResult;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.AttributeFilter;
import org.identityconnectors.framework.common.objects.filter.ContainsFilter;
import org.identityconnectors.framework.common.objects.filter.EndsWithFilter;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.identityconnectors.framework.common.objects.filter.FilterBuilder;
import org.identityconnectors.framework.common.objects.filter.StartsWithFilter;
import org.identityconnectors.framework.spi.SearchResultsHandler;

import com.evolveum.polygon.scim.ScimConnector;
import com.evolveum.polygon.scim.ScimConnectorConfiguration;


	public class TestConfiguration {

		private static final Uid TEST_UID = new Uid("00e58000000cqxLAAQ");
		private static final Uid BLANC_TEST_UID = null;
		private static final ArrayList<ConnectorObject> result = new ArrayList<>();
		private static final Log LOGGER = Log.getLog(TestConfiguration.class);

		private static final ObjectClass userClass = ObjectClass.ACCOUNT;
		private static final ObjectClass groupClass = ObjectClass.GROUP;
		private static final ObjectClass entitlementClass = new ObjectClass("Entitlements");
		private static Collection <String> mandatoriParameters = new ArrayList<String>();
		
		static {
			
			mandatoriParameters.add("clientID");
			mandatoriParameters.add("clientSecret");
			mandatoriParameters.add("endpoint");
			mandatoriParameters.add("loginUrl");
			mandatoriParameters.add("password");
			mandatoriParameters.add("service");
			mandatoriParameters.add("userName");
			mandatoriParameters.add("version");
			
		}
		
		
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
			}
			else{
				
				LOGGER.warn("Ocourance of an non defined parameter");
			}
			}
			return scimConnectorConfiguration;
			
		
		}

		private static Set<Attribute> GenericBuilderTest() {

			// Setting up attribute
			Set<Attribute> attr = new HashSet<Attribute>();

			// Map for Maultivalue attribute name

			Map<String, Map<String, Object>> nameMap = new HashMap<String, Map<String, Object>>();
			Map<String, Object> names = new HashMap<String, Object>();

			// Map for Maultivalue attribute schema extension

			Map<String, Map<String, Object>> extensionMap = new HashMap<String, Map<String, Object>>();
			Map<String, Object> extensionAtributes = new HashMap<String, Object>();

			// Map for multilayered attribute Email
			Map<String, Collection<Map<String, Object>>> emailMap = new HashMap<String, Collection<Map<String, Object>>>();
			Map<String, Object> emailAttribute1 = new HashMap<String, Object>();
			Map<String, Object> emailAttribute2 = new HashMap<String, Object>();

			// Map for multilayered attribute phoneNumbers
			Map<String, Collection<Map<String, Object>>> phoneMap = new HashMap<String, Collection<Map<String, Object>>>();
			Map<String, Object> phoneAttribute1 = new HashMap<String, Object>();
			Map<String, Object> phoneAttribute2 = new HashMap<String, Object>();

			// Map for multilayered attribute entitlements
			Map<String, Collection<Map<String, Object>>> entitlementMap = new HashMap<String, Collection<Map<String, Object>>>();
			Map<String, Object> entitlementAttribute1 = new HashMap<String, Object>();
			Map<String, Object> entitlementAttribute2 = new HashMap<String, Object>();

			// Name
			names.put("formatted", "Harry Potter");
			names.put("familyName", "Potter");
			names.put("givenName", "Harry");

			nameMap.put("name", names);

			// extension ########## Enterprise extension
			extensionAtributes.put("organization", "00D58000000YfgfEAC");

			extensionMap.put("urn:scim:schemas:extension:enterprise:1.0", extensionAtributes);

			// Email
			emailMap.put("emails", new ArrayList<Map<String, Object>>());
			emailMap.get("emails").add(emailAttribute1);

			emailAttribute1.put("type", "home");

			emailAttribute1.put("value", "someone@hometest554xz.com");

			emailAttribute2.put("primary", true);

			emailAttribute2.put("type", "work");

			emailAttribute2.put("value", "someone@hometest554xz.com");

			// Entitlements

			entitlementMap.put("entitlements", new ArrayList<Map<String, Object>>());
			entitlementMap.get("entitlements").add(entitlementAttribute1);

			entitlementAttribute1.put("display", "Custom: Support Profile");

			entitlementAttribute1.put("value", "00e58000000qvhqAAA");

			entitlementAttribute1.put("primary", true);

			// Phone

			phoneMap.put("phoneNumbers", new ArrayList<Map<String, Object>>());
			phoneMap.get("phoneNumbers").add(phoneAttribute1);
			phoneMap.get("phoneNumbers").add(phoneAttribute2);

			phoneAttribute1.put("type", "mobile");

			phoneAttribute1.put("value", "+421 910039218");

			phoneAttribute2.put("type", "work");

			phoneAttribute2.put("value", "+421 915039218");

			// Attribute
			attr.add(AttributeBuilder.build("layeredAttrribute", emailMap));
			attr.add(AttributeBuilder.build("layeredAttrribute", phoneMap));
			attr.add(AttributeBuilder.build("layeredAttrribute", entitlementMap));
			attr.add(AttributeBuilder.build("multiValueAttrribute", nameMap));
			attr.add(AttributeBuilder.build("multiValueAttrribute", extensionMap));
			attr.add(AttributeBuilder.build("nickName", "HP"));
			attr.add(AttributeBuilder.build("userName", "harryp0234@hogwarts.com"));

			return attr;
		}

		private static Set<Attribute> BuilderTestUser() {

			Set<Attribute> attributeSet = new HashSet<Attribute>();

			attributeSet.add(AttributeBuilder.build("userName", "sixthTestUser@ectestdomain.com"));

			attributeSet.add(AttributeBuilder.build("nickName", "sixthTestUser"));

			attributeSet.add(AttributeBuilder.build("emails.work.value", "sixthTestUser@ectestdomain.com"));
			attributeSet.add(AttributeBuilder.build("emails.work.primary", true));
			

			attributeSet.add(AttributeBuilder.build("name.formatted", "Test Sixth"));
			attributeSet.add(AttributeBuilder.build("name.familyName", "Sixth"));
			attributeSet.add(AttributeBuilder.build("name.givenName", "Test"));
		
			

			attributeSet.add(AttributeBuilder.build("entitlements.default.value", "00e58000000cqxLAAQ"));
			

			attributeSet.add(AttributeBuilder.build("__ENABLE__", true));

			return attributeSet;
		}

		private static Set<Attribute> BuilderTestGroup() {

			Set<Attribute> attributeSet = new HashSet<Attribute>();

			attributeSet.add(AttributeBuilder.build("displayName", "firstTestGroup"));
			attributeSet.add(AttributeBuilder.build("members.User.value", "firstTestUser@ectestdomain.com"));
			//attributeSet.add(AttributeBuilder.build("members.User.display", "teest@eastcubattor1.com"));
			return attributeSet;
		}
		
		private static Set<Attribute> BuilderTestResource() {
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

		public static void listAllfromResources(String resourceName) {
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

		public static void deleteResourceTest(Uid uid, String resourceName) {
			
			if("users".equalsIgnoreCase(resourceName)){
				conn.delete(userClass, uid, null);
				
			}else if("groups".equalsIgnoreCase(resourceName)){
				conn.delete(groupClass, uid, null);
				
			}else if("entitlements".equalsIgnoreCase(resourceName)){
				conn.delete(entitlementClass, uid, null);
				
			}

		}

		public static Uid createResourceTest(String resourceName) {
			Uid uid= null;
			
			if("users".equals(resourceName)) {
			uid= conn.create(userClass, BuilderTestUser(), null);
			}else if("groups".equals(resourceName)) {
				uid= conn.create(groupClass, BuilderTestGroup(), null);
			}
			else {
				LOGGER.warn("Non defined resource name provided for resource creation: {0}", resourceName);
			}
			
		
			//TODO test negative in salesforce/ free-plan 
			// conn.create(entitlementClass, BuilderTestResource(), null);
			return uid;
		}

		private static void updateResourceTest() {

			conn.update(userClass, TEST_UID, BuilderTestUser(), null);
			//conn.update(groupClass, BLANC_TEST_UID, BuilderTestGroup(), null);
			// conn.update(entitlementClass, ,attr, null);

		}

		public static void filterMethodsTest(AttributeFilter filter, String resourceName) {
			result.clear();

			if("users".equalsIgnoreCase(resourceName)){
		 conn.executeQuery(userClass, filter, handler, options);
			}else if("groups".equalsIgnoreCase(resourceName)){
		 conn.executeQuery(groupClass, filter, handler, options);
			}if("entitlements".equalsIgnoreCase(resourceName)){
			conn.executeQuery(entitlementClass, filter, handler, options);
			}
		}

		private static ScimConnector initConnector(ScimConnectorConfiguration conf) {
			ScimConnector conn = new ScimConnector();

			conn.init(conf);
			conn.schema();

			return conn;
		}
		
		public AttributeFilter getFilter(String filterName, String leftAttribute, Object rigthAttribute){
			AttributeFilter filter = null ;
			
			
			
			if ("contains".equalsIgnoreCase(filterName)){
			 filter = (ContainsFilter) FilterBuilder
					.contains(AttributeBuilder.build((String)leftAttribute, rigthAttribute));
			}else if ("equals".equalsIgnoreCase(filterName)){
				 filter = (EqualsFilter) FilterBuilder
						.equalTo(AttributeBuilder.build((String)leftAttribute, rigthAttribute));
			}else if ("uid".equalsIgnoreCase(filterName)){
				 filter = (EqualsFilter) FilterBuilder.equalTo((Uid)rigthAttribute);
			}else if ("startswith".equalsIgnoreCase(filterName)){
				 filter = (StartsWithFilter)
						FilterBuilder.startsWith(AttributeBuilder.build((String)leftAttribute));
			}else if ("endswith".equalsIgnoreCase(filterName)){
				
				 filter =
						(EndsWithFilter)FilterBuilder.endsWith(AttributeBuilder.build((String)leftAttribute));
			}
			
			

			//OrFilter orFilterTest = (OrFilter) FilterBuilder.or(eq, ct);

			//AndFilter andFilterTest = (AndFilter) FilterBuilder.and(con, con);

			//NotFilter notFilterTest= (NotFilter)FilterBuilder.not(eq);

			
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
				attributeSet = BuilderTestUser();
				
			}else if ("groups".equals(resourceName)){
				
				attributeSet = BuilderTestGroup();
			}
			
			
			return attributeSet;
		}

	}


