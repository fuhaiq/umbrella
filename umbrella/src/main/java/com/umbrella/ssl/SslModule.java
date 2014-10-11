package com.umbrella.ssl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Strings;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.throwingproviders.CheckedProvides;
import com.google.inject.throwingproviders.ThrowingProviderBinder;

public class SslModule extends AbstractModule{

	private final String config;
	
	public SslModule(String config) {
		this.config = config;
	}
	
	@Override
	protected void configure() {
		try (InputStream in = this.getClass().getClassLoader().getResourceAsStream(config);
				BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
			StringBuilder builder = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null) {
				builder.append(line);
			}
			bind(SslContextConfig.class).toInstance(JSON.parseObject(builder.toString(), SslContextConfig.class));
		} catch (IOException e) {
			addError(e);
		}
		install(ThrowingProviderBinder.forModule(this));
	}
	
	@CheckedProvides(SslContextProvider.class)
	@Singleton
	SSLContext provideSSLContext(SslContextConfig config) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, UnrecoverableKeyException, KeyManagementException{
		SSLContext sslContext = SSLContext.getInstance(config.getProtocol());
		String algorithm = Security.getProperty("ssl.KeyManagerFactory.algorithm");
		if (Strings.isNullOrEmpty(algorithm)) {
			algorithm = "SunX509";
		}
		KeyStore privateKs = KeyStore.getInstance("JKS");
		_loadKeyStore(privateKs, config.getPrivateKeyStore(), config.getOwnPass());
		KeyStore publicKs = KeyStore.getInstance("JKS");
		_loadKeyStore(publicKs, config.getPublicKeyStore(), config.getPartnerPass());
		KeyManagerFactory kmf = KeyManagerFactory.getInstance(algorithm);
		kmf.init(privateKs, config.getOwnPass().toCharArray());
		TrustManagerFactory tmf = TrustManagerFactory.getInstance(algorithm);
		tmf.init(publicKs);
		sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
		return sslContext;
	}
	
	private void _loadKeyStore(KeyStore keyStore, String file, String pass) throws IOException, NoSuchAlgorithmException, CertificateException {
		try(InputStream in = this.getClass().getClassLoader().getResourceAsStream(file)){
			keyStore.load(this.getClass().getClassLoader().getResourceAsStream(file), pass.toCharArray());
		}
	}
}
