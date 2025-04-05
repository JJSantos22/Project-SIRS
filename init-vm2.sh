# Init script for the web server

# Install dependencies
sudo apt update
sudo apt install maven
mvn install -pl Libraries -am

# Configure ip
sudo ifconfig eth0 192.168.2.2/24 up
sudo ip route add default via 192.168.2.254
