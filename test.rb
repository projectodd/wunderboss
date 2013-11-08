require 'modules/ruby/target/wunderboss-all.jar'

# does "container" carry too much baggage?
container = Java::IoWunderboss::WunderBoss.new

# Language implementations are always bootstraped as Java classes.
container.register_language('ruby', Java::IoWunderbossRuby::RubyLanguage.new)


# Components are written in Java only (for now)
container.register_component('web', Java::IoWunderbossWeb::WebComponent.new)
container.register_component('servlet', Java::IoWunderbossWeb::ServletComponent.new)
container.register_component('rack', Java::IoWunderbossRubyRack::RackComponent.new)

# If running inside WildFly, the .configure calls wouldn't be necessary
# for WildFly-provided services, like web. But we'd still allow it for things
# we bring to WildFly, like jobs. Perhaps confusing.
container.configure('web', 'host' => 'localhost', 'port' => '8080')

app = container.new_application('ruby')
rack = app.start('rack', 'context' => '/')

# rack.stop
# app.stop

# container.stop
