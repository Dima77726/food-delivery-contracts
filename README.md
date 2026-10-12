# food-delivery-contracts

Контракты платформы доставки еды — то, на что завязаны все сервисы сразу.

| Артефакт | Что внутри |
|---|---|
| `com.dima:event-contracts` | формы сообщений Kafka и имена топиков (Java-записи, без зависимостей) |
| `com.dima:grpc-contracts` | `.proto` и сгенерированные стабы gRPC |
| `com.dima:food-delivery-client` | Java-клиент к REST API; внутри jar — сама спецификация `openapi/food-delivery-api.yaml` |

Публикуются в GitHub Packages этого репозитория по тегу `vX.Y.Z`. Версия контрактов своя,
отдельная от версий сервисов.

## Правила изменений

- **Поля только добавляются.** Удаление поля или смена типа ломает потребителей, которые уже читают топик.
- **Статусы — строками, а не enum**: неизвестное значение не должно ронять потребителя.
- **Несовместимое изменение** — мажорная версия и новый пакет `.proto` (`catalog.v2`); старый живёт рядом, пока им пользуются.
- **Логики здесь быть не должно.** Общая библиотека с кодом — распределённый монолит: правка в ней требует синхронного релиза всех сервисов.

## Подключение

Репозиторий GitHub Packages требует токен даже для чтения. В `~/.m2/settings.xml`:

```xml
<servers>
  <server>
    <id>github</id>
    <username>ВАШ_GITHUB_ЛОГИН</username>
    <password>ТОКЕН_С_ПРАВОМ_read:packages</password>
  </server>
</servers>
```

В pom сервиса — репозиторий и зависимости:

```xml
<repositories>
  <repository>
    <id>github</id>
    <url>https://maven.pkg.github.com/Dima77726/food-delivery-contracts</url>
  </repository>
</repositories>
```

В GitHub Actions токен даёт `GITHUB_TOKEN` (`actions/setup-java` с `server-id: github`).

## Сборка

```bash
./mvnw verify
```
