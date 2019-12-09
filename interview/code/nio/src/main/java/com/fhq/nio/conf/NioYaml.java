package com.fhq.nio.conf;

public class NioYaml {

   private String read;
   
   private String scatter;
   
   private String gather;
   
   private Socket socket;
   
   private String from;
   
   private String to;

   public String getRead() {
      return read;
   }

   public void setRead(String read) {
      this.read = read;
   }

   public String getScatter() {
      return scatter;
   }

   public void setScatter(String scatter) {
      this.scatter = scatter;
   }

   public String getGather() {
      return gather;
   }

   public void setGather(String gather) {
      this.gather = gather;
   }

   public Socket getSocket() {
      return socket;
   }

   public void setSocket(Socket socket) {
      this.socket = socket;
   }

   public String getFrom() {
      return from;
   }

   public void setFrom(String from) {
      this.from = from;
   }

   public String getTo() {
      return to;
   }

   public void setTo(String to) {
      this.to = to;
   }
}
