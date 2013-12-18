require 'capybara/poltergeist'
require 'capybara/rspec'
require 'ostruct'
require 'edn'
require 'net/http'
require 'wunderboss'

Capybara.app_host = "http://localhost:8080"
Capybara.run_server = false
Capybara.default_driver = :poltergeist


$containers = []

def container(opts=nil)
  container = WunderBoss.container(opts)
  container.register_component('web', Java::OrgProjectoddWunderbossWeb::WebComponent.new)
  container.register_component('servlet', Java::OrgProjectoddWunderbossWeb::ServletComponent.new)
  container.register_language('ruby', Java::OrgProjectoddWunderbossRuby::RubyLanguage.new)
  container.register_component('rack', Java::OrgProjectoddWunderbossRubyRack::RackComponent.new)
  container.register_language('clojure', Java::OrgProjectoddWunderbossClojure::ClojureLanguage.new)
  container.register_component('ring', Java::OrgProjectoddWunderbossClojureRing::RingComponent.new)
  container.configure('web', 'host' => 'localhost', 'port' => '8080')
  $containers << container
  container
rescue Exception => ex
  puts ex.inspect
  puts ex.backtrace
  raise ex
end

def stop_containers
  $containers.each(&:stop)
  $containers = []
end
  
RSpec.configure do |config|

  config.before(:suite) do
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

  config.after(:all) do
    stop_containers
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
