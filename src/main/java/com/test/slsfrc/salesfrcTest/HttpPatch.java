package com.test.slsfrc.salesfrcTest;

import org.apache.http.client.methods.HttpPost;

public class HttpPatch extends HttpPost {
	public HttpPatch(String uri) {

		  super(uri);
	 }
	 public String getMethod() {
		 return "PATCH";
	 }


}
