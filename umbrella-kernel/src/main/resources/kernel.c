#include <stdio.h>
#include "wstp.h"

struct Kernel connect(char* url);

void destroy(struct Kernel kernel);

char* evaluate(struct Kernel kernel, const char* expression);

void error(WSLINK link);

struct Kernel {
  WSENV env;
  WSLINK link;
  const char* errMsg = NULL;
};

struct Kernel connect(const char* url) {
  struct Kernel kernel;
  int error;
  kernel.env = WSInitialize((WSEnvironmentParameter)0);
  if(kernel.env == (WSENV)0){
    kernel.errMsg = "unable to initialize WSTP environment";
    return kernel;
  }
  kernel.link = WSOpenString(kernel.env, url, &error);
  if(kernel.link == (WSLINK)0 || error != WSEOK){
    kernel.errMsg = "unable to create link to the Kernel";
    return kernel;
  }
  return kernel;
}

void destroy(struct Kernel kernel) {
  if(kernel.link) {
    WSClose(kernel.link);
  }
  if(kernel.env) {
    WSDeinitialize(kernel.env);
  }
}

char* evaluate(struct Kernel kernel, const char* expression) {
  int pkt;

  WSPutFunction(kernel.link, "EnterTextPacket", 1L);
  printf("%s\n", "step 1");
  WSPutString(kernel.link, expression);

  WSEndPacket(kernel.link);
  // while((pkt = WSNextPacket(kernel.link), pkt) && pkt != RETURNPKT) {
  printf("%s\n", "step 2");
  while((pkt = WSNextPacket(kernel.link), pkt)) {
    printf("the pkt is %d\n", pkt);
    WSNewPacket(kernel.link);
    if(WSError(kernel.link)) error(kernel.link);
	}
  printf("%s\n", "step 3");
  return (char*)"I am test";
}

void error(WSLINK link)
{
	if(WSError(link)){
    printf("Error detected by WSTP: %s.\n", WSErrorMessage(link));
	}else{
    printf("%s\n", "Error detected by this program");
	}
}

/**
-linkcreate -linkprotocol IntraProcess
**/
int main(void)
{
  printf("%s\n", "going to connect to kernel");
  struct Kernel kernel = connect("-linkcreate -linkprotocol IntraProcess");
  if(kernel.errMsg != NULL) {
    printf("error message when connecting : %s\n", kernel.errMsg);
    return -1;
  }
  printf("%s\n", "step 0");
  char* result = evaluate(kernel, "x^2");
  destroy(kernel);
  printf("%s\n", "end of main");
  return 0;
}
