require 'capybara/poltergeist'
require 'capybara/rspec'
require 'ostruct'
require 'edn'
require 'net/http'

Capybara.app_host = "http://localhost:8080"
Capybara.run_server = false
Capybara.default_driver = :poltergeist

RSpec.configure do |config|
  config.before(:suite) do
    begin
      CONTAINER = Java::OrgProjectoddWunderboss::WunderBoss.new
      CONTAINER.register_component('web', Java::OrgProjectoddWunderbossWeb::WebComponent.new)
      CONTAINER.register_component('servlet', Java::OrgProjectoddWunderbossWeb::ServletComponent.new)
      CONTAINER.register_language('ruby', Java::OrgProjectoddWunderbossRuby::RubyLanguage.new)
      CONTAINER.register_component('rack', Java::OrgProjectoddWunderbossRubyRack::RackComponent.new)
      CONTAINER.register_language('clojure', Java::OrgProjectoddWunderbossClojure::ClojureLanguage.new)
      CONTAINER.register_component('ring', Java::OrgProjectoddWunderbossClojureRing::RingComponent.new)
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

def lein_classpath(dir)
  %x{which lein}
  if $?.exitstatus != 0
    $stderr.puts <<-EOF



========================================================================

It looks like leiningen was not found. Ensure it is installed and
available in your $PATH. See http://leiningen.org/ for details.

========================================================================



EOF
    $stderr.puts ex.message
    exit 1
  end

  Dir.chdir(dir) { %x{lein classpath} }
    .strip
    .split(':')
    .map {|f| java.io.File.new(f).toURI.toURL}
end

def get_edn(path)
  EDN.read(Net::HTTP.get(URI("http://localhost:8080#{path}")))
end
