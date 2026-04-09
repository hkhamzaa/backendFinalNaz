# 🌾 Naz Rice : Premium Rice E‑Commerce Platform

demo video: https://youtu.be/yY9FHekjaSs 

A full-stack **rice e-commerce** system: customers browse, cart, and checkout; staff manage **products**, **orders**, and **users** through a secure **admin-style panel**.  
Built as our **2nd semester Database + OOP** coursework—and delivered as a **real commercial project** for a client who needed a practical storefront and back-office tools.


---

## Why we built it

| Lens | Purpose |
|------|--------|
| **Academic** | Apply **OOP** (Spring Boot, layered architecture, entities, services) and **database design** (MySQL, JPA/Hibernate, relationships, migrations via `ddl-auto`). |
| **Commercial** | Give the client a **working shop** plus **admin workflows** (catalog, pricing, discounts, orders) without rebuilding everything from scratch later. |

---

## What’s inside (short)

- **Customer experience** — Product catalog, filters, cart, checkout, account area, themed **dark/light** UI.
- **Staff / admin** — Product management, order management, worker onboarding (role-based).
- **Security** — JWT authentication, Spring Security, role separation (e.g. admin vs customer).
- **Data** — MySQL persistence; file uploads for product images (size limits in `application.properties`).

---

## Tech stack

- **Java 17** · **Spring Boot 3.2** (Web, Data JPA, Security)  
- **MySQL 8**  
- **Maven** (wrapper included: `mvnw.cmd` / `mvnw`)  
- **Front end** — Static HTML/CSS/JS served from Spring (`src/main/resources/static/`), Bootstrap 5  

---

## Prerequisites

1. **JDK 17** installed (`java -version`).  
2. **MySQL 8** reachable on **port 3306**, **or** **Docker** to run the bundled compose file.  
3. Optional: **Maven** globally installed; otherwise use **`mvnw`** (recommended).

---

## Run it properly

### 1) Database

**Option A — Docker (simplest)**

From the `backendFinalNaz` folder:

```bash
docker compose up -d
```

This starts MySQL with database `product_manager` and aligns with the default credentials in `application.properties`.

**Option B — Local MySQL**

Create a database named `product_manager` (or let JPA create it if your URL allows) and set **username / password** in:

`backendFinalNaz/src/main/resources/application.properties`

```properties
spring.datasource.username=root
spring.datasource.password=your_password
```

### 2) Start the application

```bash
cd backendFinalNaz
```

**Windows (quick):**

```bat
run.cmd
```

**Any OS (Maven Wrapper):**

```bash
./mvnw spring-boot:run
```

On Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

Wait until the log shows the app is listening on **port 8080**.

### 3) Open in the browser

- **Home:** [http://localhost:8080](http://localhost:8080)  
- **Shop (customer):** [http://localhost:8080/customer/products.html](http://localhost:8080/customer/products.html)  
- **Staff login (example):** [http://localhost:8080/login.html](http://localhost:8080/login.html)  

> The static pages call the API on **`http://localhost:8080`**. If you deploy elsewhere, update those URLs in the HTML/JS to match your host.

---

## Optional: seed an administrator

After the app (or Hibernate) has created tables, you can apply the SQL helper in:

`backendFinalNaz/scripts/seed-admin.sql`

Use your MySQL client against the `product_manager` database.  
Default **development** staff credentials (if you use that script) are noted in the **comments** at the top of the file.  
Roles are also initialized at runtime where configured in the project—see the codebase for details.

> **Production:** change JWT secret, database passwords, and turn off verbose SQL logging in `application.properties`.

---

## Project layout (where to look)

```
nazrice backend/
└── backendFinalNaz/
    ├── pom.xml                 ← Maven / dependencies
    ├── mvnw.cmd, mvnw          ← Maven Wrapper
    ├── run.cmd                 ← Windows one-click run
    ├── docker-compose.yml      ← MySQL 8 for local dev
    ├── scripts/                ← Optional DB helpers (e.g. admin seed)
    └── src/main/
        ├── java/               ← Spring Boot app (API, security, entities)
        └── resources/
            ├── application.properties
            └── static/         ← HTML, CSS, JS (shop + admin UI)
```

---

## Troubleshooting (quick)

| Issue | What to check |
|--------|----------------|
| **Port 8080 in use** | Stop the other process or change `server.port` in `application.properties`. |
| **Cannot connect to MySQL** | MySQL running? Port **3306**? User/password match `application.properties`? |
| **403 on pages** | Security rules—log in with the correct role or use permitted public routes. |
| **CORS / API errors** | Browser must use the same origin/port the JS expects (default **8080**). |

---

## License & credits

Developed as a **Database + OOP (2nd semester)** project and extended for **client delivery**.  
Use and modify according to your course or commercial agreement.

---

*Questions about deployment or extending the admin panel—document changes to `application.properties` and API URLs before going live.*
