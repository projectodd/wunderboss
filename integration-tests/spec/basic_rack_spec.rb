require 'spec_helper'

feature "basic rack" do

  before(:all) do
    app = WUNDERBOSS.deploy_application('root' => "#{apps_dir}/rack/basic",
                                         'language' => 'ruby')
    app.deploy_web('context' => '/basic-rack')
  end

  after(:all) do
    WUNDERBOSS.stop
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
