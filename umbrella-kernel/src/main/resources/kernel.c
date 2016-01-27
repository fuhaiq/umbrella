#include <stdio.h>
#include "wstp.h"

int main(void)
{
  WSENV env;
  WSLINK link;
  int error;

  env = WSInitialize((WSEnvironmentParameter)0);
  if(env == (WSENV)0){
    printf("%s\n", "unable to initialize WSTP environment");
    return -1;
  }

  link = WSOpenString(env, "-linkcreate -linkprotocol IntraProcess", &error);
  if(link == (WSLINK)0 || error != WSEOK){
    printf("%s\n", "unable to create link to the Kernel");
    return -1;
  }

  /**/

  if(link) {
    WSClose(link);
  }
  if(env) {
    WSDeinitialize(env);
  }
  printf("%s\n", "OK!");
  return 0;
}
