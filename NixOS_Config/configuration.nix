{ config, pkgs, ... }:

{
	imports = [ <nixpkgs/nixos/modules/profiles/graphical.nix> 
	    	    <nixpkgs/nixos/modules/virtualisation/virtualbox-image.nix>
		   		<nixpkgs/nixos/modules/profiles/clone-config.nix>	
		   	    <nixpkgs/nixos/modules/installer/cd-dvd/channel.nix>	
		  ];

	security.audit.enable = true;
	security.audit.rules = [ "-D"
				 "-a never,exit -F path=/dev/tty"
				 "-a always,exit -F arch=b64 -S open -S openat -F success=1 -p w -k WRITE_CMD"
				 "-a always,exit -F arch=b64 -S open -S openat -F success=1 -k NORMAL_CMD"
				 "-a always,exit -F arch=b64 -S close -F success=1 -k CLOSE_CMD"
				 "-a always,exit -F arch=b64 -S exit_group -k PROC_END"
				 "-a always,exit -F arch=b64 -S execve -k PROC_ARGS"
				 "-a always,exclude -F msgtype=PROCTITLE"
				 "-a always,exclude -F auid!=1003" 
				 "-a always,exclude -F uid!=1003" 
				];
	users.extraUsers.testResearcher1 = 
	{
		isNormalUser = true;
		home = "/home/testResearcher1/";
		description = "Test Researcher 1";
		extraGroups = [ "wheel" "networkmanager" ];
	
	environment = {
		systemPackages = with pkgs; [
			vim
			gitAndTools.gitFull
			audit
			texlive.combined.scheme-full
			eclipses.eclipse-sdk
			jdk
			firefox
			wget
			];
	};
}



