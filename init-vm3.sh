# Init script for the client

# Install dependencies
sudo apt update
sudo apt install maven
mvn install -pl BlingBankUser -am

# Configure ip
sudo ifconfig eth0 192.168.3.3/24 up
