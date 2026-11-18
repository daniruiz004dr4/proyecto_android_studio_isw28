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
                calculateResult()
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

        findViewById<Button>(R.id.btnMic).setOnClickListener {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-MX")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Dicta tu operación (ej. seno de 45 y todo eso por 2)")
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
            var expression = tvExpression.text.toString()
            if (expression.isEmpty()) return

            expression = expression.replace(")(", ")*(")
            expression = expression.replace(Regex("(\\d)\\("), "$1*(")
            expression = expression.replace(Regex("\\)(\\d)"), ")*$1")

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
        var text = spokenText.lowercase()

        val numbersMap = mapOf(
            "cero" to "0", "uno" to "1", "dos" to "2", "tres" to "3",
            "cuatro" to "4", "cinco" to "5", "seis" to "6", "siete" to "7",
            "ocho" to "8", "nueve" to "9", "diez" to "10"
        )
        for ((word, digit) in numbersMap) {
            text = text.replace(word, digit)
        }

        text = text.replace("seno de", "sen")
        text = text.replace("seno", "sen")
        text = text.replace("coseno de", "cos")
        text = text.replace("coseno", "cos")
        text = text.replace("cosen", "cos")
        text = text.replace("tangente de", "tan")
        text = text.replace("tangente", "tan")

        text = text.replace("más", "+")
        text = text.replace("mas", "+")
        text = text.replace("menos", "-")

        text = text.replace("multiplicado por", "*")
        text = text.replace("multiplicado", "*")
        text = text.replace("por", "*")
        text = text.replace("x", "*")

        text = text.replace("dividido entre", "/")
        text = text.replace("entre", "/")

        text = text.replace("punto", ".")
        text = text.replace("coma", ".")

        text = text.replace("y todo eso", "#")
        text = text.replace("todo eso", "#")
        text = text.replace("y eso", "#")
        text = text.replace("eso", "#")

        val groupCount = text.count { it == '#' }
        text = text.replace("#", ")")

        val openParens = "(".repeat(groupCount)
        text = openParens + text


        text = text.replace(" ", "")
        text = text.replace("**", "*")
        text = text.replace("//", "/")

        return text
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