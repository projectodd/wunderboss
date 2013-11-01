require 'target/wunderboss-all.jar'

require 'rack'
app, _ = Rack::Builder.parse_file "config.ru"

wunderboss = Java::IoWunderboss::WunderBoss.new
wunderboss.deploy_rack_application("/", app)
wunderboss.start('localhost', 8080)
