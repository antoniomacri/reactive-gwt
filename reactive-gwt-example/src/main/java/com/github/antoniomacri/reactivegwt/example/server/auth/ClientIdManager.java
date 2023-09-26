package com.github.antoniomacri.reactivegwt.example.server.auth;

public interface ClientIdManager {
	String[] getAllClients();

	String getServerAudience();
}
