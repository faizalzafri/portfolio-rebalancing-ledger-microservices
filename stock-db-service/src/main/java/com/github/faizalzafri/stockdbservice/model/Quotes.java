package com.github.faizalzafri.stockdbservice.model;

import java.util.List;

public class Quotes {

	private String userName;
	private List<String> quotes;

	public Quotes() {
	}

	public Quotes(String username, List<String> quotes) {
		this.userName = username;
		this.quotes = quotes;
	}

	public String getUsername() {
		return userName;
	}

	public void setUsername(String username) {
		this.userName = username;
	}

	public List<String> getQuotes() {
		return quotes;
	}

	public void setQuotes(List<String> quotes) {
		this.quotes = quotes;
	}
}