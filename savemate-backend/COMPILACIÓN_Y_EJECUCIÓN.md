# 🚀 Guía de Compilación y Ejecución - SaveMate Backend

## ✅ Arreglos Realizados

### 1. **Constructor Duplicado Eliminado**
- Removí el constructor por defecto duplicado en `TransactionDTO.java`
- `@NoArgsConstructor` ya crea el constructor por defecto automáticamente

### 2. **Imports Faltantes Agregados**
- `UserService.java`: Import `LocalDateTime`
- Todos los Controllers: Imports de sus respectivos Services

### 3. **Dependencias Actualizadas**
- MySQL: `mysql:mysql-connector-java` → `com.mysql:mysql-connector-j`
- Lombok: Configuración mejorada con annotation processors

### 4. **Self-Reference Errors Solucionados**
- Creé clases DTO separadas para resúmenes:
  - `TransactionSummaryDTO.java`
  - `AIRecommendationSummaryDTO.java`
  - `SavingSummaryDTO.java`
- Removí objetos anónimos con self-references

## 🔧 Pasos para Compilar y Ejecutar

### 1. **Compilar el Proyecto**
```bash
cd savemate-backend
mvn clean install
```

### 2. **Configurar Base de Datos MySQL**
```sql
-- Crear base de datos
CREATE DATABASE savemate_db;

-- Crear usuario (opcional)
CREATE USER 'savemate'@'localhost' IDENTIFIED BY 'tu_password';
GRANT ALL PRIVILEGES ON savemate_db.* TO 'savemate'@'localhost';
FLUSH PRIVILEGES;
```

### 3. **Configurar application.properties**
```properties
# Configuración de Base de Datos
spring.datasource.url=jdbc:mysql://localhost:3306/savemate_db
spring.datasource.username=tu_usuario_mysql
spring.datasource.password=tu_contraseña_mysql
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# Configuración JPA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect

# Configuración del Servidor
server.port=8080

# Configuración JWT
jwt.secret=miClaveSecretaSuperSegura12345678901234567890
jwt.expiration=86400000

# Configuración de Logging
logging.level.savemate=DEBUG
logging.level.org.springframework.security=DEBUG
```

### 4. **Ejecutar la Aplicación**
```bash
mvn spring-boot:run
```

### 5. **Verificar que Funciona**
Abre tu navegador y ve a:
```
http://localhost:8080/api/health
```

Deberías ver una respuesta como:
```json
{
  "status": "UP",
  "timestamp": "2024-01-01T10:00:00",
  "application": "SaveMate Backend",
  "version": "1.0.0"
}
```

## 🧪 Probar Endpoints Principales

### 1. **Registrar Usuario**
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "password123",
    "phoneNumber": "+1234567890"
  }'
```

### 2. **Login**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123"
  }'
```

### 3. **Probar con Token**
```bash
# Reemplaza TU_TOKEN con el token obtenido del login
curl -X GET http://localhost:8080/api/users/1 \
  -H "Authorization: Bearer TU_TOKEN"
```

## 🔍 Si Hay Errores de Compilación

### Error Común: "cannot find symbol"
**Solución**: Asegúrate que todos los imports estén correctos:
```java
import savemate.service.TransactionService;
import savemate.service.AIService;
import savemate.service.UserService;
import savemate.service.SavingService;
```

### Error Común: "constructor already defined"
**Solución**: Remueve constructores manuales si usas `@NoArgsConstructor`

### Error Común: "self-reference in initializer"
**Solución**: Usa clases DTO separadas en lugar de objetos anónimos:
```java
// ❌ Esto causa error
Object summary = new Object() {
    public final Double total = total; // self-reference
};

// ✅ Esto funciona
TransactionSummaryDTO summary = new TransactionSummaryDTO(total, savings, transactions);
```

### Error Común: "Lombok not working"
**Solución**: Asegúrate que el annotation processor esté configurado en `pom.xml`

## 📱 Probar la Aplicación Móvil

1. **Abre el proyecto Flutter**:
```bash
cd savemate-mobile
flutter pub get
```

2. **Configura la URL del Backend** en `lib/config/api_config.dart`:
```dart
static const String baseUrl = 'http://10.0.2.2:8080'; // Para emulador
// o
static const String baseUrl = 'http://192.168.1.100:8080'; // Tu IP local
```

3. **Ejecuta la app**:
```bash
flutter run
```

## 🎯 Checklist Final

- [ ] `mvn clean install` funciona sin errores
- [ ] Base de datos MySQL creada y configurada
- [ ] `application.properties` configurado
- [ ] `mvn spring-boot:run` inicia correctamente
- [ ] `http://localhost:8080/api/health` responde "UP"
- [ ] Registro de usuario funciona
- [ ] Login funciona y devuelve token JWT
- [ ] Endpoints protegidos funcionan con token

## 🚨 Troubleshooting

### **Error: "Access denied for user"**
- Verifica credenciales MySQL en `application.properties`
- Asegúrate que el usuario tenga permisos en la base de datos

### **Error: "Connection refused"**
- Asegúrate que MySQL esté corriendo
- Verifica que el puerto 3306 esté disponible

### **Error: "Table doesn't exist"**
- Las tablas se crean automáticamente con `ddl-auto=update`
- Si hay problemas, cambia a `ddl-auto=create-drop` para probar

### **Error: "JWT token not valid"**
- Verifica que el JWT secret sea el mismo en application.properties
- Asegúrate que el token no esté expirado

## 🎉 ¡Listo!

Si todos los pasos funcionan, tu SaveMate backend está listo para producción. La aplicación móvil Flutter debería poder conectarse sin problemas.

**Para soporte adicional, revisa los logs de la aplicación en la consola.**