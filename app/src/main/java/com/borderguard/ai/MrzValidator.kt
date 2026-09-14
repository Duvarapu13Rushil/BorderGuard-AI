package com.borderguard.ai

data class MrzValidationResult(
    val field: String,
    val isValid: Boolean,
    val message: String
)

object MrzValidator {

    fun validatePassportMrz(rawMrz: String): List<MrzValidationResult> {

        // Remove spaces and keep only MRZ characters
        val lines = rawMrz
            .lines()
            .map {
                it.uppercase()
                    .replace(" ", "")
                    .filter { char ->
                        char.isLetterOrDigit() || char == '<'
                    }
            }
            .filter { it.isNotEmpty() }

        if (lines.size < 2) {
            return listOf(
                MrzValidationResult(
                    field = "MRZ",
                    isValid = false,
                    message = "Two MRZ lines required"
                )
            )
        }

        val line2 = lines[lines.size - 1]

        if (line2.length < 44) {
            return listOf(
                MrzValidationResult(
                    field = "MRZ",
                    isValid = false,
                    message = "Second MRZ line is incomplete"
                )
            )
        }

        val results = mutableListOf<MrzValidationResult>()

        // ------------------------------------------------------------
        // Passport Number Check Digit
        // Positions 1-9 = passport number
        // Position 10 = check digit
        // ------------------------------------------------------------

        val passportNumber = line2.substring(0, 9)
        val passportCheckDigit = line2[9]

        results.add(
            checkField(
                field = "Passport Number",
                value = passportNumber,
                checkDigit = passportCheckDigit
            )
        )

        // ------------------------------------------------------------
        // Date of Birth Check Digit
        // Positions 14-19 = YYMMDD
        // Position 20 = check digit
        // ------------------------------------------------------------

        val dateOfBirth = line2.substring(13, 19)
        val dobCheckDigit = line2[19]

        results.add(
            checkField(
                field = "Date of Birth",
                value = dateOfBirth,
                checkDigit = dobCheckDigit
            )
        )

        // ------------------------------------------------------------
        // Expiry Date Check Digit
        // Positions 22-27 = YYMMDD
        // Position 28 = check digit
        // ------------------------------------------------------------

        val expiryDate = line2.substring(21, 27)
        val expiryCheckDigit = line2[27]

        results.add(
            checkField(
                field = "Expiry Date",
                value = expiryDate,
                checkDigit = expiryCheckDigit
            )
        )

        // ------------------------------------------------------------
        // Composite Check Digit
        // Position 44
        //
        // Uses:
        // passport number + check digit
        // date of birth + check digit
        // expiry date + check digit
        // optional data
        // ------------------------------------------------------------

        if (line2.length >= 44) {

            val compositeData =
                line2.substring(0, 10) +
                        line2.substring(13, 20) +
                        line2.substring(21, 43)

            val compositeCheckDigit = line2[43]

            results.add(
                checkField(
                    field = "MRZ Composite",
                    value = compositeData,
                    checkDigit = compositeCheckDigit
                )
            )
        }

        return results
    }

    // ================================================================
    // MRZ CHECK-DIGIT ALGORITHM
    // ================================================================

    private fun checkField(
        field: String,
        value: String,
        checkDigit: Char
    ): MrzValidationResult {

        if (!checkDigit.isDigit()) {
            return MrzValidationResult(
                field = field,
                isValid = false,
                message = "Invalid check digit"
            )
        }

        val expected =
            calculateCheckDigit(value)

        val actual =
            checkDigit.digitToInt()

        return if (expected == actual) {

            MrzValidationResult(
                field = field,
                isValid = true,
                message = "Check digit valid"
            )

        } else {

            MrzValidationResult(
                field = field,
                isValid = false,
                message = "Check digit mismatch"
            )
        }
    }

    private fun calculateCheckDigit(value: String): Int {

        val weights = intArrayOf(7, 3, 1)

        var total = 0

        value.forEachIndexed { index, char ->

            val characterValue =
                when {

                    char in '0'..'9' ->
                        char.digitToInt()

                    char in 'A'..'Z' ->
                        char.code - 'A'.code + 10

                    char == '<' ->
                        0

                    else ->
                        0
                }

            total +=
                characterValue * weights[index % 3]
        }

        return total % 10
    }
}