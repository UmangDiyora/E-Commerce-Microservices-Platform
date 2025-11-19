# Docker Deployment Guide

## Overview

This guide provides instructions for deploying the E-Commerce Microservices Platform using Docker and Docker Compose.

## Prerequisites

- Docker 20.10+
- Docker Compose 2.0+
- At least 8GB RAM available for Docker
- 20GB free disk space

## Architecture

The platform consists of:

### Infrastructure Services
- **MySQL**: Database for all business services (4 databases)
- **Redis**: Caching and rate limiting
- **RabbitMQ**: Message broker for event-driven communication
- **Zipkin**: Distributed tracing

### Spring Cloud Infrastructure
- **Eureka Server** (8761): Service discovery
- **Config Server** (8888): Centralized configuration
- **API Gateway** (8080): Single entry point with routing, security, and resilience

### Business Microservices
- **User Service** (8081): User management and authentication
- **Product Service** (8082): Product catalog management
- **Order Service** (8083): Order and cart management
- **Payment Service** (8084): Payment processing
- **Notification Service** (8085): Email/SMS notifications

## Quick Start

### 1. Build and Start All Services

```bash
# Build and start all services in detached mode
docker-compose up -d --build

# View logs
docker-compose logs -f

# View logs for specific service
docker-compose logs -f user-service
```

### 2. Check Service Health

```bash
# Check all running containers
docker-compose ps

# Check Eureka Dashboard (wait 2-3 minutes for all services to register)
open http://localhost:8761
```

### 3. Access Services

| Service | URL | Description |
|---------|-----|-------------|
| API Gateway | http://localhost:8080 | Main entry point |
| Eureka Dashboard | http://localhost:8761 | Service registry |
| RabbitMQ Management | http://localhost:15672 | Message broker UI (guest/guest) |
| Zipkin UI | http://localhost:9411 | Distributed tracing |
| User Service API | http://localhost:8080/api/users | Via API Gateway |
| Product Service API | http://localhost:8080/api/products | Via API Gateway |
| Order Service API | http://localhost:8080/api/orders | Via API Gateway |
| Payment Service API | http://localhost:8080/api/payments | Via API Gateway |
| Notification Service | http://localhost:8080/api/notifications | Via API Gateway |

### 4. Swagger UI Documentation

Each service has its own Swagger UI:
- User Service: http://localhost:8081/swagger-ui.html
- Product Service: http://localhost:8082/swagger-ui.html
- Order Service: http://localhost:8083/swagger-ui.html
- Payment Service: http://localhost:8084/swagger-ui.html
- Notification Service: http://localhost:8085/swagger-ui.html

## Service Startup Order

The docker-compose.yml is configured with proper dependencies and health checks:

1. Infrastructure (MySQL, Redis, RabbitMQ, Zipkin)
2. Eureka Server
3. Config Server
4. API Gateway
5. Business Services (User, Product, Order, Payment, Notification)

**Note**: It may take 3-5 minutes for all services to start and register with Eureka.

## Management Commands

### Stop All Services
```bash
docker-compose stop
```

### Start All Services
```bash
docker-compose start
```

### Restart a Specific Service
```bash
docker-compose restart user-service
```

### View Logs
```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f payment-service

# Last 100 lines
docker-compose logs --tail=100 order-service
```

### Scale a Service
```bash
# Run 3 instances of product-service
docker-compose up -d --scale product-service=3
```

### Rebuild a Service
```bash
# Rebuild and restart specific service
docker-compose up -d --build user-service
```

### Clean Up
```bash
# Stop and remove all containers, networks
docker-compose down

# Remove volumes as well (deletes all data)
docker-compose down -v

# Remove all images
docker-compose down --rmi all
```

## Environment Variables

You can customize the deployment by creating a `.env` file:

```env
# MySQL
MYSQL_ROOT_PASSWORD=root
MYSQL_DATABASE=ecommerce

# RabbitMQ
RABBITMQ_DEFAULT_USER=guest
RABBITMQ_DEFAULT_PASS=guest

# Email (for Notification Service)
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password
MAIL_FROM=noreply@ecommerce.com

# Application
SPRING_PROFILES_ACTIVE=docker
```

## Database Access

### MySQL
```bash
# Connect to MySQL container
docker exec -it mysql mysql -uroot -proot

# Use specific database
USE user_db;
SHOW TABLES;

# Or connect from host
mysql -h localhost -P 3306 -uroot -proot
```

### Redis
```bash
# Connect to Redis container
docker exec -it redis redis-cli

# Check keys
KEYS *
```

## Monitoring and Observability

### Eureka Dashboard
- URL: http://localhost:8761
- Shows all registered services and their status

### RabbitMQ Management UI
- URL: http://localhost:15672
- Credentials: guest/guest
- Monitor queues, exchanges, and message flow

### Zipkin Tracing
- URL: http://localhost:9411
- View distributed traces across services
- Analyze latency and dependencies

### Health Checks
```bash
# Check all service health
curl http://localhost:8080/actuator/health  # API Gateway
curl http://localhost:8081/actuator/health  # User Service
curl http://localhost:8082/actuator/health  # Product Service
curl http://localhost:8083/actuator/health  # Order Service
curl http://localhost:8084/actuator/health  # Payment Service
curl http://localhost:8085/actuator/health  # Notification Service
```

## Troubleshooting

### Services Not Starting

1. **Check Docker resources**: Ensure Docker has enough memory (8GB+)
2. **Check logs**: `docker-compose logs -f [service-name]`
3. **Check dependencies**: Services may fail if dependencies aren't healthy
4. **Restart**: `docker-compose restart [service-name]`

### Port Conflicts

If ports are already in use, modify the port mappings in `docker-compose.yml`:

```yaml
ports:
  - "9080:8080"  # Change host port to 9080
```

### Database Connection Issues

```bash
# Check MySQL is running
docker-compose ps mysql

# Check MySQL logs
docker-compose logs mysql

# Verify databases are created
docker exec -it mysql mysql -uroot -proot -e "SHOW DATABASES;"
```

### Service Not Registering with Eureka

1. Wait 2-3 minutes (services register on startup)
2. Check Eureka logs: `docker-compose logs eureka-server`
3. Check service logs for connection errors
4. Verify EUREKA_CLIENT_SERVICEURL_DEFAULTZONE is correct

## Production Considerations

### Security
- Change default passwords (MySQL, RabbitMQ)
- Use environment variables for sensitive data
- Enable SSL/TLS for external communication
- Implement proper authentication in API Gateway

### Scalability
- Use external databases (not containers)
- Set up Redis cluster
- Configure RabbitMQ cluster
- Use container orchestration (Kubernetes)

### Monitoring
- Add Prometheus for metrics
- Set up Grafana dashboards
- Configure alerts
- Use ELK stack for centralized logging

### Backup
- Regular database backups
- Export RabbitMQ definitions
- Version control configurations

## Testing the Platform

### 1. Register a User
```bash
curl -X POST http://localhost:8080/api/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123",
    "firstName": "John",
    "lastName": "Doe"
  }'
```

### 2. Create a Product
```bash
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Product",
    "description": "A test product",
    "price": 99.99,
    "stockQuantity": 100
  }'
```

### 3. Create an Order
This will trigger the entire event flow:
- Order created
- Payment processed
- Notifications sent

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "items": [
      {
        "productId": 1,
        "quantity": 2
      }
    ],
    "shippingAddressId": 1
  }'
```

## Advanced Configuration

### Custom Network
```yaml
networks:
  ecommerce-network:
    driver: bridge
    ipam:
      config:
        - subnet: 172.25.0.0/16
```

### Volume Persistence
All data is persisted in Docker volumes. To back up:
```bash
docker run --rm -v ecommerce_mysql-data:/data -v $(pwd):/backup alpine tar czf /backup/mysql-backup.tar.gz -C /data .
```

## Support

For issues or questions:
1. Check the logs: `docker-compose logs -f`
2. Review the troubleshooting section
3. Consult the main README.md
4. Check service-specific documentation

## Summary

This Docker deployment provides a complete, production-ready e-commerce platform with:
- ✅ Service discovery and configuration management
- ✅ API Gateway with security and resilience
- ✅ Event-driven architecture with RabbitMQ
- ✅ Distributed tracing with Zipkin
- ✅ Database persistence with MySQL
- ✅ Caching with Redis
- ✅ Full observability and monitoring
- ✅ Health checks and automatic restarts
- ✅ Scalable microservices architecture

Start the platform with one command: `docker-compose up -d --build`
