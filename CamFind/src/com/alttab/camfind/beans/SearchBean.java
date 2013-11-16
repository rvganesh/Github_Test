package com.alttab.camfind.beans;

public class SearchBean 
{
	int id;
	String searchDate,searchText,searchImagePath,searchImageThumbnailPath,searchAudioPath,searchLanguage;
	boolean isSwiped;
	public SearchBean()
	{
		
	}
	public SearchBean(String searchDate,String searchText,String imagePath,String imageThumbnailPath)
	{
		this.searchDate = searchDate;
		this.searchText = searchText;
		this.searchImagePath = imagePath;
		this.searchImageThumbnailPath = imageThumbnailPath;
	}
	public SearchBean(int id, String searchDate, String searchText,
			String searchImagePath,String imageThumbnailPath, String searcAudioPath, String searchLanguage) {
		super();
		this.id = id;
		this.searchDate = searchDate;
		this.searchText = searchText;
		this.searchImagePath = searchImagePath;
		this.searchImageThumbnailPath = imageThumbnailPath;
		this.searchAudioPath = searcAudioPath;
		this.searchLanguage = searchLanguage;
	}
	
	public SearchBean(int id, String searchDate, String searchText,
			String searchImagePath, String searchImageThumbnailPath,
			String searchAudioPath, String searchLanguage, boolean isSwiped) {
		super();
		this.id = id;
		this.searchDate = searchDate;
		this.searchText = searchText;
		this.searchImagePath = searchImagePath;
		this.searchImageThumbnailPath = searchImageThumbnailPath;
		this.searchAudioPath = searchAudioPath;
		this.searchLanguage = searchLanguage;
		this.isSwiped = isSwiped;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getSearchDate() {
		return searchDate;
	}
	public void setSearchDate(String searchDate) {
		this.searchDate = searchDate;
	}
	public String getSearchText() {
		return searchText;
	}
	public void setSearchText(String searchText) {
		this.searchText = searchText;
	}
	public String getSearchImagePath() {
		return searchImagePath;
	}
	public void setSearchImagePath(String searchImagePath) {
		this.searchImagePath = searchImagePath;
	}
	public String getSearchImageThumbnailPath() {
		return searchImageThumbnailPath;
	}
	public void setSearchImageThumbnailPath(String searchImageThumbnailPath) {
		this.searchImageThumbnailPath = searchImageThumbnailPath;
	}
	public String getSearchAudioPath() {
		return searchAudioPath;
	}
	public void setSearchAudioPath(String searcAudioPath) {
		this.searchAudioPath = searcAudioPath;
	}
	public String getSearchLanguage() {
		return searchLanguage;
	}
	public void setSearchLanguage(String searchLanguage) {
		this.searchLanguage = searchLanguage;
	}
	public boolean getSwiped()
	{
		return isSwiped;
	}
	public void setSwiped(boolean isSwiped)
	{
		this.isSwiped = isSwiped;
	}
	
}
