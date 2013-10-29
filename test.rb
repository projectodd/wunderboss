require 'target/wunderboss-all.jar'

# Setup logging to stdout with simple format
logger = java.util.logging.Logger.getLogger("")
logger.level = java.util.logging.Level::INFO
class WunderBossFormatter < java.util.logging.Formatter
  def format(record)
    prefix = "#{record.level} [#{record.logger_name}]"
    msg = "#{prefix} #{record.message}\n"
    if record.thrown
      msg << "#{prefix} #{record.thrown.message}\n"
      record.thrown.stack_trace.each do |trace|
        msg << "#{prefix} #{trace}\n"
      end
    end
    msg
  end
end
formatter = WunderBossFormatter.new
logger.handlers.each do |handler|
  handler.formatter = formatter
end


require 'rack'
app, _ = Rack::Builder.parse_file "config.ru"

wunderboss = Java::IoWunderboss::WunderBoss.new
wunderboss.deploy_rack_application("/foo", app)
wunderboss.start('localhost', 8081)
