package com.umbrella.ssl;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.SSLContext;

import com.google.inject.throwingproviders.CheckedProvider;

public interface SslContextProvider extends CheckedProvider<SSLContext> {
	
	@Override
	SSLContext get() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, UnrecoverableKeyException, KeyManagementException;
}
