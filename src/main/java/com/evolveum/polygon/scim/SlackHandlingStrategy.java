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
package com.evolveum.polygon.scim;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.util.EntityUtils;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.AlreadyExistsException;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ObjectClassInfoBuilder;
import org.identityconnectors.framework.common.objects.OperationalAttributeInfos;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.filter.AttributeFilter;
import org.identityconnectors.framework.common.objects.filter.ContainsAllValuesFilter;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * 
 * @author Macik
 *
 *         The "HandlingStrategy" methods implementation for the "Slack"
 *         service.
 *
 */

public class SlackHandlingStrategy extends StandardScimHandlingStrategy implements HandlingStrategy {

	private static final Log LOGGER = Log.getLog(SlackHandlingStrategy.class);
	private static final String SCHEMAVALUE = "urn:scim:schemas:core:1.0";
	private static final String NICKNAME = "nickName";
	private static final String USERNAME = "userName";
	private static final String TITLE = "title";
	private static final String SCHEMAS = "schemas";
	private static final String PROFILEURL = "profileUrl";
	private static final String TIMEZONE = "timezone";
	private static final String EXTERNALID = "externalId";
	private static final String PHOTOS = "photos";
	private static final String READONLY = "readOnly";
	private static final String SCHEMA = "schema";
	private static final String REQUIRED = "required";
	private static final String CASEEXSACT = "caseExact";
	private static final String STRING = "string";

	private static final String EMAILSDEFAULTVALUE = "emails.default.value";
	private static final String EMAILSDEFAULTPRIMARY = "emails.default.primary";
	private static final String ATTRIBUTES = "attributes";
	private static final String GROUPS = "groups";

	@Override
	public JSONObject injectMissingSchemaAttributes(String resourceName, JSONObject jsonObject) {

		LOGGER.warn("Processing trough slack missing schema attributes workaround for the resource: \"{0}\"",
				resourceName);
		if (USERS.equals(resourceName)) {

			Map<String, String> missingAttributes = new HashMap<String, String>();
			missingAttributes.put(USERNAME, USERNAME);
			missingAttributes.put(NICKNAME, NICKNAME);
			missingAttributes.put(TITLE, TITLE);
			missingAttributes.put(SCHEMAS, SCHEMAS);
			missingAttributes.put(PROFILEURL, PROFILEURL);
			missingAttributes.put(DISPLAYNAME, DISPLAYNAME);
			missingAttributes.put(TIMEZONE, TIMEZONE);
			missingAttributes.put(EXTERNALID, EXTERNALID);
			missingAttributes.put(ACTIVE, ACTIVE);
			missingAttributes.put(PHOTOS, PHOTOS);

			if (jsonObject.has(ATTRIBUTES)) {

				JSONArray attributesArray = new JSONArray();

				attributesArray = jsonObject.getJSONArray(ATTRIBUTES);
				for (int indexValue = 0; indexValue < attributesArray.length(); indexValue++) {
					JSONObject subAttributes = attributesArray.getJSONObject(indexValue);

					if (subAttributes.has(NAME)) {

						if (USERNAME.equals(subAttributes.get(NAME))) {

							missingAttributes.remove(USERNAME);
						} else if (NICKNAME.equals(subAttributes.get(NAME))) {

							missingAttributes.remove(NICKNAME);
						} else if (TITLE.equals(subAttributes.get(NAME))) {

							missingAttributes.remove(TITLE);
						} else if (SCHEMAS.equals(subAttributes.get(NAME))) {

							missingAttributes.remove(SCHEMAS);
						} else if (PROFILEURL.equals(subAttributes.get(NAME))) {

							missingAttributes.remove(PROFILEURL);
						} else if (DISPLAYNAME.equals(subAttributes.get(NAME))) {

							missingAttributes.remove(DISPLAYNAME);
						} else if (TIMEZONE.equals(subAttributes.get(NAME))) {

							missingAttributes.remove(TIMEZONE);
						} else if (EXTERNALID.equals(subAttributes.get(NAME))) {

							missingAttributes.remove(EXTERNALID);
						} else if (ACTIVE.equals(subAttributes.get(NAME))) {

							missingAttributes.remove(ACTIVE);
						} else if (PHOTOS.equals(subAttributes.get(NAME))) {

							missingAttributes.remove(PHOTOS);
						}
					}
				}

				for (String missingAttributeName : missingAttributes.keySet()) {

					LOGGER.warn("Building schema definition for the attribute: \"{0}\"", missingAttributeName);

					if (USERNAME.equals(missingAttributeName)) {
						JSONObject userName = new JSONObject();

						userName.put(SCHEMA, SCHEMAVALUE);
						userName.put(NAME, USERNAME);
						userName.put(READONLY, false);
						userName.put(TYPE, STRING);
						userName.put(REQUIRED, true);
						userName.put(CASEEXSACT, false);

						attributesArray.put(userName);

					} else if (ACTIVE.equals(missingAttributeName)) {
						JSONObject active = new JSONObject();

						active.put(SCHEMA, SCHEMAVALUE);
						active.put(NAME, ACTIVE);
						active.put(READONLY, false);
						active.put(TYPE, "boolean");
						active.put(REQUIRED, false);
						active.put(CASEEXSACT, false);

						attributesArray.put(active);

					} else if (EXTERNALID.equals(missingAttributeName)) {
						JSONObject externalId = new JSONObject();

						externalId.put(SCHEMA, SCHEMAVALUE);
						externalId.put(NAME, EXTERNALID);
						externalId.put(READONLY, false);
						externalId.put(TYPE, STRING);
						externalId.put(REQUIRED, false);
						externalId.put(CASEEXSACT, true);

						attributesArray.put(externalId);

					} else if (TIMEZONE.equals(missingAttributeName)) {
						JSONObject timezone = new JSONObject();

						timezone.put(SCHEMA, SCHEMAVALUE);
						timezone.put(NAME, TIMEZONE);
						timezone.put(READONLY, false);
						timezone.put(TYPE, STRING);
						timezone.put(REQUIRED, false);
						timezone.put(CASEEXSACT, false);

						attributesArray.put(timezone);

					} else if (DISPLAYNAME.equals(missingAttributeName)) {
						JSONObject displayName = new JSONObject();

						displayName.put(SCHEMA, SCHEMAVALUE);
						displayName.put(NAME, DISPLAYNAME);
						displayName.put(READONLY, false);
						displayName.put(TYPE, STRING);
						displayName.put(REQUIRED, false);
						displayName.put(CASEEXSACT, false);

						attributesArray.put(displayName);

					} else if (PROFILEURL.equals(missingAttributeName)) {
						JSONObject profileUrl = new JSONObject();

						profileUrl.put(SCHEMA, SCHEMAVALUE);
						profileUrl.put(NAME, PROFILEURL);
						profileUrl.put(READONLY, false);
						profileUrl.put(TYPE, STRING);
						profileUrl.put(REQUIRED, false);
						profileUrl.put(CASEEXSACT, false);

						attributesArray.put(profileUrl);

					} else if (NICKNAME.equals(missingAttributeName)) {
						JSONObject nickName = new JSONObject();

						nickName.put(SCHEMA, SCHEMAVALUE);
						nickName.put(NAME, NICKNAME);
						nickName.put(READONLY, false);
						nickName.put(TYPE, STRING);
						nickName.put(REQUIRED, true);
						nickName.put(CASEEXSACT, false);

						attributesArray.put(nickName);

					} else if (TITLE.equals(missingAttributeName)) {
						JSONObject title = new JSONObject();

						title.put(SCHEMA, SCHEMAVALUE);
						title.put(NAME, TITLE);
						title.put(READONLY, false);
						title.put(TYPE, STRING);
						title.put(REQUIRED, false);
						title.put(CASEEXSACT, false);

						attributesArray.put(title);
					} else if (SCHEMAS.equals(missingAttributeName)) {
						JSONObject schemas = new JSONObject();
						JSONArray subAttributeArray = new JSONArray();

						JSONObject valueBlank = new JSONObject();

						schemas.put(SCHEMA, SCHEMAVALUE);
						schemas.put(NAME, SCHEMAS);
						schemas.put(READONLY, false);
						schemas.put(TYPE, "complex");
						schemas.put(MULTIVALUED, true);
						schemas.put(REQUIRED, false);
						schemas.put(CASEEXSACT, false);

						valueBlank.put(NAME, "blank");
						valueBlank.put(READONLY, true);
						valueBlank.put(REQUIRED, false);
						valueBlank.put(MULTIVALUED, false);

						subAttributeArray.put(valueBlank);

						schemas.put(SUBATTRIBUTES, subAttributeArray);

						attributesArray.put(schemas);
					}

				}

				jsonObject.put(ATTRIBUTES, attributesArray);
			}

		}
		return jsonObject;

	}

	@Override
	public List<String> defineExcludedAttributes() {

		List<String> excludedList = new ArrayList<String>();
		excludedList.add("emails");

		return excludedList;
	}

	@Override
	public List<Map<String, Map<String, Object>>> getAttributeMapList(
			List<Map<String, Map<String, Object>>> attributeMapList) {

		if (!attributeMapList.isEmpty()) {

			for (int position = 0; position < attributeMapList.size(); position++) {
				Map<String, Map<String, Object>> resources = attributeMapList.get(position);

				if (resources.containsKey(USERNAME) && !resources.containsKey(EMAILSDEFAULTPRIMARY)
						&& !resources.containsKey(EMAILSDEFAULTVALUE)) {

					resources.put(EMAILSDEFAULTPRIMARY, null);
					resources.put(EMAILSDEFAULTVALUE, null);

					attributeMapList.remove(position);
					attributeMapList.add(position, resources);
				}

			}
		}
		return attributeMapList;

	}

	@Override
	public Set<Attribute> addAttributesToInject(Set<Attribute> injectedAttributeSet) {
		Attribute schemaAttribute = AttributeBuilder.build("schemas.default.blank", SCHEMAVALUE);
		injectedAttributeSet.add(schemaAttribute);
		return injectedAttributeSet;
	}

	@Override
	public List<String> excludeFromAssembly(List<String> excludedAttributes) {
		excludedAttributes.add(META);
		excludedAttributes.add("schemas");
		excludedAttributes.add(PHOTOS);

		return excludedAttributes;
	}

	@Override
	public StringBuilder processContainsAllValuesFilter(String p, ContainsAllValuesFilter filter,
			FilterHandler handler) {
		return null;
	}

	/**
	 * 
	 * Extends the "AttributeInfoBuilder" parameter which was provided with an
	 * attribute which this method builds.
	 * 
	 * @param builder
	 * @param attributeName
	 * @param infoBuilder
	 * @return
	 */

	private ObjectClassInfoBuilder buildMissingAttributes(ObjectClassInfoBuilder builder, String attributeName,
			AttributeInfoBuilder infoBuilder) {

		if (EMAILSDEFAULTVALUE.equals(attributeName)) {
			infoBuilder.setMultiValued(true);
			infoBuilder.setRequired(true);
			infoBuilder.setType(String.class);
			builder.addAttributeInfo(infoBuilder.build());
		} else {
			infoBuilder.setMultiValued(false);
			infoBuilder.setRequired(true);
			infoBuilder.setType(Boolean.class);
			builder.addAttributeInfo(infoBuilder.build());
		}

		return builder;

	}

	@Override
	public ObjectClassInfoBuilder schemaObjectInjection(ObjectClassInfoBuilder builder, String attributeName,
			AttributeInfoBuilder infoBuilder) {

		if (ACTIVE.equals(attributeName)) {
			builder = builder.addAttributeInfo(OperationalAttributeInfos.ENABLE);
			builder = builder.addAttributeInfo(OperationalAttributeInfos.PASSWORD);
		} else {
			builder = buildMissingAttributes(builder, attributeName, infoBuilder);
		}

		return builder;
	}

	@Override
	public List<String> populateDictionary(WorkaroundFlags flag) {
		List<String> dictionary = new ArrayList<String>();

		if (WorkaroundFlags.PARSERFLAG.getValue().equals(flag.getValue())) {
			dictionary.add(SUBATTRIBUTES);
			dictionary.add("subattributes");
		} else if (WorkaroundFlags.BUILDERFLAG.getValue().equals(flag.getValue())) {

			dictionary.add(ACTIVE);
			dictionary.add(EMAILSDEFAULTVALUE);
			dictionary.add(EMAILSDEFAULTPRIMARY);
		} else {

			LOGGER.warn("No such flag defined: {0}", flag);
		}

		return dictionary;
	}

	@Override
	public Boolean checkFilter(Filter filter, String endpointName) {

		LOGGER.info("Checking filter if contains all values handling is needed ");

		if (endpointName.equals("Groups")) {
			if (filter instanceof EqualsFilter || filter instanceof ContainsAllValuesFilter) {

				Attribute filterAttr = ((AttributeFilter) filter).getAttribute();

				String attributeName = filterAttr.getName();

				LOGGER.info("The attribute: {0}", attributeName);
				LOGGER.info("The attribute value: {0}", filterAttr);

				if ("members.default.value".equals(attributeName)) {
					LOGGER.warn("Processing trough group object class \"equals\" filter workaround.");
					return true;
				} else {

					return false;
				}
			}

		}
		return false;
	}

	@Override
	public void handleCAVGroupQuery(JSONObject jsonObject, String resourceEndPoint, ResultsHandler handler,
			String scimBaseUri, Header authHeader, ScimConnectorConfiguration conf) throws ClientProtocolException, IOException {
		String responseString = null;
		HttpClientBuilder httpClientBulder = HttpClientBuilder.create();

		if (StringUtil.isNotEmpty(conf.getProxyUrl())) {
			HttpHost proxy = new HttpHost(conf.getProxyUrl(), conf.getProxyPortNumber());
			DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);
			httpClientBulder.setRoutePlanner(routePlanner);
		}

		HttpClient httpClient = httpClientBulder.build();

		if (jsonObject.has(GROUPS)) {
			int amountOfResources = jsonObject.getJSONArray(GROUPS).length();

			for (int position = 0; position < amountOfResources; position++) {
				JSONObject minResourceJson = new JSONObject();
				minResourceJson = jsonObject.getJSONArray(GROUPS).getJSONObject(position);
				if (minResourceJson.has(VALUE)) {

					String groupUid = minResourceJson.getString(VALUE);
					if (groupUid != null && !groupUid.isEmpty()) {

						StringBuilder groupUri = new StringBuilder(scimBaseUri).append(SLASH).append(resourceEndPoint)
								.append(SLASH).append(groupUid);

						LOGGER.info("The uri to which we are sending the queri {0}", groupUri);

						HttpGet httpGetR = buildHttpGet(groupUri.toString(), authHeader);

						try (CloseableHttpResponse resourceResponse = (CloseableHttpResponse) httpClient
								.execute(httpGetR)) {
							int statusCode = resourceResponse.getStatusLine().getStatusCode();
							responseString = EntityUtils.toString(resourceResponse.getEntity());
							if (statusCode == 200) {
								JSONObject fullResourceJson = new JSONObject(responseString);

								LOGGER.info(
										"The {0}. resource json object which was returned by the service provider: {1}",
										position + 1, fullResourceJson.toString(1));

								ConnectorObject connectorObject = buildConnectorObject(fullResourceJson,
										resourceEndPoint);
								handler.handle(connectorObject);

							} else {
								ErrorHandler.onNoSuccess(responseString, statusCode, groupUri.toString());
							}
						}
					}
				} else {
					LOGGER.error("No uid present in fetched object: {0}", minResourceJson);

					throw new ConnectorException("No uid present in fetched object while processing query result");
				}
			}
		} else {

			LOGGER.error("Resource object not present in provider response to the query");

			throw new ConnectorException("No uid present in fetched object while processing query result");
		}

	}
	@Override
	public void handleBadRequest(String error){
		List<String> uniqueAttributes = new	ArrayList<String>();
		uniqueAttributes.add("nickname_taken");
		uniqueAttributes.add("bad_email_address");
		
		String [] parts = error.split("\"");
		for(String part: parts){
			if (uniqueAttributes.contains(part)){
				StringBuilder errorBuilder = new StringBuilder("Conflict. ").append(error).append(". Propably the value you have chosen is already taken, please chose another and try again.");
						throw new AlreadyExistsException(errorBuilder.toString());
			} 
		}	
			throw new ConnectorException(error);
	}

}
