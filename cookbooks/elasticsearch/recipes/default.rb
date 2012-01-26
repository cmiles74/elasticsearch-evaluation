# download elasticsearch
script "download_install_elasticsearch" do
  interpreter "bash"
  cwd "#{node[:elasticsearch][:install_dir]}"
  user "root"
  code <<-EOH
curl http://cloud.github.com/downloads/elasticsearch/elasticsearch/elasticsearch-0.18.7.tar.gz -o elasticsearch.tar.gz
mkdir elasticsearch
tar -zxvf elasticsearch.tar.gz -C elasticsearch --strip 1
rm elasticsearch.tar.gz
EOH
end

# write out our configuration file
template "elasticsearch.yml" do
  path "#{node[:elasticsearch][:install_dir]}/elasticsearch/config/elasticsearch.yml"
  source "elasticsearch.yml.erb"
end

# write out our setup file
template "elasticsearch.in.sh" do
  path "#{node[:elasticsearch][:install_dir]}/elasticsearch/bin/elasticsearch.in.sh"
  source "elasticsearch.in.sh.erb"
end

# install head
script "install_head" do
  interpreter "bash"
  cwd "#{node[:elasticsearch][:install_dir]}/elasticsearch"
  user "root"
  code <<-EOH
bin/plugin -install mobz/elasticsearch-head
EOH
end

# startup elasticsearch
script "fix_permissions" do
  interpreter "bash"
  cwd "#{node[:elasticsearch][:install_dir]}"
  user "root"
  code <<-EOH
chown -Rf #{node[:elasticsearch][:owner]}:#{node[:elasticsearch][:group]} elasticsearch
chmod -Rf ug+rwX elasticsearch
EOH
end

# startup elasticsearch
script "start_elasticsearch" do
  interpreter "bash"
  cwd "#{node[:elasticsearch][:install_dir]}/elasticsearch"
  user node[:elasticsearch][:owner]
  code <<-EOH
bin/elasticsearch
exit 0
EOH
end
