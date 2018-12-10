package org.logstash.javaapi;

import co.elastic.logstash.api.Configuration;
import co.elastic.logstash.api.Context;
import org.junit.Assert;
import org.junit.Test;
import org.logstash.Event;

import java.util.Collection;
import java.util.Collections;

public class JavaFilterExampleTest {

    @Test
    public void testJavaExampleFilter() {
        String sourceField = "foo";
        Configuration config = new Configuration(Collections.singletonMap("source", sourceField));
        Context context = new Context();
        JavaFilterExample filter = new JavaFilterExample(config, context);

        Event e = new Event();
        e.setField(sourceField, "abcdef");
        Collection<Event> results = filter.filter(Collections.singletonList(e));

        Assert.assertEquals(1, results.size());
        Assert.assertEquals("fedcba", e.getField(sourceField));
    }
}
