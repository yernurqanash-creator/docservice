FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY . .

# mvnw-қа execute permission беру
RUN chmod +x mvnw

# Maven build
RUN ./mvnw clean package -DskipTests

# Koyeb PORT береді
ENV PORT=8000

EXPOSE 8000

CMD ["java", "-jar", "target/docservice-0.0.1-SNAPSHOT.jar", "--server.port=8000"]
