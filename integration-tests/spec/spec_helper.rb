require 'capybara/poltergeist'
require 'capybara/rspec'
require 'ostruct'

Capybara.app_host = "http://localhost:8080"
Capybara.run_server = false
Capybara.default_driver = :poltergeist

RSpec.configure do |config|
  config.before(:suite) do
    begin
      CONTAINER = Java::IoWunderboss::WunderBoss.new
      CONTAINER.register_language('ruby', Java::IoWunderbossRuby::RubyLanguage.new)
      CONTAINER.register_component('web', Java::IoWunderbossWeb::WebComponent.new)
      CONTAINER.register_component('servlet', Java::IoWunderbossWeb::ServletComponent.new)
      CONTAINER.register_component('rack', Java::IoWunderbossRubyRack::RackComponent.new)
      CONTAINER.configure('web', 'host' => 'localhost', 'port' => '8080')
    rescue Exception => ex
      puts ex.inspect
      puts ex.backtrace
      raise ex
    end

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
    CONTAINER.stop
  end
end

def apps_dir
  File.join(File.dirname(__FILE__), '..', 'apps')
end
