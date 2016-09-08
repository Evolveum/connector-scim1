package com.evolveum.polygon.scim;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.ConnectorIOException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ObjectClassInfoBuilder;
import org.identityconnectors.framework.common.objects.OperationalAttributeInfos;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.SearchResult;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.ContainsAllValuesFilter;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.spi.SearchResultsHandler;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class StandardScimHandlingStrategy implements HandlingStrategy {

	private static final Log LOGGER = Log.getLog(StandardScimHandlingStrategy.class);
	private static final String TYPE = "type";
	private static final String DEFAULT = "default";
	private static final String SUBATTRIBUTES = "subAttributes";
	private static final String MULTIVALUED = "multiValued";
	private static final String CANONICALVALUES = "canonicalValues";
	private static final String REFERENCETYPES = "referenceTypes";

	@Override
	public Uid groupUpdateProcedure(HttpResponse response, JSONObject jsonObject, String uri, Header authHeader,
			CrudManagerScim manager) {
		try {
			manager.onNoSuccess(response, "updating object");
		} catch (ParseException e) {

			LOGGER.error("An exception has occurred while parsing the http response : {0}", e.getLocalizedMessage());
			LOGGER.info("An exception has occurred while parsing the http response : {0}", e);

			throw new ConnectorException("An exception has occurred while parsing the http response : {0}", e);

		} catch (IOException e) {

			LOGGER.error(
					"An error has occurred while processing the http response. Occurrence in the process of updating a resource object: {0}",
					e.getLocalizedMessage());
			LOGGER.info(
					"An error has occurred while processing the http response. Occurrence in the process of creating a resource object: {0}",
					e);

			throw new ConnectorIOException(
					"An error has occurred while processing the http response. Occurrence in the process of creating a resource object",
					e);

		}
		return null;
	}

	@Override
	public StringBuilder processContainsAllValuesFilter(String p, ContainsAllValuesFilter filter,
			FilterHandler handler) {
		StringBuilder preprocessedFilter = null;
		preprocessedFilter = handler.processArrayQ(filter, p);
		return preprocessedFilter;
	}

	@Override
	public ObjectClassInfoBuilder schemaBuilder(String attributeName, Map<String, Map<String, Object>> attributeMap,
			ObjectClassInfoBuilder builder, SchemaObjectBuilderGeneric schemaBuilder) {

		AttributeInfoBuilder infoBuilder = new AttributeInfoBuilder(attributeName);
		Boolean containsDictionaryValue = false;

		List<String> dictionary = populateDictionary("schemabuilder-workaround");

		if (dictionary.contains(attributeName)) {
			containsDictionaryValue = true;
		}

		if (!containsDictionaryValue) {
			dictionary.clear();
			Map<String, Object> schemaSubPropertiesMap = new HashMap<String, Object>();
			schemaSubPropertiesMap = attributeMap.get(attributeName);

			for (String subPropertieName : schemaSubPropertiesMap.keySet()) {
				containsDictionaryValue = false;
				dictionary = populateDictionary("schemaparser-workaround");
				if (dictionary.contains(subPropertieName)) {
					containsDictionaryValue = true;
				}

				if (containsDictionaryValue) {
					// TODO check positive cases
					infoBuilder = new AttributeInfoBuilder(attributeName);
					JSONArray jsonArray = new JSONArray();

					jsonArray = ((JSONArray) schemaSubPropertiesMap.get(subPropertieName));
					for (int i = 0; i < jsonArray.length(); i++) {
						JSONObject attribute = new JSONObject();
						attribute = jsonArray.getJSONObject(i);
					}
					break;
				} else {

					infoBuilder = schemaBuilder.subPropertiesChecker(infoBuilder, schemaSubPropertiesMap,
							subPropertieName);
					infoBuilder = injectAttributeInfoBuilderData(infoBuilder, attributeName);

				}

			}
			builder.addAttributeInfo(infoBuilder.build());
		} else {
			builder = injectObjectClassInfoBuilderData(builder, attributeName, infoBuilder);
		}
		return builder;
	}

	@Override
	public List<Map<String, Map<String, Object>>> getAttributeMapList(
			List<Map<String, Map<String, Object>>> attributeMapList) {
		return attributeMapList;
	}

	@Override
	public Set<Attribute> attributeInjection(Set<Attribute> injectedAttributeSet,
			Map<String, Object> autoriazationData) {
		return injectedAttributeSet;
	}

	@Override
	public JSONObject injectMissingSchemaAttributes(String resourceName, JSONObject jsonObject) {
		return jsonObject;
	}

	@Override
	public String checkFilter(Filter filter, String endpointName) {
		return "";
	}

	@Override
	public StringBuilder retrieveFilterQuery(StringBuilder queryUriSnippet, char prefixChar, Filter query) {

		StringBuilder filterSnippet = new StringBuilder();
		filterSnippet = query.accept(new FilterHandler(), "");

		queryUriSnippet.append(prefixChar).append("filter=").append(filterSnippet.toString());
		return queryUriSnippet;
	}

	@Override
	public Set<Attribute> addAttributeToInject(Set<Attribute> injectetAttributeSet) {
		return injectetAttributeSet;
	}

	@Override
	public List<String> excludeFromAssembly(List<String> excludedAttributes) {

		excludedAttributes.add("meta");
		excludedAttributes.add("schemas");

		return excludedAttributes;
	}

	@Override
	public Map<String, Object> translateReferenceValues(Map<String, Map<String, Object>> attributeMap,
			JSONArray referenceValues, Map<String, Object> subAttributeMap, int position, String attributeName) {

		Boolean isComplex = null;
		Map<String, Object> processedParameters = new HashMap<String, Object>();

		String sringReferenceValue = (String) referenceValues.get(position);

		for (String subAttributeKeyNames : subAttributeMap.keySet()) {
			if (!TYPE.equals(subAttributeKeyNames)) {

				StringBuilder complexAttrName = new StringBuilder(attributeName);
				attributeMap.put(complexAttrName.append(".").append(sringReferenceValue).append(".")
						.append(subAttributeKeyNames).toString(),
						(HashMap<String, Object>) subAttributeMap.get(subAttributeKeyNames));
				isComplex = true;

			}
		}
		if (isComplex != null) {
			processedParameters.put("isComplex", isComplex);
		}
		processedParameters.put("attributeMap", attributeMap);

		return processedParameters;
	}

	@Override
	public List<String> populateDictionary(String flag) {

		List<String> dictionary = new ArrayList<String>();

		if ("schemaparser-workaround".equals(flag)) {
			dictionary.add("subAttributes");
		} else if ("schemabuilder-workaround".equals(flag)) {

			dictionary.add("active");
		} else {

			LOGGER.warn("No such flag defined: {0}", flag);
		}
		return dictionary;

	}

	@Override
	public ObjectClassInfoBuilder injectObjectClassInfoBuilderData(ObjectClassInfoBuilder builder, String attributeName,
			AttributeInfoBuilder infoBuilder) {
		builder.addAttributeInfo(OperationalAttributeInfos.ENABLE);
		return builder;
	}

	@Override
	public AttributeInfoBuilder injectAttributeInfoBuilderData(AttributeInfoBuilder infoBuilder, String attributeName) {
		return infoBuilder;
	}

	@Override
	public void qeuery(Object query, String resourceEndPoint, ResultsHandler resultHandler,
			ScimConnectorConfiguration conf) {
		Header authHeader = null;
		String scimBaseUri = "";
		Map<String, Object> autoriazationData = CrudManagerScim.logIntoService(conf);
		HttpPost loginInstance = null;

		for (String data : autoriazationData.keySet()) {
			if ("authHeader".equals(data)) {

				authHeader = (Header) autoriazationData.get(data);

			} else if ("uri".equals(data)) {

				scimBaseUri = (String) autoriazationData.get(data);
			} else if ("loginInstance".equals(data)) {

				loginInstance = (HttpPost) autoriazationData.get(data);
			}
		}

		if (authHeader == null || scimBaseUri.isEmpty()) {

			throw new ConnectorException("The data needed for authorization of request to the provider was not found.");
		}

		HttpClient httpClient = HttpClientBuilder.create().build();
		String q;
		if (query instanceof Uid) {

			q = ((Uid) query).getUidValue();
		} else {

			q = (String) query;
		}

		String uri = new StringBuilder(scimBaseUri).append("/").append(resourceEndPoint).append("/").append(q)
				.toString();
		LOGGER.info("Qeury url: {0}", uri);
		HttpGet httpGet = new HttpGet(uri);
		httpGet.addHeader(authHeader);
		httpGet.addHeader(PRETTYPRINTHEADER);

		String responseString = null;
		HttpResponse response;
		try {
			long providerStartTime = System.currentTimeMillis();
			response = httpClient.execute(httpGet);
			long providerEndTime = System.currentTimeMillis();
			long providerDuration = (providerEndTime - providerStartTime);

			LOGGER.info(
					"The amouth of time it took to get the response to the query from the provider : {0} milliseconds ",
					providerDuration);

			providerDuration = 0;

			int statusCode = response.getStatusLine().getStatusCode();
			LOGGER.info("Status code: {0}", statusCode);
			if (statusCode == 200) {

				responseString = EntityUtils.toString(response.getEntity());
				if (!responseString.isEmpty()) {
					try {
						JSONObject jsonObject = new JSONObject(responseString);

						LOGGER.info("Json object returned from service provider: {0}", jsonObject.toString(1));
						try {
							if (query instanceof Uid) {

								BuilderConnectorObject builder = new BuilderConnectorObject();

								ConnectorObject connectorObject = builder.buildConnectorObject(jsonObject,
										resourceEndPoint, scimBaseUri);
								resultHandler.handle(connectorObject);

							} else {
								if (jsonObject.has("Resources")) {
									int amountOfResources = jsonObject.getJSONArray("Resources").length();
									int totalResults = 0;
									int startIndex = 0;
									int itemsPerPage = 0;

									if (jsonObject.has("startIndex") && jsonObject.has("totalResults")
											&& jsonObject.has("itemsPerPage")) {
										totalResults = (int) jsonObject.get("totalResults");
										startIndex = (int) jsonObject.get("startIndex");
										itemsPerPage = (int) jsonObject.get("itemsPerPage");
									}

									for (int i = 0; i < amountOfResources; i++) {
										JSONObject minResourceJson = new JSONObject();
										minResourceJson = jsonObject.getJSONArray("Resources").getJSONObject(i);
										if (minResourceJson.has("id") && minResourceJson.getString("id") != null) {

											if (minResourceJson.has("meta")) {

												String resourceUri = minResourceJson.getJSONObject("meta")
														.getString("location").toString();
												HttpGet httpGetR = new HttpGet(resourceUri);
												httpGetR.addHeader(authHeader);
												httpGetR.addHeader(PRETTYPRINTHEADER);

												providerStartTime = System.currentTimeMillis();
												HttpResponse resourceResponse = httpClient.execute(httpGetR);
												providerEndTime = System.currentTimeMillis();
												providerDuration = (providerEndTime - providerStartTime);

												LOGGER.info(
														"The amouth of time it took to get the response to the query from the provider : {0} milliseconds ",
														providerDuration);

												statusCode = resourceResponse.getStatusLine().getStatusCode();

												if (statusCode == 200) {
													responseString = EntityUtils.toString(resourceResponse.getEntity());
													JSONObject fullResourcejson = new JSONObject(responseString);

													LOGGER.info(
															"The {0}. resource json object which was returned by the service provider: {1}",
															i + 1, fullResourcejson);

													BuilderConnectorObject builder = new BuilderConnectorObject();
													ConnectorObject connectorObject = builder.buildConnectorObject(
															fullResourcejson, resourceEndPoint, scimBaseUri);

													resultHandler.handle(connectorObject);

												} else {

													CrudManagerScim.onNoSuccess(resourceResponse, resourceUri);
												}

											}
										} else {
											LOGGER.error("No uid present in fetched object: {0}", minResourceJson);

											throw new ConnectorException(
													"No uid present in fetchet object while processing queuery result");

										}
									}
									if (resultHandler instanceof SearchResultsHandler) {
										Boolean allResultsReturned = false;
										int remainingResult = totalResults - (startIndex - 1) - itemsPerPage;

										if (remainingResult <= 0) {
											remainingResult = 0;
											allResultsReturned = true;

										}

										LOGGER.info("The number of remaining results: {0}", remainingResult);
										SearchResult searchResult = new SearchResult("default", remainingResult,
												allResultsReturned);
										((SearchResultsHandler) resultHandler).handleResult(searchResult);
									}

								} else {

									LOGGER.error("Resource object not present in provider response to the query");

									throw new ConnectorException(
											"No uid present in fetchet object while processing queuery result");

								}
							}

						} catch (Exception e) {
							LOGGER.error(
									"Builder error. Error while building connId object. The exception message: {0}",
									e.getLocalizedMessage());
							LOGGER.info("Builder error. Error while building connId object. The excetion message: {0}",
									e);
							throw new ConnectorException(e);
						}

					} catch (JSONException jsonException) {
						if (q == null) {
							q = "the full resource representation";
						}
						LOGGER.error(
								"An exception has occurred while setting the variable \"jsonObject\". Occurrence while processing the http response to the queuey request for: {1}, exception message: {0}",
								jsonException.getLocalizedMessage(), q);
						LOGGER.info(
								"An exception has occurred while setting the variable \"jsonObject\". Occurrence while processing the http response to the queuey request for: {1}, exception message: {0}",
								jsonException, q);
						throw new ConnectorException(
								"An exception has occurred while setting the variable \"jsonObject\".", jsonException);
					}

				} else {

					LOGGER.warn("Service provider response is empty, responce returned on queuery: {0}", query);
				}
			} else {
				CrudManagerScim.onNoSuccess(response, uri);
			}

		} catch (IOException e) {

			if (q == null) {
				q = "the full resource representation";
			}

			LOGGER.error(
					"An error occurred while processing the queuery http response. Occurrence while processing the http response to the queuey request for: {1}, exception message: {0}",
					e.getLocalizedMessage(), q);
			LOGGER.info(
					"An error occurred while processing the queuery http response. Occurrence while processing the http response to the queuey request for: {1}, exception message: {0}",
					e, q);
			throw new ConnectorIOException("An error occurred while processing the queuery http response.", e);
		} finally {
			CrudManagerScim.logOut(loginInstance);
		}
	}

	@Override
	public ParserSchemaScim qeuerySchemas(String providerName, String resourceEndPoint,
			ScimConnectorConfiguration conf) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Uid create(String resourceEndPoint, ObjectTranslator objectTranslator, Set<Attribute> attributes,
			Set<Attribute> injectedAttributeSet, ScimConnectorConfiguration conf) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Uid update(Uid uid, String resourceEndPoint, JSONObject jsonObject, ScimConnectorConfiguration conf) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void delete(Uid uid, String resourceEndPoint, ScimConnectorConfiguration conf) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ParserSchemaScim processSchemaResponse(JSONObject responseObject, String providerName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void queryMembershipData(Uid uid, String resourceEndPoint, ResultsHandler resultHandler,
			String membershipResourceEndpoin, ScimConnectorConfiguration conf) {
		// TODO Auto-generated method stub
		
	}

}
