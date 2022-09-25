package calculator
import java.math.RoundingMode
import kotlin.math.pow

object Calc {
    var line = readln()
    val variables = mutableMapOf<String, String>()
    val signPriority = mapOf('~' to 0, '^' to 1, '*' to 2, '/' to 2, '+' to 3, '-' to 3)
    var skip = 0
    var newStr = ""

    init {
        this.run()
    }

    // обрабатываем многозначные +/-
    fun replaceSign(str: String): String {
        var sign = str
        while (Regex("[+-]+[+-]+").find(sign) != null) {
            sign = sign.replace("--", "+")
                .replace("++", "+")
                .replace("-+", "-")
                .replace("+-", "-")
        }
        return sign
    }

    fun getNumber(str: String, i: Int, option: String) {
        var start = i
        while (start != 0 && str[start].isDigit()) {
            start--
        }
        val number = if (str[start].isDigit()) str.slice(0..i) else str.slice(++start..i)
        skip = 1
        newStr = newStr.slice(0..(newStr.lastIndex  - number.length))
        newStr += if (option == "-") number.toInt() - 1 else number.toInt() + 1
    }

    // вычисляем ++ и --
    fun calcPostfix(str: String) {
        for (i in str.indices) {
            if (skip == 1) {
                skip = 0
            } else if (i < str.lastIndex - 1) {
                if (str[i] == '-' && str[i + 1] == '-' && (str[i + 2] == ' ' || str[i + 2] == ')') &&
                    str[i - 1].isDigit()) {
                    getNumber(str, i - 1, "-")
                } else if (str[i] == '+' && str[i + 1] == '+' && (str[i + 2] == ' ' || str[i + 2] == ')') &&
                    str[i - 1].isDigit()) {
                    getNumber(str, i - 1, "+")
                } else { newStr += str[i] }
            } else if (i == str.lastIndex - 1) {
                if (str[i] == '-' && str[i + 1] == '-' && str[i - 1].isDigit()) {
                    getNumber(str, i - 1, "-")
                } else if (str[i] == '+' && str[i + 1] == '+' && str[i - 1].isDigit()) {
                    getNumber(str, i - 1, "+")
                } else { newStr += str[i] }
            } else { newStr += str[i] }
        }
        line = newStr
        newStr = ""
    }

    fun checkAndAssignValue(name: String, value: String) {
        if (!Regex("[a-zA-Z]+").matches(name)) println("Invalid identifier")
        if (Regex("[-]?[a-zA-Z]+").matches(value)) {
            if (value in variables) {
                variables[name] = variables[value]!!
            } else {
                println("Unknown variable")
            }
        } else if (Regex("[-]?\\d+").matches(value)) {
            variables[name] = value
        } else {
            println("Invalid assignment")
        }
    }

    fun checkCorrectExpression(option: String): Boolean {
        if (Regex("[*/)^]+").find(line[0].toString()) != null) return false
        for (i in 1..line.lastIndex) {
            if (option == "var") {
                if (line[i].isLetter() && (Regex("[)\\d]+").find(line[i - 1].toString()) != null)) {
                    return false
                }
            }
            if (line[i].isDigit()) {
                if (Regex("[)a-zA-Z]+").find(line[i - 1].toString()) != null) return false
            } else if (line[i] in "*/^+-" && line[i - 1] in "*/^") {
                return false
            } else if (line[i] in "(" && Regex("[\\w]+").find(line[i - 1].toString()) != null) {
                return false
            }
        }
        return true
    }

    fun variablesToNumbers(): Boolean {
        var varName = ""
        for (i in line.indices) {
            if (line[i].isLetter()) {
                varName += line[i]
            } else if (varName != "") {
                try {
                    newStr += "${variables[varName]}${line[i]}"
                    varName = ""
                } catch (e: Exception) {
                    println("Unknown variable")
                    return false
                }
            } else {
                newStr += line[i]
            }
        }
        if (varName != "") {
            try {
                newStr += variables[varName]
            } catch (e: Exception) {
                println("Unknown variable")
                return false
            }
        }
        line = newStr
        newStr = ""
        return true
    }

    fun checkFormatAndCalc(option: String): Boolean {
        val regex = if (option == "var") Regex("[^\\-+ /*^()a-zA-Z\\d]+") else Regex("[^\\-+ /*^()\\d]+")
        if (regex.find(line) != null) {
            println("Invalid expression")
            return false
        }
        // проверяем равенство кол-ва открывающих и закрывающих скобок
        if (line.count { it == '(' } != line.count { it == ')' }) {
            println("Invalid expression")
            return false
        }
        // удаляем лишние пробелы
        line = line.replace(Regex("[ ]+"), " ")
        // вычисляем ++ и --
        calcPostfix(line)
        // избавляемся от многозначных выражений (+-+ заменяем на - и т.д.)
        line = replaceSign(line)
        if (checkCorrectExpression(option)) {
            // если есть буквы (имена переменных), то заменяем их на числа
            if (option == "var") {
                if (!variablesToNumbers()) {
                    return false
                }
            }
            // приводим к постфиксной нотации
            line = toPostfix(line)
            val return_ = сalc(line)
            println(if (return_.takeLast(3) == ".00") {
                return_.take(return_.length - 3)
            } else if (return_ == "false") {
                "Division by zero is prohibited!"
            } else {
                return_
            })
            return true
        } else {
            println("Invalid expression")
            return false
        }
    }

    fun toPostfix(infixExpr: String): String {
        var postfixExpr = ""
        val stack = mutableListOf<Char>()
        var c: Char
        var number = ""
        for (i in infixExpr.indices) {
            c = infixExpr[i]
            if (c.isDigit()) {
                if (number == "" && postfixExpr.isNotEmpty()) {
                    number += " "
                }
                number += c
            } else if (c == '(') {
                stack.add(0, c)
            } else if (c == ')') {
                postfixExpr += "$number "
                number = ""
                while (stack.size > 0 && stack[0] != '(')
                    postfixExpr += stack.removeAt(0)
                stack.removeAt(0)
            } else if (c in signPriority) {
                postfixExpr += "$number "
                number = ""
                var op = c
                if (op == '-' && (i == 0 || (i != infixExpr.lastIndex && (infixExpr[i-1] == ' ' ||
                            infixExpr[i-1] == '(') && infixExpr[i+1] in '0'..'9'))) op = '~'
                while (stack.size > 0 && stack[0] in signPriority && (signPriority[stack[0]]!! <= signPriority[op]!!))
                    postfixExpr += stack.removeAt(0)
                stack.add(0, op)
            }
        }
        if (number != "") postfixExpr += "$number "
        stack.forEach { postfixExpr += it }
        postfixExpr = postfixExpr.replace("  ", " ")
        if (postfixExpr[0] == ' ') postfixExpr = postfixExpr.slice(1..postfixExpr.lastIndex)
        return postfixExpr
    }

    fun сalc(postfixExpr: String): String {
        val locals = mutableListOf<String>()
        var number = ""
        var first: String
        var second: String
        var last: String

        for (c in postfixExpr) {
            if (c.isDigit()) {
                number += c
            } else if (c == ' ' && number != "") {
                locals.add(0, number)
                number = ""
            } else if (c in signPriority) {
                if (c == '~') {
                    last = if (locals.size > 0) locals.removeAt(0) else "0"
                    locals.add(0, execute('-', "0", last))
                    if (locals[0] == "false") return "false"
                    continue
                }
                second = if (locals.size > 0) locals.removeAt(0) else "0"
                first = if (locals.size > 0) locals.removeAt(0) else "0"

                locals.add(0, execute(c, first, second))
                if (locals[0] == "false") return "false"
            }
        }

        return locals.removeAt(locals.lastIndex)
    }

    fun execute(sign: Char, first_: String, second_: String): String {
        if (sign == '/' && second_ == "0") return "false"
        if (Regex("[\\d]{10,}") in first_ || Regex("[\\d]{10,}") in second_) {
            val first = first_.toBigDecimal()
            val second = second_.toBigDecimal()
            return when (sign) {
                '+' -> (first + second).toString()
                '-' -> (first - second).toString()
                '*' -> (first * second).toString()
                '/' -> cutZeroRemainder((first.divide(second, 2, RoundingMode.HALF_UP)).toString())
                '^' -> first.pow(second.toInt()).toString()
                else -> "0"
            }
        } else {
            val first = first_.toInt()
            val second = second_.toInt()
            return when (sign) {
                '+' -> (first + second).toString()
                '-' -> (first - second).toString()
                '*' -> (first * second).toString()
                '/' -> cutZeroRemainder((first.toFloat() / second.toFloat()).toString())
                '^' -> (first.toDouble().pow(second.toDouble())).toInt().toString()
                else -> "0"
            }
        }
    }

    // если после операции деления получилось целое число, тогда возвращаем число без запятой
    fun cutZeroRemainder(number: String): String {
        if ('.' in number && Regex("[0]+").matches(number.split(".")[1])) {
            return number.split(".")[0]
        } else {
            return number
        }
    }

    fun run() {
        while (line != "/exit") {
            try {
                newStr = ""
                if (line == "/help") {
                    println("The program calculates the sum or difference of numbers.\n" +
                            "The unary plus and minus operators are also supported.\n" +
                            "Available declaration of numeric variables, operations ^/*+- with these variables, \nas well as output of the current value of the variable.")
                } else if (Regex("\\s*").matches(line)) {
                    print("")
                } else if (line[0] == '/') {
                    println("Unknown command")
                } else if (Regex("[a-zA-Z]+") in line) {
                    if ("=" in line) {
                        val (name, value) = line.replace(Regex("\\s"), "").split("=")
                        checkAndAssignValue(name, value)
                    } else if (Regex("[a-zA-Z]+").matches(line)) {
                        if (line in variables) {
                            println(variables[line])
                        } else { println("Unknown variable") }
                    } else { checkFormatAndCalc("var") }
                } else { checkFormatAndCalc("numb") }
            } catch (e: Exception) {
                println("Invalid expression")
            }
            line = readln()
        }
        print("Bye!")
    }
}

fun main() {
    Calc
}
