package com.evolveum.polygon.scim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.InvalidAttributeValueException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.filter.AndFilter;
import org.identityconnectors.framework.common.objects.filter.AttributeFilter;
import org.identityconnectors.framework.common.objects.filter.ContainsAllValuesFilter;
import org.identityconnectors.framework.common.objects.filter.ContainsFilter;
import org.identityconnectors.framework.common.objects.filter.EndsWithFilter;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.common.objects.filter.FilterBuilder;
import org.identityconnectors.framework.common.objects.filter.FilterVisitor;
import org.identityconnectors.framework.common.objects.filter.GreaterThanFilter;
import org.identityconnectors.framework.common.objects.filter.GreaterThanOrEqualFilter;
import org.identityconnectors.framework.common.objects.filter.LessThanFilter;
import org.identityconnectors.framework.common.objects.filter.LessThanOrEqualFilter;
import org.identityconnectors.framework.common.objects.filter.NotFilter;
import org.identityconnectors.framework.common.objects.filter.OrFilter;
import org.identityconnectors.framework.common.objects.filter.StartsWithFilter;

// Missing filterVisitor methods/filters from SCIM v1 specification: not equal, present

/**
 * Contains methods needed for building a "filter" query which is sent to the
 * resource provider as a more specific search query.
 */
public class FilterHandler implements FilterVisitor<StringBuilder, String> {

	private static final Log LOGGER = Log.getLog(FilterHandler.class);

	private static final String SPACE = "%20";
	private static final String QUOTATION = "%22";
	private static final String EQUALS = "eq";
	private static final String CONTAINS = "co";
	private static final String STARTSWITH = "sw";
	private static final String ENDSWITH = "ew";
	private static final String GREATERTHAN = "gt";
	private static final String GREATEROREQ = "ge";
	private static final String LESSTHAN = "lt";
	private static final String LESSOREQ = "le";
	private static final String AND = "and";
	private static final String OR = "or";
	private static final String NOT = "not";
	private static final String TYPE = "type";
	private static final String DELIMITER = "\\.";
	private static final String LEFTPAR = "(";
	private static final String RIGHTPAR = "(";

	/**
	 * Implementation of the "visitAndFilter" filter method.
	 * 
	 * @param p
	 *            Helper parameter which may contain the "valuePath" string
	 *            value indicating the name of an complex attribute with two or
	 *            more subattributes being processed.
	 * @param filter
	 *            The filter or list of filters being processed.
	 * @return The filter part of an query.
	 */
	@Override
	public StringBuilder visitAndFilter(String p, AndFilter filter) {
		LOGGER.info("Processing request trought AND filter");

		String[] samePathIdParts = p.split(DELIMITER);// e.g valuePath.members

		if (samePathIdParts.length > 1) {
			p = samePathIdParts[1];
		}

		StringBuilder completeQuery = new StringBuilder();
		int i = 0;
		int size = filter.getFilters().size();
		boolean isFirst = true;

		for (Filter f : filter.getFilters()) {
			i++;

			if (isFirst) {
				if (!p.isEmpty() || samePathIdParts.length > 1) {
					completeQuery.append(p);
					completeQuery.append("[");
					completeQuery.append(f.accept(this, p));
					isFirst = false;
					if (i == size) {
						completeQuery.append("]");
						isFirst = false;
					}
				} else {

					completeQuery = f.accept(this, p);
					isFirst = false;
				}
			} else {
				completeQuery.append(SPACE);
				completeQuery.append(AND);
				completeQuery.append(SPACE);
				if (f instanceof OrFilter || f instanceof AndFilter) {
					completeQuery.append(LEFTPAR);
					completeQuery.append(f.accept(this, p).toString());
					completeQuery.append(RIGHTPAR);
				} else {
					completeQuery.append(f.accept(this, p).toString());
				}
				if (i == size) {
					if (!p.isEmpty() || samePathIdParts.length > 1) {
						completeQuery.append("]");
					}
				}
			}

		}

		return completeQuery;
	}

	/**
	 * Implementation of the "visitContainsFilter" filter method.
	 * 
	 * @param p
	 *            Helper parameter which may contain the resource provider name
	 *            used for workaround purposes.
	 * @param filter
	 *            The filter or list of filters being processed.
	 * @return The processed filter.
	 * @throws InvalidAttributeValueException
	 */
	@Override
	public StringBuilder visitContainsFilter(String p, ContainsFilter filter) {
		LOGGER.info("Processing request trought CONTAINS filter");
		if (!filter.getName().isEmpty()) {

			StringBuilder preprocessedFilter = processArrayQ(filter, p);
			if (preprocessedFilter == null) {
				return BuildString(filter.getAttribute(), CONTAINS, filter.getName());
			} else {
				return preprocessedFilter;
			}
		} else {

			LOGGER.error("Filter atribute key name EMPTY!");
			throw new InvalidAttributeValueException("No atribute key name provided");
		}
	}

	/**
	 * Implementation of the "visitContainsAllValuesFilter" filter method.
	 * 
	 * @param p
	 *            Helper parameter which may contain the resource provider name
	 *            used for workaround purposes.
	 * @param filter
	 *            The filter or list of filters being processed.
	 * @return The final filter query.
	 * @throws InvalidAttributeValueException
	 */
	@Override
	public StringBuilder visitContainsAllValuesFilter(String p, ContainsAllValuesFilter filter) {

		StrategyFetcher fetcher = new StrategyFetcher();

		HandlingStrategy strategy = fetcher.fetchStrategy(p);

		StringBuilder preprocessedFilter = strategy.processContainsAllValuesFilter(p, filter, this);

		if (null != preprocessedFilter) {
			return preprocessedFilter;
		} else {
			Collection<Filter> filterList = buildValueList(filter, "members");
			for (Filter f : filterList) {
				if (f instanceof EqualsFilter) {

					return f.accept(this, p);
				}
			}
			AndFilter andFilterTest = (AndFilter) FilterBuilder.and(filterList);

			return andFilterTest.accept(this, p);

		}

	}

	/**
	 * Implementation of the "visitEqualsFilter" filter method.
	 * 
	 * @param p
	 *            Helper parameter which may contain the resource provider name
	 *            used for workaround purposes.
	 * @param filter
	 *            The filter or list of filters being processed.
	 * @return The processed filter.
	 * @throws InvalidAttributeValueException
	 */
	@Override
	public StringBuilder visitEqualsFilter(String p, EqualsFilter filter) {
		LOGGER.info("Processing request trought EQUALS filter: {0}", filter);

		if (!filter.getName().isEmpty()) {
			StringBuilder preprocessedFilter = processArrayQ(filter, p);
			if (preprocessedFilter == null) {

				return BuildString(filter.getAttribute(), EQUALS, filter.getName());

			} else {
				return preprocessedFilter;
			}
		} else {

			LOGGER.error("Filter atribute key name EMPTY");
			throw new InvalidAttributeValueException("No atribute key name provided");
		}
	}

	/**
	 * Implementation of the "visitExtendedFilter" filter method is not
	 * supported in this connector.
	 * 
	 * @param p
	 *            Helper parameter which may contain the resource provider name
	 *            used for workaround purposes.
	 * @param filter
	 *            The filter or list of filters being processed.
	 * @throws NoSuchMethodError
	 */
	@Override
	public StringBuilder visitExtendedFilter(String p, Filter filter) {
		LOGGER.error("Usuported filter: {0}", filter);
		throw new NoSuchMethodError("Usuported queuery filter");
	}

	/**
	 * Implementation of the "visitGreaterThanFilter" filter method.
	 * 
	 * @param p
	 *            Helper parameter which may contain the resource provider name
	 *            used for workaround purposes.
	 * @param filter
	 *            The filter or list of filters being processed.
	 * @return The processed filter.
	 * @throws InvalidAttributeValueException
	 */
	@Override
	public StringBuilder visitGreaterThanFilter(String p, GreaterThanFilter filter) {
		LOGGER.info("Processing request trought GREATHERTHAN filter: {0}", filter);

		if (!filter.getName().isEmpty()) {

			StringBuilder preprocessedFilter = processArrayQ(filter, p);
			if (preprocessedFilter == null) {

				return BuildString(filter.getAttribute(), GREATERTHAN, filter.getName());

			} else {
				return preprocessedFilter;
			}
		} else {

			LOGGER.error("Filter atribute key name EMPTY: {0}", filter);
			throw new InvalidAttributeValueException("No atribute key name provided");
		}
	}

	/**
	 * Implementation of the "visitGreaterThanOrEqualFilter" filter method.
	 * 
	 * @param p
	 *            Helper parameter which may contain the resource provider name
	 *            used for workaround purposes.
	 * @param filter
	 *            The filter or list of filters being processed.
	 * @return The processed filter.
	 * @throws InvalidAttributeValueException
	 */
	@Override
	public StringBuilder visitGreaterThanOrEqualFilter(String p, GreaterThanOrEqualFilter filter) {
		LOGGER.info("Processing request trought GREATHERTHANOREQUAL filter: {0}", filter);
		if (!filter.getName().isEmpty()) {

			StringBuilder preprocessedFilter = processArrayQ(filter, p);
			if (preprocessedFilter == null) {

				return BuildString(filter.getAttribute(), GREATEROREQ, filter.getName());
			} else {
				return preprocessedFilter;
			}
		} else {

			LOGGER.error("Filter atribute key name EMPTY: {0}", filter);
			throw new InvalidAttributeValueException("No atribute key name provided");
		}
	}

	/**
	 * Implementation of the "visitLessThanFilter" filter method.
	 * 
	 * @param p
	 *            Helper parameter which may contain the resource provider name
	 *            used for workaround purposes.
	 * @param filter
	 *            The filter or list of filters being processed.
	 * @return The processed filter.
	 * @throws InvalidAttributeValueException
	 */
	@Override
	public StringBuilder visitLessThanFilter(String p, LessThanFilter filter) {
		LOGGER.info("Processing request trought LESSTHAN filter: {0}", filter);
		if (!filter.getName().isEmpty()) {

			StringBuilder preprocessedFilter = processArrayQ(filter, p);
			if (preprocessedFilter == null) {
				return BuildString(filter.getAttribute(), LESSTHAN, filter.getName());

			} else {
				return preprocessedFilter;
			}
		} else {

			LOGGER.error("Filter atribute key name EMPTY: {0}", filter);
			throw new InvalidAttributeValueException("No atribute key name provided");

		}
	}

	/**
	 * Implementation of the "visitLessThanOrEqualFilter" filter method.
	 * 
	 * @param p
	 *            Helper parameter which may contain the resource provider name
	 *            used for workaround purposes.
	 * @param filter
	 *            The filter or list of filters being processed.
	 * @return The processed filter.
	 * @throws InvalidAttributeValueException
	 */
	@Override
	public StringBuilder visitLessThanOrEqualFilter(String p, LessThanOrEqualFilter filter) {
		LOGGER.info("Processing request trought LESSTHANOREQUAL filter: {0}", filter);
		if (!filter.getName().isEmpty()) {

			StringBuilder preprocessedFilter = processArrayQ(filter, p);
			if (preprocessedFilter == null) {

				return BuildString(filter.getAttribute(), LESSOREQ, filter.getName());
			} else {
				return preprocessedFilter;
			}
		} else {

			LOGGER.error("Filter atribute key name EMPTY: {0}", filter);
			throw new InvalidAttributeValueException("No atribute key name provided");
		}
	}

	/**
	 * Implementation of the "visitNotFilter" filter method.
	 * 
	 * @param p
	 *            Helper parameter which may contain the resource provider name
	 *            used for workaround purposes.
	 * @param filter
	 *            The filter or list of filters being processed.
	 * @return The complete query.
	 */
	@Override
	public StringBuilder visitNotFilter(String p, NotFilter filter) {
		LOGGER.info("Processing request trought NOT filter: {0}", filter);
		StringBuilder completeQuery = new StringBuilder();

		completeQuery.append(NOT).append(SPACE).append(filter.getFilter().accept(this, p));

		return completeQuery;
	}

	/**
	 * Implementation of the "visitOrFilter" filter method.
	 * 
	 * @param p
	 *            Helper parameter which may contain the resource provider name
	 *            used for workaround purposes.
	 * @param filter
	 *            The filter or list of filters being processed.
	 * @return The complete query.
	 */
	@Override
	public StringBuilder visitOrFilter(String p, OrFilter filter) {
		LOGGER.info("Processing request trought OR filter: {0}", filter);
		StringBuilder completeQuery = new StringBuilder();

		boolean isFirst = true;

		for (Filter f : filter.getFilters()) {

			if (isFirst) {
				completeQuery = f.accept(this, p);
				completeQuery.append(SPACE);
				completeQuery.append(OR);
				isFirst = false;

			} else {

				completeQuery.append(SPACE);
				if (f instanceof OrFilter || f instanceof AndFilter) {
					completeQuery.append(LEFTPAR);
					completeQuery.append(f.accept(this, p).toString());
					completeQuery.append(RIGHTPAR);
				} else {

					completeQuery.append(f.accept(this, p).toString());
				}

			}

		}

		return completeQuery;

	}

	/**
	 * Implementation of the "visitStartsWithFilter" filter method.
	 * 
	 * @param p
	 *            Helper parameter which may contain the resource provider name
	 *            used for workaround purposes.
	 * @param filter
	 *            The filter or list of filters being processed.
	 * @return The processed filter.
	 * @throws InvalidAttributeValueException
	 */
	@Override
	public StringBuilder visitStartsWithFilter(String p, StartsWithFilter filter) {
		LOGGER.info("Processing request trought STARTSWITH filter: {0}", filter);
		if (!filter.getName().isEmpty()) {

			StringBuilder preprocessedFilter = processArrayQ(filter, p);
			if (preprocessedFilter == null) {

				return BuildString(filter.getAttribute(), STARTSWITH, filter.getName());
			} else {
				return preprocessedFilter;
			}
		} else {

			LOGGER.error("Filter atribute key name EMPTY: {0}", filter);
			throw new InvalidAttributeValueException("No atribute key name provided");
		}
	}

	/**
	 * Implementation of the "visitEndsWithFilter" filter method.
	 * 
	 * @param p
	 *            Helper parameter which may contain the resource provider name
	 *            used for workaround purposes.
	 * @param filter
	 *            The filter or list of filters being processed.
	 * @return The processed filter.
	 * @throws InvalidAttributeValueException
	 */
	@Override
	public StringBuilder visitEndsWithFilter(String p, EndsWithFilter filter) {
		LOGGER.info("Processing request trought ENDSWITH filter: {0}", filter);
		if (!filter.getName().isEmpty()) {

			StringBuilder preprocessedFilter = processArrayQ(filter, p);
			if (preprocessedFilter == null) {
				return BuildString(filter.getAttribute(), ENDSWITH, filter.getName());

			} else {
				return preprocessedFilter;
			}
		} else {

			LOGGER.error("Filter atribute key name EMPTY while processing an ends with filter: {0}", filter);
			throw new InvalidAttributeValueException(
					"No atribute key name provided while processing an ends with filter");
		}
	}

	/**
	 * Builds the string representation of an filter query.
	 * 
	 * @param attribute
	 *            The attribute on behalf of which the query result should be
	 *            filtered out .
	 * @param operator
	 *            The operator which represents the type of filter used.
	 * @param name
	 *            The name of the attribute which is being used.
	 * @return The string representation of a filter.
	 * @throws InvalidAttributeValueException
	 */
	private StringBuilder BuildString(Attribute attribute, String operator, String name) {

		LOGGER.info("String builder processing filter: {0}", operator);

		StringBuilder resultString = new StringBuilder();
		if (attribute == null) {

			LOGGER.error("Filter atribude value is EMPTY while building filter queuery, please provide atribute value ",
					attribute);
			throw new InvalidAttributeValueException("No atribute value provided while building filter queuery");
		} else {
			resultString.append(name).append(SPACE).append(operator).append(SPACE).append(QUOTATION)
					.append(AttributeUtil.getAsStringValue(attribute)).append(QUOTATION);
		}

		return resultString;
	}

	/**
	 * Processes through an filter query containing an complex attribute with
	 * subattributes.
	 * 
	 * @param filter
	 *            The filter which is being processed.
	 * @param p
	 *            Helper parameter which can contain the name of the service
	 *            provider for workaround purposes or can be populated with an
	 *            "valuePath" string value indicating the name of a complex
	 *            attribute with two or more subattributes being processed.
	 * @return The final string representation of a filter or null if the
	 *         attribute is evaluated as non complex.
	 */
	public StringBuilder processArrayQ(AttributeFilter filter, String p) {
		if (filter.getName().contains(".")) {

			String[] keyParts = filter.getName().split(DELIMITER); // eq.
			// email.work.value
			if (keyParts.length == 3) {

				StringBuilder processedString = new StringBuilder();
				Collection<Filter> filterList = new ArrayList<Filter>();
				if (filter instanceof EqualsFilter) {

					StringBuilder keyName = new StringBuilder(keyParts[0]).append(".").append(keyParts[2]);
					EqualsFilter eqfilter = (EqualsFilter) FilterBuilder.equalTo(AttributeBuilder
							.build(keyName.toString(), AttributeUtil.getAsStringValue(filter.getAttribute())));

					StringBuilder type = new StringBuilder(keyParts[0]).append(".").append(TYPE);

					EqualsFilter eq = (EqualsFilter) FilterBuilder
							.equalTo(AttributeBuilder.build(type.toString(), keyParts[1]));
					filterList.add(eqfilter);
					filterList.add(eq);

				} else if (filter instanceof ContainsAllValuesFilter) {
					StringBuilder pathName = new StringBuilder("valuePath").append(".").append(keyParts[0]);
					p = pathName.toString();

					filterList = buildValueList((ContainsAllValuesFilter) filter, keyParts[2]);

					EqualsFilter eq = (EqualsFilter) FilterBuilder.equalTo(AttributeBuilder.build(TYPE, keyParts[1]));
					filterList.add(eq);

				} else {
					LOGGER.warn("Evalated filter is not supported for querying of \"complex\" attributes: {0}.",
							filter);
					return null;
				}
				AndFilter and = (AndFilter) FilterBuilder.and(filterList);

				processedString = and.accept(this, p);
				return processedString;
			}
			LOGGER.info(
					"The attribute {0} is not a \"complex\" attribute. The filter query will be processed accordingli.",
					filter.getName());
			return null;
		}
		LOGGER.info(
				"Delimiters not found in the attribute name of {0}, the attribute is non complex. The filter query will be processed accordingly",
				filter.getName());
		return null;
	}

	/**
	 * Method is called if an attribute is processed which contains multiple
	 * values.
	 * 
	 * @param filter
	 *            The filter which is being processed.
	 * @param attributeName
	 *            The name of the attribute which is being processed.
	 * @return List of filters which was built from the list of values.
	 */
	private Collection<Filter> buildValueList(ContainsAllValuesFilter filter, String attributeName) {

		List<Object> valueList = filter.getAttribute().getValue();
		Collection<Filter> filterList = new ArrayList<Filter>();

		for (Object value : valueList) {
			if (attributeName.isEmpty()) {

				ContainsFilter containsSingleAtribute = (ContainsFilter) FilterBuilder
						.contains(AttributeBuilder.build(filter.getName(), value));
				filterList.add(containsSingleAtribute);
			} else {
				EqualsFilter containsSingleAtribute = (EqualsFilter) FilterBuilder
						.equalTo(AttributeBuilder.build(attributeName, value));
				filterList.add(containsSingleAtribute);
			}

		}

		return filterList;
	}

}
