package com.bobko.storage.domain;

/**
 * Class that provides all properties to each page of PhotoAlbum   
 * 
 * @author oleksii bobko
 * @data 12.08.2013
 */

public class StoragePage {
    
    /**
     * index is the position number of page
     * */
    private int index;
    
    /**
     * isActive = true when page is active. and vice versa   
     * */
    private boolean isActive;
    
    public StoragePage(int index, boolean isActive) {
        this.index = index;
        this.isActive = isActive;
    }
    
    public int getIndex() {
        return index;
    }
    public void setIndex(int index) {
        this.index = index;
    } 
    public boolean isActive() {
        return isActive;
    }
    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }
}
