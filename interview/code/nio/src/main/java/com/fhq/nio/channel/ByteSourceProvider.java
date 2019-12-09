package com.fhq.nio.channel;

import java.io.File;
import com.fhq.nio.conf.NioYaml;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class ByteSourceProvider implements Provider<ByteSource> {
   
   @Inject
   private NioYaml yaml;

   @Override
   public ByteSource get() {
      return Files.asByteSource(new File(yaml.getRead()));
   }

}
