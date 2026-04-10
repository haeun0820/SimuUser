FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY . .

RUN chmod +x gradlew
RUN ./gradlew build -x test --no-daemon

ENTRYPOINT ["sh", "-c", "java -jar build/libs/*.jar"]