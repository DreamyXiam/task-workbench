package cn.workbench;
import org.springframework.context.annotation.*;import org.mybatis.spring.boot.autoconfigure.ConfigurationCustomizer;import org.apache.ibatis.reflection.*;import org.apache.ibatis.reflection.wrapper.*;import java.util.*;
@Configuration public class MyBatisConfiguration {
 @Bean ConfigurationCustomizer camelMaps(){return configuration->configuration.setObjectWrapperFactory(new ObjectWrapperFactory(){public boolean hasWrapperFor(Object object){return object instanceof Map;}@SuppressWarnings("unchecked") public ObjectWrapper getWrapperFor(MetaObject meta,Object object){return new MapWrapper(meta,(Map<String,Object>)object){@Override public String findProperty(String name,boolean camel){if(!camel)return name;StringBuilder out=new StringBuilder();boolean upper=false;for(char c:name.toLowerCase(Locale.ROOT).toCharArray()){if(c=='_'){upper=true;}else{out.append(upper?Character.toUpperCase(c):c);upper=false;}}return out.toString();}};}});}
}
