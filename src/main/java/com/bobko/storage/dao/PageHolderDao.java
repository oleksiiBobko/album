package com.bobko.storage.dao;

/**
 * Contains list with <tt>AlbumPage</tt>. Provide navigation and pagination.
 * 
 * @author oleksii bobko
 * @data 12.08.2013
 * @see AlbumPagesHolderDAO
 * @see AlbumPage
 */

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Projections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.bobko.storage.common.StorageConst;
import com.bobko.storage.dao.interfaces.IPagesHolderDao;
import com.bobko.storage.domain.StoragePage;
import com.bobko.storage.domain.Document;

@Repository
public class PageHolderDao implements IPagesHolderDao {

    private int shift = 0;
    private int pagesCount = 0;
    private int rowCount = 1;

    @Autowired
    private SessionFactory sessionFactory;

    @Override
    public List<StoragePage> list() {
        return createPagesList();
    }

    /**
     * @return List of <tt>AlbumPage</tt>
     * */
    private List<StoragePage> createPagesList() {
        Session session = sessionFactory.getCurrentSession();
        Number num = ((Number) session.createCriteria(Document.class).setProjection(Projections.rowCount()).uniqueResult());
        if(num != null) {
            rowCount = num.intValue();
        }
        
        pagesCount = ((int) Math.ceil(rowCount / (double) StorageConst.PICTURE_COUNT)) - 1;
        
        int finalCount = pagesCount;
        
        List<StoragePage> pages = new ArrayList<StoragePage>();

        if (pagesCount > StorageConst.MAX_PAGES_COUNT) {
            finalCount = StorageConst.MAX_PAGES_COUNT;
        }

        for (int i = 0; i <= finalCount; i++) {
            if ((shift + i) > pagesCount) {
                break;
            }
            if (i == 0) {
                pages.add(new StoragePage(shift + i, true));
            } else {
                pages.add(new StoragePage(shift + i, false));
            }
        }
        return pages;
    }

    @Override
    public int getShift() {
        return shift;
    }

    @Override
    public void setShift(int index) {
        shift = index;
    }

    @Override
    public void nextPage() {
        if (shift < pagesCount) {
            shift++;
        } else {
            shift = pagesCount;
        }
    }

    @Override
    public void prevPage() {
        shift--;
        if (shift < 0) {
            shift = 0;
        }
    }

}
