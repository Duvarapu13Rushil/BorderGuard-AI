package com.borderguard.ai

data class RiskAssessment(
    val score: Int,
    val level: String,
    val recommendation: String
)

object RiskAssessor {

    fun calculateRisk(
        forgeryProbability: Float,
        validationResults: List<ValidationResult>,
        faceMatch: Boolean? = null
    ): RiskAssessment {

        /*
         * AI forgery risk
         *
         * Model output:
         * 1.0 = very likely REAL
         * 0.0 = very likely FAKE
         *
         * Therefore:
         * AI risk = (1 - real probability) * 100
         */

        var riskScore =
            ((1f - forgeryProbability) * 100f).toInt()


        /*
         * Document validation
         */

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


        /*
         * Face verification
         *
         * MATCH:
         * No additional risk.
         *
         * MISMATCH:
         * Significant identity risk.
         *
         * null:
         * Face verification has not been performed yet.
         */

        when (faceMatch) {

            true -> {
                // Identity matches document owner.
            }

            false -> {
                riskScore += 25
            }

            null -> {
                // Do nothing.
            }
        }


        /*
         * Keep score between 0 and 100.
         */

        riskScore =
            riskScore.coerceIn(0, 100)


        /*
         * Determine risk level.
         */

        val level: String
        val recommendation: String

        when {

            riskScore >= 60 -> {

                level = "HIGH RISK"

                recommendation =
                    "SECONDARY INSPECTION"
            }

            riskScore >= 30 -> {

                level = "MEDIUM RISK"

                recommendation =
                    "MANUAL REVIEW"
            }

            else -> {

                level = "LOW RISK"

                recommendation =
                    "NORMAL PROCESSING"
            }
        }


        return RiskAssessment(

            score = riskScore,

            level = level,

            recommendation = recommendation
        )
    }
}