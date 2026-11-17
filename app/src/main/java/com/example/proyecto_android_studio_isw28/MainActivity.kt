package com.example.proyecto_android_studio_isw28

import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.math.*

private val Orange = Color(0xFF00C26E)
private val DarkBackground = Color(0xFF000000)
private val LightBackground = Color(0xFFF6F6F6)
private val DarkButton = Color(0xFF252525)
private val LightButton = Color(0xFFE6E6E6)

private enum class Page { CALCULATOR, CONVERTER, HISTORY }

private enum class ConverterType(val title: String, val symbol: String) {
    DISCOUNT("Descuento", "%"),
    BASE("Sistema numérico", "01"),
    SPEED("Velocidad", "◴"),
    BMI("IMC", "▧")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var darkMode by rememberSaveable { mutableStateOf(true) }
            var soundEnabled by rememberSaveable { mutableStateOf(true) }
            val scheme = if (darkMode) {
                darkColorScheme(
                    primary = Orange, onPrimary = Color.White,
                    background = DarkBackground, surface = DarkBackground,
                    onSurface = Color.White, onBackground = Color.White
                )
            } else {
                lightColorScheme(
                    primary = Orange, onPrimary = Color.White,
                    background = LightBackground, surface = LightBackground,
                    onSurface = Color.Black, onBackground = Color.Black
                )
            }
            MaterialTheme(colorScheme = scheme) {
                AppScreen(
                    darkMode = darkMode,
                    soundEnabled = soundEnabled,
                    onToggleDarkMode = { darkMode = !darkMode },
                    onToggleSound = { soundEnabled = !soundEnabled }
                )
            }
        }
    }
}

@Composable
private fun AppScreen(
    darkMode: Boolean,
    soundEnabled: Boolean,
    onToggleDarkMode: () -> Unit,
    onToggleSound: () -> Unit
) {
    var page by rememberSaveable { mutableStateOf(Page.CALCULATOR) }
    var converter by rememberSaveable { mutableStateOf<ConverterType?>(null) }
    var scientific by rememberSaveable { mutableStateOf(false) }
    val history = remember { mutableStateListOf<String>() }
    val drawer = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val bg = if (darkMode) DarkBackground else LightBackground
    val fg = if (darkMode) Color.White else Color(0xFF161616)
    val muted = if (darkMode) Color.LightGray else Color.DarkGray

    fun navigate(target: Page, selectedConverter: ConverterType? = null) {
        page = target
        converter = selectedConverter
        scope.launch { drawer.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawer,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(294.dp),
                drawerContainerColor = if (darkMode) Color(0xFF171717) else Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp, vertical = 20.dp)
                ) {
                    Text("MENÚ", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Orange)
                    Spacer(Modifier.height(16.dp))
                    DrawerEntry("▦   Calculadora", page == Page.CALCULATOR, fg) {
                        navigate(Page.CALCULATOR)
                    }
                    DrawerEntry("◷   Historial", page == Page.HISTORY, fg) {
                        navigate(Page.HISTORY)
                    }
                    HorizontalDivider(Modifier.padding(vertical = 12.dp))
                    Text("CONVERSIONES", color = muted, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    ConverterType.values().forEach { option ->
                        DrawerEntry("${option.symbol}   ${option.title}", converter == option, fg) {
                            navigate(Page.CONVERTER, option)
                        }
                    }
                    HorizontalDivider(Modifier.padding(vertical = 12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Modo oscuro", modifier = Modifier.weight(1f), color = fg)
                        Switch(checked = darkMode, onCheckedChange = { onToggleDarkMode() })
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Sonido de botones", modifier = Modifier.weight(1f), color = fg)
                        Switch(checked = soundEnabled, onCheckedChange = { onToggleSound() })
                    }
                }
            }
        }
    ) {
        Column(Modifier.fillMaxSize().background(bg)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { scope.launch { drawer.open() } }) {
                    Text("☰", color = fg, fontSize = 25.sp)
                }
                Text(
                    text = when (page) {
                        Page.CALCULATOR -> "Calculadora"
                        Page.HISTORY -> "Historial"
                        Page.CONVERTER -> converter?.title ?: "Convertidor"
                    },
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = fg,
                    maxLines = 1
                )
                IconButton(
                    onClick = {
                        playClick(context, soundEnabled)
                        when (page) {
                            Page.CALCULATOR -> scientific = !scientific
                            Page.CONVERTER -> if (converter != null) converter = null else page = Page.CALCULATOR
                            Page.HISTORY -> page = Page.CALCULATOR
                        }
                    }
                ) {
                    Text(
                        text = when (page) {
                            Page.CALCULATOR -> if (scientific) "←" else "→"
                            Page.CONVERTER -> "←"
                            Page.HISTORY -> "←"
                        },
                        color = if (page == Page.CALCULATOR) Orange else fg,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            when (page) {
                Page.CALCULATOR -> CalculatorContent(
                    darkMode = darkMode,
                    scientific = scientific,
                    soundEnabled = soundEnabled,
                    history = history
                )
                Page.CONVERTER -> if (converter == null) {
                    ConverterGrid(darkMode = darkMode, onClick = { converter = it })
                } else {
                    ConverterDetail(
                        type = converter!!,
                        darkMode = darkMode,
                        onBack = { converter = null }
                    )
                }
                Page.HISTORY -> HistoryContent(
                    history = history,
                    darkMode = darkMode,
                    onClear = { history.clear() }
                )
            }
        }
    }
}

@Composable
private fun DrawerEntry(label: String, selected: Boolean, foreground: Color, onClick: () -> Unit) {
    Text(
        text = label,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (selected) Orange.copy(alpha = 0.15f) else Color.Transparent,
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        color = if (selected) Orange else foreground,
        fontSize = 15.sp
    )
}

@Composable
private fun CalculatorContent(
    darkMode: Boolean,
    scientific: Boolean,
    soundEnabled: Boolean,
    history: MutableList<String>
) {
    val context = LocalContext.current
    val foreground = if (darkMode) Color.White else Color.Black
    val muted = if (darkMode) Color(0xFF6F6F6F) else Color.Gray
    val buttonBg = if (darkMode) DarkButton else LightButton
    var expression by rememberSaveable { mutableStateOf("0") }
    var justCalculated by rememberSaveable { mutableStateOf(false) }
    var degrees by rememberSaveable { mutableStateOf(true) }
    val result = remember(expression) { evaluateMath(expression) }

    fun log(operation: String) {
        history.add(operation)
        if (history.size > 40) history.removeAt(0)
    }

    fun applyMath(label: String, fn: (Double) -> Double) {
        val current = evaluateMath(expression) ?: run { expression = "Error"; return }
        val answer = runCatching { fn(current) }.getOrNull()
        if (answer == null || !answer.isFinite()) {
            expression = "Error"
        } else {
            log("$label($expression) = ${formatNumber(answer)}")
            expression = rawNumber(answer)
            justCalculated = true
        }
    }

    fun press(key: String) {
        playClick(context, soundEnabled)
        when (key) {
            "C" -> { expression = "0"; justCalculated = false }
            "⌫" -> {
                expression = if (expression.length <= 1 || expression == "Error") "0"
                else expression.dropLast(1)
                justCalculated = false
            }
            "=" -> {
                val answer = evaluateMath(expression)
                if (answer == null) expression = "Error"
                else {
                    log("$expression = ${formatNumber(answer)}")
                    expression = rawNumber(answer)
                    justCalculated = true
                }
            }
            "%" -> applyMath("%", { it / 100.0 })
            "sen", "cos", "tan", "asen", "acos", "atan" -> {
                applyMath(if (degrees) "$key°" else key) { x ->
                    val angle = if (degrees) Math.toRadians(x) else x
                    when (key) {
                        "sen" -> sin(angle)
                        "cos" -> cos(angle)
                        "tan" -> if (abs(cos(angle)) < 1e-12) Double.NaN else tan(angle)
                        "asen" -> if (degrees) Math.toDegrees(asin(x)) else asin(x)
                        "acos" -> if (degrees) Math.toDegrees(acos(x)) else acos(x)
                        else -> if (degrees) Math.toDegrees(atan(x)) else atan(x)
                    }
                }
            }
            "√x" -> applyMath("√", { sqrt(it) })
            "lg" -> applyMath("log10", { log10(it) })
            "ln" -> applyMath("ln", { ln(it) })
            "x!" -> applyMath("factorial", { x ->
                if (x < 0 || x > 170 || x != floor(x)) Double.NaN
                else (1..x.toInt()).fold(1.0) { acc, n -> acc * n }
            })
            "1/x" -> applyMath("1/", { 1.0 / it })
            "π", "e" -> {
                expression = when {
                    justCalculated || expression == "Error" || expression == "0" -> key
                    expression.last().isDigit() || expression.last() in listOf(')', 'π', 'e') -> "$expression×$key"
                    else -> expression + key
                }
                justCalculated = false
            }
            "(" -> {
                expression = when {
                    expression == "0" || expression == "Error" || justCalculated -> "("
                    expression.last().isDigit() || expression.last() in listOf(')', 'π', 'e') -> "$expression×("
                    else -> expression + "("
                }
                justCalculated = false
            }
            ")" -> {
                val opens = expression.count { it == '(' }
                val closes = expression.count { it == ')' }
                if (opens > closes && expression.lastOrNull()?.let {
                        it.isDigit() || it == ')' || it == 'π' || it == 'e'
                    } == true) expression += ")"
                justCalculated = false
            }
            "+", "−", "×", "÷", "xʸ" -> {
                val operator = if (key == "xʸ") "^" else key
                if (expression == "Error") return
                if (expression.lastOrNull() in listOf('+', '−', '×', '÷', '^')) {
                    expression = if (operator == "−" && expression.last() != '−')
                        expression + operator
                    else expression.dropLast(1) + operator
                } else expression += operator
                justCalculated = false
            }
            "." -> {
                if (expression == "Error" || justCalculated) {
                    expression = "0."
                } else {
                    val lastPart = expression.takeLastWhile { it.isDigit() || it == '.' }
                    if (!lastPart.contains('.')) {
                        expression += if (lastPart.isEmpty()) "0." else "."
                    }
                }
                justCalculated = false
            }
            else -> if (key.length == 1 && key[0].isDigit()) {
                expression = when {
                    justCalculated || expression == "Error" || expression == "0" -> key
                    expression.lastOrNull() in listOf(')', 'π', 'e') -> "$expression×$key"
                    else -> expression + key
                }
                justCalculated = false
            }
        }
    }

    val basicRows = listOf(
        listOf("C", "⌫", "%", "÷"),
        listOf("7", "8", "9", "×"),
        listOf("4", "5", "6", "−"),
        listOf("1", "2", "3", "+"),
        listOf("0", ".", "=")
    )
    val scienceRows = listOf(
        listOf("sen", "cos", "tan", "(", ")"),
        listOf("xʸ", "√x", "π", "⌫", "C"),
        listOf("7", "8", "9", "%", "÷"),
        listOf("4", "5", "6", "×", "−"),
        listOf("1", "2", "3", "+", "="),
        listOf("0", ".")
    )
    val rows = if (scientific) scienceRows else basicRows

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().padding(start = 12.dp, end = 12.dp, bottom = 10.dp)
    ) {
        val gap = 8.dp
        val reservedDisplay = if (scientific) 154.dp else 210.dp
        val idealHeight = (maxHeight - reservedDisplay - gap * (rows.size - 1)) / rows.size
        val rowHeight = idealHeight.coerceIn(42.dp, if (scientific) 60.dp else 76.dp)
        Column(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.End
            ) {
                history.takeLast(if (scientific) 1 else 3).forEach { line ->
                    Text(
                        text = line,
                        color = muted,
                        fontSize = if (scientific) 13.sp else 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 7.dp)
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = expression,
                    color = foreground,
                    fontSize = when {
                        expression.length > 24 -> 28.sp
                        expression.length > 14 -> 38.sp
                        else -> 52.sp
                    },
                    maxLines = 2,
                    textAlign = TextAlign.End,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 54.sp,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = if (expression == "Error") "Operación no válida"
                    else if (result != null) "= ${formatNumber(result)}" else "",
                    color = if (expression == "Error") Orange else muted,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 18.dp)
                )
            }
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(gap)
            ) {
                rows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(gap)
                    ) {
                        row.forEach { key ->
                            val isOperator = key in listOf("C", "⌫", "%", "÷", "×", "−", "+")
                            val special = key == "="
                            val textColor = when {
                                special -> Color.White
                                isOperator -> Orange
                                scientific && key !in listOf("0","1","2","3","4","5","6","7","8","9",".") -> muted
                                else -> foreground
                            }
                            KeyButton(
                                label = key,
                                modifier = Modifier.weight(1f),
                                height = rowHeight,
                                color = if (special) Orange else buttonBg,
                                textColor = textColor,
                                onClick = { press(key) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyButton(
    label: String,
    modifier: Modifier,
    height: Dp,
    color: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(height),
        shape = RoundedCornerShape(22.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        contentPadding = PaddingValues(0.dp),
        elevation = null
    ) {
        Text(
            text = label,
            fontSize = when {
                label.length > 3 -> 15.sp
                label.length > 2 -> 17.sp
                else -> 26.sp
            },
            fontWeight = if (label == "=") FontWeight.Bold else FontWeight.Normal,
            color = textColor,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
private fun HistoryContent(history: List<String>, darkMode: Boolean, onClear: () -> Unit) {
    val fg = if (darkMode) Color.White else Color.Black
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Historial de cálculos", color = fg, fontSize = 22.sp,
                fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            TextButton(onClick = onClear) { Text("Borrar", color = Orange) }
        }
        if (history.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Todavía no hay cálculos", color = fg.copy(alpha = 0.6f))
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(history.size) { i ->
                    Text(
                        history[history.lastIndex - i],
                        color = fg,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp),
                        textAlign = TextAlign.End,
                        fontSize = 18.sp
                    )
                    HorizontalDivider(color = fg.copy(alpha = 0.1f))
                }
            }
        }
    }
}

@Composable
private fun ConverterGrid(darkMode: Boolean, onClick: (ConverterType) -> Unit) {
    val fg = if (darkMode) Color.White else Color(0xFF222222)
    val iconColor = if (darkMode) Color(0xFFC6C6C6) else Color.DarkGray
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(26.dp)
    ) {
        items(ConverterType.values().toList()) { item ->
            Column(
                modifier = Modifier.fillMaxWidth().height(104.dp)
                    .clickable { onClick(item) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(item.symbol, color = iconColor, fontSize = 34.sp,
                    fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Text(item.title, color = fg, fontSize = 15.sp,
                    textAlign = TextAlign.Center, lineHeight = 18.sp, maxLines = 2)
            }
        }
    }
}

@Composable
private fun ConverterDetail(type: ConverterType, darkMode: Boolean, onBack: () -> Unit) {
    val fg = if (darkMode) Color.White else Color.Black
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("‹  Volver", color = Orange) }
            Spacer(Modifier.width(12.dp))
            Text(type.title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = fg)
        }
        Spacer(Modifier.height(22.dp))
        when (type) {
            ConverterType.DISCOUNT -> DiscountTool()
            ConverterType.BASE -> NumberBaseTool()
            ConverterType.BMI -> BmiTool()
            ConverterType.SPEED -> UnitsTool(type)
        }
    }
}

private data class MeasureUnit(val name: String, val factor: Double)

private fun unitsFor(type: ConverterType): List<MeasureUnit> = when (type) {
    ConverterType.SPEED -> listOf(
        MeasureUnit("m/s", 1.0),
        MeasureUnit("km/h", 1.0 / 3.6)
    )
    else -> emptyList()
}

@Composable
private fun UnitsTool(type: ConverterType) {
    val units = remember(type) { unitsFor(type) }
    var amount by remember(type) { mutableStateOf("1") }
    var from by remember(type) { mutableIntStateOf(0) }
    var to by remember(type) { mutableIntStateOf(if (units.size > 1) 1 else 0) }
    val v = parseNumber(amount)
    val result = if (v != null && units.isNotEmpty()) {
        v * units[from].factor / units[to].factor
    } else null
    NumberField("Cantidad", amount, { amount = it })
    Spacer(Modifier.height(16.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        UnitPicker("De", units.map { it.name }, from, { from = it }, Modifier.weight(1f))
        UnitPicker("A", units.map { it.name }, to, { to = it }, Modifier.weight(1f))
    }
    Spacer(Modifier.height(20.dp))
    ResultBox(result?.let { "${formatNumber(it)} ${units[to].name}" }
        ?: "Escribe un valor válido")
}

@Composable
private fun CurrencyTool() {
    var amount by rememberSaveable { mutableStateOf("1") }
    var rate by rememberSaveable { mutableStateOf("") }
    var from by rememberSaveable { mutableIntStateOf(0) }
    var to by rememberSaveable { mutableIntStateOf(1) }
    val options = listOf("USD", "MXN")
    val value = parseNumber(amount)
    val conversionRate = parseNumber(rate)
    val result = if (value != null && conversionRate != null && conversionRate > 0) {
        when {
            from == to -> value
            from == 0 -> value * conversionRate
            else -> value / conversionRate
        }
    } else null
    Text("Introduce una tasa actual de forma manual. No se consultan cotizaciones en tiempo real.",
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), fontSize = 14.sp)
    Spacer(Modifier.height(14.dp))
    NumberField("Cantidad", amount, { amount = it })
    Spacer(Modifier.height(12.dp))
    NumberField("Tasa: pesos mexicanos por 1 dólar", rate, { rate = it }, "Ej. 18.50")
    Spacer(Modifier.height(14.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        UnitPicker("De", options, from, { from = it }, Modifier.weight(1f))
        UnitPicker("A", options, to, { to = it }, Modifier.weight(1f))
    }
    Spacer(Modifier.height(20.dp))
    ResultBox(result?.let { "${formatNumber(it)} ${options[to]}" }
        ?: "Introduce una tasa mayor que cero")
}

@Composable
private fun FinanceTool() {
    var principal by rememberSaveable { mutableStateOf("10000") }
    var rate by rememberSaveable { mutableStateOf("10") }
    var years by rememberSaveable { mutableStateOf("1") }
    Text("Interés simple", fontWeight = FontWeight.Bold, fontSize = 18.sp)
    Spacer(Modifier.height(12.dp))
    NumberField("Capital inicial ($)", principal, { principal = it })
    Spacer(Modifier.height(12.dp))
    NumberField("Tasa de interés anual (%)", rate, { rate = it })
    Spacer(Modifier.height(12.dp))
    NumberField("Tiempo (años)", years, { years = it })
    val p = parseNumber(principal)
    val r = parseNumber(rate)
    val t = parseNumber(years)
    Spacer(Modifier.height(20.dp))
    if (p != null && r != null && t != null && p >= 0 && t >= 0) {
        val interest = p * r * t / 100.0
        ResultBox("Interés: $${formatNumber(interest)}\nMonto final: $${formatNumber(p + interest)}")
    } else ResultBox("Introduce valores válidos")
    Text("Fórmula: interés = capital × tasa × años / 100.",
        modifier = Modifier.padding(top = 15.dp), fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
}

@Composable
private fun DiscountTool() {
    var price by rememberSaveable { mutableStateOf("1000") }
    var discount by rememberSaveable { mutableStateOf("20") }
    NumberField("Precio original ($)", price, { price = it })
    Spacer(Modifier.height(12.dp))
    NumberField("Descuento (%)", discount, { discount = it })
    val p = parseNumber(price)
    val d = parseNumber(discount)
    Spacer(Modifier.height(20.dp))
    if (p != null && d != null && p >= 0 && d in 0.0..100.0) {
        val saved = p * d / 100.0
        ResultBox("Ahorras: $${formatNumber(saved)}\nPrecio final: $${formatNumber(p - saved)}")
    } else ResultBox("El precio debe ser positivo y el descuento estar entre 0 y 100%")
}

@Composable
private fun BmiTool() {
    var weight by rememberSaveable { mutableStateOf("70") }
    var height by rememberSaveable { mutableStateOf("170") }
    NumberField("Peso (kg)", weight, { weight = it })
    Spacer(Modifier.height(12.dp))
    NumberField("Estatura (cm)", height, { height = it })
    val kg = parseNumber(weight)
    val cm = parseNumber(height)
    Spacer(Modifier.height(20.dp))
    if (kg != null && cm != null && kg > 0 && cm > 0) {
        val bmi = kg / (cm / 100.0).pow(2)
        val category = when {
            bmi < 18.5 -> "Bajo peso"
            bmi < 25 -> "Peso habitual"
            bmi < 30 -> "Sobrepeso"
            else -> "Obesidad"
        }
        ResultBox("IMC: ${formatNumber(bmi)}\nClasificación: $category")
        Text("Clasificación para adultos.",
            modifier = Modifier.padding(top = 14.dp), fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
    } else ResultBox("Introduce peso y estatura mayores que cero")
}

@Composable
private fun DateTool() {
    var start by rememberSaveable { mutableStateOf("01/01/2026") }
    var end by rememberSaveable { mutableStateOf("31/12/2026") }
    Text("Diferencia entre fechas (dd/MM/aaaa)", fontSize = 15.sp)
    Spacer(Modifier.height(14.dp))
    TextFieldCustom("Fecha inicial", start, { start = it })
    Spacer(Modifier.height(12.dp))
    TextFieldCustom("Fecha final", end, { end = it })
    val first = parseDate(start)
    val last = parseDate(end)
    Spacer(Modifier.height(20.dp))
    ResultBox(if (first != null && last != null) {
        val days = abs((last.time - first.time) / 86_400_000L)
        "$days días de diferencia"
    } else "Fechas no válidas. Usa dd/MM/aaaa")
}

@Composable
private fun NumberBaseTool() {
    val bases = listOf("Binario (2)", "Decimal (10)", "Hexadecimal (16)")
    val radices = listOf(2, 10, 16)
    var value by rememberSaveable { mutableStateOf("1010") }
    var from by rememberSaveable { mutableIntStateOf(0) }
    var to by rememberSaveable { mutableIntStateOf(1) }
    TextFieldCustom("Número", value, { value = it })
    Spacer(Modifier.height(16.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        UnitPicker("De", bases, from, { from = it }, Modifier.weight(1f))
        UnitPicker("A", bases, to, { to = it }, Modifier.weight(1f))
    }
    val parsed = value.trim().toLongOrNull(radices[from])
    Spacer(Modifier.height(20.dp))
    ResultBox(parsed?.let { java.lang.Long.toString(it, radices[to]).uppercase(Locale.ROOT) }
        ?: "Número no válido para esa base o fuera del rango Long")
}

@Composable
private fun NumberField(label: String, value: String, onChange: (String) -> Unit, hint: String = "0") {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        placeholder = { Text(hint) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}

@Composable
private fun TextFieldCustom(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onChange,
        label = { Text(label) }, modifier = Modifier.fillMaxWidth(), singleLine = true
    )
}

@Composable
private fun UnitPicker(
    prefix: String,
    options: List<String>,
    index: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("$prefix: ${options.getOrElse(index) { "" }} ▾", fontSize = 13.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEachIndexed { i, unit ->
                DropdownMenuItem(
                    text = { Text(unit) },
                    onClick = { onSelect(i); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun ResultBox(value: String) {
    Surface(
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp)) {
            Text("Resultado", color = Orange, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            Text(value, color = MaterialTheme.colorScheme.onSurface,
                fontSize = 23.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun parseNumber(value: String): Double? =
    value.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }

private fun parseDate(input: String): java.util.Date? = runCatching {
    val format = SimpleDateFormat("dd/MM/yyyy", Locale.ROOT)
    format.isLenient = false
    format.timeZone = TimeZone.getTimeZone("UTC")
    // ParsePosition comprueba que no queden caracteres extra después de la fecha.
    val pos = java.text.ParsePosition(0)
    val date = format.parse(input.trim(), pos)
    if (date != null && pos.index == input.trim().length) date else null
}.getOrNull()

private fun formatNumber(value: Double): String {
    if (!value.isFinite()) return "Error"
    if (value == 0.0) return "0"
    val absValue = abs(value)
    val pattern = if (absValue >= 1e12 || absValue < 1e-9)
        "0.########E0" else "#,##0.##########"
    val format = DecimalFormat(pattern, DecimalFormatSymbols(Locale.US))
    return format.format(value)
}

private fun rawNumber(value: Double): String {
    if (!value.isFinite()) return "Error"
    if (value == 0.0) return "0"
    return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()
}

private fun evaluateMath(text: String): Double? = try {
    MathParser(text).parse().takeIf { it.isFinite() }
} catch (_: Exception) { null }

private class MathParser(private val text: String) {
    private var pos = 0

    fun parse(): Double {
        val result = parseSum()
        skipSpaces()
        require(pos == text.length) { "Símbolo inesperado" }
        return result
    }

    private fun skipSpaces() {
        while (pos < text.length && text[pos].isWhitespace()) pos++
    }

    private fun match(vararg symbols: Char): Boolean {
        skipSpaces()
        if (pos < text.length && text[pos] in symbols) {
            pos++
            return true
        }
        return false
    }

    private fun parseSum(): Double {
        var value = parseProduct()
        while (true) value = when {
            match('+') -> value + parseProduct()
            match('−', '-') -> value - parseProduct()
            else -> return value
        }
    }

    private fun parseProduct(): Double {
        var value = parseUnary()
        while (true) value = when {
            match('×', '*') -> value * parseUnary()
            match('÷', '/') -> value / parseUnary()
            else -> return value
        }
    }

    private fun parseUnary(): Double = when {
        match('+') -> parseUnary()
        match('−', '-') -> -parseUnary()
        else -> parsePower()
    }

    private fun parsePower(): Double {
        val base = parsePrimary()
        return if (match('^')) base.pow(parseUnary()) else base
    }

    private fun parsePrimary(): Double {
        skipSpaces()
        if (match('(')) {
            val result = parseSum()
            require(match(')')) { "Falta cerrar paréntesis" }
            return result
        }
        if (match('π')) return PI
        if (match('e')) return E
        val start = pos
        while (pos < text.length && (text[pos].isDigit() || text[pos] == '.')) pos++
        require(pos > start) { "Se esperaba un número" }
        return text.substring(start, pos).toDouble()
    }
}


private fun playClick(
    context: Context,
    enabled: Boolean
) {
    if (!enabled) return

    try {
        val mediaPlayer = MediaPlayer.create(
            context,
            R.raw.click_sound
        )

        mediaPlayer?.apply {
            setOnCompletionListener {
                it.release()
            }

            start()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
