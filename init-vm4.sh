# Init script for the firewall

# Configure IP
sudo ifconfig eth0 192.168.1.254/24 up
sudo ifconfig eth1 192.168.2.254/24 up
sudo ifconfig eth2 192.168.3.254/24 up
sudo sysctl net.ipv4.ip_forward=1

# Configure NAT
sudo iptables -t nat -F
sudo iptables -t nat -A PREROUTING  -p tcp --dport 8443 --dst 192.168.3.254 --to-destination 192.168.2.2 -j DNAT

# Configure firewall
sudo iptables -F
sudo iptables -A INPUT -j DROP
sudo iptables -A FORWARD -p tcp --dport 5432 -d 192.168.1.1 --sport 1024:65535 -s 192.168.2.2 -j ACCEPT
sudo iptables -A FORWARD -p tcp --sport 5432 -s 192.168.1.1 --dport 1024:65535 -d 192.168.2.2 -j ACCEPT
sudo iptables -A FORWARD -p tcp --dport 8443 -d 192.168.2.2 --sport 1024:65535 -j ACCEPT
sudo iptables -A FORWARD -p tcp --sport 8443 -s 192.168.2.2 --dport 1024:65535 -j ACCEPT
sudo iptables -A FORWARD -j DROP
sudo iptables -A OUTPUT -j DROP
