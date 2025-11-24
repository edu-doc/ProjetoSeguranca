package org.example.Auxiliar.Cripto;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ImplJwt {

    // Chave secreta (deve ser forte e mantida em segredo)
    private static final String JWT_SECRET = "super_chave_secreta_para_assinatura_de_token_do_projeto_de_seguranca";
    private static final Key KEY = Keys.hmacShaKeyFor(JWT_SECRET.getBytes());

    // Tempo de validade do token (Ex: 30 minutos em milissegundos)
    private static final long EXPIRATION_TIME_MS = 30 * 60 * 1000;

    private ImplJwt() {}

    /**
     * Gera um JWT para um dado usuário.
     * @param username O nome de usuário.
     * @param role O perfil/permissão do usuário.
     * @return O token JWT assinado.
     */
    public static String gerarToken(String username, String role) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + EXPIRATION_TIME_MS);

        Map<String, Object> claims = new HashMap<>();
        claims.put("perfil", role);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(KEY, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Valida um token JWT e retorna suas reivindicações (claims).
     * @param token O token JWT.
     * @return As Claims (payload) se o token for válido e não expirado.
     * @throws JwtException Se o token for inválido ou a assinatura falhar.
     */
    public static Claims validarToken(String token) throws JwtException {
        return Jwts.parser()
                .setSigningKey(KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}