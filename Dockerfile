# ----------------------------------------------------
# GIAI ĐOẠN 1: BUILD (Sử dụng JDK để biên dịch và đóng gói)
# ----------------------------------------------------
FROM eclipse-temurin:25-jdk AS builder

# Thiết lập thư mục làm việc bên trong container
WORKDIR /app

# Copy các file cấu hình Maven/Gradle và source code
# Copy pom.xml trước để tận dụng Docker cache cho dependencies
COPY pom.xml .
COPY src /app/src

# Nếu bạn dùng Maven Wrapper:
COPY .mvn .mvn
COPY mvnw .

# Cài đặt dependencies và build dự án
# Lệnh này bỏ qua test để build nhanh hơn, và output JAR file vào /app/target/
RUN ./mvnw -B -DskipTests clean package

# ----------------------------------------------------
# GIAI ĐOẠN 2: RUNTIME (Sử dụng JRE để chạy)
# ----------------------------------------------------
# Chỉ cần Java Runtime Environment (JRE) cho giai đoạn chạy
FROM eclipse-temurin:25-jre

# Thiết lập thư mục làm việc
WORKDIR /app

# Copy file JAR từ giai đoạn 'builder'
# *jar để lấy tên file JAR đã build
COPY --from=builder /app/target/*.jar app.jar

# Cấu hình môi trường (Không bắt buộc, nhưng tốt cho việc phân biệt)
# Giả sử bạn muốn mặc định chạy profile 'prod' trên Docker
ENV SPRING_PROFILES_ACTIVE=prod

# Thiết lập múi giờ (Rất quan trọng cho server log và timestamp)
# Đặt múi giờ cho Việt Nam (Ho Chi Minh)
ENV TZ Asia/Ho_Chi_Minh

# Mở cổng mặc định của Spring Boot
EXPOSE 8080

# Lệnh chạy ứng dụng (Entrypoint)
# Sử dụng 'exec' form để xử lý tín hiệu tốt hơn (như SIGTERM khi container bị dừng)
ENTRYPOINT ["java", "-Xms128m", "-Xmx256m", "-XX:+UseG1GC", "-jar", "app.jar"]
