package com.tickaroo.tikxml.sample.rss.pojo;

import com.tickaroo.tikxml.annotation.PropertyElement;
import com.tickaroo.tikxml.annotation.Xml;

/**
 * Created by WeaponMan on 6/19/2017.
 */
@Xml(name = "image")
public class RSSImage {

    @PropertyElement(name = "link")
    public String link;

    @PropertyElement(name = "title")
    public String title;

    @PropertyElement(name = "url")
    public String url;

    @PropertyElement(name = "width")
    public int width;

    @PropertyElement(name = "height")
    public int height;

}
