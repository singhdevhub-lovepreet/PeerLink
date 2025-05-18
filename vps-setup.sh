#!/bin/bash

# PeerLink VPS Setup Script
# This script helps set up PeerLink on a fresh Ubuntu/Debian VPS

# Exit on error
set -e

echo "=== PeerLink VPS Setup Script ==="
echo "This script will install Java, Node.js, Nginx, and set up PeerLink."

# Update system
echo "Updating system packages..."
sudo apt update
sudo apt upgrade -y

# Install Java
echo "Installing Java..."
sudo apt install -y openjdk-17-jdk

# Install Node.js
echo "Installing Node.js..."
curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
sudo apt install -y nodejs

# Install Nginx
echo "Installing Nginx..."
sudo apt install -y nginx

# Install PM2
echo "Installing PM2..."
sudo npm install -g pm2

# Install Maven
echo "Installing Maven..."
sudo apt install -y maven

# Clone repository (uncomment and modify if using Git)
# echo "Cloning repository..."
# git clone https://github.com/yourusername/peerlink.git
# cd peerlink

# Build backend
echo "Building Java backend..."
mvn clean package

# Build frontend
echo "Building frontend..."
cd ui
npm install
npm run build
cd ..

# Set up Nginx
echo "Setting up Nginx..."
sudo cp nginx.conf.example /etc/nginx/sites-available/peerlink
# Replace yourdomain.com with actual domain
sudo sed -i 's/yourdomain.com/your-actual-domain.com/g' /etc/nginx/sites-available/peerlink
sudo ln -sf /etc/nginx/sites-available/peerlink /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl restart nginx

# Set up SSL with Let's Encrypt (uncomment if needed)
# echo "Setting up SSL with Let's Encrypt..."
# sudo apt install -y certbot python3-certbot-nginx
# sudo certbot --nginx -d your-actual-domain.com

# Start backend with PM2
echo "Starting backend with PM2..."
pm2 start --name peerlink-backend java -- -jar target/p2p-1.0-SNAPSHOT.jar

# Start frontend with PM2
echo "Starting frontend with PM2..."
cd ui
pm2 start npm --name peerlink-frontend -- start
cd ..

# Save PM2 configuration
pm2 save

# Set up PM2 to start on boot
echo "Setting up PM2 to start on boot..."
pm2 startup
# Follow the instructions printed by the above command

echo "=== Setup Complete ==="
echo "PeerLink is now running on your VPS!"
echo "Backend API: http://localhost:8080"
echo "Frontend: http://localhost:3000"
echo "Configure your domain DNS to point to this server's IP address."
echo "Visit https://your-actual-domain.com to access your application."
