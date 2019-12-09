package com.fhq.nio.conf;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import com.google.common.base.Strings;
import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

public class NioConfModule extends AbstractModule {

   private Map<String, String> env = System.getenv();

   @Override
   protected void configure() {
      var filePath = env.get(NioConfiguration.NIO_CONF_FILE);
      if (Strings.isNullOrEmpty(filePath)) {
         addError(String.format("环境变量%s没有设置", NioConfiguration.NIO_CONF_FILE));
         return;
      }

      bind(new TypeLiteral<Map<String, String>>() {}).annotatedWith(Names.named("ENV"))
            .toInstance(env);
      try (var stream = new FileInputStream(filePath)) {
         var yaml = new Yaml().loadAs(stream, NioYaml.class);
         if (yaml == null) {
            addError("YAML配置文件初始化实例为空值");
            return;
         }
         bind(NioYaml.class).toInstance(yaml);
      } catch (FileNotFoundException e) {
         addError(String.format("YAML配置文件%s找不到", filePath));
      } catch (IOException e) {
         addError(e.getMessage(), e);
      } catch (YAMLException e) {
         addError(e.getMessage(), e);
      }
   }

}
