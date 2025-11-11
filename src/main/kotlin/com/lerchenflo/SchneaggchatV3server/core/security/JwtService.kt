package com.lerchenflo.schneaggchatv3server.core.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Date
import kotlin.time.measureTime

@Service
class JwtService(
    //Inject jwt secret from .env -> docker-compose.yaml -> application.properties -> here
    @Value($$"${jwt.secret}") private val jwtSecret: String
) {

    private val secretKey = Keys.hmacShaKeyFor(jwtSecret.toByteArray())
    private val accessTokenValidityMs = 15L /*min*/ * 60L * 1000L    //How a user can use his access token
    val refreshTokenValidityMs = 30L /*days*/ * 24L * 60L * 60L * 1000L

    private fun generateToken(
        userId: String,
        type: String,
        expiry: Long
    ): String {

        val now = Date()
        val expiryDate = Date(now.time + expiry)

        return Jwts.builder()
            .subject(userId)
            .claim("type", type)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(secretKey, Jwts.SIG.HS256)
            .compact()
    }

    fun generateAccessToken(userId: String): String {
        return generateToken(
            userId = userId,
            type = "access_token",
            expiry = accessTokenValidityMs
        )
    }

    fun generateRefreshToken(userId: String): String {
        return generateToken(
            userId = userId,
            type = "refresh_token",
            expiry = refreshTokenValidityMs
        )
    }


    fun validateAccessToken(accessToken: String): Boolean {
        val claims = parseAllClaims(accessToken) ?: return false
        val tokentype = claims["type"] as? String ?: return false
        return tokentype == "access_token"
    }

    fun validateRefreshToken(refreshToken: String): Boolean {
        val claims = parseAllClaims(refreshToken) ?: return false
        val tokentype = claims["type"] as? String ?: return false
        return tokentype == "refresh_token"
    }

    // Authorization: Bearer <Token>
    fun getUserIdFromToken(token: String): String {
        val claims = parseAllClaims(token) ?: throw IllegalArgumentException("Invalid token")
        return claims.subject
    }



    private fun parseAllClaims(token: String): Claims? {
        val rawToken = token.replace("Bearer ", "")

        return try {
            Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(rawToken)
                .payload
        } catch (e: Exception){
            null
        }
    }

}