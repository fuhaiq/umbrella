package com.umbrella.kit;

import java.net.URI;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.protocol.HTTP;

public class XHttpDelete extends HttpDelete implements HttpEntityEnclosingRequest {
	
	
	public final static String METHOD_NAME = "XDELETE";


    public XHttpDelete() {
        super();
    }

    public XHttpDelete(final URI uri) {
        super();
        setURI(uri);
    }

    public XHttpDelete(final String uri) {
        super();
        setURI(URI.create(uri));
    }
	
	private HttpEntity entity;

	@Override
	public boolean expectContinue() {
		final Header expect = getFirstHeader(HTTP.EXPECT_DIRECTIVE);
        return expect != null && HTTP.EXPECT_CONTINUE.equalsIgnoreCase(expect.getValue());
	}

	@Override
	public void setEntity(HttpEntity entity) {
		this.entity = entity;
	}

	@Override
	public HttpEntity getEntity() {
		return this.entity;
	}

}
