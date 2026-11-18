package com.example.proyecto_android_studio_isw28

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.proyecto_android_studio_isw28.R

class MainActivity : AppCompatActivity() {

    private lateinit var tvExpression: TextView
    private lateinit var tvResult: TextView

    private val voiceResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = results?.get(0) ?: ""

            if (spokenText.isNotEmpty()) {
                val mathExpression = translateVoiceToMath(spokenText)
                tvExpression.text = mathExpression
                calculateResult() // Calcula automáticamente al terminar de hablar
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvExpression = findViewById(R.id.tvExpression)
        tvResult = findViewById(R.id.tvResult)

        val buttons = listOf(
            R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
            R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9,
            R.id.btnDot, R.id.btnAdd, R.id.btnSub, R.id.btnMul, R.id.btnDiv,
            R.id.btnOpen, R.id.btnClose
        )

        for (id in buttons) {
            findViewById<Button>(id).setOnClickListener {
                val buttonText = (it as Button).text.toString()
                appendOnExpression(buttonText)
            }
        }

        val trigButtons = listOf(R.id.btnSin, R.id.btnCos, R.id.btnTan)
        for (id in trigButtons) {
            findViewById<Button>(id).setOnClickListener {
                val buttonText = (it as Button).text.toString()
                appendOnExpression("$buttonText(")
            }
        }

        // Funciones de limpieza
        findViewById<Button>(R.id.btnC).setOnClickListener {
            tvExpression.text = ""
            tvResult.text = ""
        }

        findViewById<Button>(R.id.btnDel).setOnClickListener {
            val string = tvExpression.text.toString()
            if (string.isNotEmpty()) {
                tvExpression.text = string.substring(0, string.length - 1)
            }
        }

        findViewById<Button>(R.id.btnEquals).setOnClickListener {
            calculateResult()
        }

        // 2. Acción al presionar el botón del micrófono
        findViewById<Button>(R.id.btnMic).setOnClickListener {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-MX")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Dicta tu operación (ej. seno de 45 más 2)")
            }
            try {
                voiceResultLauncher.launch(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "El micrófono no está disponible en este dispositivo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun appendOnExpression(string: String) {
        tvResult.text = ""
        tvExpression.append(string)
    }

    private fun calculateResult() {
        try {
            val expression = tvExpression.text.toString()
            if (expression.isEmpty()) return

            val result = evaluate(expression)

            val resultStr = if (result % 1 == 0.0) {
                result.toLong().toString()
            } else {
                result.toString()
            }

            tvResult.text = resultStr
        } catch (e: Exception) {
            tvResult.text = "Error"
        }
    }

    private fun translateVoiceToMath(spokenText: String): String {
        var mathExpression = spokenText.lowercase()

        mathExpression = mathExpression
            .replace("seno de", "sen") // "seno de 45" = "sen 45"
            .replace("seno", "sen")
            .replace("coseno de", "cos")
            .replace("coseno", "cos")
            .replace("tangente de", "tan")
            .replace("tangente", "tan")
            .replace("más", "+")
            .replace("mas", "+")
            .replace("menos", "-")
            .replace("por", "*")
            .replace("x", "*")
            .replace("multiplicado por", "*")
            .replace("entre", "/")
            .replace("dividido entre", "/")
            .replace("punto", ".")
            .replace("coma", ".")
            .replace("abrir paréntesis", "(")
            .replace("cerrar paréntesis", ")")

        return mathExpression.replace(" ", "")
    }


    private fun evaluate(str: String): Double {
        return object : Any() {
            var pos = -1
            var ch = 0

            fun nextChar() {
                ch = if (++pos < str.length) str[pos].toInt() else -1
            }

            fun eat(charToEat: Int): Boolean {
                while (ch == ' '.toInt()) nextChar()
                if (ch == charToEat) {
                    nextChar()
                    return true
                }
                return false
            }

            fun parse(): Double {
                nextChar()
                val x = parseExpression()
                if (pos < str.length) throw RuntimeException("Unexpected: " + ch.toChar())
                return x
            }

            fun parseExpression(): Double {
                var x = parseTerm()
                while (true) {
                    if (eat('+'.toInt())) x += parseTerm()
                    else if (eat('-'.toInt())) x -= parseTerm()
                    else return x
                }
            }

            fun parseTerm(): Double {
                var x = parseFactor()
                while (true) {
                    if (eat('*'.toInt())) x *= parseFactor()
                    else if (eat('/'.toInt())) x /= parseFactor()
                    else return x
                }
            }

            fun parseFactor(): Double {
                if (eat('+'.toInt())) return parseFactor()
                if (eat('-'.toInt())) return -parseFactor()

                var x: Double
                val startPos = pos

                if (eat('('.toInt())) {
                    x = parseExpression()
                    eat(')'.toInt())
                } else if (ch >= '0'.toInt() && ch <= '9'.toInt() || ch == '.'.toInt()) {
                    while (ch >= '0'.toInt() && ch <= '9'.toInt() || ch == '.'.toInt()) nextChar()
                    x = str.substring(startPos, pos).toDouble()
                } else if (ch >= 'a'.toInt() && ch <= 'z'.toInt()) {
                    while (ch >= 'a'.toInt() && ch <= 'z'.toInt()) nextChar()
                    val func = str.substring(startPos, pos)
                    x = parseFactor()
                    x = when (func) {
                        "sen", "sin" -> Math.sin(Math.toRadians(x))
                        "cos" -> Math.cos(Math.toRadians(x))
                        "tan" -> Math.tan(Math.toRadians(x))
                        else -> throw RuntimeException("Unknown function: $func")
                    }
                } else {
                    throw RuntimeException("Unexpected: " + ch.toChar())
                }
                return x
            }
        }.parse()
    }
}