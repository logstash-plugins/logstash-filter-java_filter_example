# Logstash Java Plugin

[![Travis Build Status](https://travis-ci.org/logstash-plugins/logstash-filter-java_filter_example.svg)](https://travis-ci.org/logstash-plugins/logstash-filter-java_filter_example)

This is a Java plugin for [Logstash](https://github.com/elastic/logstash).

It is fully free and fully open source. The license is Apache 2.0, meaning you are free to use it however you want.

## How to write a Java filter

> <b>IMPORTANT NOTE:</b> Native support for Java plugins in Logstash is in the experimental phase. While unnecessary
changes will be avoided, anything may change in future phases. See the ongoing work on the 
[beta phase](https://github.com/elastic/logstash/pull/10232) of Java plugin support for the most up-to-date status.

### Overview 

Native support for Java plugins in Logstash consists of several components including:
* Extensions to the Java execution engine to support running Java plugins in Logstash pipelines
* APIs for developing Java plugins. The APIs are in the `co.elastic.logstash.api` package. If a Java plugin 
references any classes or specific concrete implementations of API interfaces outside that package, breakage may 
occur because the implementation of classes outside of the API package may change at any time.
* Tooling to automate the packaging and deployment of Java plugins in Logstash [not complete as of the experimental phase]

To develop a new Java filter for Logstash, you write a new Java class that conforms to the Logstash Java Filter
API, package it, and install it with the `logstash-plugin` utility. We'll go through each of those steps in this guide.

### Coding the plugin

It is recommended that you start by copying the 
[example filter plugin](https://github.com/logstash-plugins/logstash-filter-java_filter_example). The example filter
plugin allows one to configure a field in each event that will be reversed. For example, if the filter were 
configured to reverse the `day_of_week` field, an event with `day_of_week: "Monday"` would be transformed to
`day_of_week: "yadnoM"`. Let's look at the main class in that example filter:
 
```java
@LogstashPlugin(name = "java_filter_example")
public class JavaFilterExample implements Filter {

    public static final PluginConfigSpec<String> SOURCE_CONFIG =
            Configuration.stringSetting("source", "message");

    private String sourceField;

    public JavaFilterExample(Configuration config, Context context) {
        this.sourceField = config.get(SOURCE_CONFIG);
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
        return Collections.singletonList(SOURCE_CONFIG);
    }
}
```

Let's step through and examine each part of that class.

#### Class declaration
```java
@LogstashPlugin(name = "java_filter_example")
public class JavaFilterExample implements Filter {
```
There are two things to note about the class declaration:
* All Java plugins must be annotated with the `@LogstashPlugin` annotation. Additionally:
  * The `name` property of the annotation must be supplied and defines the name of the plugin as it will be used
   in the Logstash pipeline definition. For example, this filter would be referenced in the filter section of the
   Logstash pipeline defintion as `filter { java_filter_example => { .... } }`
  * The value of the `name` property must match the name of the class excluding casing and underscores.
* The class must implement the `co.elastic.logstash.api.v0.Filter` interface.

#### Plugin settings

The snippet below contains both the setting definition and the method referencing it:
```java
public static final PluginConfigSpec<String> SOURCE_CONFIG =
        Configuration.stringSetting("source", "message");

@Override
public Collection<PluginConfigSpec<?>> configSchema() {
    return Collections.singletonList(SOURCE_CONFIG);
}
```
The `PluginConfigSpec` class allows developers to specify the settings that a plugin supports complete with setting 
name, data type, deprecation status, required status, and default value. In this example, the `source` setting defines
the name of the field in each event that will be reversed. It is not a required setting and if it is not explicitly
set, its default value will be `message`.

The `configSchema` method must return a list of all settings that the plugin supports. In a future phase of the
Java plugin project, the Logstash execution engine will validate that all required settings are present and that
no unsupported settings are present.

#### Constructor and initialization
```java
private String sourceField;

public JavaFilterExample(Configuration config, Context context) {
    this.sourceField = config.get(SOURCE_CONFIG);
}
```
All Java filter plugins must have a constructor taking both a `Configuration` and `Context` argument. This is the
constructor that will be used to instantiate them at runtime. The retrieval and validation of all plugin settings
should occur in this constructor. In this example, the name of the field to be reversed in each event is retrieved
from its setting and stored in a local variable so that it can be used later in the `filter` method. 

Any additional initialization may occur in the constructor as well. If there are any unrecoverable errors encountered
in the configuration or initialization of the filter plugin, a descriptive exception should be thrown. The exception
will be logged and will prevent Logstash from starting.

#### Filter method
```java
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
```
Finally, we come to the `filter` method that is invoked by the Logstash execution engine on batches of events as
they flow through the event processing pipeline. The events to be filtered are supplied in the `events` argument
and the method should return a collection of filtered events. Filters may perform a variety of actions on events as
they flow through the pipeline including:
* Mutation -- Fields in events may be added, removed, or changed by a filter. This is the most common scenario for 
filters that perform various kinds of enrichment on events. In this scenario, the incoming `events` collection may be
returned unmodified since the events in the collection are mutated in place.
* Deletion -- Events may be removed from the event pipeline by a filter so that subsequent filters and outputs 
do not receive them. In this scenario, the events to be deleted must be removed from the collection of filtered
events before it is returned. 
* Creation -- A filter may insert new events into the event pipeline that will be seen only by subsequent
filters and outputs. In this scenario, the new events must be added to the collection of filtered events before
it is returned.
* Observation -- Events may pass unchanged by a filter through the event pipeline. This may be useful in
scenarios where a filter performs external actions (e.g., updating an external cache) based on the events observed
in the event pipeline. In this scenario, the incoming `events` collection may be returned unmodified since no
changes were made.

In the example above, the value of the `source` field is retrieved from each event and reversed if it is a string
value. Because each event is mutated in place, the incoming `events` collection can be returned.

#### Unit tests
Lastly, but certainly not least importantly, unit tests are strongly encouraged. The example filter plugin includes
an [example unit test](https://github.com/logstash-plugins/logstash-filter-java_filter_example/blob/master/src/test/java/org/logstash/javaapi/JavaFilterExampleTest.java)
that you can use as a template for your own.

### Packaging and deployment

For the purposes of dependency management and interoperability with Ruby plugins, Java plugins will be packaged
as Ruby gems. One of the goals for Java plugin support is to eliminate the need for any knowledge of Ruby or its
toolchain for Java plugin development. Future phases of the Java plugin project will automate the packaging of
Java plugins as Ruby gems so no direct knowledge of or interaction with Ruby will be required. In the experimental
phase, Java plugins must still be manually packaged as Ruby gems and installed with the `logstash-plugin` utility.

#### Compile to JAR file

The Java plugin should be compiled and assembled into a fat jar with the `vendor` task in the Gradle build file. This
will package all Java dependencies into a single jar and write it to the correct folder for later packaging into
a Ruby gem.

#### Manual packaging as Ruby gem 

Several Ruby source files are required to correctly package the jar file as a Ruby gem. These Ruby files are used
only at Logstash startup time to identify the Java plugin and are not used during runtime event processing. In a 
future phase of the Java plugin support project, these Ruby source files will be automatically generated. 

`logstash-filter-<filter-name>.gemspec`
```
Gem::Specification.new do |s|
  s.name            = 'logstash-filter-java_filter_example'
  s.version         = '0.0.1'
  s.licenses        = ['Apache-2.0']
  s.summary         = "Example filter using Java plugin API"
  s.description     = ""
  s.authors         = ['Elasticsearch']
  s.email           = 'info@elastic.co'
  s.homepage        = "http://www.elastic.co/guide/en/logstash/current/index.html"
  s.require_paths = ['lib', 'vendor/jar-dependencies']

  # Files
  s.files = Dir["lib/**/*","spec/**/*","*.gemspec","*.md","CONTRIBUTORS","Gemfile","LICENSE","NOTICE.TXT", "vendor/jar-dependencies/**/*.jar", "vendor/jar-dependencies/**/*.rb", "VERSION", "docs/**/*"]

  # Special flag to let us know this is actually a logstash plugin
  s.metadata = { 'logstash_plugin' => 'true', 'logstash_group' => 'filter'}

  # Gem dependencies
  s.add_runtime_dependency "logstash-core-plugin-api", ">= 1.60", "<= 2.99"
  s.add_runtime_dependency 'jar-dependencies'

  s.add_development_dependency 'logstash-devutils'
end
```
The above file can be used unmodified except that `s.name` must follow the `logstash-filter-<filter-name>` pattern
and `s.version` must match the `project.version` specified in the `build.gradle` file.

`lib/logstash/filters/<filter-name>.rb`
```
# encoding: utf-8
require "logstash/filters/base"
require "logstash/namespace"
require "logstash-filter-java_filter_example_jars"
require "java"

class LogStash::Filters::JavaFilterExample < LogStash::Filters::Base
  config_name "java_filter_example"

  def self.javaClass() org.logstash.javaapi.JavaFilterExample.java_class; end
end
```
The following items should be modified in the file above:
1. It should be named to correspond with the filter name.
1. `require "logstash-filter-java_filter_example_jars"` should be changed to reference the appropriate "jars" file
as described below.
1. `class LogStash::Filters::JavaFilterExample < LogStash::Filters::Base` should be changed to provide a unique and
descriptive Ruby class name.
1. `config_name "java_filter_example"` must match the name of the plugin as specified in the `name` property of
the `@LogstashPlugin` annotation.
1. `def self.javaClass() org.logstash.javaapi.JavaFilterExample.java_class; end` must be modified to return the
class of the Java filter.

`lib/logstash-filter-<filter-name>_jars.rb`
```
require 'jar_dependencies'
require_jar('org.logstash.javaapi', 'logstash-filter-java_filter_example', '0.0.1')
```
The following items should be modified in the file above:
1. It should be named to correspond with the filter name.
1. The `require_jar` directive should be modified to correspond to the `group` specified in the Gradle build file,
the name of the filter JAR file, and the version as specified in both the gemspec and Gradle build file.

Once the above files have been properly created along with the plugin JAR file, the gem can be built with the
following command:
```
gem build logstash-filter-<filter-name>.gemspec
``` 

#### Installing the Java plugin in Logstash

Once your Java plugin has been packaged as a Ruby gem, it can be installed in Logstash with the following command:
```
bin/logstash-plugin install --no-verify --local /path/to/javaPlugin.gem
```
Substitute backslashes for forward slashes as appropriate in the command above for installation on Windows platforms. 

### Feedback

If you have any feedback on Java plugin support in Logstash, please comment on our 
[main Github issue](https://github.com/elastic/logstash/issues/9215) or post in the 
[Logstash forum](https://discuss.elastic.co/c/logstash).