package com.metaweb.gridworks.oauth;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import oauth.signpost.OAuth;
import oauth.signpost.http.HttpParameters;

import com.metaweb.gridworks.util.CookiesUtilities;

public class Credentials {

    private static final String TOKEN = "oauth_token";
    private static final String SECRET = "oauth_token_secret";
    
    public enum Type { 
        REQUEST("request"), 
        ACCESS("access");
        
        private final String postfix;
        
        Type(String postfix) {
            this.postfix = postfix;
        }
        
        public String getCookieName(Provider provider) {
            if (provider == null) throw new RuntimeException("Provider can't be null");
            return provider.getHost() + "_" + postfix;
        }
    };
    
    public static Credentials getCredentials(HttpServletRequest request, Provider provider, Type type) {
        Cookie cookie = CookiesUtilities.getCookie(request, type.getCookieName(provider));
        return (cookie == null) ? null : makeCredentials(cookie.getValue(), provider);
    }

    public static void setCredentials(HttpServletRequest request, HttpServletResponse response, Credentials credentials, Type type, int max_age) {
        String name = type.getCookieName(credentials.getProvider());
        String value = credentials.toString();
        CookiesUtilities.setCookie(request, response, name, value, max_age);
    }
    
    public static void deleteCredentials(HttpServletRequest request, HttpServletResponse response, Provider provider, Type type) {
        CookiesUtilities.deleteCookie(request, response, type.getCookieName(provider));
    }

    public static Credentials makeCredentials(String str, Provider provider) {
        HttpParameters p = OAuth.decodeForm(str);
        return new Credentials(p.getFirst(TOKEN), p.getFirst(SECRET), provider);
    }
    
    private Provider provider;
    private String token;
    private String secret;
    
    public Credentials(String token, String secret, Provider provider) {
        this.token = token;
        if (token == null) throw new RuntimeException("Could not find " + TOKEN + " in auth credentials");
        this.secret = secret;
        if (secret == null) throw new RuntimeException("Could not find " + SECRET + " in auth credentials");
        this.provider = provider;
        if (provider == null) throw new RuntimeException("Provider can't be null");
    }

    public String getToken() {
        return token;
    }

    public String getSecret() {
        return secret;
    }
    
    public Provider getProvider() {
        return provider;
    }
    
    public String toString() {
        return TOKEN + "=" + OAuth.percentEncode(token) + "&" + SECRET + "=" + OAuth.percentEncode(secret);
    }
    
}
