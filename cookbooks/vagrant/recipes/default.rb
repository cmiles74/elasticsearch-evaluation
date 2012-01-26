require_recipe "apt"
require_recipe "java"

# install build tools
package "build-essential" do
  action :install
end

# install curl and wget
package "curl" do
  action :install
end

# increase the limit on open files
script "increase_open_file_limit" do
  interpreter "bash"
  user "root"
  cwd "/tmp"
  code <<-EOH
echo 'vagrant  soft  nofile  32000' >> /etc/security/limits.conf
echo 'vagrant  hard  nofile  64000' >> /etc/security/limits.conf
echo 'session required pam_limits.so' >> /etc/pam.d/common-session
  EOH
end

require_recipe "elasticsearch"
