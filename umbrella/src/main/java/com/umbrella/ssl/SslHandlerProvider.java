package com.umbrella.ssl;

import io.netty.channel.ChannelHandler;
import io.netty.handler.ssl.SslHandler;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import javax.net.ssl.SSLEngine;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class SslHandlerProvider implements Provider<ChannelHandler>{
	
	@Inject SslContextProvider context;
	
	@Inject SslContextConfig config;
	
	@Override
	public ChannelHandler get() {
		SSLEngine engine = null;
		try {
			engine = context.get().createSSLEngine();
		} catch (UnrecoverableKeyException | KeyManagementException
				| KeyStoreException | NoSuchAlgorithmException
				| CertificateException | IOException e) {
			throw new RuntimeException(e);
		}
		engine.setUseClientMode(config.isClientMode());
		return new SslHandler(engine);
	}
}
