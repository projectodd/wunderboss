require 'modules/ruby/target/wunderboss-all.jar'

# does "container" carry too much baggage?
container = Java::OrgProjectoddWunderboss::WunderBoss.new

# Language implementations are always bootstraped as Java classes.
container.register_language('ruby', Java::OrgProjectoddWunderbossRuby::RubyLanguage.new)


# Components are written in Java only (for now)
container.register_component('web', Java::OrgProjectoddWunderbossWeb::WebComponent.new)
container.register_component('servlet', Java::OrgProjectoddWunderbossWeb::ServletComponent.new)
container.register_component('rack', Java::OrgProjectoddWunderbossRubyRack::RackComponent.new)
# container.register_component('job', Java::IoWunderbossJob::JobComponent.new)

# If running inside WildFly, the .configure calls wouldn't be necessary
# for WildFly-provided services, like web. But we'd still allow it for things
# we bring to WildFly, like jobs. Perhaps confusing.
container.configure('web', 'host' => '0.0.0.0', 'port' => '8080')

my_app = container.new_application('ruby')

rack = my_app.start('rack', 'context' => '/')

# job_func = lambda { puts "!!! Running job" }
# my_app.start('job', 'cron' => '*/5 * * * * ?', 'run_function' => job_func)

Signal.trap("INT") do
  container.stop
end

# rack.stop
# app.stop

# container.stop
