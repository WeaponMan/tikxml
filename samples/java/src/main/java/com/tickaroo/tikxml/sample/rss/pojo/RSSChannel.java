package com.tickaroo.tikxml.sample.rss.pojo;

import com.tickaroo.tikxml.annotation.Element;
import com.tickaroo.tikxml.annotation.PropertyElement;
import com.tickaroo.tikxml.annotation.Xml;

import java.util.List;

/**
 * Created by WeaponMan on 6/19/2017.
 */

@Xml(name = "channel")
public class RSSChannel {

    @PropertyElement(name = "title")
    public String title;

    @PropertyElement(name = "link")
    public String link;

    @PropertyElement(name = "description")
    public String description;

    @PropertyElement(name = "language")
    public String language;

    @PropertyElement(name = "pubDate")
    public String pubDate;

    @Element(name = "image")
    public RSSImage image;

    @Element(name = "item")
    public List<RSSItem> items;
}
