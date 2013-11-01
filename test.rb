require 'target/wunderboss-all.jar'

require 'rack'
app, _ = Rack::Builder.parse_file "config.ru"

wunderboss = Java::IoWunderboss::WunderBoss.new('localhost', 8080)
wunderboss.deploy_rack_application("/", app)
