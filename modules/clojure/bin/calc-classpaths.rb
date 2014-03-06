require 'fileutils'

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

  Dir.chdir(dir) { %x{lein classpath} }.strip
end

puts "Calculating lein classpath for #{ARGV[0]}..."

target_dir = ARGV[1]
FileUtils.mkdir_p target_dir

File.open("#{target_dir}/lein-classpath", "w") do |f|
  f.write(lein_classpath(ARGV[0]))
end
