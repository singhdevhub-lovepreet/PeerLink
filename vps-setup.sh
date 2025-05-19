# Set up Nginx
echo "Setting up Nginx..."
sudo cp nginx.conf.example /etc/nginx/sites-available/peerlink
# Replace yourdomain.com with _ to listen on all hostnames for the IP
sudo sed -i 's/yourdomain.com/_/g' /etc/nginx/sites-available/peerlink
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
echo "Backend API: http://localhost:8080 (Internal - accessed via Nginx)"
echo "Frontend: http://your_lightsail_public_ip (Access via your instance's IP address)"
echo "You can access your application using your Lightsail instance's public IP address in your browser."
# echo "Visit https://your-actual-domain.com to access your application."