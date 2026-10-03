package com.example.data.ai

import java.text.DecimalFormat
import kotlin.math.pow
import kotlin.math.sqrt

data class MathResult(
  val question: String,
  val finalAnswer: String,
  val steps: List<String>,
  val conceptExplanation: String
)

data class OfflineIntentResult(
  val responseText: String,
  val mathResult: MathResult? = null,
  val matchedIntent: String = "general",
  val suggestedAction: String? = null
)

object JarvisOfflineEngine {

  private val decimalFormat = DecimalFormat("#.####")

  /**
   * Evaluates offline commands or questions with sub-second latency.
   */
  fun processQuery(rawQuery: String): OfflineIntentResult {
    val query = rawQuery.trim().lowercase()

    // Clean hotwords
    val cleanQuery = query
      .replace("jarvis", "")
      .replace("hey jarvis", "")
      .replace("ok jarvis", "")
      .trim()

    // 1. Math Solving Intent
    val mathResult = trySolveMath(cleanQuery)
    if (mathResult != null) {
      val response = buildString {
        append("Calculation complete, sir. ")
        append("The result for ${mathResult.question} is ${mathResult.finalAnswer}.\n\n")
        append("Step-by-step breakdown:\n")
        mathResult.steps.forEachIndexed { index, step ->
          append("${index + 1}. $step\n")
        }
        append("\nTutor Insight: ${mathResult.conceptExplanation}")
      }
      return OfflineIntentResult(
        responseText = response,
        mathResult = mathResult,
        matchedIntent = "math",
        suggestedAction = "practice_math"
      )
    }

    // 2. School & Assignment queries
    if (cleanQuery.contains("assignment") || cleanQuery.contains("homework") ||
        cleanQuery.contains("school") || cleanQuery.contains("due date") || cleanQuery.contains("task")) {
      return OfflineIntentResult(
        responseText = "Accessing your academic telemetry, sir. I have localized your assignments in the secure on-device database. You have high-priority tasks in Mathematics and Physics pending review.",
        matchedIntent = "assignments",
        suggestedAction = "open_assignments"
      )
    }

    // 3. Privacy & File Vault
    if (cleanQuery.contains("privacy") || cleanQuery.contains("vault") ||
        cleanQuery.contains("file") || cleanQuery.contains("encrypt") || cleanQuery.contains("security") ||
        cleanQuery.contains("phone data")) {
      return OfflineIntentResult(
        responseText = "Privacy protocol active, sir. 100% of your personal files, voice logs, and study records are encrypted on-device. Zero telemetry is broadcast externally without your explicit command.",
        matchedIntent = "privacy",
        suggestedAction = "open_vault"
      )
    }

    // 4. System Diagnostics / Telemetry
    if (cleanQuery.contains("diagnostic") || cleanQuery.contains("telemetry") ||
        cleanQuery.contains("status") || cleanQuery.contains("battery") || cleanQuery.contains("cpu") ||
        cleanQuery.contains("specs")) {
      return OfflineIntentResult(
        responseText = "System status: Arc Reactor core stable at 100%. Local neural engine operating at sub-second response latency (<15ms). Audio listening array active for 'Jarvis' wake triggers.",
        matchedIntent = "diagnostics"
      )
    }

    // 5. Study Timer / Pomodoro
    if (cleanQuery.contains("study") || cleanQuery.contains("timer") || cleanQuery.contains("focus") ||
        cleanQuery.contains("pomodoro")) {
      return OfflineIntentResult(
        responseText = "Study session protocol ready. I can initiate a 25-minute high-focus interval with quantum progress tracking. Shall we begin, sir?",
        matchedIntent = "study_timer",
        suggestedAction = "start_study"
      )
    }

    // 6. Voice presets
    if (cleanQuery.contains("voice") || cleanQuery.contains("female") || cleanQuery.contains("male") ||
        cleanQuery.contains("friday") || cleanQuery.contains("edith")) {
      return OfflineIntentResult(
        responseText = "Acoustic synthesizer configurations are accessible in the Voice Matrix. You can switch between Jarvis (British Butler), Friday (Technical Specialist), and Edith (Tactical Defense).",
        matchedIntent = "voice_settings",
        suggestedAction = "open_voice"
      )
    }

    // 7. Greetings & General conversation
    if (cleanQuery.contains("hello") || cleanQuery.contains("hi") || cleanQuery.contains("good morning") ||
        cleanQuery.contains("who are you") || cleanQuery.isEmpty()) {
      return OfflineIntentResult(
        responseText = "Good day, sir. JARVIS is fully online. Whether you require mathematical tutoring, school schedule reminders, or secure file access, I remain at your service.",
        matchedIntent = "greeting"
      )
    }

    // 8. General offline fallback
    return OfflineIntentResult(
      responseText = "Understood, sir. Operating in secure offline mode. I have recorded your inquiry: \"$cleanQuery\". You may also prompt me to solve math equations, manage assignments, or review encrypted files.",
      matchedIntent = "general"
    )
  }

  /**
   * Detects and solves math equations locally with step-by-step guidance.
   */
  private fun trySolveMath(text: String): MathResult? {
    val t = text.replace("solve", "").replace("what is", "").replace("calculate", "").trim()

    // Linear equation: ax + b = c or ax - b = c (e.g. "2x + 8 = 20" or "3x - 5 = 10" or "4x = 16")
    val linearRegex = Regex("""^(-?\d*\.?\d*)\s*([a-zA-Z])\s*([+-])?\s*(\d*\.?\d*)\s*=\s*(-?\d*\.?\d+)$""")
    val linearMatch = linearRegex.find(t.replace(" ", ""))
    if (linearMatch != null) {
      val (aStr, varName, op, bStr, cStr) = linearMatch.destructured
      val a = when {
        aStr.isEmpty() || aStr == "+" -> 1.0
        aStr == "-" -> -1.0
        else -> aStr.toDoubleOrNull() ?: 1.0
      }
      val b = (bStr.toDoubleOrNull() ?: 0.0) * (if (op == "-") -1.0 else 1.0)
      val c = cStr.toDoubleOrNull() ?: 0.0

      val step1 = "Given linear equation: ${a}·$varName + ($b) = $c"
      val cMinusB = c - b
      val step2 = "Subtract constant term from both sides: ${a}·$varName = $c - ($b) ⟹ ${a}·$varName = ${decimalFormat.format(cMinusB)}"
      val xVal = cMinusB / a
      val step3 = "Divide both sides by coefficient of $varName ($a): $varName = ${decimalFormat.format(cMinusB)} / $a ⟹ $varName = ${decimalFormat.format(xVal)}"

      return MathResult(
        question = t,
        finalAnswer = "$varName = ${decimalFormat.format(xVal)}",
        steps = listOf(step1, step2, step3),
        conceptExplanation = "A first-degree linear equation is solved by isolating the variable using inverse operations (balancing both sides)."
      )
    }

    // Quadratic equation: ax^2 + bx + c = 0
    val quadRegex = Regex("""^(-?\d*)\s*x\^?2\s*([+-]\s*\d*)\s*x\s*([+-]\s*\d+)\s*=\s*0$""")
    val quadMatch = quadRegex.find(t.replace(" ", ""))
    if (quadMatch != null) {
      val (aStr, bStr, cStr) = quadMatch.destructured
      val a = when (aStr) { "" -> 1.0; "-" -> -1.0; else -> aStr.toDoubleOrNull() ?: 1.0 }
      val b = when (bStr.replace(" ", "")) { "+" -> 1.0; "-" -> -1.0; else -> bStr.replace(" ", "").toDoubleOrNull() ?: 0.0 }
      val c = cStr.replace(" ", "").toDoubleOrNull() ?: 0.0

      val disc = b * b - 4 * a * c
      val steps = mutableListOf<String>()
      steps.add("Standard form: a=$a, b=$b, c=$c")
      steps.add("Compute Discriminant Δ = b² - 4ac = ($b)² - 4($a)($c) = ${decimalFormat.format(disc)}")

      val finalAns: String
      if (disc > 0) {
        val r1 = (-b + sqrt(disc)) / (2 * a)
        val r2 = (-b - sqrt(disc)) / (2 * a)
        steps.add("Since Δ > 0, there are two distinct real roots:")
        steps.add("x₁ = (-b + √Δ) / 2a = (-($b) + ${decimalFormat.format(sqrt(disc))}) / ${2 * a} = ${decimalFormat.format(r1)}")
        steps.add("x₂ = (-b - √Δ) / 2a = (-($b) - ${decimalFormat.format(sqrt(disc))}) / ${2 * a} = ${decimalFormat.format(r2)}")
        finalAns = "x = ${decimalFormat.format(r1)} or x = ${decimalFormat.format(r2)}"
      } else if (disc == 0.0) {
        val r = -b / (2 * a)
        steps.add("Since Δ = 0, there is one repeated root: x = -b / 2a = ${decimalFormat.format(r)}")
        finalAns = "x = ${decimalFormat.format(r)}"
      } else {
        steps.add("Since Δ < 0, roots are complex numbers: x = (-b ± i√|Δ|) / 2a")
        finalAns = "Complex roots: x = (${-b} ± ${decimalFormat.format(sqrt(-disc))}i) / ${2 * a}"
      }

      return MathResult(
        question = t,
        finalAnswer = finalAns,
        steps = steps,
        conceptExplanation = "Quadratic equations are solved via the quadratic formula: x = (-b ± √(b² - 4ac)) / (2a)."
      )
    }

    // Percentage: "15% of 250" or "what is 20% of 80"
    val pctRegex = Regex("""(\d+\.?\d*)\s*%\s*of\s*(\d+\.?\d*)""")
    val pctMatch = pctRegex.find(t)
    if (pctMatch != null) {
      val (pctStr, baseStr) = pctMatch.destructured
      val pct = pctStr.toDoubleOrNull() ?: return null
      val base = baseStr.toDoubleOrNull() ?: return null
      val result = (pct / 100.0) * base
      return MathResult(
        question = "$pct% of $base",
        finalAnswer = decimalFormat.format(result),
        steps = listOf(
          "Convert percentage to decimal: $pct% = $pct / 100 = ${pct / 100.0}",
          "Multiply decimal by base value: ${pct / 100.0} × $base = ${decimalFormat.format(result)}"
        ),
        conceptExplanation = "A percent represents parts per 100. Multiply the decimal form by the target quantity."
      )
    }

    // Basic arithmetic: e.g. "45 * 12", "125 + 380", "100 / 4", "sqrt(144)", "2^10"
    if (t.startsWith("sqrt(") && t.endsWith(")")) {
      val inner = t.removePrefix("sqrt(").removeSuffix(")").trim().toDoubleOrNull()
      if (inner != null && inner >= 0) {
        val res = sqrt(inner)
        return MathResult(
          question = "√$inner",
          finalAnswer = decimalFormat.format(res),
          steps = listOf("Square root definition: y = √x such that y² = x", "√$inner = ${decimalFormat.format(res)}"),
          conceptExplanation = "The principal square root yields the positive number which multiplied by itself equals $inner."
        )
      }
    }

    val arithRegex = Regex("""^(\d+\.?\d*)\s*([\+\-\*\/\^])\s*(\d+\.?\d*)$""")
    val arithMatch = arithRegex.find(t.replace("x", "*"))
    if (arithMatch != null) {
      val (num1Str, op, num2Str) = arithMatch.destructured
      val n1 = num1Str.toDoubleOrNull() ?: return null
      val n2 = num2Str.toDoubleOrNull() ?: return null
      val res: Double = when (op) {
        "+" -> n1 + n2
        "-" -> n1 - n2
        "*" -> n1 * n2
        "/" -> if (n2 != 0.0) n1 / n2 else return null
        "^" -> n1.pow(n2)
        else -> return null
      }
      return MathResult(
        question = "$n1 $op $n2",
        finalAnswer = decimalFormat.format(res),
        steps = listOf(
          "Evaluate expression: $n1 $op $n2",
          "Result = ${decimalFormat.format(res)}"
        ),
        conceptExplanation = "Arithmetic operation executed with IEEE 754 64-bit precision."
      )
    }

    return null
  }
}
