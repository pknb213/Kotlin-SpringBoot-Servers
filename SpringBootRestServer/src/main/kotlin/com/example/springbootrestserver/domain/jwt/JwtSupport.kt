package com.example.springbootrestserver.domain.jwt

//import io.jsonwebtoken.Jwt

//class BearerToken(val value: String) : AbstractAuthenticationToken(AuthorityUtils.NO_AUTHORITIES) {
//    override fun getCredentials() = value
//
//    override fun getPrincipal() = value
//}
//
//@Component
//class JwtSupport(
//    @Value("\${jwt.key}")
//    private val key: ByteArray
//) {
//
//    private val jwtKey = Keys.hmacShaKeyFor(key)
//    private val parser = Jwts.parserBuilder().setSigningKey(jwtKey).build()
//
//    fun generate(memberEmail: String): BearerToken {
//        val builder = Jwts.builder()
//            .setSubject(memberEmail)
//            .setIssuedAt(Date.from(Instant.now()))
//            .setExpiration(Date.from(Instant.now().plus(15, ChronoUnit.MINUTES)))
//            .signWith(jwtKey)
//        return BearerToken(builder.compact())
//    }
//
//    fun getMemberEmail(token: BearerToken): String {
//        return parser.parseClaimsJws(token.value).body.subject
//    }
//
//    /**
//     * userDetails is nullable
//     */
//    fun isValid(token: BearerToken, userDetails: UserDetails?): Boolean {
//        val claims = parser.parseClaimsJws(token.value).body
//        val unexpired = claims.expiration.after(Date.from(Instant.now()))
//        return unexpired && (claims.subject == userDetails?.username)
//    }
//}