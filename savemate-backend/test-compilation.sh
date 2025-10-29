#!/bin/bash

echo "🧪 Probando compilación de SaveMate Backend..."

# Limpiar compilaciones anteriores
echo "🧹 Limpiando compilaciones anteriores..."
mvn clean

# Compilar el proyecto
echo "🔨 Compilando el proyecto..."
mvn compile

# Verificar si la compilación fue exitosa
if [ $? -eq 0 ]; then
    echo "✅ ¡Compilación exitosa!"
    echo "🚀 Puedes ejecutar la aplicación con: mvn spring-boot:run"
else
    echo "❌ Error en la compilación. Revisa los errores arriba."
    exit 1
fi