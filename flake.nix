{
  description = "RetractableBridges Minecraft plugin dev environment";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-25.11";
  };

  outputs = { self, nixpkgs }:
    let
      systems = [ "x86_64-linux" "aarch64-linux" "x86_64-darwin" "aarch64-darwin" ];
      forAllSystems = nixpkgs.lib.genAttrs systems;
    in {
      devShells = forAllSystems (system:
        let
          pkgs = nixpkgs.legacyPackages.${system};
        in {
          default = pkgs.mkShell {
            buildInputs = [
              pkgs.jdk25
              pkgs.maven
            ];

            JAVA_HOME = "${pkgs.jdk25}";

            shellHook = ''
              echo "RetractableBridges dev shell"
              java -version 2>&1 | head -1
              mvn --version 2>&1 | head -1
            '';
          };
        }
      );
    };
}
