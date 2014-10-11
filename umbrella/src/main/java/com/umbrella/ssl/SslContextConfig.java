package com.umbrella.ssl;

public final class SslContextConfig {
	private String ownPass;
	private String partnerPass;
	private String protocol;
	private String privateKeyStore;
	private String publicKeyStore;
	private boolean clientMode;
	public String getProtocol() {
		return protocol;
	}
	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}
	public String getPrivateKeyStore() {
		return privateKeyStore;
	}
	public void setPrivateKeyStore(String privateKeyStore) {
		this.privateKeyStore = privateKeyStore;
	}
	public String getPublicKeyStore() {
		return publicKeyStore;
	}
	public void setPublicKeyStore(String publicKeyStore) {
		this.publicKeyStore = publicKeyStore;
	}
	public String getOwnPass() {
		return ownPass;
	}
	public void setOwnPass(String ownPass) {
		this.ownPass = ownPass;
	}
	public String getPartnerPass() {
		return partnerPass;
	}
	public void setPartnerPass(String partnerPass) {
		this.partnerPass = partnerPass;
	}
	public boolean isClientMode() {
		return clientMode;
	}
	public void setClientMode(boolean clientMode) {
		this.clientMode = clientMode;
	}
}
