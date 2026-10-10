
# ExpressMeal — Web-Based Food Ordering System

A full-stack, multi-role food ordering and delivery management platform built with **Spring Boot**, **Thymeleaf**, and **MySQL**. The system supports four user roles — **Customer**, **Restaurant Admin**, **Delivery Rider**, and **System Admin** — covering the complete order lifecycle: browsing the menu, placing an order, payment, rider assignment, live delivery tracking, and customer feedback.

> **Course:** SE2030 — Software Engineering (Group Project)
> **Level:** BSc (Hons) in Information Technology — Year 2, Semester 1

---

## Table of Contents

- [Overview](#overview)
- [Key Features](#key-features)
- [Tech Stack](#tech-stack)
- [System Architecture](#system-architecture)
- [Design Patterns Used](#design-patterns-used)
- [Project Structure](#project-structure)
- [Database Design](#database-design)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Installation & Setup](#installation--setup)
  - [Environment Variables](#environment-variables)
  - [Running the Application](#running-the-application)
- [User Roles & Access](#user-roles--access)
- [Screenshots](#screenshots)
- [Team](#team)
- [License](#license)

---

## Overview

**ExpressMeal** is a web-based food ordering system designed to digitize the end-to-end workflow of a single-restaurant online ordering business — from menu browsing to doorstep delivery. The system was built as a group project to demonstrate practical application of **object-oriented design principles**, **GoF design patterns**, and **layered Spring MVC architecture** in a real-world, feature-complete web application.

The application is split into independently manageable modules (customer, restaurant, order, payment, delivery, feedback, and admin), each following a consistent **Controller → Service → Repository** layering on top of Spring Data JPA.

---

## Key Features

### 👤 Customer
- Registration, login, and profile management (with password reset via token)
- Browse menu items by category, add to cart, and checkout
- Address and contact validation at checkout (server-side)
- Multiple payment methods — Card / Cash on Delivery
- Real-time order tracking with live delivery status steps
- Printable order receipts (itemized, with delivery & processing fees)
- Leave feedback and ratings for delivered orders

### 🍔 Restaurant (Admin-managed)
- Full CRUD for menu items (name, price, category, image, availability)
- **Factory-pattern** quick-add templates for common categories (Rice, Short Eats, Drinks, Desserts)

### 🛵 Delivery & Riders
- Self-service delivery acceptance (riders pick up pending orders, Uber Eats–style)
- Admin-assisted rider assignment for deliveries
- Live delivery status pipeline: `PENDING → ASSIGNED → PICKED_UP → IN_TRANSIT → DELIVERED`
- Interactive delivery-address map with automatic geocoding (Leaflet.js + OpenStreetMap Nominatim)
- Rider performance dashboard — completed jobs, active days, earnings, average customer rating
- Rider self-service profile and password management

### 💳 Payments & Reports
- Payment processing with delivery + processing fee calculation
- Admin **Sales Reports** with date-range filtering, revenue breakdown, top-selling items, and customer activity stats
- One-click **CSV export** of filtered sales data

### 🛠️ System Admin
- Central dashboard — total orders, revenue, customers, available riders, monthly revenue trend, order-status breakdown
- Full CRUD for customer accounts
- Site-wide **Announcement Banner** and maintenance-mode toggle (Singleton-backed, in-memory site settings)
- Real-time activity feed of delivery status changes (Observer pattern)

### 💬 Feedback
- Customers rate and review delivered orders
- Filterable feedback views (All / High Rating / Low Rating / My Feedback) via a pluggable **Strategy pattern**

---

## Tech Stack

| Layer              | Technology                                   |
|--------------------|-----------------------------------------------|
| Language           | Java 21                                        |
| Framework          | Spring Boot 4.1 (Spring MVC, Spring Data JPA) |
| Templating         | Thymeleaf                                      |
| Database           | MySQL 8 (H2 available for local/testing)      |
| Build Tool         | Maven                                          |
| Frontend           | Bootstrap 5, Bootstrap Icons, vanilla JS       |
| Maps               | Leaflet.js + OpenStreetMap (Nominatim geocoding) |
| Validation         | Jakarta Bean Validation (`spring-boot-starter-validation`) |
| Dev Tooling        | Spring Boot DevTools, Lombok                   |

---

## System Architecture

The application follows a classic **layered MVC architecture**:

```
Browser (Thymeleaf views)
        │
        ▼
 Controller Layer   — handles HTTP requests, session/role checks, redirects
        │
        ▼
  Service Layer      — business logic, validation, orchestration between modules
        │
        ▼
Repository Layer    — Spring Data JPA interfaces (database access)
        │
        ▼
     MySQL Database
```

Each business domain (`customer`, `restaurant`, `order`, `payment`, `delivery`, `feedback`, `admin`) is organized as its own **vertical package slice**, each containing its own `controller`, `service`, `repository`, and `entity` sub-packages — keeping the codebase modular and easy to navigate.

---

## Design Patterns Used

This project was also used as a practical exercise in applying **Gang of Four (GoF) design patterns** to real application features:

| Pattern       | Where it's used                                                                 | Why |
|---------------|-----------------------------------------------------------------------------------|-----|
| **Singleton**   | `admin/singleton/AppSettings.java`                                              | One shared, in-memory site-settings object (announcement banner, maintenance mode) accessible app-wide via `getInstance()` — no database needed. |
| **Builder**     | `order/builder/OrderReceiptBuilder.java`                                       | Assembles a printable order receipt step-by-step (order details → line items → payment info → grand total) from independent, optional parts. |
| **Factory**     | `restaurant/factory/MenuItemFactory.java`, `payment/strategy/PaymentStrategyFactory.java`, `feedback/strategy/FeedbackFilterFactory.java` | Produces pre-configured menu item templates, selects the right payment strategy, and resolves the right feedback filter — without the caller needing to know the concrete implementation. |
| **Strategy**    | `payment/strategy/` (Card, Cash on Delivery), `feedback/strategy/` (All / High Rating / Low Rating / My Feedback) | Interchangeable algorithms for processing payments and filtering feedback, selected at runtime. |
| **Observer**    | `delivery/observer/` (`DeliveryObserver`, `CustomerNotificationObserver`, `AdminNotificationObserver`) | `DeliveryService` (the Subject) notifies all registered observers whenever a delivery's status changes, decoupling the core delivery logic from customer notifications and the admin activity feed. |

---

## Project Structure

```
FoodOrderingSystem/
├── src/main/java/com/foodorderingsystem/
│   ├── admin/            # Admin dashboard, site settings (Singleton)
│   ├── auth/              # Login/role-portal routing
│   ├── common/             # Home page controller
│   ├── customer/          # Customer entity, auth, profile, admin-side CRUD
│   ├── delivery/          # Riders, deliveries, Observer pattern
│   ├── feedback/          # Feedback entity + Strategy-based filtering
│   ├── order/             # Cart, checkout, order history, Builder-based receipts
│   ├── payment/           # Payment processing, Strategy pattern, sales reports
│   └── restaurant/        # Menu management, Factory-based item templates
├── src/main/resources/
│   ├── templates/         # Thymeleaf views (one folder per module)
│   └── application.properties
├── pom.xml
└── README.md
```

---

## Database Design

Core entities and their relationships:

- **Customer** `1 ── * Order`
- **Order** `1 ── * OrderItem` `── * MenuItem`
- **Order** `1 ── 1 Payment`
- **Order** `1 ── 1 Delivery` `── 1 Rider`
- **Order** `1 ── 1 Feedback`

The schema is auto-managed by Hibernate (`spring.jpa.hibernate.ddl-auto=update`), so tables are created/updated automatically on startup based on the JPA entity classes — no manual migration scripts required for local development.

---

## Getting Started

### Prerequisites

- **Java 21** (JDK)
- **Maven 3.9+** (or use the included Maven Wrapper `./mvnw`)
- **MySQL 8** running locally or remotely
- *(Optional)* An **OpenStreetMap/Leaflet** setup needs no API key — it works out of the box

### Installation & Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/Lojitha-Heshan/Express-Meal-Food-Ordering-System.git
   cd FoodOrderingSystem
   ```

2. **Create the database**
   ```sql
   CREATE DATABASE foodordering_db;
   ```

3. **Configure environment variables** (see below)

### Environment Variables

The application reads its configuration from environment variables (see `.env.example`). Create a `.env` file or export these in your shell:

| Variable              | Description                                      | Example                                             |
|------------------------|---------------------------------------------------|------------------------------------------------------|
| `DB_URL`               | JDBC URL for the MySQL database                  | `jdbc:mysql://localhost:3306/foodordering_db`        |
| `DB_USERNAME`           | Database username                                | `root`                                                |
| `DB_PASSWORD`           | Database password                                | `your_password`                                       |
| `GOOGLE_MAPS_API_KEY`   | *(Optional, legacy)* Google Maps key — not required since delivery maps now use Leaflet/OpenStreetMap | `YOUR_GOOGLE_MAPS_API_KEY` |

### Running the Application

Using the Maven Wrapper:

```bash
./mvnw spring-boot:run
```

Or build and run the JAR directly:

```bash
./mvnw clean package
java -jar target/FoodOrderingSystem-0.0.1-SNAPSHOT.jar
```

The application will start on **http://localhost:8080**.

---

## User Roles & Access

| Role              | Entry Point           | Description                                                        |
|-------------------|------------------------|----------------------------------------------------------------------|
| **Customer**        | `/customer/login`      | Browse menu, place orders, track deliveries, leave feedback         |
| **Delivery Rider**  | `/delivery/login`      | Accept/manage deliveries, update delivery status, view earnings     |
| **System Admin**    | `/admin/login`         | Manage customers, menu, riders, deliveries, site settings, and sales reports |

> Admin credentials are configured via `app.admin.username` / `app.admin.password` (defaults to `admin@expressmeal.com` / `admin123` for development).

---

## Screenshots

<img width="1894" height="911" alt="Screenshot 2026-10-07 190904" src="https://github.com/user-attachments/assets/021452dc-71fe-42b8-b57f-f184a7dbe41b" />
<img width="1885" height="839" alt="Screenshot 2026-10-07 190919" src="https://github.com/user-attachments/assets/25ddb0de-3d21-40e7-a8d1-4e9f1b826890" />
<img width="1881" height="908" alt="Screenshot 2026-10-07 190937" src="https://github.com/user-attachments/assets/c1882212-c322-4563-bc85-ff28d0370b34" />
<img width="825" height="680" alt="Screenshot 2026-10-07 190947" src="https://github.com/user-attachments/assets/58ab79ac-2e4f-43e3-8e46-21ae2a27714a" />
<img width="1895" height="915" alt="Screenshot 2026-10-07 191110" src="https://github.com/user-attachments/assets/3e699ca9-734b-4a52-9da1-2a9e3c90d8da" />
<img width="644" height="762" alt="Screenshot 2026-10-07 191117" src="https://github.com/user-attachments/assets/ee739fb0-81de-4ce1-a733-0f526faa5a96" />
<img width="1355" height="813" alt="Screenshot 2026-10-07 191203" src="https://github.com/user-attachments/assets/152d2ccc-b588-483a-9108-9c55bc8305d8" />
<img width="400" height="710" alt="Screenshot 2026-10-07 191308" src="https://github.com/user-attachments/assets/c3cd9e4c-9483-4a8b-8016-bb3a572a1bf9" />
<img width="404" height="468" alt="Screenshot 2026-10-07 191322" src="https://github.com/user-attachments/assets/49b82d5c-2387-4292-a8d5-b9084d1ff97d" />
<img width="416" height="502" alt="Screenshot 2026-10-07 191342" src="https://github.com/user-attachments/assets/379775d0-88dc-42a4-80a5-59cc772296b4" />
<img width="1892" height="863" alt="Screenshot 2026-10-07 191427" src="https://github.com/user-attachments/assets/f17c64c9-944e-46a6-b81e-794876ed9580" />
<img width="417" height="671" alt="Screenshot 2026-10-07 191501" src="https://github.com/user-attachments/assets/6dff8c77-394f-43cb-a345-7918e6bb02f4" />
<img width="1675" height="920" alt="Screenshot 2026-10-07 191650" src="https://github.com/user-attachments/assets/59b0c578-09c3-4c2d-af21-cad7bd26c555" />
<img width="1320" height="630" alt="Screenshot 2026-10-07 191658" src="https://github.com/user-attachments/assets/8c73b81f-6c7f-4793-9cc4-a294a78b651c" />





---

## Team

| Name | Role | 
|------|------|
| De Silva G L H     |Delivery Management  |
| Weerasekara N D  | Customer Management |
| Gunaratne A. G. O. N  | Restaurant & Menu Management |
| Lewliyadda L. M. D  | Order Management  |
| Tennakoon A. S   | Payment & Finance Management 
| Nethaya K. V. S   | Customer Feedback & Ratings  |

**Module ownership:**
- Customer management & authentication
- Restaurant menu management (Factory pattern)
- Order & checkout flow (Builder pattern)
- Payment processing & sales reports (Strategy pattern)
- Delivery & rider management (Observer pattern)
- Admin dashboard & site settings (Singleton pattern)
- Feedback system (Strategy pattern)

---

## License

This project was developed for academic purposes as part of the SE2030 Software Engineering module. All rights reserved by the project authors.
