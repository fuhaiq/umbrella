package com.fhq.nio.conf;

public class Server {
   public String getHost() {
      return host;
   }
   public void setHost(String host) {
      this.host = host;
   }
   public int getPort() {
      return port;
   }
   public void setPort(int port) {
      this.port = port;
   }
   private String host;
   private int port;
}