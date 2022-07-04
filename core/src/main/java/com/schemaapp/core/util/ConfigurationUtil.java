package com.schemaapp.core.util;

import java.util.Dictionary;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigurationUtil {
    private static Logger LOG = LoggerFactory.getLogger(ConfigurationUtil.class);

    /**
     * Method accepts a configuration property name to retrieve, configuration PID,
     * and a ConfigurationAdmin object and returns the property
     *
     * @param configurationProperty     Configuration property to retrieve
     * @param configurationPID          The PID of the configuration file to pull data from
     * @param configurationAdmin        ConfigurationAdmin object to retrieve the config from
     * @return T                        Returns Configuration property
     */
    public static <T> T getConfiguration(String configurationProperty, String configurationPID, ConfigurationAdmin configurationAdmin, T emptyValue) {
        T property = emptyValue;

        try {
            Configuration configuration = configurationAdmin.getConfiguration(configurationPID);
            Dictionary<String, Object> configurationProperties = configuration.getProperties();
            property = (T) configurationProperties.get(configurationProperty);
        } catch (Exception ex) {
            LOG.error("Error retrieving configuration information: {}", ex.getMessage());
        }

        return property;
    }

    ConfigurationUtil() {
        throw new IllegalStateException("Utility class");
    }
}