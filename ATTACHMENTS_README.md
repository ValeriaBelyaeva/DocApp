# Система управления вложениями

## Обзор

Реализована современная система управления вложениями для Android приложения DocApp с поддержкой:

- Внутреннего хранения файлов в приватной директории приложения
- Миграции со старых внешних URI
- Множественных фото и PDF на документ
- Удаления, шаринга, предпросмотра
- Сборки "сирот" (неиспользуемых файлов)
- Опционального шифрования вложений

## Архитектура

### Слой данных (Data Layer)

#### Сущности
- `AttachmentEntity` - сущность для хранения метаданных вложений в БД
- `AttachmentDao` - интерфейс для работы с БД
- `AttachmentDaoSql` - реализация DAO через SQLCipher

#### Хранилище
- `AttachStorage` - интерфейс для работы с файлами
- `AttachStorageImpl` - реализация с потоковым копированием
- `FileGc` - сборщик мусора для неиспользуемых файлов
- `AttachmentCrypto` - опциональное шифрование файлов

### Слой домена (Domain Layer)

#### Репозитории
- `AttachmentRepository` - интерфейс репозитория
- `AttachmentRepositoryImpl` - реализация репозитория

#### Use Cases
- `ImportAttachmentsUseCase` - импорт вложений
- `DeleteAttachmentUseCase` - удаление вложений
- `CleanupOrphansUseCase` - очистка неиспользуемых файлов
- `MigrateExternalUrisUseCase` - миграция старых URI

### Слой представления (UI Layer)

#### Компоненты
- `AttachmentManager` - управление вложениями документа
- `ImportAttachmentsButton` - кнопка импорта для существующих документов
- `ImportAttachmentsForNewDocument` - импорт для новых документов
- `MigrationScreen` - экран миграции и очистки

## Структура базы данных

### Таблица attachments_new

```sql
CREATE TABLE attachments_new(
    id TEXT PRIMARY KEY,
    docId TEXT,                    -- null для временно не привязанных
    name TEXT NOT NULL,            -- отображаемое имя файла
    mime TEXT NOT NULL,            -- MIME тип
    size INTEGER NOT NULL,         -- размер в байтах
    sha256 TEXT NOT NULL,          -- хеш для дедупликации
    path TEXT NOT NULL,            -- абсолютный путь к файлу
    uri TEXT NOT NULL,             -- content:// URI через FileProvider
    createdAt INTEGER NOT NULL,    -- время создания
    FOREIGN KEY(docId) REFERENCES documents(id) ON DELETE CASCADE
);
```

## Безопасность

### Файловое хранилище
- Файлы хранятся в приватной директории приложения: `files/attachments/`
- Доступ через FileProvider с временными разрешениями
- Исключение из облачных бэкапов через `backup_rules.xml`

### Опциональное шифрование
- Включение через флаг `AttachmentCrypto.encryptionEnabled`
- Использование Android Keystore с MasterKey
- Шифрование через EncryptedFile (AES256_GCM)

## Производительность

### Потоковое копирование
- Буфер 8 KiB для копирования файлов
- SHA-256 вычисляется поблочно во время копирования
- Операции на Dispatchers.IO

### Дедупликация
- Проверка SHA-256 перед копированием
- Возможность переиспользования существующих файлов

## Миграция

### Автоматическая миграция БД
- Создание таблицы `attachments_new` при первом запуске
- Сохранение старой таблицы `attachments` для совместимости

### Миграция URI
- Поиск документов со старыми внешними URI
- Копирование файлов в локальное хранилище
- Обновление записей с новыми content:// URI
- Очистка старых записей после успешной миграции

## Использование

### Импорт вложений

```kotlin
// Импорт для существующего документа
val result = useCases.importAttachments(context, docId, uris)

// Импорт для нового документа
val result = useCases.importAttachments(context, null, uris)
```

### Управление вложениями

```kotlin
// Получение вложений документа
val attachments = attachmentRepository.getAttachmentsByDoc(docId)

// Удаление вложения
val deleted = attachmentRepository.deleteAttachment(attachmentId)

// Очистка неиспользуемых файлов
val result = attachmentRepository.cleanupOrphans()
```

### Миграция

```kotlin
// Запуск миграции старых URI
val result = useCases.migrateExternalUris(context)
```

## Тестирование

### Unit тесты
- `AttachmentStorageTest` - тесты хранилища файлов
- `AttachmentDaoTest` - тесты работы с БД
- `FileGcTest` - тесты сборщика мусора

### Instrumented тесты
- `AttachmentIntegrationTest` - полный жизненный цикл вложений

## Конфигурация

### FileProvider
```xml
<!-- AndroidManifest.xml -->
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.fileprovider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_paths" />
</provider>
```

### Backup Rules
```xml
<!-- backup_rules.xml -->
<data-extraction-rules>
  <cloud-backup>
    <exclude domain="file" path="attachments/"/>
    <exclude domain="database" path="*"/>
  </cloud-backup>
</data-extraction-rules>
```

## Критерии приёмки

✅ Импорт ≥10 фото и ≥5 PDF без сбоев
✅ Корректные метаданные (имя, размер, MIME, SHA256)
✅ Предпросмотр и шаринг работают
✅ Удаление документа → нет связанных файлов
✅ Сборка сирот очищает остатки
✅ Вложения доступны после удаления исходника
✅ Шифрование работает при включении
✅ Сборка успешна, регрессий нет

## Флаги и настройки

### Шифрование
```kotlin
// Включение шифрования вложений
AttachmentCrypto.encryptionEnabled = true
```

### Отладка
```kotlin
// Логирование операций с файлами
AppLogger.log("AttachStorage", "Importing file...")
```
