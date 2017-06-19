package com.tickaroo.tikxml.sample.rss.pojo;

import com.tickaroo.tikxml.annotation.Element;
import com.tickaroo.tikxml.annotation.PropertyElement;
import com.tickaroo.tikxml.annotation.Xml;

/**
 * Created by WeaponMan on 6/19/2017.
 */

@Xml(name = "item")
public class RSSItem {

    @PropertyElement(name = "title")
    public String title;

    @PropertyElement(name = "link")
    public String link;

    @PropertyElement(name = "description")
    public String description;

    @PropertyElement(name = "author")
    public String author;

    @PropertyElement(name = "pubDate")
    public String pubDate;

    @Element(name = "media:thumbnail")
    public MediaThumbnail thumbnail;

    @Element(name = "guid")
    public Guid guid;

}
