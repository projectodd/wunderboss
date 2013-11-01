require 'target/wunderboss-all.jar'

config = {'web_host' => 'localhost', 'web_port' => '8080'}
wunderboss = Java::IoWunderboss::WunderBoss.new(config)
wunderboss.deploy_ruby_application('.', {'web_context' => '/'})
