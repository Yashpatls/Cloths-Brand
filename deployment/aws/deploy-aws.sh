#!/usr/bin/env bash
# ==============================================================================
# StyleHub — AWS EC2 Automated Deployment & Provisioning Script
# Target OS: Ubuntu 22.04 / 24.04 LTS
# ==============================================================================

set -euo pipefail

echo "========================================================"
echo " Starting StyleHub AWS EC2 Deployment"
echo "========================================================"

# 1. Update OS packages
echo "==> Updating apt repositories..."
sudo apt update -y
sudo apt upgrade -y

# 2. Install OpenJDK 21, Nginx, and essential utilities
echo "==> Installing OpenJDK 21, Nginx, and tools..."
sudo apt install -y openjdk-21-jre-headless nginx curl git ufw

# 3. Create stylehub system user & app directory
echo "==> Setting up /opt/stylehub application directory..."
sudo mkdir -p /opt/stylehub
sudo mkdir -p /opt/stylehub/uploads
sudo chown -R ubuntu:ubuntu /opt/stylehub

# 4. Copy runnable jar
if [ -f "target/stylehub-1.0.0.jar" ]; then
    echo "==> Copying stylehub-1.0.0.jar to /opt/stylehub/..."
    cp target/stylehub-1.0.0.jar /opt/stylehub/stylehub.jar
else
    echo "==> Warning: target/stylehub-1.0.0.jar not found locally. Ensure jar is placed at /opt/stylehub/stylehub.jar"
fi

# 5. Setup environment configuration template if not present
if [ ! -f "/etc/stylehub.env" ]; then
    echo "==> Generating /etc/stylehub.env..."
    sudo tee /etc/stylehub.env > /dev/null <<EOF
# StyleHub AWS Production Environment
SPRING_PROFILES_ACTIVE=prod
PORT=8080
DB_URL=jdbc:mysql://YOUR-RDS-ENDPOINT:3306/stylehub?useSSL=true&allowPublicKeyRetrieval=true&serverTimezone=UTC
DB_USERNAME=stylehub_admin
DB_PASSWORD=REPLACE_WITH_RDS_PASSWORD
AWS_REGION=ap-south-1
AWS_S3_BUCKET=stylehub-media-bucket
RAZORPAY_KEY_ID=rzp_test_mock_stylehub
RAZORPAY_KEY_SECRET=mock_secret_key_12345
EOF
    sudo chmod 600 /etc/stylehub.env
    echo "==> Please update /etc/stylehub.env with your real RDS endpoint & secrets."
fi

# 6. Install systemd service
echo "==> Installing systemd service..."
sudo cp deployment/aws/stylehub.service /etc/systemd/system/stylehub.service
sudo systemctl daemon-reload
sudo systemctl enable stylehub

# 7. Configure Nginx Reverse Proxy
echo "==> Configuring Nginx..."
sudo cp deployment/aws/nginx-stylehub.conf /etc/nginx/sites-available/stylehub
sudo ln -sf /etc/nginx/sites-available/stylehub /etc/nginx/sites-enabled/stylehub
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t
sudo systemctl restart nginx

# 8. Start StyleHub Service
echo "==> Starting StyleHub Spring Boot service..."
sudo systemctl restart stylehub || true

echo "========================================================"
echo " StyleHub Deployment Complete!"
echo " Check status with: sudo systemctl status stylehub"
echo " View logs with: sudo journalctl -u stylehub -f"
echo "========================================================"
