# 🚀 Quick Start Guide

## Choose Your Path

### 🎯 **Option 1: Minimal Setup** (Recommended if Docker RAM < 8GB)

**Perfect for:** Testing, learning, low-resource systems  
**Memory:** 2-3 GB  
**Time:** 8-10 minutes  
**Services:** 5 core + 2 databases

```bash
chmod +x start-minimal.sh
./start-minimal.sh
```

**What you get:**
- Authentication & user management
- Service discovery
- API Gateway
- PostgreSQL + Redis

**Test it:**
```bash
# Register
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"john","email":"john@test.com","password":"Test123!"}'

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"john","password":"Test123!"}'
```

---

### 💪 **Option 2: Full Local Setup** (If Docker RAM ≥ 12GB)

**Perfect for:** Full-stack development, production simulation  
**Memory:** 12-16 GB  
**Time:** 10-15 minutes (parallel builds)  
**Services:** All 21 containers

**First, increase Docker memory:**
1. Docker Desktop → Settings → Resources
2. Memory: 12-16 GB
3. Swap: 2-4 GB
4. Apply & Restart

**Then run:**
```bash
chmod +x start-local.sh
./start-local.sh
```

---

### ☁️ **Option 3: Cloud Deployment** (Zero local resources!)

**Perfect for:** Production, sharing with team, 24/7 availability  
**Memory:** Zero local usage  
**Cost:** Free tier available

**Quick deploy to Railway:**
```bash
npm i -g @railway/cli
railway login
railway init
railway up
```

**Full guide:** See `CLOUD_DEPLOYMENT_GUIDE.md`

---

## 📊 Service URLs (After Startup)

### Core Infrastructure
- **Eureka Dashboard:** http://localhost:8761
- **API Gateway:** http://localhost:8080
- **Config Server:** http://localhost:8888

### APIs (via Gateway)
- **Auth:** http://localhost:8080/api/auth
- **Users:** http://localhost:8080/api/users
- **Products:** http://localhost:8080/api/products
- **Orders:** http://localhost:8080/api/orders
- **Payments:** http://localhost:8080/api/payments

### Databases
- **PostgreSQL:** localhost:5432 (postgres/postgres)
- **MongoDB:** localhost:27017 (admin/admin123)
- **Redis:** localhost:6379

### Observability (Full setup only)
- **Prometheus:** http://localhost:9090
- **Grafana:** http://localhost:3000 (admin/admin)
- **Zipkin:** http://localhost:9411
- **Kibana:** http://localhost:5601
- **Kafka UI:** http://localhost:8090

---

## 🧪 Testing with Postman

1. **Import collection:**
   ```
   File → Import → postman/E-commerce-Microservices.postman_collection.json
   ```

2. **Import environment:**
   ```
   File → Import → postman/Local.postman_environment.json
   ```

3. **Select environment:**
   Top-right dropdown → Local

4. **Run requests:**
   - Auth → Register (creates user)
   - Auth → Login (gets JWT token - auto-saved)
   - Test any other endpoint!

---

## 🔧 Useful Commands

### View Logs
```bash
# All services
docker-compose -f docker/docker-compose.minimal.yml logs -f

# Specific service
docker-compose -f docker/docker-compose.minimal.yml logs -f auth-service
```

### Check Status
```bash
docker-compose -f docker/docker-compose.minimal.yml ps
```

### Restart Service
```bash
docker-compose -f docker/docker-compose.minimal.yml restart auth-service
```

### Stop All Services
```bash
docker-compose -f docker/docker-compose.minimal.yml down
```

### Cleanup Everything
```bash
docker-compose -f docker/docker-compose.minimal.yml down -v  # Removes volumes too
```

---

## 📚 Documentation

| Document | What's Inside |
|----------|--------------|
| `VERIFICATION_SUMMARY.md` | Verification results & solutions |
| `DEPLOYMENT_OPTIONS.md` | Compare all deployment options |
| `CLOUD_DEPLOYMENT_GUIDE.md` | Deploy to Railway/AWS/GCP/Azure |
| `DOCKER_MEMORY_FIX.md` | Fix memory issues |
| `API_DOCUMENTATION.md` | All API endpoints |
| `AUTH_GUIDE.md` | Authentication details |
| `POSTMAN_SETUP_GUIDE.md` | Postman testing guide |
| `MULTITHREADING_GUIDE.md` | Concurrency patterns |
| `ARCHITECTURE.md` | System design |

---

## ❓ Troubleshooting

### Build Fails with Memory Error
→ Use `./start-minimal.sh` instead

### Port Already in Use
```bash
# Find process using port
lsof -ti:8080 | xargs kill -9
```

### Service Won't Start
```bash
# Check logs
docker-compose -f docker/docker-compose.minimal.yml logs SERVICE_NAME

# Rebuild
docker-compose -f docker/docker-compose.minimal.yml build --no-cache SERVICE_NAME

# Restart
docker-compose -f docker/docker-compose.minimal.yml restart SERVICE_NAME
```

### Can't Connect to Database
```bash
# Check if running
docker ps | grep postgres

# Restart database
docker-compose -f docker/docker-compose.minimal.yml restart postgres
```

---

## 🎓 Learning Path

1. **Day 1:** Run minimal setup, test Auth & User APIs
2. **Day 2:** Add Product & Order services
3. **Day 3:** Add observability (Zipkin, Prometheus)
4. **Day 4:** Full stack with Kafka & ELK
5. **Day 5:** Deploy to cloud

---

## 🆘 Need Help?

1. Check `VERIFICATION_SUMMARY.md` for common issues
2. View service logs for errors
3. Consult specific documentation files
4. Check Docker/Java versions match requirements

---

## ✅ Success Checklist

- [ ] Docker is running (4 GB+ RAM for minimal, 12 GB+ for full)
- [ ] Java 21 installed (for local builds)
- [ ] Script executed successfully
- [ ] Eureka Dashboard accessible
- [ ] Can register a user
- [ ] Can login and get JWT token
- [ ] API Gateway routes requests correctly

---

**Ready to start? Pick your option above and run the script! 🚀**

