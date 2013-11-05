require 'spec_helper'

feature "basic sinatra test" do

  before(:all) do
    app = WUNDERBOSS.deploy_application('root' => "#{apps_dir}/sinatra/basic",
                                        'language' => 'ruby')
    app.deploy_web('context' => '/basic-sinatra')
  end

  after(:all) do
    WUNDERBOSS.stop
  end

  it "should work" do
    visit "/basic-sinatra"
    page.should have_content('it worked')
    page.should have_selector('div.sinatra-basic')
    find("#success").should have_content('it worked')
  end

  it "should return a valid request scheme" do
    visit "/basic-sinatra/request-mapping"
    find("#success #scheme").text.should eql("http")
  end

  it "should return a static page beneath default 'public' dir" do
    visit "/basic-sinatra/some_page.html"
    page.find('#success')[:class].should == 'default'
  end

  it "should return 304 for unmodified static assets" do
    uri = URI.parse("#{Capybara.app_host}/basic-sinatra/some_page.html")
    http = Net::HTTP.new(uri.host, uri.port)
    request = Net::HTTP::Get.new(uri.request_uri)
    request.add_field('If-Modified-Since', 'Sat, 31 Dec 2050 00:00:00 GMT')
    response = http.request(request)
    response.code.should == "304"
  end

  it "should post something" do
    visit "/basic-sinatra/poster"
    fill_in 'field', :with => 'something'
    click_button 'submit'
    find('#success').should have_content("you posted something")
  end


  it "should allow headers through" do
    uri = URI.parse("#{Capybara.app_host}/basic-sinatra/")
    http = Net::HTTP.new(uri.host, uri.port)
    request = Net::HTTP::Get.new(uri.request_uri)
    response = http.request(request)
    response['Biscuit'].should == 'Gravy'
  end

  it "should allow OPTIONS requests" do
    uri = URI.parse("#{Capybara.app_host}/basic-sinatra/")
    http = Net::HTTP.new(uri.host, uri.port)
    request = Net::HTTP::Options.new(uri.request_uri)
    response = http.request(request)
    response['access-control-allow-origin'].should == '*'
    response['access-control-allow-methods'].should == 'POST'

  end

  it "should test Sir Postalot" do
    500.times do |i|
      print '.' if (i % 10 == 0)
      visit "/basic-sinatra/poster"
      click_button 'submit'
      find('#success').text.should == "you posted nothing"
    end
    puts " complete!"
  end

end
