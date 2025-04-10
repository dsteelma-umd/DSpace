package org.dspace.app;

import org.apache.catalina.valves.JsonAccessLogValve;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.ServerProperties.Tomcat.Accesslog;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.stereotype.Component;

/**
 * UMD customization of the Spring Boot embedded Tomcat Server
 */
@Component
public class UmdTomcatWebServerFactoryCustomizer implements WebServerFactoryCustomizer<TomcatServletWebServerFactory> {
    private final ServerProperties serverProperties;

    @Autowired
    private ConfigurationService configurationService;

    public UmdTomcatWebServerFactoryCustomizer(ServerProperties serverProperties) {
        this.serverProperties = serverProperties;
    }

    @Override
    public void customize(TomcatServletWebServerFactory factory) {
        provideJsonLogging(factory);
    }

    /**
     * Adds the JsonAccessLogValve to the Tomcat configuration, enabling logs
     * to be output in the JSON format.
     *
     * The valve is enabled by a UMD custom
     * "umd.server.tomcat.accesslog.json.enabled" property
     *
     * A custom  property, instead of the Spring Boot standard
     * "server.tomcat.accesslog.enabled" property is used to prevent
     * double-logging from the stock Spring Book AccessLogValve
     *
     * @param factory the TomcatServletWebServerFactory to configure
     */
    protected void provideJsonLogging(TomcatServletWebServerFactory factory) {
        // Control enabling of the JsonAccessLogValve based on UMD custom
        // "umd.server.tomcat.accesslog.json.enabled" property, instead of
        // the Spring Boot standard "server.tomcat.accesslog.enabled" to prevent
        // double-logging from the stock Spring Book AccessLogValve
        boolean jsonLoggerEnabled = Boolean
                .parseBoolean(configurationService.getProperty("umd.server.tomcat.accesslog.json.enabled", "false"));

        // Set JsonAccessLogValve settings from standard Spring Boot
        // server properties.
        //
        // Copied from spring-boot-project/spring-boot-autoconfigure/
        //   src/main/java/org/springframework/boot/autoconfigure/web/embedded/TomcatWebServerFactoryCustomizer.java
        ServerProperties.Tomcat tomcatProperties = this.serverProperties.getTomcat();
        JsonAccessLogValve valve = new JsonAccessLogValve();
        PropertyMapper map = PropertyMapper.get();
        Accesslog accessLogConfig = tomcatProperties.getAccesslog();

        // UMD Customization
        // Append "logfile" JSON attribute to identify the log
        String logPattern = (new StringBuilder())
            .append("{")
            .append("&quot;host&quot;:&quot;%h&quot;,")
            .append("&quot;logicalUserName&quot;:&quot;%l&quot;,")
            .append("&quot;user&quot;:&quot;%u&quot;,")
            .append("&quot;time&quot;:&quot;%t&quot;,")
            .append("&quot;request&quot;:&quot;&quot;%r&quot;&quot;,")
            .append("&quot;statusCode&quot;:&quot;%s&quot;,")
            .append("&quot;size&quot;:&quot;%b&quot;,")
            .append("&quot;elapsedTime&quot;:&quot;%D&quot;,")
            .append("&quot;logfile&quot;:&quot;access.log&quot;")
            .append("}")
            .toString();
        // End UMD Customization

        System.out.println("*****UmdTomcatWebServerFactoryCustomizer::accesLogConfig.getPattern()=" + accessLogConfig.getPattern());
        System.out.println("*****UmdTomcatWebServerFactoryCustomizer::logPattern=" + logPattern);
        map.from(accessLogConfig.getConditionIf()).to(valve::setConditionIf);
        map.from(accessLogConfig.getConditionUnless()).to(valve::setConditionUnless);
        map.from(logPattern).to(valve::setPattern);
        map.from(accessLogConfig.getDirectory()).to(valve::setDirectory);
        map.from(accessLogConfig.getPrefix()).to(valve::setPrefix);
        map.from(accessLogConfig.getSuffix()).to(valve::setSuffix);
        map.from(accessLogConfig.getEncoding()).whenHasText().to(valve::setEncoding);
        map.from(accessLogConfig.getLocale()).whenHasText().to(valve::setLocale);
        map.from(accessLogConfig.isCheckExists()).to(valve::setCheckExists);
        map.from(accessLogConfig.isRotate()).to(valve::setRotatable);
        map.from(accessLogConfig.isRenameOnRotate()).to(valve::setRenameOnRotate);
        map.from(accessLogConfig.getMaxDays()).to(valve::setMaxDays);
        map.from(accessLogConfig.getFileDateFormat()).to(valve::setFileDateFormat);
        map.from(accessLogConfig.isIpv6Canonical()).to(valve::setIpv6Canonical);
        map.from(accessLogConfig.isRequestAttributesEnabled()).to(valve::setRequestAttributesEnabled);
        map.from(accessLogConfig.isBuffered()).to(valve::setBuffered);
        map.from(jsonLoggerEnabled).to(valve::setEnabled);

        factory.addEngineValves(valve);
    }
}
