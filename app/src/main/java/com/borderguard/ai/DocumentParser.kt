package com.borderguard.ai

data class DocumentData(
    val documentType: String = "Unknown",
    val passportNumber: String = "",
    val surname: String = "",
    val givenNames: String = "",
    val nationality: String = "",
    val dateOfBirth: String = "",
    val sex: String = "",
    val expiryDate: String = "",
    val rawMrz: String = ""
)

object DocumentParser {

    fun parsePassport(ocrText: String): DocumentData {

        val lines = ocrText
            .lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        // Find the MRZ lines.
        val mrzLines = lines.filter {
            it.contains("<") && it.length >= 20
        }

        var passportNumber = ""
        var nationality = ""
        var dateOfBirth = ""
        var sex = ""
        var expiryDate = ""
        var surname = ""
        var givenNames = ""

        if (mrzLines.size >= 2) {

            val mrz1 = mrzLines[mrzLines.size - 2]
                .replace(" ", "")

            val mrz2 = mrzLines[mrzLines.size - 1]
                .replace(" ", "")

            // -----------------------------
            // MRZ LINE 1
            // P<LVASURNAME<<GIVEN<NAMES
            // -----------------------------

            if (mrz1.startsWith("P")) {

                val namePart = mrz1
                    .substringAfter("<<", "")

                val nameSection = mrz1
                    .substringAfter("P<LVA", "")

                val beforeNames = nameSection
                    .substringBefore("<<")

                surname = beforeNames
                    .replace("<", " ")
                    .trim()

                givenNames = namePart
                    .replace("<", " ")
                    .trim()
            }

            // -----------------------------
            // MRZ LINE 2
            // Passport number
            // Nationality
            // DOB
            // Sex
            // Expiry
            // -----------------------------

            if (mrz2.length >= 27) {

                passportNumber = mrz2
                    .substring(0, 9)
                    .replace("<", "")
                    .trim()

// The 9th character is the MRZ check digit.
// The printed passport number itself is normally 8 characters here.
                if (passportNumber.length == 9 && passportNumber.last().isDigit()) {
                    passportNumber = passportNumber.substring(0, 8)
                }

                nationality = mrz2
                    .substring(10, 13)
                    .replace("<", "")
                    .trim()

                val dobRaw = mrz2.substring(13, 19)

                dateOfBirth = formatMrzDate(dobRaw)

                sex = when (mrz2[20]) {
                    'M' -> "Male"
                    'F' -> "Female"
                    else -> "Unknown"
                }

                val expiryRaw = mrz2.substring(21, 27)

                expiryDate = formatMrzDate(expiryRaw)
            }
        }

        // Fallback: try to find passport number directly from OCR.
        if (passportNumber.isEmpty()) {
            val passportPattern = Regex(
                """[A-Z]{2}\d{7}"""
            )

            passportNumber = passportPattern
                .find(ocrText.replace(" ", ""))
                ?.value
                ?: ""
        }

        return DocumentData(
            documentType = if (
                ocrText.contains("PASSPORT", ignoreCase = true) ||
                ocrText.contains("PASSEPORT", ignoreCase = true)
            ) {
                "Passport"
            } else {
                "Unknown"
            },
            passportNumber = passportNumber,
            surname = surname,
            givenNames = givenNames,
            nationality = nationality,
            dateOfBirth = dateOfBirth,
            sex = sex,
            expiryDate = expiryDate,
            rawMrz = mrzLines.joinToString("\n")
        )
    }

    private fun formatMrzDate(value: String): String {

        if (value.length != 6) {
            return ""
        }

        val year = value.substring(0, 2)
        val month = value.substring(2, 4)
        val day = value.substring(4, 6)

        return "$day/$month/$year"
    }
}