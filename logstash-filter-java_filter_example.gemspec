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
