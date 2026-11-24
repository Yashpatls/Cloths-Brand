# StyleHub — Complete Clothing E-Commerce Platform

A production-grade, full-stack clothing e-commerce web application built using **Java 21, Spring Boot 3.4.2, MySQL 8.0, Thymeleaf, Bootstrap 5, and Amazon Web Services (AWS)**.

---

## 🌟 Key Features

### 🛍️ Customer Experience & Storefront
- **Premium Modern Fashion Design**: Clean, high-fashion editorial aesthetics with Playfair Display & DM Sans typography.
- **Dynamic Catalog & Faceted Filters**: Filter garments by category (Men, Women, Kids, Accessories, Footwear), price range, size, shade, and keywords.
- **Curated Wardrobe Landing Pages**: Direct collection URLs for `/category/men`, `/category/women`, and `/category/kids`.
- **Product Details & Imagery**: High-resolution image galleries with thumbnail switcher, real-time stock indicator, size selector, and shade selector.
- **Wishlist & Saved Items**: Save favorite garments with instant AJAX toggling; one-click "Move to Bag".
- **Shopping Bag & Coupon Discounts**: Modify quantities, remove items, and apply percentage/fixed discount coupon codes (e.g., `STYLE20`, `WELCOME100`).
- **Multi-Step Checkout & Saved Addresses**: Store multiple delivery destinations, select default shipping, and calculate automated tax (5% GST) and shipping policies.
- **Dual-Mode Payment Gateway**:
  - **Razorpay (Test Mode)**: Real-time popup checkout with server-side HMAC-SHA256 signature verification.
  - **Instant Demo Checkout / Cash on Delivery (COD)**: Zero-config immediate order fulfillment for local grading and testing.
- **Live Order Tracking**: Visual 5-step progress stepper (Placed → Confirmed → Tailoring & Pack → In Transit → Delivered).
- **Verified Reviews & Ratings**: Submit star ratings and customer feedback.
- **Client Concierge**: Contact page with messaging form, store locations, and FAQ accordion.

### 🛡️ Administrative Management Suite (`/admin`)
- **Executive KPI Dashboard**: Live tracking of Gross Revenue, Total Orders, Active Garments, Registered Clients, and low-stock alerts.
- **Garment & Catalog CRUD**: Add, edit, and archive garments with SKU, regular price, sale price, stock count, and AWS S3 image uploads.
- **Wardrobe Categories**: Create and organize clothing categories with custom URL slugs and banners.
- **Inventory Control**: Real-time stock overview with inline quantity adjustment.
- **Order Fulfillment**: Track customer packages, inspect order invoices, and update shipping status.
- **Client Profiles**: View registered customer directory, contact numbers, and delivery addresses.
- **Payment Ledger**: Audit transaction IDs, payment methods, and settlement statuses.
- **Coupon Management**: Create promotional privilege codes with expiry dates and order thresholds.
- **Reports & Analytics**: Print-ready sales summaries and revenue reports.
- **Store Configuration**: Key-value settings for store name, currency, tax rates, and support hotlines.

---

## 🏗️ Technical Architecture

### 1. Backend Layer (Java Spring Boot 3.4.2)
- **Controller Layer**: REST APIs and Thymeleaf view controllers (`HomeController`, `ProductController`, `CartController`, `OrderController`, `UserController`, `WishlistController`, `AdminController`, `ReviewController`, `ContactController`).
- **Service Layer**: Transactional domain services (`ProductService`, `OrderService`, `PaymentService`, `CartService`, `UserService`, `CategoryService`, `CouponService`, `WishlistService`, `ReviewService`, `StorageService`, `ReportService`).
- **Security**: Spring Security 6 with BCrypt hashing, role-based authorization (`ROLE_CUSTOMER`, `ROLE_ADMIN`), and remember-me authentication.
- **Storage**: AWS S3 SDK v2 with automatic fallback to local directory (`uploads/`).

### 2. Database Layer (MySQL 8.0)
Normalized 16-table relational schema with foreign key constraints, indexes, and cascading rules:
- `users`, `addresses`, `categories`, `products`, `product_images`, `product_sizes`, `product_colors`, `cart`, `cart_items`, `customer_orders`, `order_items`, `payments`, `wishlist`, `coupons`, `reviews`, `store_settings`.

### 3. AWS Cloud Infrastructure
- **EC2 (Ubuntu 24.04 LTS)**: Runs Spring Boot JAR as a resilient `systemd` service.
- **Amazon RDS (MySQL 8.0)**: Private managed database.
- **Amazon S3**: Private media bucket for product image uploads.
- **Amazon CloudFront**: Low-latency HTTPS content delivery network.
- **Nginx**: Reverse proxy with Gzip compression and SSL/TLS termination.
- **CloudFormation**: Infrastructure-as-Code deployment template (`deployment/aws/cloudformation-template.yaml`).

---

## 🚀 Running Locally

### Prerequisites
- JDK 21+
- MySQL 8.0+ (running locally on port 3306 or via Docker/XAMPP)
- Maven 3.9+

### 1. Database Setup
Start MySQL and create the database:
```sql
CREATE DATABASE stylehub CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```
*(Optional)* You can execute `database/schema.sql` directly or allow Spring Boot JPA to auto-create and seed tables on initial startup.

### 2. Run the Application
In your terminal:
```bash
mvn spring-boot:run
```
Visit: **`http://localhost:8080`**

---

## 🔑 Demo Accounts

The application automatically seeds initial accounts on first startup:

| Account Role | Email | Password | Access |
|---|---|---|---|
| **Administrator** | `admin@stylehub.local` | `Admin@12345` | `/admin` control suite & storefront |
| **Customer** | `customer@stylehub.local` | `Customer@12345` | Storefront, cart, checkout, profile |

### Active Demo Promo Codes
- **`STYLE20`**: 20% off on orders over ₹1,200.
- **`WELCOME100`**: Flat ₹100 off on orders over ₹999.
- **`LUXURY500`**: Flat ₹500 off on orders over ₹2,500.

---

## ☁️ AWS Cloud Deployment

Complete deployment scripts, templates, and documentation are provided in `deployment/aws/`:
- **`deployment/aws/AWS_DEPLOYMENT_GUIDE.md`**: Step-by-step production deployment instructions.
- **`deployment/aws/deploy-aws.sh`**: Automated provisioning script for EC2.
- **`deployment/aws/cloudformation-template.yaml`**: One-click AWS CloudFormation stack.
- **`deployment/aws/stylehub.service`**: Systemd unit definition.
- **`deployment/aws/nginx-stylehub.conf`**: Nginx proxy configuration.

---

## 🧪 Testing

Run all automated unit and integration tests using the in-memory H2 database:
```bash
mvn test
```
The test suite covers:
- User registration, BCrypt password hashing, and duplicate detection.
- Faceted catalog filtering and category lookups.
- Cart quantity increments, decrements, and price recalculations.
- Promotional coupon validation and percentage/flat discount caps.
- Transactional checkout, stock decrements, and order number generation.

---

## 📄 License
StyleHub Platform · Educational & Commercial Starter License.
