package com.j256.simpleschemareg.entities;

import com.google.gson.annotations.SerializedName;

/**
 * Response sent with http error-code and message..
 */
public class ErrorResponse {

	@SerializedName("error_code")
	private final int errorCode;
	private final String message;

	public ErrorResponse(int errorCode, String message) {
		this.errorCode = errorCode;
		this.message = message;
	}

	public int getErrorCode() {
		return errorCode;
	}

	public String getMessage() {
		return message;
	}

	@Override
	public String toString() {
		return "code " + errorCode + ": " + message;
	}
}
