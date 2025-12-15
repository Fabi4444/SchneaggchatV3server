package com.lerchenflo.schneaggchatv3server.core

import com.google.gson.stream.MalformedJsonException
import com.lerchenflo.schneaggchatv3server.util.LogType
import com.lerchenflo.schneaggchatv3server.util.LoggingService
import org.bson.types.ObjectId
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ResponseStatusException
import java.util.function.Consumer

@RestControllerAdvice
class GlobalExceptionHandler(
    private val loggingService: LoggingService
) {

    //Exception handling for annotations (For example Registerrequest: Email)
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationError(e: MethodArgumentNotValidException): ResponseEntity<Map<String, Any>> {
        println("Validation Error happened: ${e.message}")
        val errors = e.bindingResult.allErrors.map {
            it.defaultMessage ?: "Invalid value"
        }
        logError(e)
        return ResponseEntity
            .status(400)
            .body(mapOf("errors" to errors))
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(e: IllegalArgumentException): ResponseEntity<String> {
        println("Illegal argument Error happened: ${e.message}")
        val error = e.message
        logError(e)
        return ResponseEntity
            .status(400)
            .body(error)
    }

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatusException(e: ResponseStatusException): ResponseEntity<String> {
        println("ResponseStatus Error happened: ${e.message}")
        val error = e.message
        logError(e)
        return ResponseEntity
            .status(e.statusCode)
            .body(error)
    }


    private fun logError(e : Exception) {
        val requestingUserId =
            SecurityContextHolder.getContext().authentication?.principal as? String

        if (requestingUserId != null) {
            loggingService.log(
                userId = ObjectId(requestingUserId),
                logType = LogType.EXCEPTION_THROWN,
                message = e.message,
            )
        }
    }

}