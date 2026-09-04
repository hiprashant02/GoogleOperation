package com.opp.googleoperation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DecoyCalculatorScreen(
    onMasterUnlock: () -> Unit,
    onDuressUnlock: () -> Unit,
    modifier: Modifier = Modifier
) {
    var display by remember { mutableStateOf("0") }
    var expression by remember { mutableStateOf("") }

    val masterPin = "9119"
    val duressPin = "9999"

    fun evaluateSimpleMath(expr: String): String {
        return try {
            val clean = expr.replace("×", "*").replace("÷", "/")
            val tokens = clean.split(Regex("(?<=[-+*/])|(?=[-+*/])")).map { it.trim() }
            if (tokens.isEmpty()) return "0"

            var res = tokens[0].toDoubleOrNull() ?: return "Error"
            var i = 1
            while (i < tokens.size - 1) {
                val op = tokens[i]
                val nextVal = tokens[i + 1].toDoubleOrNull() ?: return "Error"
                when (op) {
                    "+" -> res += nextVal
                    "-" -> res -= nextVal
                    "*" -> res *= nextVal
                    "/" -> if (nextVal != 0.0) res /= nextVal else return "Error"
                }
                i += 2
            }
            if (res == res.toLong().toDouble()) {
                res.toLong().toString()
            } else {
                String.format("%.4f", res).trimEnd('0').trimEnd('.')
            }
        } catch (_: Exception) {
            "Error"
        }
    }

    fun onButtonClick(btn: String) {
        when (btn) {
            "C" -> {
                display = "0"
                expression = ""
            }
            "⌫" -> {
                if (display.length > 1) {
                    display = display.dropLast(1)
                } else {
                    display = "0"
                }
            }
            "=" -> {
                // check secret triggers
                if (display == masterPin || expression == masterPin) {
                    onMasterUnlock()
                    return
                }
                if (display == duressPin || expression == duressPin) {
                    onDuressUnlock()
                    return
                }

                // standard calculation
                val fullExpr = if (expression.isNotEmpty()) expression + display else display
                val result = evaluateSimpleMath(fullExpr)
                display = result
                expression = ""
            }
            "+", "-", "×", "÷" -> {
                expression = "$display $btn "
                display = "0"
            }
            "." -> {
                if (!display.contains(".")) {
                    display += "."
                }
            }
            "+/-" -> {
                display = if (display.startsWith("-")) {
                    display.substring(1)
                } else if (display != "0") {
                    "-$display"
                } else {
                    "0"
                }
            }
            else -> {
                display = if (display == "0") btn else display + btn
            }
        }
    }

    val buttons = listOf(
        listOf("C", "+/-", "⌫", "÷"),
        listOf("7", "8", "9", "×"),
        listOf("4", "5", "6", "-"),
        listOf("1", "2", "3", "+"),
        listOf("0", ".", "=", "")
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF171717))
            .padding(16.dp),
        verticalArrangement = Arrangement.Bottom
    ) {
        // Calculation history
        Text(
            text = expression,
            color = Color(0xFFA3A3A3),
            fontSize = 20.sp,
            textAlign = TextAlign.End,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Main Display
        Text(
            text = display,
            color = Color.White,
            fontSize = if (display.length > 8) 36.sp else 52.sp,
            fontWeight = FontWeight.Light,
            textAlign = TextAlign.End,
            maxLines = 1,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Button Grid
        buttons.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { btn ->
                    if (btn.isEmpty()) {
                        Spacer(modifier = Modifier.weight(1f))
                    } else {
                        val isOperator = btn in listOf("÷", "×", "-", "+", "=")
                        val isFunction = btn in listOf("C", "+/-", "⌫")

                        val btnColor = when {
                            btn == "=" -> Color(0xFFF97316) // orange accent
                            isOperator -> Color(0xFFF97316)
                            isFunction -> Color(0xFF525252)
                            else -> Color(0xFF262626)
                        }

                        val textColor = Color.White

                        Box(
                            modifier = Modifier
                                .weight(if (btn == "0") 2.1f else 1f)
                                .aspectRatio(if (btn == "0") 2.1f else 1f)
                                .clip(CircleShape)
                                .background(btnColor)
                                .clickable { onButtonClick(btn) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = btn,
                                color = textColor,
                                fontSize = 24.sp,
                                fontWeight = if (isOperator || isFunction) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
