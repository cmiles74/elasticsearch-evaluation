Vagrant::Config.run do |config|

  # extra configuration settings
  chef_settings = {

    # we want to use sun's java implementation
    :java => {
      :install_flavor => "sun" },

    # our elasticsearch settings
    :elasticsearch => {
      :owner => "vagrant",
      :group => "vagrant",
      :install_dir => "/home/vagrant",
      :nodes => ["33.33.33.10", "33.33.33.11", "33.33.33.12"],
      :java_opts => "-server",
      :iface => "_eth1:ipv4_"}}

  config.vm.define "virtual1" do |config|
    config.vm.box = "lucid32"
    config.vm.network("33.33.33.10")
    config.vm.provision :chef_solo do |chef|
      chef.cookbooks_path = "cookbooks"
      chef.add_recipe "vagrant"
      chef.json.merge!(chef_settings)
    end
  end

  config.vm.define "virtual2" do |config|
    config.vm.box = "lucid32"
    config.vm.network("33.33.33.11")
    config.vm.provision :chef_solo do |chef|
      chef.cookbooks_path = "cookbooks"
      chef.add_recipe "vagrant"
      chef.json.merge!(chef_settings)
    end
  end

  config.vm.define "virtual3" do |config|
    config.vm.box = "lucid32"
    config.vm.network("33.33.33.12")
    config.vm.provision :chef_solo do |chef|
      chef.cookbooks_path = "cookbooks"
      chef.add_recipe "vagrant"
      chef.json.merge!(chef_settings)
    end
  end
end
