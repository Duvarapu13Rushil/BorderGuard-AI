package com.borderguard.ai

data class RiskAssessment(
    val score: Int,
    val level: String,
    val recommendation: String
)

object RiskAssessor {

    fun calculateRisk(
        forgeryProbability: Float,
        validationResults: List<ValidationResult>
    ): RiskAssessment {

        // AI probability represents probability of REAL document.
        // Therefore:
        // 0.0 = fake
        // 1.0 = real

        var riskScore = ((1f - forgeryProbability) * 100f).toInt()

        // Add risk for validation problems
        validationResults.forEach { result ->

            when (result.status) {

                Status.INVALID -> {
                    riskScore += 15
                }

                Status.WARNING -> {
                    riskScore += 5
                }

                Status.VALID -> {
                    // No additional risk
                }
            }
        }

        // Keep score between 0 and 100
        riskScore = riskScore.coerceIn(0, 100)

        val level: String
        val recommendation: String

        when {

            riskScore >= 60 -> {
                level = "HIGH RISK"
                recommendation = "SECONDARY INSPECTION"
            }

            riskScore >= 30 -> {
                level = "MEDIUM RISK"
                recommendation = "MANUAL REVIEW"
            }

            else -> {
                level = "LOW RISK"
                recommendation = "NORMAL PROCESSING"
            }
        }

        return RiskAssessment(
            score = riskScore,
            level = level,
            recommendation = recommendation
        )
    }
}