package com.example.calculator

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.calculator.databinding.ActivityMainBinding
import java.text.DecimalFormat

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var expression = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState != null) {
            expression = savedInstanceState.getString(KEY_EXPRESSION, "")
            binding.tvExpression.text = expression
            binding.tvResult.text = savedInstanceState.getString(KEY_RESULT, "")
        }

        val digitButtons = listOf(
            binding.btn0, binding.btn1, binding.btn2, binding.btn3, binding.btn4,
            binding.btn5, binding.btn6, binding.btn7, binding.btn8, binding.btn9
        )
        digitButtons.forEachIndexed { index, button ->
            button.setOnClickListener { append(index.toString()) }
        }

        binding.btnDot.setOnClickListener { append(".") }
        binding.btnPlus.setOnClickListener { appendOperator("+") }
        binding.btnMinus.setOnClickListener { appendOperator("-") }
        binding.btnMultiply.setOnClickListener { appendOperator("×") }
        binding.btnDivide.setOnClickListener { appendOperator("÷") }
        binding.btnPercent.setOnClickListener { onPercent() }
        binding.btnClear.setOnClickListener { clearAll() }
        binding.btnBackspace.setOnClickListener { backspace() }
        binding.btnEquals.setOnClickListener { onEquals() }
    }

    private fun append(token: String) {
        if (binding.tvResult.text.isNotEmpty() && binding.tvResult.text != getString(R.string.error)) {
            // Start a fresh calculation after a previous result
            expression = ""
            binding.tvResult.text = ""
        }
        expression += token
        binding.tvExpression.text = expression
    }

    private fun appendOperator(op: String) {
        binding.tvResult.text = ""
        if (expression.isEmpty()) {
            if (op == "-") {
                expression = op
                binding.tvExpression.text = expression
            }
            return
        }
        val last = expression.last()
        if (last in OPERATORS) {
            expression = expression.dropLast(1) + op
        } else {
            expression += op
        }
        binding.tvExpression.text = expression
    }

    private fun onPercent() {
        if (expression.isEmpty()) return
        val last = expression.last()
        if (last.isDigit() || last == '.') {
            expression += "%"
            binding.tvExpression.text = expression
        }
    }

    private fun backspace() {
        if (expression.isNotEmpty()) {
            expression = expression.dropLast(1)
            binding.tvExpression.text = expression.ifEmpty { "0" }
            if (expression.isEmpty()) binding.tvExpression.text = ""
        }
    }

    private fun clearAll() {
        expression = ""
        binding.tvExpression.text = ""
        binding.tvResult.text = ""
    }

    private fun onEquals() {
        if (expression.isEmpty()) return
        try {
            val value = ExpressionParser.evaluate(expression)
            val formatted = FORMATTER.format(value)
            binding.tvResult.text = formatted
        } catch (e: Exception) {
            binding.tvResult.text = getString(R.string.error)
            Toast.makeText(this, R.string.invalid_expression, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_EXPRESSION, expression)
        outState.putString(KEY_RESULT, binding.tvResult.text.toString())
    }

    companion object {
        private val OPERATORS = setOf('+', '-', '×', '÷')
        private val FORMATTER = DecimalFormat("#.##########")
        private const val KEY_EXPRESSION = "expression"
        private const val KEY_RESULT = "result"
    }
}

/** Tiny recursive-descent parser for + - × ÷ and % (percent of the preceding number). */
object ExpressionParser {

    fun evaluate(input: String): Double {
        val tokens = tokenize(input)
        var pos = 0

        fun parsePrimary(): Double {
            var sign = 1.0
            if (pos < tokens.size && tokens[pos] == "-") {
                sign = -1.0
                pos++
            }
            val num = tokens.getOrNull(pos)?.toDoubleOrNull()
                ?: throw IllegalArgumentException("Expected number")
            pos++
            var value = num
            if (pos < tokens.size && tokens[pos] == "%") {
                value /= 100.0
                pos++
            }
            return sign * value
        }

        fun parseTerm(): Double {
            var result = parsePrimary()
            while (pos < tokens.size && (tokens[pos] == "×" || tokens[pos] == "÷")) {
                val op = tokens[pos]
                pos++
                val rhs = parsePrimary()
                result = if (op == "×") result * rhs else {
                    if (rhs == 0.0) throw ArithmeticException("Division by zero")
                    result / rhs
                }
            }
            return result
        }

        fun parseExpression(): Double {
            var result = parseTerm()
            while (pos < tokens.size && (tokens[pos] == "+" || tokens[pos] == "-")) {
                val op = tokens[pos]
                pos++
                val rhs = parseTerm()
                result = if (op == "+") result + rhs else result - rhs
            }
            return result
        }

        val value = parseExpression()
        if (pos != tokens.size) throw IllegalArgumentException("Unexpected token")
        return value
    }

    private fun tokenize(input: String): List<String> {
        val tokens = mutableListOf<String>()
        var i = 0
        while (i < input.length) {
            val c = input[i]
            when {
                c.isDigit() || c == '.' -> {
                    val start = i
                    while (i < input.length && (input[i].isDigit() || input[i] == '.')) i++
                    tokens.add(input.substring(start, i))
                }
                c in "+-×÷%" -> {
                    tokens.add(c.toString())
                    i++
                }
                else -> throw IllegalArgumentException("Invalid character: $c")
            }
        }
        return tokens
    }
}
