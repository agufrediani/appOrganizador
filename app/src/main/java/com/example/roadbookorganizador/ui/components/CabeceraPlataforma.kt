package com.example.roadbookorganizador.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roadbookorganizador.data.local.UsuarioSesion
import com.example.roadbookorganizador.data.local.entity.RallyEntity
import com.example.roadbookorganizador.ui.theme.ThemeManager

/** Colores de la plataforma (App.tsx). */
object ColoresPlataforma {
    val celeste = Color(0xFF00B4D8)
    val azul = Color(0xFF0284C7)
    val amarillo = Color(0xFFFFB703)
    val verde = Color(0xFF06D6A0)
    val fondoOscuro = Color(0xFF0A0E17)
    val tarjetaOscura = Color(0xFF131B2A)
    val superficieOscura = Color(0xFF1C273A)
    val bordeOscuro = Color(0xFF2A374D)

    @Composable fun fondo() = if (ThemeManager.isDarkTheme) fondoOscuro else Color(0xFFF1F5F9)
    @Composable fun tarjeta() = if (ThemeManager.isDarkTheme) tarjetaOscura else Color.White
    @Composable fun superficie() = if (ThemeManager.isDarkTheme) superficieOscura else Color(0xFFF1F5F9)
    @Composable fun borde() = if (ThemeManager.isDarkTheme) bordeOscuro else Color(0xFFE2E8F0)
    @Composable fun texto() = if (ThemeManager.isDarkTheme) Color.White else Color(0xFF0F172A)
    @Composable fun apagado() = if (ThemeManager.isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)
}

/**
 * Cabecera de la plataforma: bandera, "FREDIANI ROADBOOK" con "SISTEMA OFICIAL", subtítulo,
 * lo del medio ([centro]) y a la derecha Preferencias, tema, usuario y cerrar sesión.
 */
@Composable
fun CabeceraPlataforma(
    sesion: UsuarioSesion,
    subtitulo: String,
    onPreferencias: () -> Unit,
    onLogout: () -> Unit,
    /** El indicador de conexión chiquito al lado del tema (false si la pantalla ya lo muestra grande). */
    conIndicador: Boolean = true,
    centro: @Composable RowScope.() -> Unit = {}
) {
    val isDark = ThemeManager.isDarkTheme
    val c = ColoresPlataforma
    if (tamanoPantalla() == TamanoPantalla.CHICO) {
        CabeceraCelular(sesion, subtitulo, onPreferencias, onLogout, centro)
        return
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isDark) c.fondoOscuro.copy(alpha = 0.95f) else Color.White)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logo e identificador
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(c.celeste)
        ) { Icon(Icons.Default.Flag, contentDescription = null, tint = c.fondoOscuro, modifier = Modifier.size(20.dp)) }
        Spacer(Modifier.width(12.dp))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("FREDIANI ROADBOOK", fontSize = 14.sp, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp,
                    color = if (isDark) c.celeste else c.azul)
                Spacer(Modifier.width(8.dp))
                Text(
                    "SISTEMA OFICIAL", fontSize = 10.sp, fontWeight = FontWeight.Black, color = c.amarillo,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(c.amarillo.copy(alpha = 0.2f))
                        .border(1.dp, c.amarillo.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                )
            }
            Text(subtitulo, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = c.apagado(), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }

        Spacer(Modifier.width(16.dp))
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) { centro() }
        Spacer(Modifier.width(16.dp))

        // Acciones de usuario y tema
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (conIndicador) IndicadorConexion()
            BotonCuadrado(Icons.Default.Settings, "Preferencias", tinte = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155), onClick = onPreferencias)
            BotonTema()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDark) c.tarjetaOscura else Color(0xFFF1F5F9))
                    .border(1.dp, c.borde(), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(c.celeste.copy(alpha = 0.2f))
                ) {
                    Text(sesion.username.take(2).uppercase().ifBlank { "--" }, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = c.celeste)
                }
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(sesion.nombre.ifBlank { sesion.username.ifBlank { "Usuario" } }, fontSize = 12.sp, fontWeight = FontWeight.Black, color = c.texto(), maxLines = 1)
                    Text(sesion.rol, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = c.apagado(), maxLines = 1)
                }
            }
            BotonCuadrado(
                Icons.AutoMirrored.Filled.Logout, "Cerrar Sesión", tinte = Color(0xFFFB7185),
                fondo = Color(0x1AF43F5E), borde = Color(0x4DF43F5E), onClick = onLogout
            )
        }
    }
    HorizontalDivider(color = ColoresPlataforma.borde())
}

/**
 * Cabecera en el celular: arriba la bandera, el nombre y los botones (preferencias, tema, salir);
 * abajo lo del medio (selector de rally o la cantidad de rallies), que se puede deslizar.
 */
@Composable
private fun CabeceraCelular(
    sesion: UsuarioSesion,
    subtitulo: String,
    onPreferencias: () -> Unit,
    onLogout: () -> Unit,
    centro: @Composable RowScope.() -> Unit
) {
    val isDark = ThemeManager.isDarkTheme
    val c = ColoresPlataforma
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isDark) c.fondoOscuro.copy(alpha = 0.95f) else Color.White)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(c.celeste)
            ) { Icon(Icons.Default.Flag, contentDescription = null, tint = c.fondoOscuro, modifier = Modifier.size(18.dp)) }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("FREDIANI ROADBOOK", fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 0.6.sp,
                    color = if (isDark) c.celeste else c.azul, maxLines = 1)
                Text(
                    listOf(subtitulo, sesion.nombre.ifBlank { sesion.username }).filter { it.isNotBlank() }.joinToString(" • "),
                    fontSize = 10.sp, fontWeight = FontWeight.Bold, color = c.apagado(), maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IndicadorConexion()
                BotonCuadrado(Icons.Default.Settings, "Preferencias", tinte = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155), onClick = onPreferencias)
                BotonTema()
                BotonCuadrado(
                    Icons.AutoMirrored.Filled.Logout, "Cerrar Sesión", tinte = Color(0xFFFB7185),
                    fondo = Color(0x1AF43F5E), borde = Color(0x4DF43F5E), onClick = onLogout
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically
        ) { centro() }
    }
    HorizontalDivider(color = ColoresPlataforma.borde())
}

/** "< MIS RALLIES" + selector del rally, como la cabecera de la plataforma dentro de un rally. */
@Composable
fun SelectorRallyPlataforma(
    rallyActivo: RallyEntity?,
    rallies: List<RallyEntity>,
    onMisRallies: () -> Unit,
    onElegir: (RallyEntity) -> Unit
) {
    val isDark = ThemeManager.isDarkTheme
    val c = ColoresPlataforma
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(c.superficieOscura)
                .border(1.dp, c.bordeOscuro, RoundedCornerShape(12.dp))
                .clickable { onMisRallies() }
                .padding(horizontal = 12.dp, vertical = 7.dp)
        ) {
            Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = c.celeste, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("MIS RALLIES", fontSize = 12.sp, fontWeight = FontWeight.Black, color = c.celeste, letterSpacing = 0.6.sp)
        }
        var abierto by remember { mutableStateOf(false) }
        Box {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDark) c.tarjetaOscura else Color(0xFFF1F5F9))
                    .border(1.dp, if (isDark) c.bordeOscuro else Color(0xFFCBD5E1), RoundedCornerShape(12.dp))
                    .clickable { abierto = true }
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Text(
                    rallyActivo?.let { r -> r.nombre + (r.sede.substringAfterLast(", ", "").takeIf { it.isNotBlank() }?.let { " ($it)" } ?: "") } ?: "Elegí un rally",
                    fontSize = 12.sp, fontWeight = FontWeight.Black, color = c.texto(), maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = c.apagado(), modifier = Modifier.size(18.dp))
            }
            DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
                rallies.forEach { r ->
                    DropdownMenuItem(
                        text = { Text(r.nombre + (r.sede.substringAfterLast(", ", "").takeIf { it.isNotBlank() }?.let { " ($it)" } ?: ""), fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                        onClick = { abierto = false; onElegir(r) }
                    )
                }
            }
        }
    }
}

/** Pestañas del rally (sub-navegación de la plataforma), en el mismo orden que la web. */
enum class PestanaRally(val titulo: String, val icono: ImageVector) {
    PANEL("Panel General", Icons.Default.Dashboard),
    TRAMOS("Tramos", Icons.Default.Layers),
    CRONOGRAMA("Cronograma", Icons.Default.CalendarMonth),
    ZONAS("Zonas y puntos de interés", Icons.Default.Groups),
    CARRERA("Director de carrera", Icons.Default.Campaign),
    ROADBOOK("Roadbook", Icons.Default.Download)
}

@Composable
fun PestanasRally(actual: PestanaRally, onElegir: (PestanaRally) -> Unit) {
    val isDark = ThemeManager.isDarkTheme
    val c = ColoresPlataforma
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isDark) Color(0xFF0E1420) else Color(0xFFF8FAFC))
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PestanaRally.entries.forEach { p ->
            val sel = p == actual
            val fondo = if (sel) (if (isDark) c.celeste else c.azul) else Color.Transparent
            val tinta = if (sel) (if (isDark) c.fondoOscuro else Color.White) else (if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(fondo)
                    .clickable { if (!sel) onElegir(p) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Icon(p.icono, contentDescription = null, tint = tinta, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(p.titulo.uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Black, color = tinta, letterSpacing = 0.6.sp)
            }
        }
    }
    HorizontalDivider(color = ColoresPlataforma.borde())
}

/**
 * El botón del tema (claro / oscuro), el mismo en todas las pantallas: el de la cabecera de
 * la plataforma (luna en tema claro, sol en tema oscuro).
 */
@Composable
fun BotonTema() {
    val isDark = ThemeManager.isDarkTheme
    BotonCuadrado(
        if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
        "Alternar entre modo oscuro y modo blanco",
        tinte = if (isDark) Color(0xFFFBBF24) else Color(0xFF334155)
    ) { ThemeManager.toggleTheme() }
}

@Composable
private fun BotonCuadrado(
    icono: ImageVector, ayuda: String, tinte: Color,
    fondo: Color? = null, borde: Color? = null, onClick: () -> Unit
) {
    val isDark = ThemeManager.isDarkTheme
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(fondo ?: (if (isDark) ColoresPlataforma.superficieOscura else Color(0xFFF1F5F9)))
            .border(1.dp, borde ?: (if (isDark) ColoresPlataforma.bordeOscuro else Color(0xFFCBD5E1)), RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) { Icon(icono, contentDescription = ayuda, tint = tinte, modifier = Modifier.size(16.dp)) }
}

/**
 * Marco de todas las pantallas de adentro de un rally, igual que la plataforma: cabecera con
 * "< MIS RALLIES" y el selector de rally, las pestañas (Panel General, Tramos, Zonas y puntos
 * de interés) y abajo el contenido de la pestaña.
 */
@Composable
fun MarcoRally(
    sesion: UsuarioSesion,
    rallyActivo: RallyEntity?,
    rallies: List<RallyEntity>,
    pestana: PestanaRally,
    onPestana: (PestanaRally) -> Unit,
    onMisRallies: () -> Unit,
    onElegirRally: (RallyEntity) -> Unit,
    onPreferencias: () -> Unit,
    onLogout: () -> Unit,
    contenido: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(ColoresPlataforma.fondo())) {
        CabeceraPlataforma(
            sesion = sesion,
            subtitulo = rallyActivo?.nombre ?: "Campeonato de Rally Oficial",
            onPreferencias = onPreferencias,
            onLogout = onLogout
        ) {
            SelectorRallyPlataforma(
                rallyActivo = rallyActivo,
                rallies = rallies,
                onMisRallies = onMisRallies,
                onElegir = onElegirRally
            )
        }
        PestanasRally(pestana) { p -> if (p != pestana) onPestana(p) }
        Box(modifier = Modifier.fillMaxWidth().weight(1f).navigationBarsPadding()) { contenido() }
    }
}
