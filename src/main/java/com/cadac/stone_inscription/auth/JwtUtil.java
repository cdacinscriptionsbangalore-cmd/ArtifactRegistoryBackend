package com.cadac.stone_inscription.auth;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.text.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.security.SecureRandom;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;

@Service
public class JwtUtil {

	private String jwtKey;

	@Autowired
	private StoneInscriptionUserDetailservice  stoneInscriptionUserDetailservice;

	JwtUtil() {
		SecureRandom secureRandom = new SecureRandom();
		byte[] keyBytes = new byte[32]; // 32 bytes for a 256-bit key
		secureRandom.nextBytes(keyBytes);
		this.jwtKey = Base64.getEncoder().encodeToString(keyBytes);
	}

	public String generateToken(UserDetails userDetails, String role) throws JOSEException {
		Map<String, Object> claims = new HashMap<>();

		System.out.println("token genration is called ");


		claims.put("user", userDetails.getUsername());

		// Collection<? extends GrantedAuthority> roles = userDetails.getAuthorities();
		// List<String> roleNames =
		// roles.stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList());

		// Map<String, Map<String, List<String>>> rolesFunction = new HashMap<>();

		// roleNames.stream().forEach((role -> {
		// Map<String, List<String>> functionality = new HashMap<>();

		// List<TRoleAuthorize> temp =
		// tRoleAuthorizeRepo.findAllByRoleId(tRoleRepo.findByRoleName(role).getId());

		// temp.stream().forEach(func -> {

		// if (!functionality.containsKey(func.getMain())) {
		// functionality.put(func.getMain(), Stream.of(func.getSub())
		// .collect(Collectors.toCollection(LinkedList::new)));
		// } else {
		// functionality.get(func.getMain()).add(func.getSub());
		// }
		// rolesFunction.put(role, functionality);
		// });

		// }));
		// claims.put("rolesFunction", rolesFunction);
		claims.put("role", role); // Add role names to claims

		return doGenerateToken(claims);
	}

	public String doGenerateToken(Map<String, Object> claims) throws JOSEException {
		// long expirationTimeInMillis = 20 * 60 * 1000; // 20 minutes in milliseconds
		long expirationTimeInMillis =  3L * 60 * 1000;// 3 min

		// long expirationTimeInMillis = 60000;
		Date issuedAt = new Date(System.currentTimeMillis());
		Date expirationDate = new Date(issuedAt.getTime() + expirationTimeInMillis);

		JWSSigner signer = new MACSigner(jwtKey);

		JWTClaimsSet.Builder claimsSetBuilder = new JWTClaimsSet.Builder();

		// Set the claims
		for (Map.Entry<String, Object> entry : claims.entrySet()) {
			Object value = entry.getValue();
			if (value instanceof Object[]) {
				// Handle array values
				Object[] array = (Object[]) value;
				claimsSetBuilder.claim(entry.getKey(), Arrays.asList(array)); // Convert array to list
			} else {
				claimsSetBuilder.claim(entry.getKey(), value);
			}
		}

		// Build the JWT claims set
		JWTClaimsSet claimsSet = claimsSetBuilder.issueTime(issuedAt).expirationTime(expirationDate).build();

		SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet);

		signedJWT.sign(signer);

		return signedJWT.serialize();
	}

	public boolean validateToken(String authToken) {
    try {
        if (authToken == null || authToken.isEmpty()) {
            return false;
        }

        SignedJWT signedJWT = SignedJWT.parse(authToken);
        JWSVerifier verifier = new MACVerifier(jwtKey);

        // Verify signature
        if (!signedJWT.verify(verifier)) {
            return false;
        }

        // Check expiration
        Date expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime();

        if (expirationTime == null) {
            return false;
        }

        // Token must NOT be expired
        return new Date().before(expirationTime);

    } catch (Exception e) {
        // Any parsing / verification error = invalid token
        return false;
    }
}

	// public boolean validateToken(String authToken) {
	// 	try {
	// 		System.out.println("validation");
	// 		// Jwt token has not been tampered with
	// 		SignedJWT signedJWT = SignedJWT.parse(authToken);
	// 		JWSVerifier verifier = new MACVerifier(jwtKey);

	// 		if (signedJWT.verify(verifier)) {
	// 			// Check if the token has expired
	// 			Date expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime();
	// 			if (expirationTime == null || new Date().after(expirationTime)) {
	// 				throw new BadCredentialsException("JWT token has expired.");
	// 			}
	// 			return true;
	// 		} else {
	// 			throw new BadCredentialsException(
	// 					"JWT signature does not match locally computed signature. JWT validity cannot be asserted and should not be trusted.");
	// 		}
	// 	} catch (JOSEException ex) {
	// 		throw new BadCredentialsException("1", ex);
	// 	} catch (ParseException ex) {
	// 		throw new BadCredentialsException("2", ex);
	// 	}
	// }

	public String getUsernameFromToken(String token) {
		try {
			// Parse the JWT string
			SignedJWT signedJWT = SignedJWT.parse(token);

			// Retrieve the JWT claims...
			JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();

			// Get the username from the "user" claim
			return (String) claimsSet.getClaim("user");
		} catch (ParseException ex) {
			throw new RuntimeException("Could not parse JWT token", ex);
		}
	}

	public List<SimpleGrantedAuthority> getRolesFromToken(String authToken) {
		System.out.println("inside roles");
		try {
			// Parse the JWT string
			SignedJWT signedJWT = SignedJWT.parse(authToken);

			// Retrieve the JWT claims...
			JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();

			ArrayList<SimpleGrantedAuthority> roles = new ArrayList<>();

			@SuppressWarnings("unchecked")
			String role = (String) claimsSet.getClaim("role");
			roles.add(new SimpleGrantedAuthority(role));
			// rolesList.forEach(role -> roles.add(new SimpleGrantedAuthority(role)));

			return roles;

		} catch (ParseException ex) {
			throw new RuntimeException("Could not parse JWT token", ex);
		}
	}

	@SuppressWarnings("unchecked")
	public String refreshToken(String token) throws JOSEException, ParseException {
		SignedJWT signedJWT = SignedJWT.parse(token);
		JWSVerifier verifier = new MACVerifier(jwtKey);

		if (!signedJWT.verify(verifier)) {
			System.out.println("Token verification failed!");
			throw new BadCredentialsException("Invalid token.");
		}

		Date expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime();
		if (expirationTime == null || new Date().after(expirationTime)) {
			System.out.println("Token has expired!");
			throw new BadCredentialsException("Token has expired.");
		}

		// Extract username and role
		String username = getUsernameFromToken(token);
		String role = (String) signedJWT.getJWTClaimsSet().getClaim("role");

		System.out.println("Refreshing token for user: " + username);
		// Create a dummy UserDetails object (Spring Security requires it)
		// UserDetails userDetails = new User(username, "", Collections.emptyList());

		UserDetails userDetails = stoneInscriptionUserDetailservice.loadUserByUsername(username);
		// Generate new token with updated expiration
		return generateToken(userDetails, role);
	}
}
