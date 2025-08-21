# CNH Backend Service - Spring Boot Version

This is a Spring Boot 3.5 implementation of the CNH Warehouse Management System (WMS) backend service, migrated from the original NestJS version.

## Features

- **Authentication & Authorization**: JWT-based authentication with role-based access control
- **User Management**: Complete user lifecycle management with role assignments
- **Product Management**: Product catalog with categories, inventory tracking
- **Order Management**: Sales orders with line items and status tracking
- **Purchase Order Management**: Supplier orders with approval workflows
- **Payment Request Management**: Payment approval workflows
- **Customer & Supplier Management**: Partner relationship management
- **Redis Caching**: High-performance caching for frequently accessed data
- **Database Migrations**: Flyway-based database schema management
- **Logging & Monitoring**: ELK stack integration for centralized logging
- **API Documentation**: OpenAPI 3.0 documentation with Swagger UI
- **Docker Support**: Containerized deployment with Docker Compose

## Technology Stack

- **Java 21**: Latest LTS version with modern language features
- **Spring Boot 3.5**: Latest Spring Boot version with Spring Security 6
- **Spring Data JPA**: Database access with Hibernate
- **PostgreSQL**: Primary database
- **Redis**: Caching and session storage
- **Flyway**: Database migration tool
- **JWT**: JSON Web Token authentication
- **OpenAPI 3.0**: API documentation
- **Docker**: Containerization
- **ELK Stack**: Logging and monitoring (Elasticsearch, Logstash, Kibana)

## Prerequisites

- Java 21 or higher
- Maven 3.8+
- Docker and Docker Compose
- PostgreSQL 15+
- Redis 7+

## Quick Start

### Using Docker Compose (Recommended)

1. Clone the repository:
```bash
git clone <repository-url>
cd cnh-be-service-java
```

2. Start all services:
```bash
docker-compose up -d
```

3. Access the application:
- Application: http://localhost:8080
- API Documentation: http://localhost:8080/swagger-ui.html
- Kibana: http://localhost:5601

### Local Development

1. Set up the database:
```bash
# Start PostgreSQL and Redis
docker-compose up -d postgres redis
```

2. Configure environment variables:
```bash
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=cnh_wms
export DB_USERNAME=postgres
export DB_PASSWORD=password
export REDIS_HOST=localhost
export REDIS_PORT=6379
```

3. Run the application:
```bash
./mvnw spring-boot:run
```

## API Endpoints

### Authentication
- `POST /api/v1/auth/login` - User login
- `POST /api/v1/auth/refresh` - Refresh token
- `POST /api/v1/auth/logout` - User logout

### Users
- `GET /api/v1/users` - Get all users
- `GET /api/v1/users/{id}` - Get user by ID
- `POST /api/v1/users` - Create user
- `PUT /api/v1/users/{id}` - Update user
- `DELETE /api/v1/users/{id}` - Delete user

### Products
- `GET /api/v1/products` - Get all products
- `GET /api/v1/products/{id}` - Get product by ID
- `POST /api/v1/products` - Create product
- `PUT /api/v1/products/{id}` - Update product
- `DELETE /api/v1/products/{id}` - Delete product

### Orders
- `GET /api/v1/orders` - Get all orders
- `GET /api/v1/orders/{id}` - Get order by ID
- `POST /api/v1/orders` - Create order
- `PUT /api/v1/orders/{id}` - Update order
- `DELETE /api/v1/orders/{id}` - Delete order

## Default Credentials

- **Username**: admin
- **Password**: admin123

## Database Schema

The application uses Flyway for database migrations. All migration files are located in `src/main/resources/db/migration/`.

### Key Tables
- `users` - User accounts and authentication
- `roles` - User roles
- `permissions` - System permissions
- `products` - Product catalog
- `categories` - Product categories
- `orders` - Sales orders
- `order_lines` - Order line items
- `purchase_orders` - Purchase orders
- `customers` - Customer information
- `suppliers` - Supplier information
- `payment_requests` - Payment approval requests

## Configuration

The application configuration is managed through `application.yml` and environment variables.

### Key Configuration Properties

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/cnh_wms
    username: postgres
    password: password
  
  redis:
    host: localhost
    port: 6379

jwt:
  secret: your-secret-key-here
  expiration: 86400000 # 24 hours

app:
  cors:
    allowed-origins: http://localhost:3000
```

## Development

### Project Structure

```
src/main/java/com/cnh/ies/
├── config/          # Configuration classes
├── controller/      # REST controllers
├── service/         # Business logic services
├── repository/      # Data access layer
├── entity/          # JPA entities
├── dto/             # Data transfer objects
├── exception/       # Custom exceptions
├── security/        # Security configuration
└── util/            # Utility classes
```

### Adding New Features

1. Create the entity in `entity/` package
2. Create the repository in `repository/` package
3. Create the service in `service/` package
4. Create the controller in `controller/` package
5. Create DTOs in `dto/` package
6. Add database migration in `src/main/resources/db/migration/`

### Testing

```bash
# Run unit tests
./mvnw test

# Run integration tests
./mvnw test -Dtest=*IntegrationTest

# Run all tests with coverage
./mvnw test jacoco:report
```

## Deployment

### Production Deployment

1. Build the application:
```bash
./mvnw clean package -DskipTests
```

2. Create production configuration:
```bash
cp src/main/resources/application.yml src/main/resources/application-prod.yml
# Edit production configuration
```

3. Run with production profile:
```bash
java -jar target/cnh-be-service-2.0.2.jar --spring.profiles.active=prod
```

### Docker Deployment

```bash
# Build image
docker build -t cnh-be-service .

# Run container
docker run -p 8080:8080 cnh-be-service
```

## Monitoring and Logging

The application integrates with the ELK stack for centralized logging:

- **Logstash**: Processes and transforms logs
- **Elasticsearch**: Stores and indexes logs
- **Kibana**: Visualizes and searches logs

### Log Levels

- `ERROR`: Application errors
- `WARN`: Warning messages
- `INFO`: General information
- `DEBUG`: Debug information (development only)

## Security

- JWT-based authentication
- Role-based access control (RBAC)
- Password encryption with BCrypt
- CORS configuration
- Input validation
- SQL injection prevention

## Performance Optimizations

- Redis caching for frequently accessed data
- Database connection pooling
- JPA query optimization
- Pagination for large datasets
- Async processing for heavy operations

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## License

This project is proprietary software. All rights reserved.

## Support

For support and questions, please contact:
- Email: trantuannghia94@gmail.com
- Author: Nghia Tran
