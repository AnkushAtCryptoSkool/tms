# TMS - Transaction Management System

A horizontally scalable, high-performance transaction management system built with Spring Boot, designed to handle concurrent requests and support horizontal scaling.

## 🚀 Features

### Horizontal Scalability
- **Async Processing**: Non-blocking transaction processing using CompletableFuture
- **Thread Pool Management**: Configurable thread pools for different operations
- **Connection Pooling**: HikariCP for optimal database connection management
- **Load Balancing**: Nginx-based load balancing for multiple application instances

### Concurrent Request Support
- **Circuit Breaker Pattern**: Resilience4j integration for fault tolerance
- **Rate Limiting**: Per-client rate limiting to prevent abuse
- **Async Operations**: Background processing for audit, notifications, and logging
- **Batch Processing**: Support for processing multiple transactions concurrently

### Performance & Monitoring
- **Redis Caching**: Distributed caching for improved performance
- **Metrics Collection**: Prometheus integration for monitoring
- **Health Checks**: Comprehensive health monitoring for all components
- **Performance Metrics**: Custom metrics for transaction processing

## 🏗️ Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Load Balancer │    │   Load Balancer │    │   Load Balancer │
│    (Nginx)      │    │    (Nginx)      │    │    (Nginx)      │
└─────────┬───────┘    └─────────┬───────┘    └─────────┬───────┘
          │                      │                      │
          ▼                      ▼                      ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   TMS App #1    │    │   TMS App #2    │    │   TMS App #N    │
│   (Port 8080)   │    │   (Port 8080)   │    │   (Port 8080)   │
└─────────┬───────┘    └─────────┬───────┘    └─────────┬───────┘
          │                      │                      │
          └──────────────────────┼──────────────────────┘
                                 │
                    ┌─────────────┴─────────────┐
                    │        Redis Cache        │
                    │      (Port 6379)         │
                    └─────────────┬─────────────┘
                                  │
                    ┌─────────────┴─────────────┐
                    │      PostgreSQL DB        │
                    │      (Port 5432)         │
                    └───────────────────────────┘
```

## 🛠️ Technology Stack

- **Backend**: Spring Boot 3.4.8, Java 17
- **Database**: PostgreSQL (production), H2 (development)
- **Caching**: Redis
- **Load Balancer**: Nginx
- **Monitoring**: Prometheus + Grafana
- **Containerization**: Docker + Docker Compose
- **Async Processing**: CompletableFuture + Spring Async
- **Resilience**: Resilience4j (Circuit Breaker, Rate Limiter)
- **Metrics**: Micrometer + Prometheus

## 🚀 Quick Start

### Prerequisites
- Docker and Docker Compose
- Java 17+ (for local development)
- Maven 3.9+ (for local development)

### Using Docker Compose (Recommended)

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd tms
   ```

2. **Start all services**
   ```bash
   docker-compose up -d
   ```

3. **Verify services are running**
   ```bash
   docker-compose ps
   ```

4. **Access the application**
   - **TMS API**: http://localhost:80/api/
   - **Grafana**: http://localhost:3000 (admin/admin)
   - **Prometheus**: http://localhost:9090
   - **H2 Console**: http://localhost:80/h2-console

### Local Development

1. **Start Redis and PostgreSQL**
   ```bash
   docker-compose up -d redis postgres
   ```

2. **Run the application**
   ```bash
   cd tms
   mvn spring-boot:run
   ```

## 📊 API Endpoints

### Transaction Management
- `POST /api/transactions` - Create transaction (synchronous)
- `POST /api/transactions/async` - Create transaction (asynchronous)
- `POST /api/transactions/batch` - Process multiple transactions
- `GET /api/transactions/health` - Health check

### Monitoring
- `GET /actuator/health` - Application health
- `GET /actuator/metrics` - Application metrics
- `GET /actuator/prometheus` - Prometheus metrics

## 🔧 Configuration

### Application Properties
Key configuration options in `application.properties`:

```properties
# Async Configuration
spring.task.execution.pool.core-size=10
spring.task.execution.pool.max-size=50
spring.task.execution.pool.queue-capacity=100

# Connection Pooling
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5

# Redis Caching
spring.data.redis.host=localhost
spring.data.redis.port=6379

# Circuit Breaker
resilience4j.circuitbreaker.instances.transactionService.sliding-window-size=10
resilience4j.circuitbreaker.instances.transactionService.failure-rate-threshold=50
```

### Horizontal Scaling
To scale the application horizontally:

1. **Scale Docker services**
   ```bash
   docker-compose up -d --scale tms-app=3
   ```

2. **Update Nginx configuration** for additional instances

3. **Monitor performance** using Grafana dashboards

## 📈 Performance Tuning

### JVM Tuning
The Dockerfile includes optimized JVM settings:
- G1GC garbage collector
- Container-aware memory settings
- String deduplication
- Optimized string concatenation

### Database Tuning
- Connection pooling with HikariCP
- Batch processing for multiple operations
- Optimized Hibernate settings

### Caching Strategy
- Redis for distributed caching
- Configurable TTL for different data types
- Cache hit/miss monitoring

## 🧪 Testing

### Load Testing
Use tools like Apache JMeter or Artillery to test horizontal scalability:

```bash
# Example with Artillery
npm install -g artillery
artillery run load-test.yml
```

### Performance Testing
Monitor key metrics during testing:
- Transaction processing time
- Cache hit rates
- Database connection usage
- Memory and CPU utilization

## 📊 Monitoring & Alerting

### Key Metrics
- Transaction throughput (TPS)
- Response time percentiles
- Error rates
- Cache performance
- Database performance
- System resources

### Grafana Dashboards
Pre-configured dashboards for:
- Application performance
- Infrastructure metrics
- Business metrics
- Error tracking

## 🔒 Security

- Rate limiting per client IP
- Circuit breaker protection
- Input validation
- Secure headers via Nginx
- Authentication support (configurable)

## 🚀 Deployment

### Production Considerations
1. **SSL/TLS**: Configure HTTPS in Nginx
2. **Secrets Management**: Use environment variables or secrets
3. **Backup Strategy**: Database and Redis backup procedures
4. **Monitoring**: Set up alerting for critical metrics
5. **Scaling**: Configure auto-scaling based on metrics

### Kubernetes Deployment
The system can be easily deployed to Kubernetes:
- Use the provided Docker images
- Configure horizontal pod autoscaling
- Use Redis cluster for high availability
- Implement proper health checks

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## 📝 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🆘 Support

For support and questions:
- Create an issue in the repository
- Check the documentation
- Review the monitoring dashboards

## 🔄 Changelog

### v1.0.0
- Initial release with horizontal scalability
- Async processing support
- Redis caching integration
- Comprehensive monitoring
- Docker containerization
- Load balancing support
