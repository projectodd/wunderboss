require 'spec_helper'

feature "basic rack" do

  before(:all) do
    @app = CONTAINER.new_application('ruby', 'root' => "#{apps_dir}/rack/basic")
    @app.start('rack', 'context' => '/basic-rack')
  end

  after(:all) do
    @app.stop
  end

  before(:each) do
    visit "/basic-rack"
    page.should have_content('it worked')
  end

  it "should work" do
    page.find("#success")[:class].strip.should == 'basic-rack'
    page.find("#path_info").text.strip.should == '/'
    page.find("#request_uri").text.strip.should == '/basic-rack/'
  end

  it "should be running under the proper ruby version" do
    page.find("#ruby-version").text.strip.should == RUBY_VERSION
  end

  it "should not have a vfs path for __FILE__" do
    page.find("#path").text.strip.should_not match(/^vfs:/)
  end

  it "should not decode characters in URL" do
    visit "/basic-rack/foo%23%2C"
    page.find("#path_info").text.strip.should == '/foo%23%2C'
    page.find("#request_uri").text.strip.should == '/basic-rack/foo%23%2C'
  end

end