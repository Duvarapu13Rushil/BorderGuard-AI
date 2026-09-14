package com.borderguard.ai

data class ValidationResult(
    val field: String,
    val status: Status,
    val message: String
)

enum class Status {
    VALID,
    WARNING,
    INVALID
}

object DocumentValidator {

    fun validatePassport(data: DocumentData): List<ValidationResult> {

        val results = mutableListOf<ValidationResult>()

        // Document type
        if (data.documentType == "Passport") {
            results.add(
                ValidationResult(
                    "Document Type",
                    Status.VALID,
                    "Passport detected"
                )
            )
        } else {
            results.add(
                ValidationResult(
                    "Document Type",
                    Status.INVALID,
                    "Passport not recognized"
                )
            )
        }

        // Passport number
        if (data.passportNumber.isNotEmpty()) {
            results.add(
                ValidationResult(
                    "Passport Number",
                    Status.VALID,
                    "Passport number detected"
                )
            )
        } else {
            results.add(
                ValidationResult(
                    "Passport Number",
                    Status.INVALID,
                    "Passport number missing"
                )
            )
        }

        // Name
        if (data.surname.isNotEmpty() && data.givenNames.isNotEmpty()) {
            results.add(
                ValidationResult(
                    "Name",
                    Status.VALID,
                    "${data.givenNames} ${data.surname}"
                )
            )
        } else {
            results.add(
                ValidationResult(
                    "Name",
                    Status.INVALID,
                    "Name information missing"
                )
            )
        }

        // Nationality
        if (data.nationality.isNotEmpty()) {
            results.add(
                ValidationResult(
                    "Nationality",
                    Status.VALID,
                    data.nationality
                )
            )
        } else {
            results.add(
                ValidationResult(
                    "Nationality",
                    Status.INVALID,
                    "Nationality missing"
                )
            )
        }

        // Date of birth
        if (data.dateOfBirth.isNotEmpty()) {
            results.add(
                ValidationResult(
                    "Date of Birth",
                    Status.VALID,
                    data.dateOfBirth
                )
            )
        } else {
            results.add(
                ValidationResult(
                    "Date of Birth",
                    Status.INVALID,
                    "Date of birth missing"
                )
            )
        }

        // Sex
        if (data.sex.isNotEmpty() && data.sex != "Unknown") {
            results.add(
                ValidationResult(
                    "Sex",
                    Status.VALID,
                    data.sex
                )
            )
        } else {
            results.add(
                ValidationResult(
                    "Sex",
                    Status.WARNING,
                    "Sex could not be confidently determined"
                )
            )
        }

        // Expiry date
        if (data.expiryDate.isNotEmpty()) {

            results.add(
                ValidationResult(
                    "Expiry Date",
                    Status.VALID,
                    data.expiryDate
                )
            )

        } else {

            results.add(
                ValidationResult(
                    "Expiry Date",
                    Status.INVALID,
                    "Expiry date missing"
                )
            )
        }

        // MRZ
        if (data.rawMrz.isNotEmpty()) {
            results.add(
                ValidationResult(
                    "MRZ",
                    Status.VALID,
                    "Machine Readable Zone detected"
                )
            )
        } else {
            results.add(
                ValidationResult(
                    "MRZ",
                    Status.WARNING,
                    "MRZ not detected"
                )
            )
        }

        return results
    }
}