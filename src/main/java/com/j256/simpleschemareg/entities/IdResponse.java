package com.j256.simpleschemareg.entities;

/**
 * Response with just an id.
 */
public class IdResponse {

	private final long id;

	public IdResponse(long id) {
		this.id = id;
	}

	public long getId() {
		return id;
	}
}
