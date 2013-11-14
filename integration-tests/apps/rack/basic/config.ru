require 'rack'

use Rack::Lint

app = lambda { |env|
  body = <<-EOF
<div id='success' class='basic-rack'>it worked</div>
<div id='ruby-version'>#{RUBY_VERSION}</div>
<div id='path'>#{__FILE__}</div>
<div id='path_info'>#{env['PATH_INFO']}</div>
<div id='request_uri'>#{env['REQUEST_URI']}</div>
<div id='accept_header'>#{env['HTTP_ACCEPT']}</div>
EOF
  [200, { 'Content-Type' => 'text/html' }, [body]]
}
run app
