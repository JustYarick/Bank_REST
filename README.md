<h1>🚀ЗАПУСК</h1>

<h2>Требования</h2>
<ul>
  <li><strong>Docker</strong> - для контейнеризации приложения</li>
  <li><strong>Docker Compose</strong> - для оркестрации контейнеров</li>
</ul>

<h2>Быстрый запуск</h2>

<h3>1. Склонируйте репозиторий</h3>
<pre><code>git clone &lt;URL_репозитория&gt;
cd &lt;название_папки_проекта&gt;</code></pre>

<h3>2. Запустите приложение</h3>
<pre><code>docker-compose up -d --build</code></pre>

<h3>3. Проверьте статус контейнеров</h3>
<pre><code>docker-compose ps</code></pre>

<h3>4. Откройте приложение</h3>
<p>Приложение будет доступно по адресу:</p>
<pre><code>http://localhost:8080</code></pre>

<h2>Остановка приложения</h2>

<p><strong>Остановить все сервисы:</strong></p>
<pre><code>docker-compose down</code></pre>

<p><strong>Остановить с удалением данных:</strong></p>
<pre><code>docker-compose down -v</code></pre>

<h2>Полезные команды</h2>

<p><strong>Просмотр логов:</strong></p>
<pre><code>docker-compose logs -f</code></pre>

<p><strong>Перезапуск сервисов:</strong></p>
<pre><code>docker-compose restart</code></pre>

<p><strong>Пересборка и запуск:</strong></p>
<pre><code>docker-compose up -d --build --force-recreate</code></pre>

<p><strong>Список запущенных контейнеров:</strong></p>
<pre><code>docker-compose ps</code></pre>

<p><strong>Полная очистка (контейнеры + образы + volumes):</strong></p>
<pre><code>docker-compose down -v --rmi all</code></pre>

<h2>Отладка</h2>

<p><strong>Подключиться к контейнеру:</strong></p>
<pre><code>docker-compose exec &lt;service_name&gt; bash</code></pre>

<p><strong>Просмотр ресурсов:</strong></p>
<pre><code>docker-compose top</code></pre>

<h2>Дополнительная информация</h2>
<ul>
  <li><strong>Swagger UI:</strong> <code>http://localhost:8080/swagger-ui.html</code></li>
  <li><strong>База данных:</strong> PostgreSQL на порту 5432</li>
  <li><strong>Логи:</strong> Доступны через <code>docker-compose logs</code></li>
</ul>

<hr>

<h1>🚀 Разработка Системы Управления Банковскими Картами</h1>

<h2>📁 Стартовая структура</h2>
  <p>
    Проектная структура с директориями и описательными файлами (<code>README Controller.md</code>, <code>README Service.md</code> и т.д.) уже подготовлена.<br />
    Все реализации нужно добавлять <strong>в соответствующие директории</strong>.
  </p>
  <p>
    После завершения разработки <strong>временные README-файлы нужно удалить</strong>, чтобы они не попадали в итоговую сборку.
  </p>

<h2>📝 Описание задачи</h2>
  <p>Разработать backend-приложение на Java (Spring Boot) для управления банковскими картами:</p>
  <ul>
    <li>Создание и управление картами</li>
    <li>Просмотр карт</li>
    <li>Переводы между своими картами</li>
  </ul>

<h2>💳 Атрибуты карты</h2>
  <ul>
    <li>Номер карты (зашифрован, отображается маской: <code>**** **** **** 1234</code>)</li>
    <li>Владелец</li>
    <li>Срок действия</li>
    <li>Статус: Активна, Заблокирована, Истек срок</li>
    <li>Баланс</li>
  </ul>

<h2>🧾 Требования</h2>

<h3>✅ Аутентификация и авторизация</h3>
  <ul>
    <li>Spring Security + JWT</li>
    <li>Роли: <code>ADMIN</code> и <code>USER</code></li>
  </ul>

<h3>✅ Возможности</h3>
<strong>Администратор:</strong>
  <ul>
    <li>Создаёт, блокирует, активирует, удаляет карты</li>
    <li>Управляет пользователями</li>
    <li>Видит все карты</li>
  </ul>

<strong>Пользователь:</strong>
  <ul>
    <li>Просматривает свои карты (поиск + пагинация)</li>
    <li>Запрашивает блокировку карты</li>
    <li>Делает переводы между своими картами</li>
    <li>Смотрит баланс</li>
  </ul>

<h3>✅ API</h3>
  <ul>
    <li>CRUD для карт</li>
    <li>Переводы между своими картами</li>
    <li>Фильтрация и постраничная выдача</li>
    <li>Валидация и сообщения об ошибках</li>
  </ul>

<h3>✅ Безопасность</h3>
  <ul>
    <li>Шифрование данных</li>
    <li>Ролевой доступ</li>
    <li>Маскирование номеров карт</li>
  </ul>

<h3>✅ Работа с БД</h3>
  <ul>
    <li>PostgreSQL или MySQL</li>
    <li>Миграции через Liquibase (<code>src/main/resources/db/migration</code>)</li>
  </ul>

<h3>✅ Документация</h3>
  <ul>
    <li>Swagger UI / OpenAPI — <code>docs/openapi.yaml</code></li>
    <li><code>README.md</code> с инструкцией запуска</li>
  </ul>

<h3>✅ Развёртывание и тестирование</h3>
  <ul>
    <li>Docker Compose для dev-среды</li>
    <li>Liquibase миграции</li>
    <li>Юнит-тесты ключевой бизнес-логики</li>
  </ul>

<h2>📊 Оценка</h2>
  <ul>
    <li>Соответствие требованиям</li>
    <li>Чистота архитектуры и кода</li>
    <li>Безопасность</li>
    <li>Обработка ошибок</li>
    <li>Покрытие тестами</li>
    <li>ООП и уровни абстракции</li>
  </ul>

<h2>💡 Технологии</h2>
  <p>
    Java 17+, Spring Boot, Spring Security, Spring Data JPA, PostgreSQL/MySQL, Liquibase, Docker, JWT, Swagger (OpenAPI)
  </p>
