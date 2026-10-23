package edu.net.environment.model;

import java.io.Serializable;

public record SessionUser(String id, String name, String role) implements Serializable {}
