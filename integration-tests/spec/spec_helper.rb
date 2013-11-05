require 'selenium-webdriver'
require 'capybara/rspec'
require 'ostruct'

Capybara.app_host = "http://localhost:8080"
Capybara.run_server = false
Capybara.default_driver = :selenium

Java::IoWunderboss::WunderBoss.register_language('ruby', 'io.wunderboss.ruby.RubyApplication')
WUNDERBOSS = Java::IoWunderboss::WunderBoss.new('web_host' => 'localhost', 'web_port' => '8080')

def apps_dir
  File.join(File.dirname(__FILE__), '..', 'apps')
end

# Monkey patches against the Selenium WebDriver to get easier access
# to cookies
class BrowserCookies
  def initialize(manage)
    @manage = manage
  end

  def clear
    @manage.delete_all_cookies()
  end

  def count
    @manage.all_cookies.size
  end

  def [](name)
    @manage.all_cookies.each do |cookie|
      return OpenStruct.new(cookie) if (cookie[:name] == name)
    end
    nil
  end
end
class Capybara::Selenium::Driver
  def cookies
    @cookies ||= BrowserCookies.new(browser.manage);
  end

  def reset!
    @cookies.clear if @cookies
    @cookies = nil
  end
end
