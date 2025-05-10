# Project Overview

This document explains the **what**, **why**, and **how** of implementing JWT-based authentication in this Spring Boot API.

---

## 1. Goals and Motivation

- **What**: Secure access to database-backed resources via registration and login.
- **Why**: Enforce stateless, scalable authentication using JSON Web Tokens (JWT) instead of sessions.
- **How**: Spring Security + custom filters + JWT utilities.

---

## 2. Project Structure

```
src/main/java/com/ToDoList/auth/
 ├─ config/              # Security & JWT configurations
 ├─ controller/          # REST endpoints (register, login)
 ├─ dto/                 # Data Transfer Objects for requests/responses
 ├─ model/               # JPA entities: User, PendingUser
 ├─ repository/          # Spring Data JPA interfaces
 └─ service/             # Business logic: AuthService & JwtService
``` 

---

## 3. Spring Boot Configuration

- **application.properties**: Database URL, JPA settings, JWT secret & expiration.
- **ApplicationConfig**: Bean definitions (PasswordEncoder, AuthenticationManager).

**Why**: Centralize common beans for injection.

---

## 4. SecurityConfig

- **Extends** `WebSecurityConfigurerAdapter`:
  - Disables CSRF (stateless API).
  - Permits `/auth/**` publicly; secures other endpoints.
  - Registers `JwtAuthenticationFilter` before username/password filter.

**How**: Chain rules to check JWT on each request.

---

## 5. JWT Components

### JwtService

- Generates and validates tokens using `io.jsonwebtoken`.
- Embeds username and roles as claims.
- Signs with HS256 algorithm and secret.

**Why**: Abstract token operations.

### JwtAuthenticationFilter

- Resolves token from `Authorization: Bearer <token>` header.
- Validates token, retrieves user details, sets `SecurityContext`.

**How**: Intercepts every request, calls `JwtService`, loads user from repository.

---

## 6. AuthService

- **register(RegisterRequest)**:
  - Validates uniqueness.
  - Creates `PendingUser` or `User` directly.
  - (Optional) Email verification.
- **login(LoginRequest)**:
  - Authenticates via `AuthenticationManager`.
  - Generates JWT on success.

**Why**: Separate business logic from controllers.

---

## 7. Controllers & DTOs

- **AuthController**:
  - `POST /auth/register` → `RegisterRequest` → returns `AuthResponse`.
  - `POST /auth/login` → `LoginRequest` → returns token.

- **DTOs**:
  - `LoginRequest` (username, password).
  - `RegisterRequest` (user details).
  - `AuthResponse` (JWT token and metadata).

**How**: Spring MVC handles JSON binding, validation.

---

## 8. Data Model & Repositories

- **User**: Active users stored in `users` table.
- **PendingUser**: (Optional) Holds registrations awaiting approval.

- **UserRepository & PendingUserRepository**: CRUD via Spring Data JPA.

**Why**: Decouple persistence; auto-implementation of queries.

---

## 9. Authentication Flow

1. **Register**: Client sends user info → `AuthController` → `AuthService.register` → save user → 201 Created.
2. **Login**: Client sends credentials → Spring AuthManager verifies → `JwtService.generateToken` → return JWT.
3. **Access Protected Resource**: Client includes `Authorization` header → `JwtAuthenticationFilter` validates & loads user → Controller serves request.

---

## 10. Dependencies & Tools

- Spring Boot Starter Web, Security, Data JPA
- H2 or external database
- JJWT (`io.jsonwebtoken`)
- Lombok (DTO builders)

---

## 11. Next Steps

- Refresh token endpoints
- Role-based access control (RBAC)
- Email verification for pending users
- Centralized exception handling

---

## 12. References

- Spring Security JWT Guide
- JJWT Documentation
- Spring Data JPA Reference