# 💰 SaveMate – Tu asistente de ahorro inteligente

**SaveMate** es una **aplicación móvil (Android/iOS)** diseñada para ayudar a las personas a **ahorrar dinero de manera automática e inteligente**.  
Su finalidad es transformar los **gastos diarios pequeños** (☕ cafés, 🍫 snacks, 🚕 transporte) en **ahorro real** mediante redondeo automático, control de gastos y **predicciones con Inteligencia Artificial** que anticipan tus consumos más probables.

---

## 🔎 Problema identificado
Muchas personas no logran ahorrar porque:
- ❌ No llevan control de los **gastos pequeños**.
- ❌ El dinero “desaparece” al final del mes sin saber en qué se fue.
- ❌ No tienen **hábitos financieros consistentes**.

---

## 💡 Solución propuesta
SaveMate ofrece:
- 🔗 Conexión con **billeteras digitales o bancos**.
- 💵 **Redondeo automático** en cada compra y registro como ahorro.
- 👥 Gestión de **usuarios**.
- 🧾 Registro de **transacciones** (gastos/ingresos).
- 🎯 Creación y seguimiento de **metas de ahorro**.
- 🤖 **IA predictiva** que anticipa en qué podrías gastar hoy o esta semana.

---

## ⚙️ Mecánica de redondeo automático
El funcionamiento de SaveMate se basa en **redondear los pagos realizados** para destinar la diferencia a un fondo de ahorro.

📌 **Ejemplo práctico**
- Pago realizado: **$15.200**
- Redondeo al múltiplo superior: **$16.000**
- Ahorro generado: **$800**

El excedente ($800) se registra como **ahorro en la app** y, en integración futura con APIs bancarias (Nequi, Daviplata), se transferirá automáticamente a la billetera o cuenta vinculada.

---

## 🔄 Manejo en caso de saldo insuficiente
El usuario podrá elegir entre tres opciones:

1. **Opción A – No realizar el ahorro:**  
   La transacción se ejecuta normalmente, pero sin apartar dinero.

2. **Opción B – Ahorro pendiente:**  
   El sistema lo registra como “pendiente” y lo descuenta en la **próxima recarga**.

3. **Opción C – Saldo mínimo seguro:**  
   El usuario define un **mínimo de saldo** (ej: $10.000). Si el redondeo baja de ese mínimo, no se ejecuta.

---

## 🧠 Inteligencia Artificial integrada

SaveMate incorpora un sistema de **IA predictiva** que aprende de los hábitos de consumo de cada usuario para **anticipar en qué podría gastar**.

- 🔮 **Predicciones diarias/semanales:** Basadas en historial, la app estima dónde es más probable que gastes (ej: Uber, cafés, snacks).
- 📊 **Análisis de patrones:** Detecta gastos frecuentes según día y hora.
- 📱 **Alertas personalizadas:** Recomienda ahorrar **antes de gastar**.

👉 Ejemplo de notificación:
> “Hoy martes sueles gastar en 🚕 Uber y 🍫 snacks. ¿Quieres apartar $5.000 antes de que ocurra el gasto?”

---

## 🆓💎 Versiones del producto

### 🆓 Versión Gratuita
- Registro de **gastos e ingresos**.
- **Filtros de transacciones** por día, semana, mes, trimestre, semestre y año.
- Ahorro mediante redondeo, pero limitado a **20 transacciones al mes**.

### 💎 Versión Paga
Incluye todas las funciones de la versión Free más:
- 💵 **Ahorro automático ilimitado** en cada transacción.
- 🎯 **Metas de ahorro avanzadas**: varias metas simultáneas con distribución porcentual.
- ⚙️ Opciones configurables de ahorro:
    - Redondeo.
    - % del valor de cada transacción.
- 📊 **Reportes descargables en PDF/Excel** de gastos y ahorros.
- 🤖 **Predicciones personalizadas mejoradas** con IA (más precisión y alertas inteligentes).

---

## 🏗️ Arquitectura del proyecto
El proyecto está dividido en dos partes principales:

```
/savemate-backend     -> API REST en Spring Boot (gestión de datos y lógica de negocio)
/savemate-mobile      -> App móvil Flutter (UI Android/iOS + integración con IA)
```

### 📂 Estructura del backend
```bash
src/main/java/com/savemate/
│
├── config/                  # ⚙️ Configuración de seguridad
│   └── SecurityConfig.java
│
├── controller/              # 🌐 Controladores REST (endpoints)
│   ├── UserController.java
│   ├── TransactionController.java
│   └── SavingController.java
│
├── dto/                     # 📦 Objetos de transferencia
│   ├── UserDTO.java
│   ├── TransactionDTO.java
│   └── SavingDTO.java
│
├── exception/               # 🚨 Manejo de errores
│   ├── GlobalExceptionHandler.java
│   └── ResourceNotFoundException.java
│
├── model/                   # 🗄️ Entidades JPA
│   ├── User.java
│   ├── Transaction.java
│   └── SavingGoal.java
│
├── repository/              # 🛢️ Repositorios JPA
│   ├── UserRepository.java
│   ├── TransactionRepository.java
│   └── SavingRepository.java
│
├── service/                 # 🧠 Lógica de negocio
│   ├── UserService.java
│   ├── TransactionService.java
│   └── SavingService.java
│
├── util/                    # 🛠️ Utilidades
│   ├── DateUtils.java
│   ├── RoundingUtils.java
│   └── ReportGeneratorUtils.java   # 📊 Exportación PDF/Excel (versión Pro)
│
└── SaveMateApplication.java # 🚀 Clase principal
```

---

## 🛠️ Tecnologías utilizadas
### Backend
- ☕ Java 17
- 🌱 Spring Boot 3
- 🗂️ Spring Data JPA
- 🔐 Spring Security
- 🛢️ MySQL
- 📦 Maven
- ☁️ AWS *(futuro despliegue)*

### Móvil
- 📱 Flutter (Android/iOS)
- 🤖 TensorFlow Lite *(IA en dispositivo)*
- 🔗 HTTP/Dio *(consumo de API REST)*

### Visualización futura
- 📊 Power BI / Grafana

---

## ⚡ Instalación y ejecución local

### 📥 Clonar repositorio
```bash
git clone https://github.com/tuusuario/savemate.git
```

### 🛢️ Configurar base de datos
```bash
CREATE DATABASE savemate_db;
```

### ⚙️ Configurar `application.properties`
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/savemate_db
spring.datasource.username=tu_usuario
spring.datasource.password=tu_password
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.database-platform=org.hibernate.dialect.MySQL8Dialect
server.port=8080
```

### ▶️ Ejecutar el backend
```bash
cd savemate-backend
mvn spring-boot:run
```

### ▶️ Ejecutar la app móvil
```bash
cd savemate-mobile
flutter pub get
flutter run
```

---

## 📡 Endpoints principales

### 👥 Usuarios
* `POST /api/users` → Crear usuario
* `GET /api/users/{id}` → Obtener usuario por ID

### 🧾 Transacciones
* `POST /api/transactions` → Registrar gasto/ingreso
* `GET /api/transactions/user/{id}` → Listar transacciones de un usuario

### 🎯 Metas de ahorro
* `POST /api/savings` → Crear meta de ahorro
* `GET /api/savings/user/{id}` → Listar metas de un usuario

---

## 🚀 Futuras mejoras
- 🔗 Integración directa con **APIs bancarias reales**.
- 📱 **Notificaciones push** en la app móvil.
- 🏅 Sistema de **recompensas por ahorro**.
- 🤝 Modo colaborativo: metas de ahorro en grupo.

---

## 👨‍💻 Equipo
- Manuel Suárez
- Yeltzyn Sierra
- Cristian Zeballos
