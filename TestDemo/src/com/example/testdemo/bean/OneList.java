package com.example.testdemo.bean;

public class OneList {
	private String title;
	private String bookUrl;
	private String headImageUrl;
	private String startTime;
	private String days;
	private String likeCount;
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getBookUrl() {
		return bookUrl;
	}
	public void setBookUrl(String bookUrl) {
		this.bookUrl = bookUrl;
	}
	public String getHeadImageUrl() {
		return headImageUrl;
	}
	public void setHeadImageUrl(String headImageUrl) {
		this.headImageUrl = headImageUrl;
	}
	public String getStartTime() {
		return startTime;
	}
	public void setStartTime(String startTime) {
		this.startTime = startTime;
	}
	public String getDays() {
		return days;
	}
	public void setDays(String days) {
		this.days = days;
	}
	public String getLikeCount() {
		return likeCount;
	}
	public void setLikeCount(String likeCount) {
		this.likeCount = likeCount;
	}
	@Override
	public String toString() {
		return "OneList [title=" + title + ", bookUrl=" + bookUrl + ", headImageUrl=" + headImageUrl + ", startTime="
				+ startTime + ", days=" + days + ", likeCount=" + likeCount + "]";
	}
	
}
