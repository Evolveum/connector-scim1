package com.evolveum.polygon.salesfrconn;

import org.identityconnectors.framework.common.objects.filter.AndFilter;
import org.identityconnectors.framework.common.objects.filter.ContainsAllValuesFilter;
import org.identityconnectors.framework.common.objects.filter.ContainsFilter;
import org.identityconnectors.framework.common.objects.filter.EndsWithFilter;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.common.objects.filter.FilterVisitor;
import org.identityconnectors.framework.common.objects.filter.GreaterThanFilter;
import org.identityconnectors.framework.common.objects.filter.GreaterThanOrEqualFilter;
import org.identityconnectors.framework.common.objects.filter.LessThanFilter;
import org.identityconnectors.framework.common.objects.filter.LessThanOrEqualFilter;
import org.identityconnectors.framework.common.objects.filter.NotFilter;
import org.identityconnectors.framework.common.objects.filter.OrFilter;
import org.identityconnectors.framework.common.objects.filter.StartsWithFilter;

public class FilterHandler implements FilterVisitor<StringBuilder, Void> {

	@Override
	public StringBuilder visitAndFilter(Void p, AndFilter filter) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StringBuilder visitContainsFilter(Void p, ContainsFilter filter) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StringBuilder visitContainsAllValuesFilter(Void p, ContainsAllValuesFilter filter) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StringBuilder visitEqualsFilter(Void p, EqualsFilter filter) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StringBuilder visitExtendedFilter(Void p, Filter filter) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StringBuilder visitGreaterThanFilter(Void p, GreaterThanFilter filter) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StringBuilder visitGreaterThanOrEqualFilter(Void p, GreaterThanOrEqualFilter filter) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StringBuilder visitLessThanFilter(Void p, LessThanFilter filter) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StringBuilder visitLessThanOrEqualFilter(Void p, LessThanOrEqualFilter filter) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StringBuilder visitNotFilter(Void p, NotFilter filter) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StringBuilder visitOrFilter(Void p, OrFilter filter) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StringBuilder visitStartsWithFilter(Void p, StartsWithFilter filter) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StringBuilder visitEndsWithFilter(Void p, EndsWithFilter filter) {
		// TODO Auto-generated method stub
		return null;
	}

}
