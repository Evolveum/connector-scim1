package com.evolveum.polygon.scim;

/**
 * 
 * @author Matus
 *
 *
 *Enumeration representing the two flag values needed for conditional 
 */

public enum WorkaroundFlags {

	PARSERFLAG("schemaparser-workaround"), BUILDERFLAG("schemabuilder-workaround");

	private String value;

	private WorkaroundFlags(String value) {
		this.value = value;
	}

	public String getValue() {
		return this.value;
	}

}
