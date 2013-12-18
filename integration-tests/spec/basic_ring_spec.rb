require 'spec_helper'

feature "basic ring at non-root context" do

  before(:all) do
    dir = "#{apps_dir}/ring/basic"
    @app = container('root' => dir,
                     'classpath' => lein_classpath(dir))
      .new_application('clojure',
                       'ring-handler' => "basic.core/handler")
    @app.start('ring', 'context' => '/basic-ring')
  end

  after(:all) do
    @app.stop
  end

  it "should work for basic requests" do
    data = get_edn "/basic-ring"
    data[:uri].should == '/basic-ring'
  end

  # it "should work for subcontext root with trailing slash" do
  #   data = get_edn "/basic-ring/"
  #   data[:uri].should == '/basic-ring/'
  #   data[:"path-info"].should == '/'
  # end

  # it "should be running under the proper ruby version" do
  #   visit "/basic-ring/"
  #   page.find("#ruby-version").text.strip.should == RUBY_VERSION
  # end

  # it "should not have a vfs path for __FILE__" do
  #   visit "/basic-ring/"
  #   page.find("#path").text.strip.should_not match(/^vfs:/)
  # end

  # it "should not decode characters in URL" do
  #   visit "/basic-ring/foo%23%2C"
  #   page.find("#path_info").text.strip.should == '/foo%23%2C'
  #   page.find("#request_uri").text.strip.should == '/basic-ring/foo%23%2C'
  # end

  # it "should contain correct request headers" do
  #   uri = URI.parse("#{Capybara.app_host}/basic-ring/")
  #   Net::HTTP.start(uri.host, uri.port) do |http|
  #     accept = 'text/html;q=0.9,*/*;q=0.7'
  #     response = http.get(uri.request_uri, {'Accept' => accept})
  #     response.code.should == "200"
  #     response.body.should include("<div id='accept_header'>#{accept}</div>")
  #   end
  # end

  # it "should read post bodies via gets" do
  #   uri = URI.parse("#{Capybara.app_host}/basic-ring/gets")
  #   Net::HTTP.start(uri.host, uri.port) do |http|
  #     request = Net::HTTP::Post.new(uri.request_uri)
  #     request.form_data = {'field' => 'nothing'}
  #     response = http.request(request)
  #     response.body.should include("<div id='posted'>field=nothing</div>")
  #   end
  # end

  # it "should read post bodies via read" do
  #   uri = URI.parse("#{Capybara.app_host}/basic-ring/read")
  #   Net::HTTP.start(uri.host, uri.port) do |http|
  #     request = Net::HTTP::Post.new(uri.request_uri)
  #     request.form_data = {'field' => 'nothing'}
  #     response = http.request(request)
  #     response.body.should include("<div id='posted'>field=nothing</div>")
  #   end
  # end

  # it "should read post bodies via each" do
  #   uri = URI.parse("#{Capybara.app_host}/basic-ring/each")
  #   Net::HTTP.start(uri.host, uri.port) do |http|
  #     request = Net::HTTP::Post.new(uri.request_uri)
  #     request.form_data = {'field' => 'nothing'}
  #     response = http.request(request)
  #     response.body.should include("<div id='posted'>field=nothing</div>")
  #   end
  # end

end

feature "basic ring at root context" do
  # before(:all) do
  #   dir = "#{apps_dir}/ring/basic"
  #   @app = CONTAINER.new_application('clojure',
  #                                    'root' => dir,
  #                                    'classpath' => lein_classpath(dir),
  #                                    'ring-handler' => "basic.core/handler")
  #   @app.start('ring', 'context' => '/')
  # end

  # after(:all) do
  #   @app.stop
  # end

  # it "should have correct path information" do
  #   visit "/plaintext"
  #   page.should have_content('it worked')
  #   page.find("#success")[:class].strip.should == 'basic-ring'
  #   page.find("#path_info").text.strip.should == '/plaintext'
  #   page.find("#request_uri").text.strip.should == '/plaintext'
  # end

end
