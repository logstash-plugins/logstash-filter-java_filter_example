package org.logstash.javaapi;

import co.elastic.logstash.api.Configuration;
import co.elastic.logstash.api.Context;
import co.elastic.logstash.api.Filter;
import co.elastic.logstash.api.PluginConfigSpec;
import org.apache.commons.lang3.StringUtils;
import org.logstash.Event;

import java.util.Collection;
import java.util.Collections;

// class name must match plugin name
public class JavaFilterExample implements Filter {

    public static final PluginConfigSpec<String> SOURCE_CONFIG =
            Configuration.stringSetting("source", "message");

    private String sourceField = SOURCE_CONFIG.defaultValue();

    public JavaFilterExample(Configuration config, Context context) {
        // constructors should validate configuration options
        if (config.contains(SOURCE_CONFIG)) {
            Object o = config.get(SOURCE_CONFIG);
            if (o instanceof String) {
                this.sourceField = (String)o;
            } else {
                throw new IllegalStateException(
                        String.format("Invalid value '%s' for config option %s", o, SOURCE_CONFIG));
            }
        }
    }

    @Override
    public Collection<Event> filter(Collection<Event> events) {
        for (Event e : events) {
            Object f = e.getField(sourceField);
            if (f instanceof String) {
                e.setField(sourceField, StringUtils.reverse((String)f));
            }
        }
        return events;
    }

    @Override
    public Collection<PluginConfigSpec<?>> configSchema() {
        // should return a list of all configuration options for this plugin
        return Collections.singletonList(SOURCE_CONFIG);
    }
}
