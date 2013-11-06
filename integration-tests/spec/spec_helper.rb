require 'capybara/poltergeist'
require 'capybara/rspec'
require 'ostruct'

Capybara.app_host = "http://localhost:8080"
Capybara.run_server = false
Capybara.default_driver = :poltergeist

RSpec.configure do |config|
  config.before(:suite) do
    Java::IoWunderboss::WunderBoss.register_language('ruby', 'io.wunderboss.ruby.RubyApplication')
    WUNDERBOSS = Java::IoWunderboss::WunderBoss.new('web_host' => 'localhost', 'web_port' => '8080')
    begin
      Capybara.visit "/"
    rescue Exception => ex
      if ex.message.include?('phantomjs')
        $stderr.puts <<-EOF



========================================================================

It looks like phantomjs was not found. Ensure it is installed and
available in your $PATH. See http://phantomjs.org/download.html for
details.

========================================================================



EOF
        $stderr.puts ex.message
        exit 1
      else
        raise ex
      end
    end
  end

  config.after(:suite) do
    WUNDERBOSS.stop
  end
end

def apps_dir
  File.join(File.dirname(__FILE__), '..', 'apps')
end
