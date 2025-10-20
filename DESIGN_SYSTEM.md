# 🎨 DocApp Design System

## Светлая тема с зеленым акцентом и Glassmorphism

### 🌟 Основные принципы

**Читаемость превыше всего** - все тексты имеют высокий контраст для комфортного чтения
**Glassmorphism для интерактивных элементов** - карточки, кнопки, навигация
**Зеленый акцент** - современный, природный цвет для primary действий
**Мягкие фоны** - светлые оттенки для комфортного восприятия

---

## 🎨 Цветовая палитра

### Primary Colors (Зеленый акцент)
```kotlin
PrimaryGreen = #2E7D32        // Темно-зеленый для primary действий
PrimaryContainer = #A5D6A7     // Светло-зеленый контейнер
SecondaryGreen = #43A047      // Средний зеленый для secondary
SecondaryContainer = #DFF4E1  // Очень светло-зеленый контейнер
TertiaryTeal = #00695C        // Тeal для tertiary
TertiaryContainer = #A6F2E2    // Светло-тeal контейнер
```

### Background Colors (Читаемость)
```kotlin
SoftBackground = #F2F7F1      // Очень светло-зеленый основной фон
SurfaceBase = #FBFDF9         // Почти белая поверхность
SurfaceVariant = #E0F2E3      // Светло-зеленый вариант
OnBackground = #121613        // Почти черный для высокого контраста
OnSurface = #121613          // Почти черный для высокого контраста
OnSurfaceVariant = #3C4A3E   // Темно-серый на светлых поверхностях
```

### Glassmorphism Colors
```kotlin
GlassTintTop = #CCFFFFFF      // 80% белый для glass верха
GlassTintBottom = #B3FFFFFF   // 70% белый для glass низа
GlassHighlight = #55FFFFFF    // 33% белый highlight
GlassShadow = #26000000       // 15% черный shadow
BorderBright = #B3FFFFFF      // 70% белый яркая граница
BorderShadow = #332E7D32      // 20% зеленый теневая граница
```

---

## 🔧 Glassmorphism Система

### GlassCard Компонент
```kotlin
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    shape: Shape = GlassShape,
    content: @Composable ColumnScope.() -> Unit
)
```

### Особенности GlassCard:
- **Полупрозрачный фон** с градиентом от верха к низу
- **Мягкая тень** для создания глубины
- **Градиентная граница** от белого к зеленому
- **Внутренний highlight** для эффекта "стекла"
- **Ripple эффект** для интерактивности
- **Поддержка click и longClick**

### GlassColors через CompositionLocal
```kotlin
@Immutable
data class GlassColors(
    val containerTop: Color,      // Верхний цвет контейнера
    val containerBottom: Color,   // Нижний цвет контейнера
    val highlight: Color,         // Цвет подсветки
    val borderBright: Color,      // Яркая граница
    val borderShadow: Color,      // Теневая граница
    val shadowColor: Color        // Цвет тени
)
```

---

## 📐 Радиусы и отступы

### Радиусы
```kotlin
cardCorner = 12.dp              // Стандартные карточки
```

### Отступы
```kotlin
screenPadding = 16.dp           // Основные отступы экрана
cardPadding = 12.dp             // Отступы в карточках
spaceXs = 6.dp                  // Минимальные отступы
spaceSm = 8.dp                  // Малые отступы
spaceMd = 12.dp                 // Средние отступы
spaceLg = 16.dp                 // Большие отступы
spaceXl = 24.dp                 // Очень большие отступы
```

---

## 🎯 Применение в интерфейсе

### ✅ Где использовать GlassCard:
- **Карточки папок** - основной контент с glass эффектом
- **Карточки документов** - список документов
- **Модальные окна** - диалоги и формы
- **Навигационные элементы** - боковые панели
- **Информационные блоки** - статистика, настройки

### ❌ Где НЕ использовать:
- **Длинные тексты** - падает читаемость
- **Формы ввода** - нужен четкий контраст
- **Критичный контент** - важна четкость

### 🎨 Цветовые схемы:
- **Primary действия** - `MaterialTheme.colorScheme.primary` (зеленый)
- **Текст** - `MaterialTheme.colorScheme.onSurface` (почти черный)
- **Фоны** - `MaterialTheme.colorScheme.background` (светло-зеленый)
- **Поверхности** - `MaterialTheme.colorScheme.surface` (почти белый)

---

## 🚀 Примеры использования

### GlassCard для папки:
```kotlin
GlassCard(
    onClick = { openFolder(folder.id) },
    modifier = Modifier.fillMaxWidth()
) {
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        Text(
            text = folder.name,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "${folder.documents.size} документов",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
```

### GlassCard для документа:
```kotlin
GlassCard(
    onClick = { openDocument(doc.id) },
    modifier = Modifier.fillMaxWidth()
) {
    Row(
        modifier = Modifier.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Description,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = doc.name,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
    }
}
```

### GlassCard для модального окна:
```kotlin
GlassCard(
    modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
) {
    Column(
        modifier = Modifier.padding(20.dp)
    ) {
        Text(
            text = "Настройки",
            style = MaterialTheme.typography.headlineSmall
        )
        // Содержимое модального окна
    }
}
```

---

## 🎨 Результат

Приложение получает:
- ✨ **Современный glassmorphism дизайн** с полупрозрачными элементами
- 🌿 **Природный зеленый акцент** для primary действий
- 📖 **Высокую читаемость** благодаря контрастным цветам текста
- 💎 **Ощущение дорогих материалов** через glass эффекты
- 🎯 **Четкую иерархию** через разные уровни прозрачности
- 🌟 **Глубину и "воздух"** в интерфейсе

### Технические особенности:
- **CompositionLocal** для централизованного управления glass цветами
- **Material 3** цветовая схема с зеленым акцентом
- **Высокий контраст** текста для доступности
- **Градиентные эффекты** для реалистичного glass
- **Ripple анимации** для интерактивности

Система готова к использованию и легко расширяется для новых компонентов!
