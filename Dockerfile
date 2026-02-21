FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY . .

#  mvnw-қа execute permission беру
RUN chmod +x mvnw

RUN ./mvnw clean package -DskipTests

EXPOSE 8080

CMD ["java", "-jar", "target/docservice-0.0.1-SNAPSHOT.jar"]
