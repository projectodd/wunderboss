require 'modules/ruby/target/wunderboss-all.jar'

# TODO: This should all be wrapped in a Ruby API vs calling
# the Java API directly
Java::IoWunderboss::WunderBoss.register_language('ruby', 'io.wunderboss.ruby.RubyApplication')
wunderboss = Java::IoWunderboss::WunderBoss.new('web_host' => 'localhost', 'web_port' => '8080')
# app1 = wunderboss.deploy_application('root' => '.', 'language' => 'ruby')
app1 = wunderboss.deploy_application('root' => '/home/bbrowning/src/wunderboss/integration-tests/apps/sinatra/basic', 'language' => 'ruby')
app1.deploy_web('context' => '/basic-sinatra')
# app1.deploy_job('* * * * * ?') do
#   puts "!!! Running job"
# end
