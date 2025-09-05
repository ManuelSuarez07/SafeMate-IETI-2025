# 💰 SaveMate – Asistente de ahorro automático

**SaveMate** es un proyecto diseñado para ayudar a las personas a **ahorrar dinero de forma automática y sencilla**.  
Su finalidad es transformar los **gastos diarios pequeños** (☕ cafés, 🍫 snacks, 🚌 transporte) en **ahorro real** mediante redondeo automático, además de ofrecer un sistema de **gestión de usuarios, transacciones y metas de ahorro**.

---

## 🔎 Problema identificado
Muchas personas no logran ahorrar porque:
- ❌ No llevan control de los **gastos pequeños**.
- ❌ El dinero “desaparece” al final del mes sin saber en qué se fue.
- ❌ No tienen **hábitos financieros disciplinados**.

---

## 💡 Solución propuesta
- 🔗 Aplicación conectada a **billeteras digitales o bancos**.
- 💵 **Redondea los gastos diarios** y guarda el excedente en una “alcancía digital”.
- 👥 Gestión de **usuarios**.
- 🧾 Registro de **transacciones** (gastos/ingresos).
- 🎯 Creación y seguimiento de **metas de ahorro**.

---

## ⚙️ Mecánica de ahorro automático
El funcionamiento de **SaveMate** se basa en **mecanismos flexibles de ahorro** que permiten al usuario escoger cómo desea apartar dinero en cada transacción:

📌 **Ejemplo práctico (Redondeo)**
- Pago realizado: **$15.200**
- Redondeo al múltiplo superior: **$16.000**
- Ahorro generado: **$800**

📌 **Ejemplo práctico (Porcentaje)**
- Pago realizado: **$20.000**
- Configuración: 5% de cada gasto
- Ahorro generado: **$1.000**

El monto generado se registra como **ahorro en la aplicación** y, en una futura integración con APIs bancarias o billeteras digitales (como Nequi o Daviplata), sería transferido automáticamente a la cuenta de ahorro vinculada.

---

## 🔄 Manejo en caso de saldo insuficiente
En situaciones donde el usuario no tenga suficiente saldo para cubrir el ahorro, SaveMate contempla **tres opciones configurables por el usuario**:

1. **Opción A – No realizar el ahorro:**  
   La transacción se ejecuta normalmente, pero sin apartar dinero para ahorro.

2. **Opción B – Ahorro pendiente:**  
   El sistema registra el monto de ahorro como “pendiente” y lo descuenta automáticamente en la **próxima recarga o movimiento** de la billetera/cuenta.

3. **Opción C – Saldo mínimo seguro:**  
   El usuario define un **mínimo de saldo** (por ejemplo, $10.000). Si el ahorro implica bajar de ese mínimo, no se ejecuta.

---

## 🆓💎 Versiones del producto

### 🆓 Versión Free
- Registro de **gastos e ingresos**.
- Posibilidad de **filtrar transacciones** por:
    - Día
    - Semana
    - Mes
    - 3 meses
    - Semestre
    - Año
- 💵 **Ahorro automático limitado**: hasta **12 transacciones de ahorro al mes** (redondeo únicamente).

### 💎 Versión Pro (de pago)
Incluye todas las funcionalidades de la versión Free más:
- 💵 **Ahorro automático sin límites** en cada transacción.
- ⚙️ Configuración avanzada del ahorro:
    - Redondeo al múltiplo superior.
    - Un **porcentaje fijo** de cada transacción (ej: 5%).
- 🎯 **Metas de ahorro avanzadas** (varias metas simultáneas con porcentajes de distribución).
- 📊 **Reportes descargables en PDF/Excel** de gastos y ahorros.

---

## 🏗️ Arquitectura del proyecto
El proyecto está desarrollado en **Java con Spring Boot** y diseñado para ser escalable y fácil de desplegar en **AWS**.

### 📂 Estructura de paquetes
```bash
src/main/java/com/savemate/
│
├── config/                  # ⚙️ Configuración de seguridad y global
│   └── SecurityConfig.java
│
├── controller/              # 🌐 Controladores REST (endpoints)
│   ├── UserController.java
│   ├── TransactionController.java
│   └── SavingController.java
│
├── dto/                     # 📦 Objetos de transferencia de datos
│   ├── UserDTO.java
│   ├── TransactionDTO.java
│   └── SavingDTO.java
│
├── exception/               # 🚨 Manejo de errores y excepciones
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
├── util/                    # 🛠️ Utilidades del proyecto
│   ├── DateUtils.java
│   ├── RoundingUtils.java
│   ├── ReportGeneratorUtils.java   # 📊 Exportación PDF/Excel (solo versión Pro)
│   └── SavingStrategyUtils.java    # ⚙️ Redondeo o % (solo versión Pro)
│
└── SaveMateApplication.java # 🚀 Clase principal de Spring Boot
```

---

## 🛠️ Tecnologías utilizadas
- ☕ Java 17
- 🌱 Spring Boot 3
- 🗂️ Spring Data JPA
- 🔐 Spring Security
- 🛢️ MySQL
- 📦 Maven
- 📊 Power BI / Grafana *(para dashboards en fases futuras)*
- ☁️ AWS *(futuro despliegue)*

---

## ⚡ Instalación y ejecución local

### 📋 Requisitos previos
- ☕ Java 17+
- 📦 Maven
- 🛢️ MySQL 8

### 📥 Clonar repositorio
```bash
git clone https://github.com/tuusuario/savemate.git
cd savemate
```

### 🛢️ Configurar base de datos
Crea la base de datos en MySQL:
```bash
CREATE DATABASE savemate_db;
```

### ⚙️ Configurar `application.properties`
Ubicado en `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/savemate_db
spring.datasource.username=tu_usuario
spring.datasource.password=tu_password
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.database-platform=org.hibernate.dialect.MySQL8Dialect
server.port=8080
```

### ▶️ Ejecutar la aplicación
```bash
mvn spring-boot:run
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
- 🔗 Integración con **APIs bancarias reales**.
- 📊 Dashboard visual en **Power BI / Grafana**.
- 📱 Notificaciones push en la app móvil.
- 🏅 Sistema de recompensas por ahorro.

---

## 👨‍💻 Equipo
- Manuel Suárez
- Yeltzyn Sierra
- Cristian Zeballos  
