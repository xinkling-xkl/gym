@echo off
echo ===== Building all services =====

echo [1/6] Building gym...
cd gym && call mvn clean package -DskipTests -q && cd ..

echo [2/6] Building login...
cd login && call mvn clean package -DskipTests -q && cd ..

echo [3/6] Building gateway...
cd gateway && call mvn clean package -DskipTests -q && cd ..

echo [4/6] Building file-service...
cd file-service && call mvn clean package -DskipTests -q && cd ..

echo [5/6] Building ai-chat...
cd ai-chat && call mvn clean package -DskipTests -q && cd ..

echo [6/6] Building notification-service...
cd notification-service && call mvn clean package -DskipTests -q && cd ..

echo ===== All builds complete! =====
echo Next step: docker-compose --env-file .env.docker up -d
pause
